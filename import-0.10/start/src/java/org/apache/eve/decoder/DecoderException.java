/*
 * $Id: DecoderException.java,v 1.2 2003/03/13 18:27:09 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.decoder ;


import org.apache.avalon.framework.CascadingRuntimeException ;


/** This exception is thrown when protocol errors occurred */
public class DecoderException extends CascadingRuntimeException
{
    /**
     * Constructs an Exception with a detailed message.
     * @param Message The message associated with the exception.
     */
    public DecoderException(String message, Throwable t)
    {
        super(message, t) ;
    }


    /**
     * Constructs an Exception with a detailed message.
     * @param Message The message associated with the exception.
     */
    public DecoderException(String message)
    {
        super(message, null) ;
    }
}
