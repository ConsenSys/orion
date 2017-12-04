package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.server.Controller;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/** Used by a client to get a payload (without base64 encoding) */
public class ReceiveRawController implements Controller {
  private final Enclave enclave;
  private final Storage storage;

  public ReceiveRawController(Enclave enclave, Storage storage) {
    this.enclave = enclave;
    this.storage = storage;
  }

  @Override
  public FullHttpResponse handle(HttpRequest request, FullHttpResponse response) {
    return response;
  }
}
