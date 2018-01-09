package net.consensys.athena.impl.http.server.vertx;

import net.consensys.athena.impl.http.data.ApiError;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class ApiErrorHandler implements Handler<RoutingContext> {

  public ApiErrorHandler(Serializer serializer) {
    this.serializer = serializer;
  }

  private final Serializer serializer;

  @Override
  public void handle(RoutingContext failureContext) {
    // TODO here we should do some clean error management, error codes, etc.
    // check the exeception type ...

    int statusCode = failureContext.statusCode();
    statusCode = statusCode < 0 ? 500 : statusCode;

    HttpServerResponse response = failureContext.response().setStatusCode(statusCode);

    if (failureContext.failure() != null) {
      ApiError apiError = new ApiError(failureContext.failure().getMessage());
      Buffer buffer = Buffer.buffer(serializer.serialize(ContentType.JSON, apiError));
      response.putHeader(HttpHeaders.CONTENT_TYPE, ContentType.JSON.httpHeaderValue).end(buffer);
    } else {
      response.end();
    }
  }
}
