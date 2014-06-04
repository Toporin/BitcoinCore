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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A block is composed of one or more transactions.  The first transaction is called the coinbase transaction
 * and it assigns the block reward to the miner who solved the block hash.  The remaining transactions move coins
 * from Input A to Output B.  A single transaction can contain multiple inputs and multiple outputs.  The sum of
 * the inputs minus the sum of the output represents the mining fee for that transaction.</p>
 *
 * <p>Each transaction input is connected to the output of a proceeding transaction.  The input contains the
 * first half of a script (ScriptSig) and the output contains the second half (ScriptPubKey).  The script
 * is interpreted to determines if the transaction input is allowed to spend the transaction output.
 *
 * <p>Transaction</p>
 * <pre>
 *   Size           Field               Description
 *   ====           =====               ===========
 *   4 bytes        Version             Currently 1
 *   VarInt         InputCount          Number of inputs
 *   Variable       InputList           Inputs
 *   VarInt         OutputCount         Number of outputs
 *   Variable       OutputList          Outputs
 *   4 bytes        LockTime            Transaction lock time
 * </pre>
 */
public class Transaction {

    /** Serialized transaction data */
    private byte[] txData;

    /** Transaction version */
    private long txVersion;

    /** Transaction hash */
    private Sha256Hash txHash;

    /** Normalized transaction ID */
    private Sha256Hash normID;

    /** Transaction lock time */
    private long txLockTime;

    /* This a coinbase transaction */
    private boolean coinBase;

    /** List of transaction inputs */
    private List<TransactionInput> txInputs;

    /** List of transaction outputs */
    private List<TransactionOutput> txOutputs;

    /**
     * Creates a new transaction using the provided inputs
     *
     * @param       inputs                  List of signed inputs
     * @param       outputs                 List of outputs
     * @throws      ECException             Unable to sign transaction
     * @throws      IOException             Unable to serialize transaction
     * @throws      VerificationException   Transaction verification failure
     */
    public Transaction(List<SignedInput> inputs, List<TransactionOutput> outputs)
                                            throws ECException, IOException, VerificationException {
        txVersion = 1;
        txOutputs = outputs;
        //
        // Create the transaction inputs
        //
        txInputs = new ArrayList<>(inputs.size());
        for (int i=0; i<inputs.size(); i++)
            txInputs.add(new TransactionInput(this, i, inputs.get(i).getOutPoint()));
        //
        // Now sign each input and create the input scripts
        //
        for (int i=0; i<inputs.size(); i++) {
            SignedInput input = inputs.get(i);
            ECKey key = input.getKey();
            byte[] contents;
            //
            // Serialize the transaction for signing using the SIGHASH_ALL hash type
            //
            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024)) {
                serializeForSignature(i, ScriptOpCodes.SIGHASH_ALL, input.getScriptBytes(), outStream);
                Utils.uint32ToByteStreamLE(ScriptOpCodes.SIGHASH_ALL, outStream);
                contents = outStream.toByteArray();
            }
            //
            // Create the DER-encoded signature
            //
            ECDSASignature sig = key.createSignature(contents);
            byte[] encodedSig = sig.encodeToDER();
            //
            // Create the input script using the SIGHASH_ALL hash type
            //   <sig> <pubKey>
            //
            byte[] pubKey = key.getPubKey();
            byte[] scriptBytes = new byte[1+encodedSig.length+1+1+pubKey.length];
            scriptBytes[0] = (byte)(encodedSig.length+1);
            System.arraycopy(encodedSig, 0, scriptBytes, 1, encodedSig.length);
            int offset = encodedSig.length+1;
            scriptBytes[offset++] = (byte)ScriptOpCodes.SIGHASH_ALL;
            scriptBytes[offset++] = (byte)pubKey.length;
            System.arraycopy(pubKey, 0, scriptBytes, offset, pubKey.length);
            txInputs.get(i).setScriptBytes(scriptBytes);
        }
        //
        // Serialize the entire transaction
        //
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024)) {
            bitcoinSerialize(outStream);
            txData = outStream.toByteArray();
        }
        //
        // Calculate the transaction hash using the serialized data
        //
        txHash = new Sha256Hash(Utils.reverseBytes(Utils.doubleDigest(txData)));
        //
        // Calculate the normalized transaction ID
        //
        List<byte[]> bufferList = new ArrayList<>(txInputs.size()+txOutputs.size());
        txInputs.stream().forEach((txInput) -> {
            bufferList.add(txInput.getOutPoint().bitcoinSerialize());
        });
        txOutputs.stream().forEach((txOutput) -> {
            bufferList.add(txOutput.bitcoinSerialize());
        });
        normID = new Sha256Hash(Utils.reverseBytes(Utils.doubleDigest(bufferList)));
    }

    /**
     * Creates a new transaction from the serialized data in the byte stream
     *
     * @param       inStream                Byte stream
     * @throws      EOFException            Byte stream is too short
     * @throws      IOException             Error while reading the input stream
     * @throws      VerificationException   Verification error
     */
    public Transaction(SerializedInputStream inStream)
                                    throws EOFException, IOException, VerificationException {
        byte[] buf = new byte[4];
        //
        // Mark our current position within the input stream
        //
        inStream.setStart();
        //
        // Get the transaction version
        //
        int count = inStream.read(buf, 0, 4);
        if (count < 4)
            throw new EOFException("End-of-data while building Transaction");
        txVersion = Utils.readUint32LE(buf, 0);
        //
        // Get the transaction inputs
        //
        int inCount = new VarInt(inStream).toInt();
        if (inCount < 0)
            throw new EOFException("Transaction input count is negative");
        txInputs = new ArrayList<>(Math.max(inCount, 1));
        for (int i=0; i<inCount; i++)
            txInputs.add(new TransactionInput(this, i, inStream));
        //
        // A coinbase transaction has a single unconnected input with a transaction hash of zero
        // and an output index of -1
        //
        if (txInputs.size() == 1) {
            OutPoint outPoint = txInputs.get(0).getOutPoint();
            if (outPoint.getHash().equals(Sha256Hash.ZERO_HASH) && outPoint.getIndex() == -1)
                coinBase = true;
        }
        //
        // Get the transaction outputs
        //
        int outCount = new VarInt(inStream).toInt();
        if (outCount < 0)
            throw new EOFException("Transaction output count is negative");
        txOutputs = new ArrayList<>(Math.max(outCount, 1));
        for (int i=0; i<outCount; i++)
            txOutputs.add(new TransactionOutput(i, inStream));
        //
        // Get the transaction lock time
        //
        count = inStream.read(buf, 0, 4);
        if (count < 4)
            throw new EOFException("End-of-data while building Transaction");
        txLockTime = Utils.readUint32LE(buf, 0);
        //
        // Save a copy of the serialized transaction
        //
        txData = inStream.getBytes();
        //
        // Calculate the transaction hash using the serialized data
        //
        txHash = new Sha256Hash(Utils.reverseBytes(Utils.doubleDigest(txData)));
        //
        // Calculate the normalized transaction ID
        //
        List<byte[]> bufferList = new ArrayList<>(txInputs.size()+txOutputs.size());
        if (!coinBase) {
            for (TransactionInput txInput : txInputs)
                bufferList.add(txInput.getOutPoint().bitcoinSerialize());
        }
        for (TransactionOutput txOutput : txOutputs)
            bufferList.add(txOutput.bitcoinSerialize());
        normID = new Sha256Hash(Utils.reverseBytes(Utils.doubleDigest(bufferList)));
        //
        // Transaction must have at least one input and one output
        //
        if (inCount == 0)
            throw new VerificationException("Transaction has no inputs", NetParams.REJECT_INVALID, txHash);
        if (outCount == 0)
            throw new VerificationException("Transaction has no outputs", NetParams.REJECT_INVALID, txHash);
    }

    /**
     * Serialize the transaction
     *
     * @param       outStream           Output stream
     * @throws      IOException         Unable to create the serialized data
     */
    public final void bitcoinSerialize(OutputStream outStream) throws IOException {
        //
        // Encode the transaction version
        //
        Utils.uint32ToByteStreamLE(txVersion, outStream);
        //
        // Encode the transaction inputs
        //
        outStream.write(VarInt.encode(txInputs.size()));
        for (TransactionInput input : txInputs)
            input.bitcoinSerialize(outStream);
        //
        // Encode the transaction outputs
        //
        outStream.write(VarInt.encode(txOutputs.size()));
        for (TransactionOutput output : txOutputs)
            output.bitcoinSerialize(outStream);
        //
        // Encode the lock time
        //
        Utils.uint32ToByteStreamLE(txLockTime, outStream);
    }

    /**
     * Returns the transaction version
     *
     * @return      Transaction version
     */
    public long getVersion() {
        return txVersion;
    }

    /**
     * Returns the transaction lock time
     *
     * @return      Transaction lock time or zero
     */
    public long getLockTime() {
        return txLockTime;
    }

    /**
     * Returns the transaction hash
     *
     * @return      Transaction hash
     */
    public Sha256Hash getHash() {
        return txHash;
    }

    /**
     * Returns the normalized transaction ID
     *
     * @return      Normalized transaction ID
     */
    public Sha256Hash getNormalizedID() {
        return normID;
    }

    /**
     * Returns the transaction hash as a printable string
     *
     * @return      Transaction hash
     */
    public String getHashAsString() {
        return txHash.toString();
    }

    /**
     * Returns the list of transaction inputs
     *
     * @return      List of transaction inputs
     */
    public List<TransactionInput> getInputs() {
        return txInputs;
    }

    /**
     * Returns the list of transaction outputs
     *
     * @return      List of transaction outputs
     */
    public List<TransactionOutput> getOutputs() {
        return txOutputs;
    }

    /**
     * Checks if this is the coinbase transaction
     *
     * @return      TRUE if this is the coinbase transaction
     */
    public boolean isCoinBase() {
        return coinBase;
    }

    /**
     * Returns the original serialized transaction data
     *
     * @return      Serialized transaction data
     */
    public byte[] getBytes() {
        return txData;
    }

    /**
     * Returns the hash code for this transaction.  This is based on the transaction hash but is
     * not the same value.
     *
     * @return      Hash code
     */
    @Override
    public int hashCode() {
        return getHash().hashCode();
    }

    /**
     * Compare this transaction to another transaction to determine if they are equal.
     *
     * @param       obj             The transaction to compare
     * @return      TRUE if they are equal
     */
    @Override
    public boolean equals(Object obj) {
        boolean areEqual = false;
        if (obj != null && (obj instanceof Transaction))
            areEqual = getHash().equals(((Transaction)obj).getHash());

        return areEqual;
    }

    /**
     * Returns a string representation of this transaction
     *
     * @return      Formatted string
     */
    @Override
    public String toString() {
        return String.format("Transaction: %s\n  %d inputs, %d outputs, %s",
                              getHashAsString(), txInputs.size(), txOutputs.size(),
                              (coinBase ? "Coinbase" : "Not coinbase"));
    }

    /**
     * Serializes the transaction for use in a signature
     *
     * @param       index                   Current transaction index
     * @param       sigHashType             Signature hash type
     * @param       subScriptBytes          Replacement script for the current input
     * @param       outStream               The output stream
     * @throws      IOException             Unable to serialize data
     * @throws      VerificationException   Invalid input index or signature hash type
     */
    public final void serializeForSignature(int index, int sigHashType, byte[] subScriptBytes,
                                OutputStream outStream) throws IOException, VerificationException {
        int hashType;
        boolean anyoneCanPay;
        //
        // The transaction input must be within range
        //
        if (index < 0 || index >= txInputs.size())
            throw new VerificationException("Transaction input index is not valid");
        //
        // Check for a valid hash type
        //
        // Note that SIGHASH_ANYONE_CAN_PAY is or'ed with one of the other hash types.  So we need
        // to remove it when checking for a valid signature.
        //
        // SIGHASH_ALL:    This is the default. It indicates that everything about the transaction is signed
        //                 except for the input scripts.
        // SIGHASH_NONE:   The outputs are not signed and can be anything. This mode allows others to update
        //                 the transaction by changing their inputs sequence numbers.  This means that all
        //                 input sequence numbers are set to 0 except for the current input.
        // SIGHASH_SINGLE: Outputs up to and including the current input index number are included.  Outputs
        //                 before the current index have a -1 value and an empty script.  All input sequence
        //                 numbers are set to 0 except for the current input.
        //
        // The SIGHASH_ANYONE_CAN_PAY modifier can be combined with the above three modes. When set, only that
        // input is signed and the other inputs can be anything.
        //
        // In all cases, the script for the current input is replaced with the script from the connected
        // output.  All other input scripts are set to an empty script.
        //
        anyoneCanPay = ((sigHashType&ScriptOpCodes.SIGHASH_ANYONE_CAN_PAY) != 0);
        hashType = sigHashType&(255-ScriptOpCodes.SIGHASH_ANYONE_CAN_PAY);
        if (hashType != ScriptOpCodes.SIGHASH_ALL && hashType != ScriptOpCodes.SIGHASH_NONE &&
                                                     hashType != ScriptOpCodes.SIGHASH_SINGLE)
            throw new VerificationException("Unsupported signature hash type");
        //
        // Serialize the version
        //
        Utils.uint32ToByteStreamLE(txVersion, outStream);
        //
        // Serialize the inputs
        //
        // For SIGHASH_ANYONE_CAN_PAY, only the current input is included in the signature.
        // Otherwise, all inputs are included.
        //
        List<TransactionInput> sigInputs;
        if (anyoneCanPay) {
            sigInputs = new ArrayList<>(1);
            sigInputs.add(txInputs.get(index));
        } else {
            sigInputs = txInputs;
        }
        outStream.write(VarInt.encode(sigInputs.size()));
        byte[] emptyScriptBytes = new byte[0];
        for (TransactionInput txInput : sigInputs) {
            txInput.serializeForSignature(index, hashType,
                                          (txInput.getIndex()==index?subScriptBytes:emptyScriptBytes),
                                          outStream);
        }
        //
        // Serialize the outputs
        //
        if (hashType == ScriptOpCodes.SIGHASH_NONE) {
            //
            // There are no outputs for SIGHASH_NONE
            //
            outStream.write(0);
        } else if (hashType == ScriptOpCodes.SIGHASH_SINGLE) {
            //
            // The output list is resized to the input index+1
            //
            if (txOutputs.size() <= index)
                throw new VerificationException("Input index out-of-range for SIGHASH_SINGLE");
            outStream.write(VarInt.encode(index+1));
            for (TransactionOutput txOutput : txOutputs) {
                if (txOutput.getIndex() > index)
                    break;
                txOutput.serializeForSignature(index, hashType, outStream);
            }
        } else {
            //
            // All outputs are serialized for SIGHASH_ALL
            outStream.write(VarInt.encode(txOutputs.size()));
            for (TransactionOutput txOutput : txOutputs) {
                txOutput.serializeForSignature(index, hashType, outStream);
            }
        }
        //
        // Serialize the lock time
        //
        Utils.uint32ToByteStreamLE(txLockTime, outStream);
    }
}
