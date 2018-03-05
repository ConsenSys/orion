package net.consensys.orion.acceptance.send.receive;

import static org.junit.Assert.assertTrue;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.config.TomlConfigBuilder;
import net.consensys.orion.impl.http.OrionClient;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import okhttp3.HttpUrl;

/** Utility functions for performing the send and receive acceptance tests. */
public class SendReceiveUtil {

  private static final String HTTP_PROTOCOL = "http";

  public String url(String host, int port) {
    return new HttpUrl.Builder().scheme(HTTP_PROTOCOL).host(host).port(port).build().toString();
  }

  public Config nodeConfig(
      String baseUrl, int port, String nodeName, String otherNodes, String pubKeys, String privKeys)
      throws UnsupportedEncodingException {

    final String confString =
        new StringBuilder()
            .append("url = \"")
            .append(baseUrl)
            .append("\"\nport = ")
            .append(port)
            .append("\nstorage = \"leveldb:database/" + nodeName + "\"")
            .append("\nothernodes = [\"")
            .append(otherNodes)
            .append(
                "\"]\n"
                    + "publickeys = [\""
                    + pubKeys
                    + "\"]\n"
                    + "privatekeys = [\""
                    + privKeys
                    + "\"]")
            .toString();

    return new TomlConfigBuilder()
        .build(new ByteArrayInputStream(confString.getBytes(StandardCharsets.UTF_8.name())));
  }

  public int freePort() throws Exception {
    final ServerSocket socket = new ServerSocket(0);
    final int toReturn = socket.getLocalPort();
    socket.close();

    return toReturn;
  }

  /** It's the callers responsibility to stop the started Orion. */
  public Orion startOrion(Config config) throws ExecutionException, InterruptedException {
    final Orion orion = new Orion();
    orion.run(config);

    return orion;
  }

  public OrionClient client(String baseUrl) {
    final OrionClient client = new OrionClient(baseUrl);
    assertTrue(client.upCheck());
    return client;
  }
}
