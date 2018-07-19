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
package net.consensys.orion.http.handler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.io.Base64.encodeBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.http.handler.send.SendRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SendRequestTest {

  @Test
  void invalidBase64Payload() {
    SendRequest request = new SendRequest("something", "foo", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  void noFromValid() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), null, new String[] {"foo"});
    assertTrue(request.isValid());
  }

  @Test
  void emptyFromInvalid() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  void missingPayload() {
    SendRequest request = new SendRequest((byte[]) null, "foo", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  void emptyPayload() {
    SendRequest request = new SendRequest("".getBytes(UTF_8), "foo", new String[] {"foo"});
    assertFalse(request.isValid());
  }

  @Test
  void emptyToAddresses() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "foo", new String[0]);
    assertFalse(request.isValid());
  }

  @Test
  void nullToAddresses() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "foo", null);
    assertFalse(request.isValid());
  }

  @Test
  void toAddressesContainNull() {
    SendRequest request = new SendRequest("something".getBytes(UTF_8), "foo", new String[] {null, "foo"});
    assertFalse(request.isValid());
  }

  @Test
  void jsonToObject() throws Exception {

    String json = "{\"payload\":\"" + encodeBytes("foo".getBytes(UTF_8)) + "\", \"from\":\"foo\", \"to\":[\"foo\"]}";
    ObjectMapper mapper = new ObjectMapper();
    SendRequest req = mapper.readerFor(SendRequest.class).readValue(json);

    assertEquals("foo", req.from().get());
    assertEquals("foo", req.to()[0]);
  }
}
