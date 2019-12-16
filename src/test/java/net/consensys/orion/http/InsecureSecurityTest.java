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
package net.consensys.orion.http;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.orion.TestUtils.configureJDKTrustStore;
import static net.consensys.orion.TestUtils.generateAndLoadConfiguration;
import static net.consensys.orion.TestUtils.writeServerCertToConfig;
import static org.apache.tuweni.net.tls.TLS.certificateHexFingerprint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLHandshakeException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.SelfSignedCertificate;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class InsecureSecurityTest {

  private final static Vertx vertx = vertx();
  private static Path knownClientsFile;
  private static String exampleComFingerprint;
  private static HttpClient httpClient;
  private static int nodePort;
  private static Orion orion;

  @BeforeAll
  static void setUp(@TempDirectory final Path tempDir) throws Exception {
    final SelfSignedCertificate serverCertificate = SelfSignedCertificate.create("localhost");
    final Config config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsservertrust='insecure-no-validation'\n");
      writeServerCertToConfig(writer, serverCertificate);
    });

    configureJDKTrustStore(serverCertificate, tempDir);
    knownClientsFile = config.tlsKnownClients();

    final SelfSignedCertificate clientCertificate = SelfSignedCertificate.create("example.com");
    exampleComFingerprint = certificateHexFingerprint(Paths.get(clientCertificate.keyCertOptions().getCertPath()));
    httpClient = vertx
        .createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(clientCertificate.keyCertOptions()));

    nodePort = config.nodePort();
    orion = new Orion(vertx);
    orion.run(System.out, System.err, config);
  }

  @AfterAll
  static void tearDown() {
    orion.stop();
    vertx.close();
  }

  @Test
  void testUpCheckOnNodePort() throws Exception {
    assertTrue(Files.readAllLines(knownClientsFile).isEmpty());
    for (int i = 0; i < 5; i++) {
      final HttpClientRequest req = httpClient.get(nodePort, "localhost", "/upcheck");
      final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
      req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
      final HttpClientResponse resp = result.get();
      assertEquals(200, resp.statusCode());
    }
    final List<String> fingerprints = Files.readAllLines(knownClientsFile);
    assertEquals(1, fingerprints.size());
    assertEquals("example.com " + exampleComFingerprint, fingerprints.get(0));
  }

  @Test
  void testWithoutSSLConfiguration() {
    final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    final HttpClient insecureClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true));
    final HttpClientRequest req = insecureClient.get(nodePort, "localhost", "/upcheck");
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();

    final CompletionException e = assertThrows(CompletionException.class, result::get);
    assertTrue(e.getCause() instanceof SSLHandshakeException);
  }
}
