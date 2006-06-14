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
 * Add protocol operation request used to add a new entry to the DIT.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public interface AddRequest extends SingleReplyRequest, AbandonableRequest
{
    /** LDAPv3 add request type enum code */
    MessageTypeEnum TYPE = MessageTypeEnum.ADDREQUEST;

    /** LDAPv3 add response type enum code */
    MessageTypeEnum RESP_TYPE = AddResponse.TYPE;


    /**
     * Gets the distinguished name of the entry to add.
     * 
     * @return the Dn of the added entry.
     */
    LdapDN getEntry();


    /**
     * Sets the distinguished name of the entry to add.
     * 
     * @param entry
     *            the Dn of the added entry.
     */
    void setEntry( LdapDN entry );


    /**
     * Gets the attributes of the entry to add.
     * 
     * @return the Attributes containing attribute value pairs.
     */
    Attributes getAttributes();


    /**
     * Sets the attribute value pairs of the entry to add.
     * 
     * @param attributes
     *            the Attributes with attribute value pairs for the added entry.
     */
    void setAttributes( Attributes attributes );
}
