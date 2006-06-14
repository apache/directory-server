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
 * $Id$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.directory.shared.ldap.message;


import javax.naming.directory.Attributes;

import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Search entry protocol response message used to return non referral entries to
 * the client in response to a search request message.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: jmachols $
 * @version $Revision$
 */
public interface SearchResponseEntry extends Response
{
    /** Search entry response message type enumeration value */
    MessageTypeEnum TYPE = MessageTypeEnum.SEARCHRESENTRY;


    /**
     * Gets the distinguished name of the entry object returned.
     * 
     * @return the Dn of the entry returned.
     */
    LdapDN getObjectName();


    /**
     * Sets the distinguished name of the entry object returned.
     * 
     * @param a_dn
     *            the Dn of the entry returned.
     */
    void setObjectName( LdapDN dn );


    /**
     * Gets the set of attributes and all their values in a MultiMap.
     * 
     * @return the set of attributes and all their values
     */
    Attributes getAttributes();


    /**
     * Sets the set of attributes and all their values in a MultiMap.
     * 
     * @param a_attributes
     *            the set of attributes and all their values
     */
    void setAttributes( Attributes a_attributes );
}
