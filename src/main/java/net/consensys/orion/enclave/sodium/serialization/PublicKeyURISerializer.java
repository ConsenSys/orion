/*
 * Copyright 2020 ConsenSys AG.
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
package net.consensys.orion.enclave.sodium.serialization;

import static org.apache.tuweni.io.Base64.encode;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.tuweni.bytes.Bytes;

public class PublicKeyURISerializer extends JsonSerializer<Iterable<Map.Entry<Bytes, URI>>> {

  @Override
  public void serialize(
      final Iterable<Map.Entry<Bytes, URI>> entries,
      final JsonGenerator gen,
      final SerializerProvider serializers) throws IOException {
    gen.writeStartObject();
    for (Map.Entry<Bytes, URI> entry : entries) {
      gen.writeObjectField(encode(entry.getKey()), entry.getValue().toString());
    }
    gen.writeEndObject();
  }
}
