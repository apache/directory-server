/*
 * $Id: KeyExpiryException.java,v 1.1 2003/03/22 19:55:29 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.client;

/**
 * Exception thrown when an accessor method is used on an expired ClientKey
 */
public class KeyExpiryException extends Exception {
    /** Constructs an Exception without a message. */
    public KeyExpiryException() {
        super();
    }

    /**
     * Constructs an Exception with a detailed message.
     * @param Message The message associated with the exception.
     */
    public KeyExpiryException(String message) {
        super(message);
    }
}
