package net.consensys.athena.api.cmd;

import static org.junit.Assert.*;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.impl.enclave.sodium.storage.StoredPrivateKey;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Serializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Optional;

import org.junit.Test;

public class AthenaTest {
  Athena athena = new Athena();

  //  @Test
  //  public void testServerStartWithFullConfig() throws Exception {
  //    TomlConfigBuilder configBuilder = new TomlConfigBuilder();
  //    Config config =
  //        configBuilder.build(
  //            this.getClass().getClassLoader().getResourceAsStream("fullConfigTest.toml"));
  //    NettyServer server = athena.startServer(config);
  //
  //    HttpServerSettings settings = server.getSettings();
  //
  //    assertEquals(Optional.of(9001), settings.getHttpPort());
  //
  //    File expectedSocket = new File("athena.ipc");
  //    assertEquals(Optional.of(expectedSocket), settings.getDomainSocketPath());
  //
  //    server.stop();
  //  }

  @Test
  public void testLoadConfigForTheStandardConfig() throws Exception {
    Config config = athena.loadConfig(Optional.of("src/main/resources/sample.conf"));
    assertEquals(9001, config.port());

    File expectedSocket = new File("athena.ipc");
    assertTrue(config.socket().isPresent());
    assertEquals(expectedSocket, config.socket().get());
  }

  @Test
  public void testDefaultConfigIsUsedWhenNoneProvided() throws Exception {
    Config config = athena.loadConfig(Optional.empty());

    assertEquals(8080, config.port());
    assertFalse(config.socket().isPresent());
  }

  @Test
  public void testGenerateKeysArgumentWithNoKeyNamesProvided() throws Exception {
    String[] args1 = {"--generatekeys"};
    athena.run(args1);
  }

  @Test
  public void testGenerateKeysArgumentProvided() throws Exception {
    //Test "--generatekeys" option
    String[] args1 = {"--generatekeys", "testkey1"};
    String input = "\n";
    InputStream in = new ByteArrayInputStream(input.getBytes());
    System.setIn(in);
    athena.run(args1);

    File privateKey1 = new File("testkey1.key");
    File publicKey1 = new File("testkey1.pub");
    assertTrue(privateKey1.exists());
    assertTrue(publicKey1.exists());
    if (privateKey1.exists()) {
      privateKey1.delete();
    }
    if (publicKey1.exists()) {
      publicKey1.delete();
    }

    //Test "-g" option and multiple key files
    args1 = new String[] {"-g", "testkey2,testkey3"};

    String input2 = "\n\n";
    InputStream in2 = new ByteArrayInputStream(input2.getBytes());
    System.setIn(in2);

    athena.run(args1);

    File privateKey2 = new File("testkey2.key");
    File publicKey2 = new File("testkey2.pub");
    File privateKey3 = new File("testkey3.key");
    File publicKey3 = new File("testkey3.pub");

    assertTrue(privateKey2.exists());
    assertTrue(publicKey2.exists());

    assertTrue(privateKey3.exists());
    assertTrue(publicKey3.exists());

    if (privateKey2.exists()) {
      privateKey2.delete();
    }
    if (publicKey2.exists()) {
      publicKey2.delete();
    }
    if (privateKey3.exists()) {
      privateKey3.delete();
    }
    if (publicKey3.exists()) {
      publicKey3.delete();
    }
  }

  @Test
  public void testGenerateUnlockedKey() throws Exception {

    String[] args1 = {"--generatekeys", "testkey1"};
    String input = "\n";
    InputStream in = new ByteArrayInputStream(input.getBytes());
    System.setIn(in);
    athena.run(args1);

    File privateKey1 = new File("testkey1.key");
    File publicKey1 = new File("testkey1.pub");

    if (privateKey1.exists()) {
      Serializer serializer = new Serializer();
      StoredPrivateKey storedPrivateKey =
          serializer.readFile(HttpContentType.JSON, privateKey1, StoredPrivateKey.class);

      assertEquals(StoredPrivateKey.UNLOCKED, storedPrivateKey.getType());

      privateKey1.delete();
    } else {
      fail("Key was not created");
    }

    if (publicKey1.exists()) {
      publicKey1.delete();
    }
  }

  @Test
  public void testGenerateLockedKey() throws Exception {

    String[] args1 = {"--generatekeys", "testkey1"};
    String input = "abc\n";
    InputStream in = new ByteArrayInputStream(input.getBytes());
    System.setIn(in);
    athena.run(args1);

    File privateKey1 = new File("testkey1.key");
    File publicKey1 = new File("testkey1.pub");

    if (privateKey1.exists()) {
      Serializer serializer = new Serializer();
      StoredPrivateKey storedPrivateKey =
          serializer.readFile(HttpContentType.JSON, privateKey1, StoredPrivateKey.class);

      assertEquals(StoredPrivateKey.ARGON2_SBOX, storedPrivateKey.getType());

      privateKey1.delete();
    } else {
      fail("Key was not created");
    }

    if (publicKey1.exists()) {
      publicKey1.delete();
    }
  }
}
