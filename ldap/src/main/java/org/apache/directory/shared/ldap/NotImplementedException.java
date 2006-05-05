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
 $Id$

 -- (c) LDAPd Group                                                    --
 -- Please refer to the LICENSE.txt file in the root directory of      --
 -- any LDAPd project for copyright and distribution information.      --

 */

package org.apache.directory.shared.ldap;


/**
 * This exception is thrown when a Backend operation is either temporarily
 * unsupported or perminantly unsupported as part of its implementation. Write
 * operations on a backend set to readonly throw a type of unsupported exception
 * called ReadOnlyException.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public class NotImplementedException extends RuntimeException
{
    static final long serialVersionUID = -5899307402675964298L;


    /**
     * Constructs an Exception with a detailed message.
     */
    public NotImplementedException()
    {
        super( "N O T   I M P L E M E N T E D   Y E T !" );
    }


    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param a_msg
     *            The message associated with the exception.
     */
    public NotImplementedException(String a_msg)
    {
        super( "N O T   I M P L E M E N T E D   Y E T !\n" + a_msg );
    }
}
