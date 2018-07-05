/*
 * Copyright 2018 ConsenSys AG.
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

package net.consensys.orion.impl.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.impl.enclave.sodium.LibSodiumSettings;
import net.consensys.orion.impl.enclave.sodium.SodiumCombinedKey;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.http.server.HttpContentType;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import com.muquit.libsodiumjna.SodiumLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class SerializerTest {

  @Test
  void jsonSerialization() {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = Serializer.serialize(HttpContentType.JSON, dummyObjectOriginal);
    DummyObject dummyObject = Serializer.deserialize(HttpContentType.JSON, DummyObject.class, bytes);
    assertEquals(dummyObjectOriginal, dummyObject);
  }

  @Test
  void cborSerialization() {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = Serializer.serialize(HttpContentType.CBOR, dummyObjectOriginal);
    DummyObject dummyObject = Serializer.deserialize(HttpContentType.CBOR, DummyObject.class, bytes);
    assertEquals(dummyObjectOriginal, dummyObject);
  }

  @Test
  void sodiumEncryptedPayloadSerialization(@TempDirectory Path tempDir) {
    SodiumLibrary.setLibraryPath(LibSodiumSettings.defaultLibSodiumPath());
    final KeyStore memoryKeyStore = new SodiumMemoryKeyStore();
    KeyConfig keyConfig = new KeyConfig(tempDir.resolve("ignore"), Optional.empty());

    SodiumCombinedKey[] combinedKeys = new SodiumCombinedKey[0];
    byte[] combinedKeyNonce = {};
    byte[] nonce = {};
    SodiumPublicKey sender = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    SodiumEncryptedPayload original =
        new SodiumEncryptedPayload(sender, nonce, combinedKeyNonce, combinedKeys, toEncrypt);

    SodiumEncryptedPayload processed = Serializer.deserialize(
        HttpContentType.CBOR,
        SodiumEncryptedPayload.class,
        Serializer.serialize(HttpContentType.CBOR, original));

    assertEquals(original, processed);
  }

  static class DummyObject implements Serializable {
    public String name;
    public int age;

    DummyObject() {
      this.name = "john";
      this.age = 42;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DummyObject that = (DummyObject) o;
      return age == that.age && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + age;
      return result;
    }
  }
}
