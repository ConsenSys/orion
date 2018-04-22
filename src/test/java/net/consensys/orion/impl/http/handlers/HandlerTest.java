package net.consensys.orion.impl.http.handlers;

import static java.nio.file.Files.createTempDirectory;
import static org.junit.Assert.assertEquals;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.api.storage.StorageKeyBuilder;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.enclave.sodium.LibSodiumSettings;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.helpers.StubEnclave;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;
import net.consensys.orion.impl.storage.EncryptedPayloadStorage;
import net.consensys.orion.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.orion.impl.storage.file.MapDbStorage;
import net.consensys.orion.impl.utils.Serializer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.After;
import org.junit.Before;

public abstract class HandlerTest {

  private Path tempDir;

  // http client
  OkHttpClient httpClient = new OkHttpClient();
  String nodeBaseUrl;
  String clientBaseUrl;

  // these are re-built between tests
  ConcurrentNetworkNodes networkNodes;
  protected MemoryConfig config;
  protected Enclave enclave;

  private Vertx vertx;
  private Integer nodeHTTPServerPort;
  private HttpServer nodeHttpServer;
  private Integer clientHTTPServerPort;
  private HttpServer clientHttpServer;

  private StorageEngine<EncryptedPayload> storageEngine;
  protected Storage<EncryptedPayload> storage;

  @Before
  public void setUp() throws Exception {
    tempDir = createTempDirectory(this.getClass().getSimpleName() + "-data");

    // Setup ports for Public and Private API Servers
    setupPorts();

    // Initialize the base HTTP url in two forms: String and OkHttp's HttpUrl object to allow for simpler composition
    // of complex URLs with path parameters, query strings, etc.
    HttpUrl nodeHTTP =
        new Builder().scheme("http").host(InetAddress.getLocalHost().getHostAddress()).port(nodeHTTPServerPort).build();
    nodeBaseUrl = nodeHTTP.toString();

    // orion dependencies, reset them all between tests
    config = new MemoryConfig();
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    config.setWorkDir(tempDir);
    Orion.generateCertificatesAndMissingFiles(config);
    config.setTls("off");
    networkNodes = new ConcurrentNetworkNodes(nodeHTTP.url());
    enclave = buildEnclave();

    Path path = tempDir.resolve("routerdb");
    Files.createDirectories(path);
    storageEngine = new MapDbStorage<>(SodiumEncryptedPayload.class, path);
    // create our vertx object
    vertx = Vertx.vertx();
    StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
    storage = new EncryptedPayloadStorage(storageEngine, keyBuilder);
    Router publicRouter = Router.router(vertx);
    Router privateRouter = Router.router(vertx);
    Orion.configureRoutes(vertx, networkNodes, enclave, storage, publicRouter, privateRouter, config);

    setupNodeServer(publicRouter);
    setupClientServer(privateRouter);
  }

  private void setupNodeServer(Router router) throws InterruptedException, java.util.concurrent.ExecutionException {
    HttpServerOptions publicServerOptions = new HttpServerOptions();
    publicServerOptions.setPort(nodeHTTPServerPort);

    CompletableFuture<Boolean> future = new CompletableFuture<>();
    nodeHttpServer = vertx.createHttpServer(publicServerOptions).requestHandler(router::accept).listen(result -> {
      if (result.succeeded()) {
        future.complete(true);
      } else {
        future.completeExceptionally(result.cause());
      }
    });
    future.get();
  }

  private void setupClientServer(Router router) throws UnknownHostException,
      InterruptedException,
      java.util.concurrent.ExecutionException {
    HttpUrl clientHTTP = new Builder()
        .scheme("http")
        .host(InetAddress.getLocalHost().getHostAddress())
        .port(clientHTTPServerPort)
        .build();
    clientBaseUrl = clientHTTP.toString();

    HttpServerOptions privateServerOptions = new HttpServerOptions();
    privateServerOptions.setPort(clientHTTPServerPort);

    CompletableFuture<Boolean> future = new CompletableFuture<>();
    clientHttpServer = vertx.createHttpServer(privateServerOptions).requestHandler(router::accept).listen(result -> {
      if (result.succeeded()) {
        future.complete(true);
      } else {
        future.completeExceptionally(result.cause());
      }
    });
    future.get();
  }

  private void setupPorts() throws IOException {
    // get a free httpServerPort for Public API
    ServerSocket socket1 = new ServerSocket(0);
    nodeHTTPServerPort = socket1.getLocalPort();

    // get a free httpServerPort for Private API
    ServerSocket socket2 = new ServerSocket(0);
    clientHTTPServerPort = socket2.getLocalPort();

    socket1.close();
    socket2.close();
  }

  @After
  public void tearDown() throws Exception {
    nodeHttpServer.close();
    clientHttpServer.close();
    vertx.close();
    storageEngine.close();
  }

  protected Enclave buildEnclave() {
    return new StubEnclave();
  }

  protected Request buildPrivateAPIRequest(String path, HttpContentType contentType, Object payload) {
    return buildPostRequest(clientBaseUrl, path, contentType, Serializer.serialize(contentType, payload));
  }

  protected Request buildPublicAPIRequest(String path, HttpContentType contentType, Object payload) {
    return buildPostRequest(nodeBaseUrl, path, contentType, Serializer.serialize(contentType, payload));
  }

  private Request buildPostRequest(String baseurl, String path, HttpContentType contentType, byte[] payload) {
    RequestBody body = RequestBody.create(MediaType.parse(contentType.httpHeaderValue), payload);

    if (path.startsWith("/")) {
      path = path.substring(1, path.length());
    }

    return new Request.Builder().post(body).url(baseurl + path).build();
  }

  protected void assertError(final OrionErrorCode expected, final Response actual) throws IOException {
    assertEquals(String.format("{\"error\":\"%s\"}", expected.code()), actual.body().string());
  }
}
