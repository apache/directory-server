/*
 * $Id: ListenerException.java,v 1.3 2003/03/13 18:27:38 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.listener ;


import org.apache.avalon.framework.CascadingRuntimeException ;


/** This exception is thrown when protocol errors occurred */
public class ListenerException extends CascadingRuntimeException
{
    /**
     * Constructs an Exception with a detailed message.
     * @param Message The message associated with the exception.
     */
    public ListenerException(String message, Throwable t)
    {
        super(message, t) ;
    }


    /**
     * Constructs an Exception with a detailed message.
     * @param Message The message associated with the exception.
     */
    public ListenerException(String message)
    {
        super(message, null) ;
    }
}
