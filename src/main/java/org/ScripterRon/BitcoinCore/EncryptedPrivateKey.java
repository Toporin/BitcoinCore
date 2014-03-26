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

import java.util.Arrays;

/**
 * EncryptedPrivateKey contains an encrypted private key, the initial vector used
 * to encrypt the key, and the salt used to derive the encryption key.
 */
public class EncryptedPrivateKey {

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
     * @throws      EOFException        Unable to read from the input stream
     */
    public EncryptedPrivateKey(byte[] keyBytes) throws EOFException {
        //
        // Get the enrypted private key bytes
        //
        VarInt varCount = new VarInt(keyBytes, 0);
        int offset = varCount.getEncodedSize();
        int length = varCount.toInt();
        if (offset+length > keyBytes.length)
            throw new EOFException("End-of-data while processing encrypted private key");
        encKeyBytes = Arrays.copyOfRange(keyBytes, offset, offset+length);
        offset += length;
        //
        // Get the initial vector
        //
        varCount = new VarInt(keyBytes, offset);
        offset += varCount.getEncodedSize();
        length = varCount.toInt();
        if (offset+length > keyBytes.length)
            throw new EOFException("End-of-data while processing encrypted private key");
        iv = Arrays.copyOfRange(keyBytes, offset, offset+length);
        offset += length;
        //
        // Get the salt used to derive the encryption key
        //
        varCount = new VarInt(keyBytes, offset);
        offset += varCount.getEncodedSize();
        length = varCount.toInt();
        if (offset+length > keyBytes.length)
            throw new EOFException("End-of-data while processing encrypted private key");
        salt = Arrays.copyOfRange(keyBytes, offset, offset+length);
    }

    /**
     * Get the byte stream for this encrypted private key
     *
     * @return                      Byte array containing the serialized encrypted private key
     */
    public byte[] getBytes() {
        byte[] keyLength = VarInt.encode(encKeyBytes.length);
        byte[] ivLength = VarInt.encode(iv.length);
        byte[] saltLength = VarInt.encode(salt.length);
        byte[] keyBytes = new byte[keyLength.length+encKeyBytes.length+
                                   ivLength.length+iv.length+
                                   saltLength.length+salt.length];
        //
        // Process the encrypted private key
        //
        System.arraycopy(keyLength, 0, keyBytes, 0, keyLength.length);
        int offset = keyLength.length;
        System.arraycopy(encKeyBytes, 0, keyBytes, offset, encKeyBytes.length);
        offset += encKeyBytes.length;
        //
        // Process the initial vector
        //
        System.arraycopy(ivLength, 0, keyBytes, offset, ivLength.length);
        offset += ivLength.length;
        System.arraycopy(iv, 0, keyBytes, offset, iv.length);
        offset += iv.length;
        //
        // Process the salt
        //
        System.arraycopy(saltLength, 0, keyBytes, offset, saltLength.length);
        offset += saltLength.length;
        System.arraycopy(salt, 0, keyBytes, offset, salt.length);
        return keyBytes;
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
