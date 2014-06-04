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
import java.math.BigInteger;
import java.util.List;

/**
 * BlockHeader contains the block header.  It is used to validate transactions sent to us
 * and to determine if a transaction has become unconfirmed because the block containing it
 * is no longer on the block chain.
 */
public class BlockHeader {

    /** Block header length */
    public static final int HEADER_SIZE = 80;

    /** The number that is one greater than the largest representable SHA-256 hash */
    private static final BigInteger LARGEST_HASH = BigInteger.ONE.shiftLeft(256);

    /** Block hash */
    private Sha256Hash blockHash;

    /** Previous block hash */
    private Sha256Hash prevHash;

    /** Time block was mined */
    private long blockTime;

    /** Merkle root */
    private Sha256Hash merkleRoot;

    /** Target difficulty */
    private long targetDifficulty;

    /** On chain */
    private boolean onChain;

    /** Block height */
    private int blockHeight;

    /** Cumulative chain work */
    private BigInteger chainWork;

    /** Matched transactions */
    private List<Sha256Hash> matches;

    /**
     * Creates a new BlockHeader
     *
     * @param       blockHash           Block hash
     * @param       prevHash            Previous block hash
     * @param       blockTime           Time block was mined
     * @param       targetDifficulty    Target difficulty
     * @param       merkleRoot          Merkle root
     * @param       matches             List of matched transactions or null
     */
    public BlockHeader(Sha256Hash blockHash, Sha256Hash prevHash, long blockTime, long targetDifficulty,
                                        Sha256Hash merkleRoot, List<Sha256Hash> matches) {
        this(blockHash, prevHash, blockTime, targetDifficulty, merkleRoot, false, 0, BigInteger.ZERO, matches);
    }

    /**
     * Creates a BlockHeader from a database entry
     *
     * @param       blockHash           Block hash
     * @param       prevHash            Previous block hash
     * @param       blockTime           Time block was mined
     * @param       targetDifficulty    Target difficulty
     * @param       merkleRoot          Merkle root
     * @param       onChain             TRUE if the block is on the block chain
     * @param       blockHeight         Block height
     * @param       chainWork           Cumulative chain work
     * @param       matches             Matched transactions for this block
     */
    public BlockHeader(Sha256Hash blockHash, Sha256Hash prevHash, long blockTime, long targetDifficulty,
                                        Sha256Hash merkleRoot, boolean onChain, int blockHeight,
                                        BigInteger chainWork, List<Sha256Hash> matches) {
        this.blockHash = blockHash;
        this.prevHash = prevHash;
        this.blockTime = blockTime;
        this.targetDifficulty = targetDifficulty;
        this.merkleRoot = merkleRoot;
        this.onChain = onChain;
        this.blockHeight = blockHeight;
        this.chainWork = chainWork;
        this.matches = matches;
    }

    /**
     * Creates a BlockHeader from the serialized block header
     *
     * @param       bytes                   Serialized data
     * @throws      EOFException            Serialized data is too short
     * @throws      VerificationException   Block verification failed
     */
    public BlockHeader(byte[] bytes) throws EOFException, VerificationException {
        if (bytes.length < HEADER_SIZE)
            throw new EOFException("Header is too short");
        //
        // Compute the block hash from the serialized block header
        //
        blockHash = new Sha256Hash(Utils.reverseBytes(Utils.doubleDigest(bytes, 0, HEADER_SIZE)));
        //
        // Parse the block header
        //
        prevHash = new Sha256Hash(Utils.reverseBytes(bytes, 4, 32));
        merkleRoot = new Sha256Hash(Utils.reverseBytes(bytes, 36, 32));
        blockTime = Utils.readUint32LE(bytes, 68);
        targetDifficulty = Utils.readUint32LE(bytes, 72);
        onChain = false;
        blockHeight = 0;
        chainWork = BigInteger.ZERO;
        //
        // Ensure this block does in fact represent real work done.  If the difficulty is high enough,
        // we can be fairly certain the work was done by the network.
        //
        // The block hash must be less than or equal to the target difficulty (the difficulty increases
        // by requiring an increasing number of leading zeroes in the block hash)
        //
        BigInteger target = Utils.decodeCompactBits(targetDifficulty);
        if (target.signum() <= 0 || target.compareTo(NetParams.PROOF_OF_WORK_LIMIT) > 0)
            throw new VerificationException("Target difficulty is not valid",
                                            NetParams.REJECT_INVALID, blockHash);
        BigInteger hash = blockHash.toBigInteger();
        if (hash.compareTo(target) > 0)
            throw new VerificationException("Block hash is higher than target difficulty",
                                            NetParams.REJECT_INVALID, blockHash);
        //
        // Verify the block timestamp
        //
        long currentTime = System.currentTimeMillis()/1000;
        if (blockTime > currentTime+NetParams.ALLOWED_TIME_DRIFT)
            throw new VerificationException("Block timestamp is too far in the future",
                                            NetParams.REJECT_INVALID, blockHash);
    }

    /**
     * Returns the block hash
     *
     * @return                          Block hash
     */
    public Sha256Hash getHash() {
        return blockHash;
    }

    /**
     * Returns the previous block hash
     *
     * @return                          Previous block hash
     */
    public Sha256Hash getPrevHash() {
        return prevHash;
    }

    /**
     * Returns the block time
     *
     * @return                          Block time
     */
    public long getBlockTime() {
        return blockTime;
    }

    /**
     * Returns the Merkle root
     *
     * @return                          Merkle root
     */
    public Sha256Hash getMerkleRoot() {
        return merkleRoot;
    }

    /**
     * Returns the target difficulty
     *
     * @return                          Target difficulty
     */
    public long getTargetDifficulty() {
        return targetDifficulty;
    }

    /**
     * Returns the block work
     *
     * @return                          Block work
     */
    public BigInteger getBlockWork() {
        BigInteger target = Utils.decodeCompactBits(targetDifficulty);
        return LARGEST_HASH.divide(target.add(BigInteger.ONE));
    }

    /**
     * Returns the list of matched transactions or null if there are no matched transactions
     *
     * @return                          List of matched transactions
     */
    public List<Sha256Hash> getMatches() {
        return matches;
    }

    /**
     * Sets the list of matched transactions
     *
     * @param       matches             List of matched transactions or null if there are no matched transactions
     */
    public void setMatches(List<Sha256Hash> matches) {
        this.matches = matches;
    }

    /**
     * Checks if the block is on the block chain
     *
     * @return                          TRUE if the block is on the block chain
     */
    public boolean isOnChain() {
        return onChain;
    }

    /**
     * Sets the block chain status
     *
     * @param       onChain             TRUE if the block is on the block chain
     */
    public void setChain(boolean onChain) {
        this.onChain = onChain;
    }

    /**
     * Returns the block height
     *
     * @return                          Block height
     */
    public int getBlockHeight() {
        return blockHeight;
    }

    /**
     * Sets the block height
     *
     * @param       blockHeight         Block height
     */
    public void setBlockHeight(int blockHeight) {
        this.blockHeight = blockHeight;
    }

    /**
     * Returns the cumulative chain work for this block
     *
     * @return                          Chain work
     */
    public BigInteger getChainWork() {
        return chainWork;
    }

    /**
     * Sets the cumulative chain work for this block
     *
     * @param       chainWork           Chain work
     */
    public void setChainWork(BigInteger chainWork) {
        this.chainWork = chainWork;
    }
}
