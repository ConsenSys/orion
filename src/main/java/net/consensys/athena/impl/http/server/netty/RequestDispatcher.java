package net.consensys.athena.impl.http.server.netty;

import static java.util.Optional.empty;
import static net.consensys.athena.impl.http.data.Result.internalServerError;

import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.EmptyPayload;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.RequestImpl;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Router;
import net.consensys.athena.impl.http.server.Serializer;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RequestDispatcher implements BiConsumer<FullHttpRequest, ChannelHandlerContext> {
  private static final Logger log = LogManager.getLogger();
  private final Router router;
  private final Serializer serializer;

  public RequestDispatcher(Router router, Serializer serializer) {
    this.router = router;
    this.serializer = serializer;
  }

  @Override
  public void accept(FullHttpRequest httpRequest, ChannelHandlerContext ctx) {
    Controller controller = router.lookup(httpRequest);
    // log incoming http request
    log.debug(
        "{} processing route: {} {}",
        controller.getClass().getSimpleName(),
        httpRequest.method().name(),
        httpRequest.uri());

    Result result;
    try {
      // deserialize payload
      Request request = buildControllerRequest(httpRequest, controller);
      // process http request
      result = controller.handle(request);
    } catch (RuntimeException e) {
      // if an exception occurred, return a formatted ApiError
      log.error(e.getMessage());
      result = internalServerError(e.getMessage());
    }

    log.debug(
        "{} result: {}", controller.getClass().getSimpleName(), result.getStatus().toString());

    // build httpResponse
    FullHttpResponse response =
        buildHttpResponse(result, httpRequest.headers().get(HttpHeaderNames.ACCEPT_ENCODING));
    ctx.writeAndFlush(response);
    httpRequest.release();
  }

  // read httpRequest and deserialize the payload into controller expected typed object
  private Request buildControllerRequest(FullHttpRequest httpRequest, Controller controller) {
    int requestPayloadSize = httpRequest.content().readableBytes();

    // if the controller doesn't expect a payload
    if (controller.expectedRequest().equals(EmptyPayload.class)) {
      if (requestPayloadSize > 0) {
        throw new IllegalArgumentException("did not expect payload, yet one is provided");
      }
      return new RequestImpl(empty());
    }

    // controller expects a payload
    // let's check if Content-encoding is set
    String contentEncoding = httpRequest.headers().get(HttpHeaderNames.CONTENT_ENCODING);
    if (contentEncoding == null) {
      log.warn("Content-encoding is not set, trying JSON as default fallback.");
      // TODO if we do strict HTTP validation, we should reject the request, not sure that plays well
      // with current Constellation Haskell implementation, thus the JSON fallback ?
      contentEncoding = HttpHeaderValues.APPLICATION_JSON.toString();
    }

    // read httpRequest payload bytes
    byte[] requestPayload;
    if (httpRequest.content().hasArray()) {
      requestPayload = httpRequest.content().array();
    } else {
      requestPayload = new byte[requestPayloadSize];
      httpRequest.content().getBytes(httpRequest.content().readerIndex(), requestPayload);
    }

    // deserialize the bytes into expected type by controller
    try {
      ContentType cType = ContentType.fromHttpContentEncoding(contentEncoding);
      Object payload = serializer.deserialize(requestPayload, cType, controller.expectedRequest());
      return new RequestImpl(Optional.of(payload));
    } catch (Exception e) {
      throw new RuntimeException(contentEncoding + "isn't supported: " + e.getMessage());
    }
  }

  private FullHttpResponse buildHttpResponse(Result result, String acceptEncoding) {
    // if result does have a payload, and specified encoding is in accept-encoding headers or accept-encoding is empty
    // encode as is and return
    // else, use first accept-encoding header to serialize the payload

    // base http response
    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, result.getStatus());
    response.headers().add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

    // add extra headers
    Optional<HttpHeaders> extraHeaders = result.getExtraHeaders();
    if (extraHeaders.isPresent()) {
      HttpHeaders headers = extraHeaders.get();
      response.headers().add(headers);
    }

    // result doesn't have a payload
    if (!result.getPayload().isPresent()) {
      return response;
    }

    // default; use controller specified encoding
    ContentType contentType = result.defaultContentType();

    // accept encoding header is specified and doesn't support controller encoding
    if (acceptEncoding != null && !acceptEncoding.contains(contentType.httpHeaderValue)) {
      log.info("accept encoding header is specified and doesn't match value set by controller");
      // TODO parse acceptEncoding and set contentType variable accordingly
    }

    // add content encoding header to response
    response.headers().add(HttpHeaderNames.CONTENT_ENCODING, contentType.httpHeaderValue);

    // serialize the payload
    try {
      byte[] bytes = serializer.serialize(result.getPayload().get(), contentType);
      return response.replace(Unpooled.copiedBuffer(bytes));
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new RuntimeException(e);
      // TODO shall we do the RuntimeExpection or return a internal server error ?
      // TODO /!\ Controller did his job, we should tell the user /!\
    }
  }
}
