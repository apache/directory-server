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

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.UUID;

import org.apache.directory.shared.ldap.model.name.Dn;

import org.apache.directory.server.core.api.partition.index.Serializer;
import org.apache.directory.server.core.api.txn.logedit.AbstractLogEdit;
import org.apache.directory.server.core.api.txn.logedit.DataChange;

import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.core.api.txn.TxnManager;


/**
 * A container for index and entry changes. If any entry change is contained, then they are for a single entry.All changes 
 * contained for an entry belong to a single version and can be atomically applied to the entry in question.
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DataChangeContainer extends AbstractLogEdit
{
    /** id of the entry if the container contains a change for an entry */
    private UUID entryID;

    /** Transaction under which the change is done */
    private long txnID;

    /** partition this change applies to */
    private Dn partitionDn;

    /** List of data changes */
    private List<DataChange> changes = new LinkedList<DataChange>();


    //For externalizable
    public DataChangeContainer()
    {

    }


    public DataChangeContainer( Dn partitionDn )
    {
        this.partitionDn = partitionDn;
    }


    public long getTxnID()
    {
        return txnID;
    }


    public void setTxnID( long id )
    {
        txnID = id;
    }


    public Dn getPartitionDn()
    {
        return partitionDn;
    }


    public UUID getEntryID()
    {
        return entryID;
    }


    public void setEntryID( UUID id )
    {
        entryID = id;
    }


    public List<DataChange> getChanges()
    {
        return changes;
    }

    public void addChange( DataChange change )
    {
        changes.add( change );
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        
        boolean hasID = in.readBoolean();
        
        if ( hasID )
        {
            entryID = UUID.fromString( in.readUTF() );
        }
        else
        {
            entryID = null;
        }
        
        txnID = in.readLong();

        partitionDn = new Dn();
        partitionDn.readExternal( in );

        DataChange change;
        int numChanges = in.readInt();

        for ( int idx = 0; idx < numChanges; idx++ )
        {
            change = ( DataChange ) in.readObject();
            changes.add( change );
        }
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        DataChange change;
        
        if ( entryID != null )
        {
            out.writeBoolean( true );
            out.writeUTF( entryID.toString() );
        }
        else
        {
            out.writeBoolean( false );
        }
        
        out.writeLong( txnID );

        partitionDn.writeExternal( out );

        out.writeInt( changes.size() );

        Iterator<DataChange> it = changes.iterator();

        while ( it.hasNext() )
        {
            change = it.next();
            change.writeExternal( out );
        }
    }
}
