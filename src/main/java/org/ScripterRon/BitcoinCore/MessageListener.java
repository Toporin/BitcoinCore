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
     * Handle an inventory item request
     *
     * This method is called when a 'getdata' message is received.  The application
     * should send the inventory item to the requesting peer.
     *
     * @param       peer            Peer requesting the inventory item
     * @param       type            Type of inventory item (INV_BLOCK or INV_TX)
     * @param       hash            Item hash
     * @return                      TRUE if the item was sent, FALSE if it was not sent
     */
    public boolean sendInventory(Peer peer, int type, Sha256Hash hash);

    /**
     * Handle an inventory item available notification
     *
     * This method is called when an 'inv' message is received.  The application
     * should request the inventory item from the peer if the item is needed.
     *
     * @param       peer            Peer announcing inventory item
     * @param       type            Type of inventory item (INV_BLOCK or INV_TX)
     * @param       hash            Item hash
     */
    public void requestInventory(Peer peer, int type, Sha256Hash hash);

    /**
     * Handle a request completion
     *
     * This method is called when a 'block', 'merkleblock' or 'tx' message is received.
     * It notifies the application that an inventory request has been completed.
     *
     * @param       peer            Peer sending the response
     * @param       type            Type of inventory item (INV_BLOCK, INV_FILTERED_BLOCK or INV_TX)
     * @param       hash            Item hash
     */
    public void requestCompleted(Peer peer, int type, Sha256Hash hash);

    /**
     * Handle a request not found
     *
     * This method is called when a 'notfound' message is received.  It notifies the
     * application that an inventory request cannot be completed because the item was
     * not found.  The request can be discarded or retried by sending it to a different
     * peer.
     *
     * @param       peer            Peer sending the response
     * @param       type            Type of inventory item (INV_BLOCK or INV_TX)
     * @param       hash            Item hash
     */
    public void requestNotFound(Peer peer, int type, Sha256Hash hash);

    /**
     * Process a peer address list
     *
     * This method is called when an 'addr' message is received.  The address list
     * contains peers that have been active recently.
     *
     * @param       addresses       Peer address list
     */
    public void processAddresses(List<PeerAddress> addresses);

    /**
     * Process an alert
     *
     * This method is called when an 'alert' message is received
     *
     * @param       alert           Alert
     */
    public void processAlert(Alert alert);

    /**
     * Process a block
     *
     * This method is called when a 'block' message is received
     *
     * @param       block           Block
     */
    public void processBlock(Block block);

    /**
     * Process a block header
     *
     * This method is called when a 'headers' or 'merkleblock' message is received
     *
     * @param       blockHeader     Block header
     */
    public void processBlockHeader(BlockHeader blockHeader);

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
     * Process a transaction
     *
     * This method is called when a 'tx' message is received
     *
     * @param       tx              Transaction
     */
    public void processTransaction(Transaction tx);

    /**
     * Process a message rejection
     *
     * This method is called when a 'reject' message is received
     *
     * @param       peer            Peer sending the message
     * @param       cmd             Failing message command
     * @param       reasonCode      Failure reason code
     * @param       description     Description of the failure
     * @param       hash            Item hash
     */
    public void processReject(Peer peer, String cmd, int reasonCode, String description, Sha256Hash hash);
}
