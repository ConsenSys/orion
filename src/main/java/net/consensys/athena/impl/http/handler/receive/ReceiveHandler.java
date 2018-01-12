package net.consensys.athena.impl.http.handler.receive;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Base64;
import net.consensys.athena.impl.utils.Serializer;

import java.util.Optional;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/** Retrieve a base 64 encoded payload. */
public class ReceiveHandler implements Handler<RoutingContext> {
  private final Enclave enclave;
  private final Storage storage;
  private final Serializer serializer;

  public ReceiveHandler(Enclave enclave, Storage storage, Serializer serializer) {
    this.enclave = enclave;
    this.storage = storage;
    this.serializer = serializer;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    ReceiveRequest receiveRequest =
        serializer.deserialize(
            HttpContentType.JSON, ReceiveRequest.class, routingContext.getBody().getBytes());

    Optional<EncryptedPayload> encryptedPayload = storage.get(receiveRequest.key);
    if (!encryptedPayload.isPresent()) {
      routingContext.fail(404);
      return;
    }

    // Haskell doc: let's check if receipients is set = it's a payload that we sent. TODO @gbotrel
    // if not, it's a payload sent to us
    byte[] decryptedPayload = enclave.decrypt(encryptedPayload.get(), receiveRequest.publicKey);

    // build a ReceiveResponse
    Buffer toReturn =
        Buffer.buffer(
            serializer.serialize(
                HttpContentType.JSON, new ReceiveResponse(Base64.encode(decryptedPayload))));

    routingContext.response().end(toReturn);
  }
}
