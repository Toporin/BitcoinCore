/**
 * Copyright 2013-2014 Ronald W Hoffman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.BitcoinCore;

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

/**
  * <p>TransactionOutput</p>
 * <pre>
 *   Size           Field               Description
 *   ====           =====               ===========
 *   8 bytes        TxOutValue          Value expressed in Satoshis (0.00000001 BTC)
 *   VarInt         TxOutScriptLength   Script length
 *   Variable       TxOutScript         Script
 * </pre>
 */
public class TransactionOutput {

    /** Output value in Satoshis (0.00000001 BTC) */
    private BigInteger value;

    /** Transaction output index */
    private final int txIndex;

    /** Output script */
    private byte[] scriptBytes;

    /**
     * Creates a transaction output for the specified amount using a
     * PAY_TO_PUBKEY_HASH script
     *
     * @param       txIndex                 Transaction output index
     * @param       value                   Transaction output value
     * @param       address                 Send address
     */
    public TransactionOutput(int txIndex, BigInteger value, Address address) {
        this.txIndex = txIndex;
        this.value = value;
        //
        // Create the output script for PAY_TO_PUBKEY_HASH
        //   OP_DUP OP_HASH160 <pubkey-hash> OP_EQUALVERIFY OP_CHECKSIG
        //
        scriptBytes = new byte[1+1+1+20+1+1];
        scriptBytes[0] = (byte)ScriptOpCodes.OP_DUP;
        scriptBytes[1] = (byte)ScriptOpCodes.OP_HASH160;
        scriptBytes[2] = (byte)20;
        System.arraycopy(address.getHash(), 0, scriptBytes, 3, 20);
        scriptBytes[23] = (byte)ScriptOpCodes.OP_EQUALVERIFY;
        scriptBytes[24] = (byte)ScriptOpCodes.OP_CHECKSIG;
    }

    /**
     * Creates a transaction output for the specified amount using the supplied script
     *
     * @param       txIndex                 Transaction output index
     * @param       value                   Transaction output value
     * @param       scriptBytes             Transaction output script
     */
    public TransactionOutput(int txIndex, BigInteger value, byte[] scriptBytes) {
        this.txIndex = txIndex;
        this.value = value;
        this.scriptBytes = scriptBytes;
    }

    /**
     * Creates a transaction output from the encoded byte stream
     *
     * @param       txIndex                 Index within the transaction output list
     * @param       inStream                Input stream
     * @throws      EOFException            Input stream is too short
     * @throws      IOException             Error reading the input stream
     * @throws      VerificationException   Verification failed
     */
    public TransactionOutput(int txIndex, InputStream inStream)
                                throws EOFException, IOException, VerificationException {
        this.txIndex = txIndex;
        //
        // Get the amount
        //
        byte[] bytes = new byte[8];
        int count = inStream.read(bytes, 0, 8);
        if (count != 8)
            throw new EOFException("End-of-data while building TransactionOutput");
        value = BigInteger.valueOf(Utils.readUint64LE(bytes, 0));
        //
        // Get the script
        //
        int scriptCount = new VarInt(inStream).toInt();
        if (scriptCount < 0)
            throw new VerificationException("Script byte count is not valid");
        scriptBytes = new byte[scriptCount];
        if (scriptCount > 0) {
            count = inStream.read(scriptBytes);
            if (count != scriptCount)
                throw new EOFException("End-of-data while building TransactionOutput");
        }
    }

    /**
     * Serialize the transaction output
     *
     * @param       outStream           Output stream
     * @throws      IOException         Unable to create the serialized data
     */
    public final void bitcoinSerialize(OutputStream outStream) throws IOException {
        //
        // Encode the value
        //
        Utils.uint64ToByteStreamLE(value.longValue(), outStream);
        //
        // Encode the script bytes
        //
        outStream.write(VarInt.encode(scriptBytes.length));
        if (scriptBytes.length > 0)
            outStream.write(scriptBytes);
    }

    /**
     * Serialize the transaction output
     *
     * @return                          Serialized transaction output
     */
    public byte[] bitcoinSerialize() {
        byte[] varLength = VarInt.encode(scriptBytes.length);
        byte[] bytes = new byte[8+varLength.length+scriptBytes.length];
        Utils.uint64ToByteArrayLE(value.longValue(), bytes, 0);
        System.arraycopy(varLength, 0, bytes, 8, varLength.length);
        if (scriptBytes.length > 0)
            System.arraycopy(scriptBytes, 0, bytes, 8+varLength.length, scriptBytes.length);
        return bytes;
    }

    /**
     * Returns the output amount
     *
     * @return      Output amount
     */
    public BigInteger getValue() {
        return value;
    }

    /**
     * Returns the transaction index for this output
     *
     * @return      Transaction index
     */
    public int getIndex() {
        return txIndex;
    }

    /**
     * Returns the script bytes
     *
     * @return      Script bytes or null
     */
    public byte[] getScriptBytes() {
        return scriptBytes;
    }

    /**
     * Serializes this output for use in a transaction signature
     *
     * @param       index           Index of input being signed
     * @param       hashType        The signature hash type
     * @param       outStream       Output stream
     * @throws      IOException     Unable to serialize data
     */
    public void serializeForSignature(int index, int hashType, OutputStream outStream) throws IOException {
        if (hashType == ScriptOpCodes.SIGHASH_SINGLE && index != txIndex) {
            //
            // For SIGHASH_SINGLE, we have a zero-length script and a value of -1
            //
            Utils.uint64ToByteStreamLE(-1L, outStream);
            outStream.write(0);
        } else {
            //
            // Encode normally
            //
            Utils.uint64ToByteStreamLE(value, outStream);
            if (scriptBytes.length > 0) {
                outStream.write(VarInt.encode(scriptBytes.length));
                outStream.write(scriptBytes);
            } else {
                outStream.write(0);
            }
        }
    }
}
