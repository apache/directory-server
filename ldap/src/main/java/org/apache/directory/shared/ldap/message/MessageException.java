/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

/*
 * $Id: MessageException.java,v 1.3 2003/07/31 21:44:49 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.directory.shared.ldap.message ;


import org.apache.directory.shared.ldap.RuntimeMultiException;


/**
 * This exception is thrown when a message processing error occurs.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public class MessageException extends RuntimeMultiException
{
    static final long serialVersionUID = -155089078576745029L;


    /**
     * Constructs an Exception without a message.
     */
    public MessageException()
    {
        super() ;
    }


    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param a_message The message associated with the exception.
     */
    public MessageException( String a_message )
    {
        super( a_message ) ;
    }
}
