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
package org.apache.directory.server.core.shared.txn.logedit;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.txn.logedit.EntryModification;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * A Change class for entry modification
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryReplace implements EntryModification
{
    /** New Entry */
    private Entry newEntry;

    /** Old enty */
    private Entry oldEntry;


    // For externalizable
    public EntryReplace()
    {

    }


    public EntryReplace( Entry newEntry, Entry oldEntry )
    {
        this.newEntry = newEntry;
        this.oldEntry = oldEntry;
    }


    public Entry getNewEntry()
    {
        return newEntry;
    }


    public Entry getOldEntry()
    {
        return oldEntry;
    }


    /**
     * {@inheritDoc}
     */
    public Entry applyModification( Partition partition, Entry curEntry, UUID entryId, long changeLsn, boolean recovery )
    {
        // Currently this log edit is only for existing exntries
        if ( curEntry == null )
        {
            if ( recovery == false )
            {
                throw new IllegalStateException( " Trying to replace a non existing entry:" + entryId + " entry:"
                    + newEntry );
            }
            else
            {
                return null;
            }
        }

        // TODO check the version of the entry and see which entry is more recent
        return newEntry;
    }


    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        newEntry = new DefaultEntry();
        newEntry.readExternal( in );

        oldEntry = new DefaultEntry();
        oldEntry.readExternal( in );
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        newEntry.writeExternal( out );
        oldEntry.writeExternal( out );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "EntryReplace : \n" );
        sb.append( "   new " ).append( newEntry );
        sb.append( "   old " ).append( oldEntry );

        return sb.toString();
    }
}
