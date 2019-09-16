/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.orion.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.io.Base64.encodeBytes;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.kv.KeyValueStore;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.utils.Serializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class QueryPrivacyGroupStorage implements Storage<QueryPrivacyGroupPayload> {
  private static final byte[] BYTES = Bytes.fromHexString("5375ba871e5c3d0f1d055b5da0ac02ea035bed38").toArrayUnsafe();

  private final KeyValueStore store;
  private final Enclave enclave;

  public QueryPrivacyGroupStorage(final KeyValueStore store, final Enclave enclave) {
    this.store = store;
    this.enclave = enclave;
  }

  @Override
  public AsyncResult<String> put(final QueryPrivacyGroupPayload data) {
    final String key = generateDigest(data);
    final Bytes keyBytes = Bytes.wrap(key.getBytes(UTF_8));
    final Bytes dataBytes = Bytes.wrap(Serializer.serialize(HttpContentType.CBOR, data));
    return store.putAsync(keyBytes, dataBytes).thenSupply(() -> key);
  }

  @Override
  public String generateDigest(final QueryPrivacyGroupPayload data) {
    final Box.PublicKey[] publicKeys =
        Arrays.stream(data.addresses()).map(enclave::readKey).toArray(Box.PublicKey[]::new);
    return encodeBytes(enclave.generatePrivacyGroupId(publicKeys, BYTES, PrivacyGroupPayload.Type.PANTHEON));
  }

  @Override
  public AsyncResult<Optional<QueryPrivacyGroupPayload>> get(final String key) {
    final Bytes keyBytes = Bytes.wrap(key.getBytes(UTF_8));
    return store.getAsync(keyBytes).thenApply(
        maybeBytes -> Optional.ofNullable(maybeBytes).map(
            bytes -> Serializer
                .deserialize(HttpContentType.CBOR, QueryPrivacyGroupPayload.class, bytes.toArrayUnsafe())));
  }

  @Override
  public AsyncResult<Optional<QueryPrivacyGroupPayload>> update(final String key, final QueryPrivacyGroupPayload data) {
    return get(key).thenApply((result) -> {
      final List<String> listPrivacyGroupIds;
      final QueryPrivacyGroupPayload queryPrivacyGroupPayload;
      if (result.isPresent()) {
        queryPrivacyGroupPayload = handleAlreadyPresentUpdate(data, result.get());
      } else {
        listPrivacyGroupIds = Collections.singletonList(data.privacyGroupToAppend());
        queryPrivacyGroupPayload = new QueryPrivacyGroupPayload(data.addresses(), listPrivacyGroupIds);
      }

      put(queryPrivacyGroupPayload);
      return Optional.of(queryPrivacyGroupPayload);
    });
  }

  private QueryPrivacyGroupPayload handleAlreadyPresentUpdate(
      final QueryPrivacyGroupPayload data,
      final QueryPrivacyGroupPayload result) {
    final List<String> listPrivacyGroupIds;
    final QueryPrivacyGroupPayload queryPrivacyGroupPayload;
    if (data.isToDelete()) {
      result.privacyGroupId().remove(data.privacyGroupToAppend());
    } else if (!result.privacyGroupId().contains(data.privacyGroupToAppend())) {
      result.privacyGroupId().add(data.privacyGroupToAppend());
    }
    listPrivacyGroupIds = result.privacyGroupId();
    queryPrivacyGroupPayload = new QueryPrivacyGroupPayload(result.addresses(), listPrivacyGroupIds);
    return queryPrivacyGroupPayload;
  }
}
