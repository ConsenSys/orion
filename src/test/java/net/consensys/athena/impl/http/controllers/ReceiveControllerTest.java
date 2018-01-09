package net.consensys.athena.impl.http.controllers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.athena.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.athena.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.athena.impl.http.controllers.ReceiveController.ReceiveRequest;
import net.consensys.athena.impl.http.controllers.ReceiveController.ReceiveResponse;
import net.consensys.athena.impl.http.data.Base64;
import net.consensys.athena.impl.http.data.ContentType;

import java.security.PublicKey;
import java.util.Optional;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Test;

public class ReceiveControllerTest extends ControllerTest {
  private KeyConfig keyConfig = new KeyConfig("ignore", Optional.empty());;
  private final SodiumMemoryKeyStore memoryKeyStore = new SodiumMemoryKeyStore();

  @Override
  protected Enclave buildEnclave() {
    return new LibSodiumEnclave(config, memoryKeyStore);
  }

  @Test
  public void testPayloadIsRetrieved() throws Exception {
    // ref to storage
    final Storage storage = routes.getStorage();

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt a payload
    SodiumPublicKey senderKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    SodiumPublicKey recipientKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    EncryptedPayload originalPayload =
        enclave.encrypt(toEncrypt, senderKey, new PublicKey[] {recipientKey});

    // store it
    String key = storage.put(originalPayload);

    // Receive operation, sending a ReceivePayload request
    ReceiveRequest receiveRequest = new ReceiveRequest(key, recipientKey);
    RequestBody body =
        RequestBody.create(
            MediaType.parse(ContentType.JSON.httpHeaderValue),
            serializer.serialize(ContentType.JSON, receiveRequest));

    Request request = new Request.Builder().post(body).url(baseUrl + AthenaRoutes.RECIEVE).build();

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());

    ReceiveResponse receiveResponse =
        serializer.deserialize(ContentType.JSON, ReceiveResponse.class, resp.body().bytes());

    byte[] decodedPayload = Base64.decode(receiveResponse.payload);
    assertArrayEquals(toEncrypt, decodedPayload);
  }

  @Test
  public void testResponseWhenKeyNotFound() throws Exception {
    // Receive operation, sending a ReceivePayload request
    ReceiveRequest receiveRequest = new ReceiveRequest("notForMe", null);
    RequestBody body =
        RequestBody.create(
            MediaType.parse(ContentType.JSON.httpHeaderValue),
            serializer.serialize(ContentType.JSON, receiveRequest));

    Request request = new Request.Builder().post(body).url(baseUrl + AthenaRoutes.RECIEVE).build();

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(404, resp.code());
  }
}
