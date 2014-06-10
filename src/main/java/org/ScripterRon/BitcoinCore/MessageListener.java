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

import java.util.List;

/**
 * A MessageListener is called during message processing to handle application-specific tasks.
 */
public interface MessageListener {

    /**
     * Handle an inventory request
     *
     * This method is called when a 'getdata' message is received.  The application
     * should send the inventory items to the requesting peer.  A 'notfound' message
     * should be returned to the requesting peer if one or more items cannot be sent.
     *
     * @param       peer            Peer requesting the inventory item
     * @param       invList         Inventory item list
     */
    public void sendInventory(Peer peer, List<InventoryItem> invList);

    /**
     * Handle an inventory item available notification
     *
     * This method is called when an 'inv' message is received.  The application
     * should request any needed inventory items from the peer.
     *
     * @param       peer            Peer announcing inventory item
     * @param       invList         Inventory item list
     */
    public void requestInventory(Peer peer, List<InventoryItem> invList);

    /**
     * Handle a request not found
     *
     * This method is called when a 'notfound' message is received.  It notifies the
     * application that an inventory request cannot be completed because the item was
     * not found.  The request can be discarded or retried by sending it to a different
     * peer.
     *
     * @param       peer            Peer sending the response
     * @param       invList         Inventory item list
     */
    public void requestNotFound(Peer peer, List<InventoryItem> invList);

    /**
     * Handle a request for the transaction memory pool
     *
     * This method is called when a 'mempool' message is received.  The application
     * should return an 'inv' message listing the transactions in the memory pool.
     *
     * @param       peer            Peer sending the request
     */
    public void requestMemoryPool(Peer peer);

    /**
     * Process a peer address list
     *
     * This method is called when an 'addr' message is received.  The address list
     * contains peers that have been active recently.
     *
     * @param       peer            Peer sending the address list
     * @param       addresses       Peer address list
     */
    public void processAddresses(Peer peer, List<PeerAddress> addresses);

    /**
     * Process an alert
     *
     * This method is called when an 'alert' message is received
     *
     * @param       peer            Peer sending the alert message
     * @param       alert           Alert
     */
    public void processAlert(Peer peer, Alert alert);

    /**
     * Process a block
     *
     * This method is called when a 'block' message is received
     *
     * @param       peer            Peer sending the block
     * @param       block           Block
     */
    public void processBlock(Peer peer, Block block);

    /**
     * Process a block header
     *
     * This method is called when a 'headers' message is received
     *
     * @param       peer            Peer sending the headers
     * @param       hdrList         Block header list
     */
    public void processBlockHeaders(Peer peer, List<BlockHeader> hdrList);

    /**
     * Process a Bloom filter load request
     *
     * This method is called when a 'filterload' message is received.  The peer bloom
     * filter has been updated before this method is called.
     *
     * @param       peer            Peer sending the message
     * @param       oldFilter       Previous bloom filter
     * @param       newFilter       New bloom filter
     */
    public void processFilterLoad(Peer peer, BloomFilter oldFilter, BloomFilter newFilter);

    /**
     * Process a get address request
     *
     * This method is called when a 'getaddr' message is received.  The application should
     * call AddressMessage.buildAddressMessage() to build the response message.
     *
     * @param       peer            Peer sending the message
     */
    public void processGetAddress(Peer peer);

    /**
     * Process a request for the latest blocks
     *
     * This method is called when a 'getblocks' message is received.  The application should
     * use the locator block list to find the latest common block and then send an 'inv'
     * message to the peer for the blocks following the common block.
     *
     * @param       peer            Peer sending the message
     * @param       version         Negotiated version
     * @param       blockList       Locator block list
     * @param       stopBlock       Stop block (Sha256Hash.ZERO_HASH if all blocks should be sent)
     */
    public void processGetBlocks(Peer peer, int version, List<Sha256Hash> blockList, Sha256Hash stopBlock);

    /**
     * Process a request for the latest headers
     *
     * This method is called when a 'getheaders' message is received.  The application should
     * use the locator block list to find the latest common block and then send a 'headers'
     * message to the peer for the blocks following the common block.
     *
     * @param       peer            Peer sending the message
     * @param       version         Negotiated version
     * @param       blockList       Locator block list
     * @param       stopBlock       Stop block (Sha256Hash.ZERO_HASH if all blocks should be sent)
     */
    public void processGetHeaders(Peer peer, int version, List<Sha256Hash> blockList, Sha256Hash stopBlock);

    /**
     * Process a Merkle block
     *
     * This method is called when a 'merkleblock' message is received
     *
     * @param       peer            Peer sending the Merkle block
     * @param       blkHeader       Merkle block header
     */
    public void processMerkleBlock(Peer peer, BlockHeader blkHeader);

    /**
     * Process a ping
     *
     * This method is called when a 'ping' message is received.  The application should
     * return a 'pong' message to the sender.
     *
     * @param       peer            Peer sending the ping
     * @param       nonce           Nonce
     */
    public void processPing(Peer peer, long nonce);

    /**
     * Process a pong
     *
     * This method is called when a 'pong' message is received.
     *
     * @param       peer            Peer sending the pong
     * @param       nonce           Nonce
     */
    public void processPong(Peer peer, long nonce);

    /**
     * Process a message rejection
     *
     * This method is called when a 'reject' message is received
     *
     * @param       peer            Peer sending the message
     * @param       cmd             Failing message command
     * @param       reasonCode      Failure reason code
     * @param       description     Description of the failure
     * @param       hash            Item hash or Sha256Hash.ZERO_HASH
     */
    public void processReject(Peer peer, String cmd, int reasonCode, String description, Sha256Hash hash);

    /**
     * Process a transaction
     *
     * This method is called when a 'tx' message is received
     *
     * @param       peer            Peer sending the transaction
     * @param       tx              Transaction
     */
    public void processTransaction(Peer peer, Transaction tx);

    /**
     * Process a version message
     *
     * This method is called when a 'version' message is received.  The application
     * should return a 'verack' message to the sender if the connection is accepted.
     *
     * @param       peer            Peer sending the message
     * @param       localAddress    Local address as seen by the peer
     */
    public void processVersion(Peer peer, PeerAddress localAddress);

    /**
     * Process a version acknowledgment
     *
     * This method is called when a 'verack' message is received
     *
     * @param       peer            Peer sending the message
     */
    public void processVersionAck(Peer peer);
}
