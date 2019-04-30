/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.ldap.replication;


import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.controls.ChangeType;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;


/**
 * A place holder storing an Entry and the operation applied on it
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaEventMessage
{
    /** The message change type */
    private ChangeType changeType;
    
    /** The entry */
    private Entry entry;

    /**
     * Create a new ReplicaEvent instance for a Add/Delete+Modify operation
     * @param changeType The change type
     * @param entry The entry
     */
    public ReplicaEventMessage( ChangeType changeType, Entry entry )
    {
        this.changeType = changeType;
        
        if ( entry instanceof ClonedServerEntry )
        {
            this.entry = ( ( ClonedServerEntry ) entry ).getClonedEntry();
        }
        else
        {
            this.entry = entry;
        }
    }


    /**
     * @return The changeType
     */
    public ChangeType getChangeType()
    {
        return changeType;
    }


    /**
     * @return The stored Entry
     */
    public Entry getEntry()
    {
        return entry;
    }


    /**
     * checks if the event's CSN is older than the given CSN
     *
     * @param csn the CSN
     * @return true if the event's CSN is older than the given CSN
     * @throws LdapException if there are any extreme conditions like a null entry or missing entryCSN attribute.
     */
    public boolean isEventOlderThan( String csn ) throws LdapException
    {
        if ( csn == null )
        {
            return false;
        }
        
        String entryCsn = entry.get( SchemaConstants.ENTRY_CSN_AT ).getString();
        
        int i = entryCsn.compareTo( csn );
        
        return ( i <= 0 );
    }
}
