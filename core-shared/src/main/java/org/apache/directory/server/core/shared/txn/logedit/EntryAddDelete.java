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
 * A Change class for entry addition or deletion. Every time we add or delete an entry, we
 * used this class to store the added or deleted entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryAddDelete implements EntryModification
{
    /** Added or deleted entry */
    private Entry entry;

    /** Type of change */
    Type type;

    /**
     * The change type : ADD or DELETE
     */
    public enum Type
    {
        ADD,
        DELETE
    }


    /**
     * A default constructor used for deserialisation only
     */
    public EntryAddDelete()
    {
    }


    /**
     * Create a new Add/Del change instance
     * 
     * @param entry The entry being added or deleted
     * @param type ADD for an addition, DELETE for a deletion
     */
    public EntryAddDelete( Entry entry, Type type )
    {
        this.entry = entry;
        this.type = type;
    }


    /**
     * @return The stored entry
     */
    public Entry getChangedEntry()
    {
        return entry;
    }


    /**
     * @return The type, ADD or DELETE
     */
    public Type getType()
    {
        return type;
    }


    /**
     * {@inheritDoc}
     */
    public Entry applyModification( Partition partition, Entry curEntry, UUID entryId, long changeLsn, boolean recovery )
    {
        if ( type == Type.ADD )
        {
            if ( curEntry != null )
            {
                if ( recovery == false )
                {
                    throw new IllegalStateException( "Entry is being added while it already exists:" + entryId
                        + " curEntry:" + curEntry + " entry:" + entry );
                }
                else
                {
                    // TODO verify the curEnty is more recent
                    return curEntry;
                }
            }

            curEntry = entry;
        }
        else
        {
            if ( curEntry == null )
            {
                if ( recovery == false )
                {
                    throw new IllegalStateException( "Entry is being delete while it doesnt exist:" + entryId
                        + " curEntry:" + curEntry + " entry:" + entry );
                }
                else
                {
                    return null;
                }
            }

            curEntry = null;
        }

        return curEntry;
    }


    /**
     * Read back the entry from the stream.
     */
    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        type = Type.values()[in.readInt()];
        entry = new DefaultEntry();
        entry.readExternal( in );
    }


    /**
     * Write the change in a stream. The format is : <br/>
     * <ul>
     * <li>type (0 for ADD, 1 for DELETE</li>
     * <li>entry</li>
     * </ul>
     */
    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        out.writeInt( type.ordinal() );
        entry.writeExternal( out );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "EntryAddDelete : " );
        sb.append( type );

        if ( type == Type.ADD )
        {
            sb.append( '\n' ).append( entry );
        }
        else
        {
            sb.append( entry.getDn() );
        }

        return sb.toString();
    }
}
