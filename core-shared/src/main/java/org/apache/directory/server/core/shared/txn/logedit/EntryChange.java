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
import org.apache.directory.shared.ldap.model.entry.AttributeUtils;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.exception.LdapException;


/**
 * A Change class for entry modification.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryChange implements EntryModification
{
    /** redo change */
    private Modification redoChange;

    /** undo change */
    private Modification undoChange;


    //For externalizable
    public EntryChange()
    {
    }


    public EntryChange( Modification redo, Modification undo )
    {
        redoChange = redo;
        undoChange = undo;
    }


    public Modification getRedoChange()
    {
        return redoChange;
    }


    public Modification getUndoChange()
    {
        return undoChange;
    }


    /**
     * {@inheritDoc}
     */
    public Entry applyModification( Partition partition, Entry curEntry, UUID entryId, long changeLsn, boolean recovery )
    {
        if ( curEntry == null )
        {
            if ( recovery == false )
            {
                throw new IllegalStateException( "Entry with id:" + entryId + " not found while applying changes to it"
                    + this );
            }
            else
            {
                // In recovery mode, null might be a more future version of the entry
                return null;
            }
        }

        // TODO in reovery mode, check the version of the entry. 
        try
        {
            AttributeUtils.applyModification( curEntry, redoChange );
        }
        catch ( LdapException e )
        {
            // Shouldnt happen as this change is already verified
            e.printStackTrace();
            throw new IllegalStateException( "Application of redo change failed:" + entryId + "curEntry:" + curEntry
                + "change:" + redoChange, e );
        }

        return curEntry;
    }


    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        redoChange = new DefaultModification();
        redoChange.readExternal( in );

        undoChange = new DefaultModification();
        undoChange.readExternal( in );

    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        redoChange.writeExternal( out );
        undoChange.writeExternal( out );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "EntryChange : \n" );
        sb.append( "redo " ).append( redoChange );
        sb.append( "undo " ).append( redoChange );

        return sb.toString();
    }
}
