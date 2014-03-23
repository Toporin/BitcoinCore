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
 * VerificationException is thrown when an error is detected while
 * deserializing or verifying a block or transaction
 */
public class VerificationException extends Exception {

    /** The block or transaction causing the exception */
    protected Sha256Hash hash = Sha256Hash.ZERO_HASH;

    /** The reason for the exception */
    protected int reason = NetParams.REJECT_INVALID;

    /**
     * Creates a new exception with a detail message
     *
     * @param       msg             Detail message
     */
    public VerificationException(String msg) {
        super(msg);
    }

    /**
     * Creates a new exception with a detail message and reason code
     *
     * @param       msg             Detail message
     * @param       reason          Reason code
     */
    public VerificationException(String msg, int reason) {
        super(msg);
        this.reason = reason;
    }

    /**
     * Creates a new exception with a detail message and block/transaction hash
     *
     * @param       message         Detail message
     * @param       hash            Block or transaction hash
     */
    public VerificationException(String message, Sha256Hash hash) {
        super(message);
        this.hash = hash;
    }

    /**
     * Creates a new exception with a detail message, reason code and block/transaction hash
     *
     * @param       message         Detail message
     * @param       reason          Reason code
     * @param       hash            Block or transaction hash
     */
    public VerificationException(String message, int reason, Sha256Hash hash) {
        super(message);
        this.reason = reason;
        this.hash = hash;
    }

    /**
     * Creates a new exception with a detail message and cause
     *
     * @param       msg             Detail message
     * @param       t               Caught exception
     */
    public VerificationException(String msg, Throwable t) {
        super(msg, t);
    }

    /**
     * Creates a new exception with a detail message, reason code and cause
     *
     * @param       msg             Detail message
     * @param       reason          Reason code
     * @param       t               Caught exception
     */
    public VerificationException(String msg, int reason, Throwable t) {
        super(msg, t);
        this.reason = reason;
    }

    /**
     * Creates a new exception with a detail message, block/transaction hash and cause
     *
     * @param       msg             Detail message
     * @param       hash            Block or transaction hash
     * @param       t               Caught exception
     */
    public VerificationException(String msg, Sha256Hash hash, Throwable t) {
        super(msg, t);
        this.hash = hash;
    }

    /**
     * Creates a new exception with a detail message, reason code, block/transaction hash and cause
     *
     * @param       message         Detail message
     * @param       reason          Reason code
     * @param       hash            Block or transaction hash
     * @param       t               Caught exception
     */
    public VerificationException(String message, int reason, Sha256Hash hash, Throwable t) {
        super(message, t);
        this.reason = reason;
        this.hash = hash;
    }

    /**
     * Returns the block or transaction hash
     *
     * @return      Block or transaction hash
     */
    public Sha256Hash getHash() {
        return hash;
    }

    /**
     * Returns the reason code
     *
     * @return      Reason code
     */
    public int getReason() {
        return reason;
    }
}
