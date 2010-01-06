/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

package org.apache.directory.shared.ldap.client.api.messages;


import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Search entry protocol response message used to return non referral entries to
 * the client in response to a search request message.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 760984 $
 */
public class SearchResultEntry extends AbstractMessage implements SearchResponse
{
    /** The found entry */
    private Entry entry;
    
    /**
     * Creates a new instance of SearchResultEntryImpl.
     */
    public SearchResultEntry()
    {
        super();
    }

    
    /**
     * Gets the distinguished name of the entry object returned.
     * 
     * @return the Dn of the entry returned.
     */
    public LdapDN getObjectName()
    {
        if ( entry != null )
        {
            return entry.getDn();
        }
        else
        {
            return LdapDN.EMPTY_LDAPDN;
        }
    }


    /**
     * Gets the entry.
     * 
     * @return the entry
     */
    public Entry getEntry()
    {
        return entry;
    }


    /**
     * Sets an entry
     * 
     * @param entry the entry
     */
    public void setEntry( Entry entry )
    {
        this.entry = entry;
    }
}
