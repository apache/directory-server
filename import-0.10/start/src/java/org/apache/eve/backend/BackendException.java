/*
 * $Id: BackendException.java,v 1.2 2003/03/13 18:26:42 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;

import org.apache.avalon.framework.CascadingException ;


/**
 * This exception is thrown when Error encountered on backend operation.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class BackendException
    extends CascadingException
{
    /**
     * Constructs an Exception with a detailed message.
     * @param a_message The message associated with the exception.
     */
    public BackendException(String a_message)
    {
        super(a_message) ;
    }


    /**
     * Constructs an Exception with a detailed message and an error.
     * @param a_message The message associated with the exception.
     */
    public BackendException(String a_message, Throwable a_throwable)
    {
        super(a_message, a_throwable) ;
    }
}

