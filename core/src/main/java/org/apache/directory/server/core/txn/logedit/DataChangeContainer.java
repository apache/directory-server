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
package org.apache.directory.server.core.txn.logedit;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import org.apache.directory.shared.ldap.model.name.Dn;

import org.apache.directory.server.core.api.partition.index.Serializer;

import org.apache.directory.server.core.txn.TxnManagerFactory;


/**
 * A container for index and entry changes. If any entry change is contained, then they are for a single entry.All changes 
 * contained for an entry belong to a single version and can be atomically applied to the entry in question.
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DataChangeContainer<ID> extends AbstractLogEdit<ID>
{
    /** Set to the uuid of the entry if the container contains a change for the entry, null otherwise */
    private String uuid;

    /** id of the entry if the container contains a change for an entry */
    private ID entryID;

    /** Transaction under which the change is done */
    private long txnID;

    /** partition this change applies to */
    private Dn partitionDn;

    /** List of data changes */
    private List<DataChange<ID>> changes = new LinkedList<DataChange<ID>>();


    //For externalizable
    public DataChangeContainer()
    {

    }


    public DataChangeContainer( Dn partitionDn )
    {
        this.partitionDn = partitionDn;
    }


    public String getUUID()
    {
        return uuid;
    }


    public void setUUID( String entryUUID )
    {
        this.uuid = entryUUID;
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


    public ID getEntryID()
    {
        return entryID;
    }


    public void setEntryID( ID id )
    {
        entryID = id;
    }


    public List<DataChange<ID>> getChanges()
    {
        return changes;
    }


    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        Serializer idSerializer = TxnManagerFactory.txnManagerInstance().getIDSerializer();
        boolean uuidNotNull = in.readBoolean();

        if ( uuidNotNull )
        {
            uuid = in.readUTF();
        }

        int len = in.readInt();

        if ( len < 0 )
        {
            entryID = null;
        }
        else
        {
            byte[] buf = new byte[len];
            in.readFully( buf );
            entryID = ( ID ) idSerializer.deserialize( buf );
        }

        txnID = in.readLong();

        partitionDn = new Dn();
        partitionDn.readExternal( in );

        DataChange<ID> change;
        int numChanges = in.readInt();

        for ( int idx = 0; idx < numChanges; idx++ )
        {
            change = ( DataChange<ID> ) in.readObject();
            changes.add( change );
        }
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        Serializer idSerializer = TxnManagerFactory.txnManagerInstance().getIDSerializer();
        DataChange<ID> change;

        if ( uuid != null )
        {
            out.writeBoolean( true );
            out.writeUTF( uuid );
        }
        else
        {
            out.writeBoolean( false );
        }

        if ( entryID == null )
        {
            out.writeInt( -1 );
        }
        else
        {
            byte[] buf = idSerializer.serialize( entryID );
            out.writeInt( buf.length );
            out.write( buf );
        }

        out.writeLong( txnID );

        partitionDn.writeExternal( out );

        out.writeInt( changes.size() );

        Iterator<DataChange<ID>> it = changes.iterator();

        while ( it.hasNext() )
        {
            change = it.next();
            change.writeExternal( out );
        }
    }
}
