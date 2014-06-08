/*
 * Copyright 2013-2014 Ronald W Hoffman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.BitcoinCore;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.io.EOFException;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * EncryptedPrivateKey contains an encrypted private key, the initial vector used
 * to encrypt the key, and the salt used to derive the encryption key.
 */
public class EncryptedPrivateKey implements ByteSerializable {

    /** Key length (bytes) */
    private static final int KEY_LENGTH = 32;

    /** AES block size (bytes) */
    private static final int BLOCK_LENGTH = 16;

    /** Strong random number generator */
    private static final SecureRandom secureRandom = new SecureRandom();

    /** Encrypted private key bytes */
    private byte[] encKeyBytes;

    /** Encryption initial vector */
    private byte[] iv;

    /** Salt used to derive the encryption key */
    private byte[] salt;

    /**
     * Create a new EncryptedPrivateKey using the supplied private key and key phrase
     *
     * @param       privKey                 Private key
     * @param       keyPhrase               Phrase used to derive the encryption key
     * @throws      ECException             Unable to complete a cryptographic function
     */
    public EncryptedPrivateKey(BigInteger privKey, String keyPhrase) throws ECException {
        //
        // Derive the AES encryption key
        //
        salt = new byte[KEY_LENGTH];
        secureRandom.nextBytes(salt);
        KeyParameter aesKey = deriveKey(keyPhrase, salt);
        //
        // Encrypt the private key using the generated AES key
        //
        try {
            iv = new byte[BLOCK_LENGTH];
            secureRandom.nextBytes(iv);
            ParametersWithIV keyWithIV = new ParametersWithIV(aesKey, iv);
            CBCBlockCipher blockCipher = new CBCBlockCipher(new AESFastEngine());
            BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(blockCipher);
            cipher.init(true, keyWithIV);
            byte[] privKeyBytes = privKey.toByteArray();
            int encryptedLength = cipher.getOutputSize(privKeyBytes.length);
            encKeyBytes = new byte[encryptedLength];
            int length = cipher.processBytes(privKeyBytes, 0, privKeyBytes.length, encKeyBytes, 0);
            cipher.doFinal(encKeyBytes, length);
        } catch (Exception exc) {
            throw new ECException("Unable to encrypt the private key", exc);
        }
    }

    /**
     * Creates a new EncryptedPrivateKey from the serialized data
     *
     * @param       keyBytes            Serialized key
     * @throws      EOFException        End-of-data while processing serialized data
     */
    public EncryptedPrivateKey(byte[] keyBytes) throws EOFException {
        this(new SerializedBuffer(keyBytes));
    }

    /**
     * Creates a new EncryptedPrivateKey from the serialized data
     *
     * @param       inBuffer            Input buffer
     * @throws      EOFException        End-of-data while processing serialized data
     */
    public EncryptedPrivateKey(SerializedBuffer inBuffer) throws EOFException {
        encKeyBytes = inBuffer.getBytes();
        iv = inBuffer.getBytes();
        salt = inBuffer.getBytes();
    }

    /**
     * Get the byte stream for this encrypted private key
     *
     * @param       outBuffer           Output buffer
     * @return                          Output buffer
     */
    @Override
    public SerializedBuffer getBytes(SerializedBuffer outBuffer) {
        outBuffer.putVarInt(encKeyBytes.length)
                 .putBytes(encKeyBytes)
                 .putVarInt(iv.length)
                 .putBytes(iv)
                 .putVarInt(salt.length)
                 .putBytes(salt);
        return outBuffer;
    }

    /**
     * Get the byte stream for this encrypted private key
     *
     * @return                      Byte array containing the serialized encrypted private key
     */
    @Override
    public byte[] getBytes() {
        return getBytes(new SerializedBuffer()).toByteArray();
    }

    /**
     * Returns the decrypted private key
     *
     * @param       keyPhrase       Key phrase used to derive the encryption key
     * @return                      Private key
     * @throws      ECException     Unable to complete a cryptographic function
     */
    public BigInteger getPrivKey(String keyPhrase) throws ECException {
        KeyParameter aesKey = deriveKey(keyPhrase, salt);
        //
        // Decrypt the private key using the generated AES key
        //
        BigInteger privKey;
        try {
            ParametersWithIV keyWithIV = new ParametersWithIV(aesKey, iv);
            CBCBlockCipher blockCipher = new CBCBlockCipher(new AESFastEngine());
            BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(blockCipher);
            cipher.init(false, keyWithIV);
            int bufferLength = cipher.getOutputSize(encKeyBytes.length);
            byte[] outputBytes = new byte[bufferLength];
            int length1 = cipher.processBytes(encKeyBytes, 0, encKeyBytes.length, outputBytes, 0);
            int length2 = cipher.doFinal(outputBytes, length1);
            int actualLength = length1 + length2;
            byte[] privKeyBytes = new byte[actualLength];
            System.arraycopy(outputBytes, 0, privKeyBytes, 0, actualLength);
            privKey = new BigInteger(privKeyBytes);
        } catch (Exception exc) {
            throw new ECException("Unable to decrypt the private key", exc);
        }
        return privKey;
    }

    /**
     * Derive the AES encryption key from the key phrase and the salt
     *
     * @param       keyPhrase           Key phrase
     * @param       salt                Salt
     * @return                          Key parameter
     * @throws      ECException         Unable to complete cryptographic function
     */
    private KeyParameter deriveKey(String keyPhrase, byte[] salt) throws ECException {
        KeyParameter aesKey;
        try {
            byte[] stringBytes = keyPhrase.getBytes("UTF-8");
            byte[] digest = Utils.singleDigest(stringBytes);
            byte[] doubleDigest = new byte[digest.length+salt.length];
            System.arraycopy(digest, 0, doubleDigest, 0, digest.length);
            System.arraycopy(salt, 0, doubleDigest, digest.length, salt.length);
            byte[] keyBytes = Utils.singleDigest(doubleDigest);
            aesKey = new KeyParameter(keyBytes);
        } catch (Exception exc) {
            throw new ECException("Unable to convert passphrase to a byte array", exc);
        }
        return aesKey;
    }
}
