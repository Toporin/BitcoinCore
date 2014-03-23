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

/**
 * An outpoint describes the transaction output that is connected to a transaction input.  It consists
 * of the hash for the transaction containing the output and the index of the output within the transaction.
 */
public class OutPoint {

    /** The transaction hash */
    private Sha256Hash txHash;

    /** The output index */
    private int outputIndex;

    /**
     * Creates a new transaction output point
     *
     * @param       txHash          Hash for the transaction output
     * @param       outputIndex     Index of the output within the transaction
     */
    public OutPoint(Sha256Hash txHash, int outputIndex) {
        this.txHash = txHash;
        this.outputIndex = outputIndex;
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
     * Returns the output index
     *
     * @return      Output index
     */
    public int getIndex() {
        return outputIndex;
    }

    /**
     * Serializes the outpoint
     *
     * @return                      Byte array containing serialized data
     */
    public byte[] bitcoinSerialize() {
        byte[] bytes = new byte[36];
        System.arraycopy(Utils.reverseBytes(txHash.getBytes()), 0, bytes, 0, 32);
        Utils.uint32ToByteArrayLE(outputIndex, bytes, 32);
        return bytes;
    }

    /**
     * Returns the hash code
     *
     * @return      Hash code
     */
    @Override
    public int hashCode() {
        return txHash.hashCode() ^ (int)(outputIndex|(outputIndex<<16));
    }

    /**
     * Compares two objects
     *
     * @param       obj             Object to compare
     * @return                      TRUE if they are equal
     */
    @Override
    public boolean equals(Object obj) {
        boolean areEqual = false;
        if (obj != null && (obj instanceof OutPoint)) {
            OutPoint out = (OutPoint)obj;
            areEqual = txHash.equals(out.txHash) && outputIndex == out.outputIndex;
        }
        return areEqual;
    }
}
