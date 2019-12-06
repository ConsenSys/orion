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
package net.consensys.orion.http.handler;

import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.http.server.HttpContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.Box.PublicKey;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;
import net.consensys.orion.helpers.FakePeer;
import net.consensys.orion.http.handler.privacy.DeletePrivacyGroupRequest;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;
import net.consensys.orion.http.handler.privacy.PrivacyGroupRequest;
import net.consensys.orion.http.handler.privacy.RetrievePrivacyGroupRequest;
import net.consensys.orion.utils.Serializer;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Collections;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RetrievePrivacyGroupHandlerTest extends HandlerTest {

  private static final String PRIVACY_GROUP_NAME = "test";
  private static final String PRIVACY_GROUP_DESCRIPTION = "desc";

  private final MemoryKeyStore memoryKeyStore = new MemoryKeyStore();
  private String privacyGroupId;
  private FakePeer peer;
  private Box.PublicKey senderKey;
  private String[] privacyGroupMembers;

  @Override
  protected Enclave buildEnclave(final Path tempDir) {
    memoryKeyStore.generateKeyPair();
    return new SodiumEnclave(memoryKeyStore);
  }

  @BeforeEach
  void setup() throws IOException {
    senderKey = memoryKeyStore.generateKeyPair();
    final Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    privacyGroupMembers = new String[] {encodeBytes(senderKey.bytesArray()), encodeBytes(recipientKey.bytesArray())};
    peer = new FakePeer(recipientKey);

    privacyGroupId = createPrivacyGroupId(recipientKey);
  }

  private String createPrivacyGroupId(final PublicKey recipientKey) throws IOException {
    final PrivacyGroupRequest privacyGroupRequestExpected = buildPrivacyGroupRequest(
        privacyGroupMembers,
        encodeBytes(senderKey.bytesArray()),
        PRIVACY_GROUP_NAME,
        PRIVACY_GROUP_DESCRIPTION);
    final Request request = buildPrivateAPIRequest(CREATE_PRIVACY_GROUP, JSON, privacyGroupRequestExpected);

    final byte[] privacyGroupId = enclave.generatePrivacyGroupId(
        new PublicKey[] {senderKey, recipientKey},
        privacyGroupRequestExpected.getSeed().get(),
        PrivacyGroupPayload.Type.PANTHEON);

    peer.addResponse(new MockResponse().setBody(encodeBytes(privacyGroupId)));
    networkNodes.addNode(Collections.singletonList(peer.publicKey), peer.getURL());

    final Response resp = httpClient.newCall(request).execute();

    final PrivacyGroup privacyGroup = Serializer.deserialize(JSON, PrivacyGroup.class, resp.body().bytes());
    return privacyGroup.getPrivacyGroupId();
  }

  @Test
  void knownPrivacyGroupIsRetrieved() throws IOException {
    final RetrievePrivacyGroupRequest retrievePrivacyGroupRequest = buildGetPrivacyGroupRequest(privacyGroupId);
    final Request request = buildPrivateAPIRequest(RETRIEVE_PRIVACY_GROUP, JSON, retrievePrivacyGroupRequest);
    peer.addResponse(new MockResponse().setBody(privacyGroupId));

    final Response resp = httpClient.newCall(request).execute();

    assertThat(resp.code()).isEqualTo(200);
    final PrivacyGroup privacyGroup = Serializer.deserialize(JSON, PrivacyGroup.class, resp.body().bytes());
    assertThat(privacyGroup.getPrivacyGroupId()).isEqualTo(privacyGroupId);
    assertThat(privacyGroup.getName()).isEqualTo(PRIVACY_GROUP_NAME);
    assertThat(privacyGroup.getDescription()).isEqualTo(PRIVACY_GROUP_DESCRIPTION);
    assertThat(privacyGroup.getMembers()).isEqualTo(privacyGroupMembers);
  }

  @Test
  void unknownPrivacyGroupIdReturnsNotFoundError() throws IOException {
    final RetrievePrivacyGroupRequest retrievePrivacyGroupRequest =
        buildGetPrivacyGroupRequest("unknownPrivacyGroupId");
    final Request request = buildPrivateAPIRequest(RETRIEVE_PRIVACY_GROUP, JSON, retrievePrivacyGroupRequest);

    final Response resp = httpClient.newCall(request).execute();

    assertThat(resp.code()).isEqualTo(404);
  }

  @Test
  void deletedPrivacyGroupReturnsNotFoundError() throws IOException {
    peer.addResponse(new MockResponse().setBody(privacyGroupId));

    final DeletePrivacyGroupRequest deletePrivacyGroupRequest =
        buildDeletePrivacyGroupRequest(privacyGroupId, encodeBytes(senderKey.bytesArray()));
    final Request deleteRequest = buildPrivateAPIRequest(DELETE_PRIVACY_GROUP, JSON, deletePrivacyGroupRequest);
    final Response deleteResponse = httpClient.newCall(deleteRequest).execute();
    assertThat(deleteResponse.code()).isEqualTo(200);

    final RetrievePrivacyGroupRequest retrievePrivacyGroupRequest = buildGetPrivacyGroupRequest(privacyGroupId);
    final Request getGroupRequest = buildPrivateAPIRequest(RETRIEVE_PRIVACY_GROUP, JSON, retrievePrivacyGroupRequest);
    final Response getGroupResponse = httpClient.newCall(getGroupRequest).execute();
    assertThat(getGroupResponse.code()).isEqualTo(404);
  }

  @Test
  void requestWithoutPrivacyGroupIdFails() throws IOException {
    final RetrievePrivacyGroupRequest retrievePrivacyGroupRequest = buildGetPrivacyGroupRequest(null);
    final Request request = buildPrivateAPIRequest(RETRIEVE_PRIVACY_GROUP, JSON, retrievePrivacyGroupRequest);

    final Response resp = httpClient.newCall(request).execute();

    assertThat(resp.code()).isEqualTo(400);
  }

  private PrivacyGroupRequest buildPrivacyGroupRequest(
      final String[] addresses,
      final String from,
      final String name,
      final String description) {
    final PrivacyGroupRequest privacyGroupRequest = new PrivacyGroupRequest(addresses, from, name, description);
    // create a random seed
    final SecureRandom random = new SecureRandom();
    final byte[] bytes = new byte[20];
    random.nextBytes(bytes);
    privacyGroupRequest.setSeed(bytes);

    return privacyGroupRequest;
  }

  private RetrievePrivacyGroupRequest buildGetPrivacyGroupRequest(final String key) {
    return new RetrievePrivacyGroupRequest(key);
  }

  private DeletePrivacyGroupRequest buildDeletePrivacyGroupRequest(final String key, final String from) {
    return new DeletePrivacyGroupRequest(key, from);
  }
}
