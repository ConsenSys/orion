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

import java.nio.charset.StandardCharsets;

public class Base64 {

  private Base64() {}

  public static String encode(byte[] bytes) {
    return new String(java.util.Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
  }

  public static byte[] decode(String b64) {
    return java.util.Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8));
  }
}
