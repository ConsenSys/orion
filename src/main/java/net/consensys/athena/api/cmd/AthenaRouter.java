package net.consensys.athena.api.cmd;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.api.storage.KeyValueStore;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageIdBuilder;
import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.athena.impl.enclave.sodium.SodiumFileKeyStore;
import net.consensys.athena.impl.http.controllers.DeleteController;
import net.consensys.athena.impl.http.controllers.PartyInfoController;
import net.consensys.athena.impl.http.controllers.PushController;
import net.consensys.athena.impl.http.controllers.ReceiveController;
import net.consensys.athena.impl.http.controllers.ResendController;
import net.consensys.athena.impl.http.controllers.SendController;
import net.consensys.athena.impl.http.controllers.UpcheckController;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Router;
import net.consensys.athena.impl.http.server.Serializer;
import net.consensys.athena.impl.storage.Sha512_256StorageIdBuilder;
import net.consensys.athena.impl.storage.StorageKeyValueStorageDelegate;
import net.consensys.athena.impl.storage.file.MapDbStorage;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AthenaRouter implements Router {

  private static final Logger log = LogManager.getLogger();

  private static final KeyValueStore KEY_VALUE_STORE = new MapDbStorage("routerdb");

  private final Enclave enclave;
  private Serializer serializer;
  private final Storage storage;
  private final NetworkNodes networkNodes;

  public AthenaRouter(
      NetworkNodes info, Config config, Serializer serializer, ObjectMapper jsonObjectMapper) {
    networkNodes = info;
    enclave = new LibSodiumEnclave(config, new SodiumFileKeyStore(config, jsonObjectMapper));
    this.serializer = serializer;

    StorageIdBuilder keyBuilder = new Sha512_256StorageIdBuilder(enclave);
    storage = new StorageKeyValueStorageDelegate(KEY_VALUE_STORE, keyBuilder);
  }

  @Override
  public Controller lookup(HttpRequest request) {
    try {
      URI uri = new URI(request.uri());
      if (uri.getPath().startsWith("/upcheck")) {
        return new UpcheckController();
      }
      if (uri.getPath().startsWith("/sendraw")) {
        return new SendController(enclave, storage, ContentType.RAW);
      }
      if (uri.getPath().startsWith("/receiveraw")) {
        return new ReceiveController(enclave, storage, ContentType.RAW, serializer);
      }
      if (uri.getPath().startsWith("/send")) {
        return new SendController(enclave, storage, ContentType.JSON);
      }
      if (uri.getPath().startsWith("/receive")) {
        return new ReceiveController(enclave, storage, ContentType.JSON, serializer);
      }
      if (uri.getPath().startsWith("/delete")) {
        return new DeleteController(storage);
      }
      if (uri.getPath().startsWith("/resend")) {
        return new ResendController(enclave, storage);
      }
      if (uri.getPath().startsWith("/partyinfo")) {
        return new PartyInfoController(networkNodes);
      }
      if (uri.getPath().startsWith("/push")) {
        return new PushController(storage, serializer);
      }

      throw new RuntimeException("Unsupported uri: " + uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Unable to handle request.", e);
    }
  }
}
