/*
 * $Id: ReadOnlyException.java,v 1.4 2003/07/29 22:08:02 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import javax.naming.OperationNotSupportedException ;


/**
 * This exception is thrown when write operations are attempted against a
 * Backend configured to be read-only.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.4 $
 */
public class ReadOnlyException
    extends OperationNotSupportedException
{
    /**
     * Constructs an Exception with a detailed message.
     * @param a_message The message associated with the exception.
     */
    public ReadOnlyException(String a_message)
    {
        super(a_message) ;
    }
}
