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
package net.consensys.orion.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.orion.enclave.EncryptedKey;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.http.server.HttpContentType;

import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

import org.apache.tuweni.crypto.sodium.Box;
import org.junit.jupiter.api.Test;

class SerializerTest {

  @Test
  void jsonSerialization() {
    final DummyObject dummyObjectOriginal = new DummyObject();
    final byte[] bytes = Serializer.serialize(HttpContentType.JSON, dummyObjectOriginal);
    final DummyObject dummyObject = Serializer.deserialize(HttpContentType.JSON, DummyObject.class, bytes);
    assertEquals(dummyObjectOriginal, dummyObject);
  }

  @Test
  void cborSerialization() {
    final DummyObject dummyObjectOriginal = new DummyObject();
    final byte[] bytes = Serializer.serialize(HttpContentType.CBOR, dummyObjectOriginal);
    final DummyObject dummyObject = Serializer.deserialize(HttpContentType.CBOR, DummyObject.class, bytes);
    assertEquals(dummyObjectOriginal, dummyObject);
  }

  @Test
  void sodiumEncryptedPayloadSerialization() {
    final EncryptedKey[] encryptedKeys = new EncryptedKey[0];
    final byte[] nonce = {};
    final Box.PublicKey sender = Box.KeyPair.random().publicKey();

    // generate random byte content
    final byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    final EncryptedPayload original = new EncryptedPayload(sender, nonce, encryptedKeys, toEncrypt, new byte[0]);

    final EncryptedPayload processed = Serializer.deserialize(
        HttpContentType.CBOR,
        EncryptedPayload.class,
        Serializer.serialize(HttpContentType.CBOR, original));

    assertEquals(original, processed);
  }

  static class DummyObject implements Serializable {
    public final String name;
    public final int age;

    DummyObject() {
      this.name = "john";
      this.age = 42;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final DummyObject that = (DummyObject) o;
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
