/**
 * Copyright 2012 Matt Corallo
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
 * <p>A Bloom filter is a probabilistic data structure which can be sent to another client
 * so that it can avoid sending us transactions that aren't relevant to our set of keys.
 * This allows for significantly more efficient use of available network bandwidth and CPU time.</p>
 *
 * <p>Because a Bloom filter is probabilistic, it has a configurable false positive rate.
 * So the filter will sometimes match transactions that weren't inserted into it, but it will
 * never fail to match transactions that were. This is a useful privacy feature - if you have
 * spare bandwidth the false positive rate can be increased so the remote peer gets a noisy
 * picture of what transactions are relevant to your wallet.</p>
 *
 * <p>Bloom Filter</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   VarInt     Count               Number of bytes in the filter
 *   Variable   Filter              Filter data
 *   4 bytes    nHashFuncs          Number of hash functions
 *   4 bytes    nTweak              Random value to add to the hash seed
 *   1 byte     nFlags              Filter update flags
 * </pre>
 */
public class BloomFilter {

    /** Bloom filter - Filter is not adjusted for matching outputs */
    public static final int UPDATE_NONE = 0;

    /** Bloom filter - Filter is adjusted for all matching outputs */
    public static final int UPDATE_ALL = 1;

    /** Bloom filter - Filter is adjusted only for pay-to-pubkey or pay-to-multi-sig */
    public static final int UPDATE_P2PUBKEY_ONLY = 2;

    /** Maximum filter size */
    public static final int MAX_FILTER_SIZE = 36000;

    /** Maximum number of hash functions */
    public static final int MAX_HASH_FUNCS = 50;

    /** Filter data */
    private byte[] filter;

    /** Number of hash functions */
    private long nHashFuncs;

    /** Random tweak nonce */
    private long nTweak = Double.valueOf(Math.random()*Long.MAX_VALUE).longValue();

    /** Filter update flags */
    private int nFlags = UPDATE_P2PUBKEY_ONLY;

    /**
     * <p>Constructs a new Bloom Filter which will provide approximately the given false positive
     * rate when the given number of elements have been inserted.
     * f the filter would otherwise be larger than the maximum allowed size, it will be
     * automatically resized to the maximum size.</p>
     *
     * <p>The anonymity of which coins are yours to any peer which you send a BloomFilter to is
     * controlled by the false positive rate.
     * For reference, as of block 187,000, the total number of addresses used in the chain was
     * roughly 4.5 million.
     * Thus, if you use a false positive rate of 0.001 (0.1%), there will be, on average, 4,500
     * distinct public keys/addresses which will be thought to be yours by nodes which have your
     * bloom filter, but which are not actually yours.</p>
     *
     * @param       elements            Number of elements in the filter
     */
    public BloomFilter(int elements) {
        //
        // We will use a false-positive rate of 0.0005 (0.05%)
        //
        double falsePositiveRate = 0.0005;
        //
        // Allocate the filter array
        //
        int size = Math.min((int)(-1/(Math.pow(Math.log(2), 2)) * elements*Math.log(falsePositiveRate)),
                            MAX_FILTER_SIZE*8)/8;
        filter = new byte[size<=0 ? 1 : size];
        //
        // Optimal number of hash functions for a given filter size and element count.
        //
        nHashFuncs = Math.min((int)(filter.length*8 / (double)elements*Math.log(2)), MAX_HASH_FUNCS);
    }

    /**
     * Serialize the filter and return a byte array
     *
     * @return                          Serialized filter
     */
    public byte[] bitcoinSerialize() {
        byte[] varLength = VarInt.encode(filter.length);
        byte[] bytes = new byte[varLength.length+filter.length+4+4+1];
        System.arraycopy(varLength, 0, bytes, 0, varLength.length);
        int offset = varLength.length;
        System.arraycopy(filter, 0, bytes, offset, filter.length);
        offset += filter.length;
        Utils.uint32ToByteArrayLE(nHashFuncs, bytes, offset);
        Utils.uint32ToByteArrayLE(nTweak, bytes, offset+4);
        bytes[offset+8] = (byte)nFlags;
        return bytes;
    }

    /**
     * Returns the filter flags
     *
     * @return      Filter flags
     */
    public int getFlags() {
        return nFlags;
    }

    /**
     * Checks if the filter contains the specified object
     *
     * @param       object          Object to test
     * @return                      TRUE if the filter contains the object
     */
    public boolean contains(byte[] object) {
        for (int i=0; i<nHashFuncs; i++) {
            if (!Utils.checkBitLE(filter, hash(i, object, 0, object.length)))
                return false;
        }
        return true;
    }

    /**
     * Checks if the filter contains the specified object
     *
     * @param       object          Object to test
     * @param       offset          Starting offset
     * @param       length          Length to check
     * @return                      TRUE if the filter contains the object
     */
    public boolean contains(byte[] object, int offset, int length) {
        for (int i=0; i<nHashFuncs; i++) {
            if (!Utils.checkBitLE(filter, hash(i, object, offset, length)))
                return false;
        }
        return true;
    }

    /**
     * Inserts an object into the filter
     *
     * @param       object          Object to insert
     */
    public void insert(byte[] object) {
        for (int i=0; i<nHashFuncs; i++) {
            Utils.setBitLE(filter, hash(i, object, 0, object.length));
        }
    }

    /**
     * Rotate a 32-bit value left by the specified number of bits
     *
     * @param       x               The bit value
     * @param       count           The number of bits to rotate
     * @return                      The rotated value
     */
    private int ROTL32(int x, int count) {
        return (x<<count) | (x>>>(32-count));
    }

    /**
     * Performs a MurmurHash3
     *
     * @param       hashNum         The hash number
     * @param       object          The byte array to hash
     * @param       offset          The starting offset
     * @param       length          Length to hash
     * @return                      The hash of the object using the specified hash number
     */
    private int hash(int hashNum, byte[] object, int offset, int length) {
        int h1 = (int)(hashNum * 0xFBA4C795L + nTweak);
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;
        int numBlocks = (length / 4) * 4;
        //
        // Body
        //
        for(int i=0; i<numBlocks; i+=4) {
            int k1 = ((int)object[offset+i]&0xFF) | (((int)object[offset+i+1]&0xFF)<<8) |
                     (((int)object[offset+i+2]&0xFF)<<16) | (((int)object[offset+i+3]&0xFF)<<24);
            k1 *= c1;
            k1 = ROTL32(k1,15);
            k1 *= c2;
            h1 ^= k1;
            h1 = ROTL32(h1,13);
            h1 = h1*5+0xe6546b64;
        }
        int k1 = 0;
        switch(length & 3) {
            case 3:
                k1 ^= (object[offset+numBlocks + 2] & 0xff) << 16;
                // Fall through.
            case 2:
                k1 ^= (object[offset+numBlocks + 1] & 0xff) << 8;
                // Fall through.
            case 1:
                k1 ^= (object[offset+numBlocks] & 0xff);
                k1 *= c1; k1 = ROTL32(k1,15);
                k1 *= c2;
                h1 ^= k1;
                // Fall through.
            default:
                // Do nothing.
                break;
        }
        //
        // Finalization
        //
        h1 ^= length;
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;
        return (int)((h1&0xFFFFFFFFL) % (filter.length * 8));
    }
}
