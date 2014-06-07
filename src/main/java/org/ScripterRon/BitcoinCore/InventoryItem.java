/*
 * Copyright 2014 Ronald Hoffman.
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

/**
 * InventoryItem represents an inventory item (block or transaction).  Inventory items
 * are used in messages that announce the availability of an item or request an item
 * from a peer.
 */
public class InventoryItem implements ByteSerializable {

    /** Item hash */
    private final Sha256Hash hash;

    /** Item type */
    private final int type;

    /**
     * Create an inventory item
     *
     * @param       type                Inventory item type (INV_BLOCK, INV_FILTERED_BLOCK, INV_TX)
     * @param       hash                Inventory item hash
     */
    public InventoryItem(int type, Sha256Hash hash) {
        this.hash = hash;
        this.type = type;
    }

    /**
     * Return the inventory item type
     *
     * @return                          Inventory item type
     */
    public int getType() {
        return type;
    }

    /**
     * Return the inventory item hash
     *
     * @return                          Inventory item hash
     */
    public Sha256Hash getHash() {
        return hash;
    }
}
