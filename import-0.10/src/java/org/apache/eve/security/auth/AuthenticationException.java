/*
 * $Id: AuthenticationException.java,v 1.3 2003/03/13 18:28:11 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.security.auth;

import org.apache.avalon.framework.CascadingException;

/** This exception is thrown when failed to authenticate occurred */
public class AuthenticationException extends CascadingException
{
    /** Constructs an Exception without a message. */
    public AuthenticationException(Throwable a_throwable)
    {
        super(a_throwable.getMessage()) ;
    }

    /**
     * Constructs an Exception with a detailed message.
     * @param Message The message associated with the exception.
     */
    public AuthenticationException(String message, Throwable a_throwable)
    {
        super(message, a_throwable) ;
    }

    /**
     * Constructs an Exception with a detailed message.
     * @param Message The message associated with the exception.
     */
    public AuthenticationException(String message)
    {
        super(message) ;
    }
}
