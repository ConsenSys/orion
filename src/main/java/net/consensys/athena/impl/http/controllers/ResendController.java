package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.server.Result.notImplemented;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Result;

import io.netty.handler.codec.http.HttpRequest;

/**
 * ask to resend a single transaction or all transactions. Useful in situations where a
 * constellation node has lost it's database and wants to recover lost transactions.
 */
public class ResendController implements Controller {
  private final Enclave enclave;
  private final Storage storage;

  public ResendController(Enclave enclave, Storage storage) {
    this.enclave = enclave;
    this.storage = storage;
  }

  @Override
  public Result handle(HttpRequest request) {
    return notImplemented(ContentType.HASKELL_ENCODED);
  }
}
