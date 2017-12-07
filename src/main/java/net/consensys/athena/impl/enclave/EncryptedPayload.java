package net.consensys.athena.impl.enclave;

import net.consensys.athena.api.enclave.CombinedKey;

import java.security.PublicKey;
import static java.lang.System.*;

public class EncryptedPayload implements net.consensys.athena.api.enclave.EncryptedPayload {

    private final long nonce;
    //private final PublicKey publicKey;
    private final byte[] cipherText;

    @Override
    public PublicKey getSender() {
        return null;
    }

    @Override
    public byte[] getCipherText() {
        return cipherText;
    }

    @Override
    public long getNonce() {
        return nonce;
    }

    @Override
    public CombinedKey[] getCombinedKeys() {
        //Not sure if this guy should be an array
        return new CombinedKey[0];
    }

    @Override
    public long getCombinedKeyNonce() {
        return 0;
    }

    public EncryptedPayload(byte[] senderKey, byte[] cipherText) {
        //TODO: CONSTRUCT KEY
        //this.publicKey = senderKey;
        this.cipherText = cipherText;

        this.nonce = currentTimeMillis();
    }
}
