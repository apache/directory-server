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
package org.apache.directory.server.config;


import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;

import javax.naming.InvalidNameException;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.partition.ldif.AbstractLdifPartition;


/**
 * This class implements a read-only configuration partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReadOnlyConfigurationPartition extends AbstractLdifPartition
{
    /** The input stream */
    private InputStream inputStream;


    /**
     * Creates a new instance of ReadOnlyConfigurationPartition.
     *
     * @param inputStream
     *      the input stream
     * @param schemaManager
     *      the schema manager
     */
    public ReadOnlyConfigurationPartition( InputStream inputStream, SchemaManager schemaManager )
    {
        super( schemaManager );
        this.inputStream = inputStream;
        id = "config";

        try
        {
            suffixDn = new Dn( schemaManager, "ou=config" );
        }
        catch ( LdapInvalidDnException lide )
        {
            // Nothing to do
        }
    }


    /**
     * {@inheritDoc}
     */
    protected void doInit() throws InvalidNameException, Exception
    {
        if ( !initialized )
        {
            // Initializing the wrapped partition
            super.doInit();

            // Load LDIF entries
            loadLdifEntries();
        }
    }


    /**
     * Loads the LDIF entries from the input stream.
     * 
     * @throws Exception
     */
    private void loadLdifEntries() throws Exception
    {
        if ( inputStream != null )
        {
            // Initializing the reader and the entry iterator
            LdifReader reader = new LdifReader( inputStream );
            Iterator<LdifEntry> itr = reader.iterator();

            // Exiting if there's no entry
            if ( !itr.hasNext() )
            {
                reader.close();

                return;
            }

            // Getting the context entry
            LdifEntry ldifEntry = itr.next();
            Entry contextEntry = new DefaultEntry( schemaManager, ldifEntry.getEntry() );

            // Checking the context entry
            if ( suffixDn.equals( contextEntry.getDn() ) )
            {
                addMandatoryOpAt( contextEntry );

                super.add( new AddOperationContext( null, contextEntry ) );
            }
            else
            {
                reader.close();

                throw new LdapException( "The given LDIF file doesn't contain the context entry" );
            }

            // Iterating on all entries
            while ( itr.hasNext() )
            {
                Entry entry = new DefaultEntry( schemaManager, itr.next().getEntry() );
                addMandatoryOpAt( entry );

                super.add( new AddOperationContext( null, entry ) );
            }

            // Closing the reader
            reader.close();
        }
    }


    /**
     * Adds the CSN and UUID attributes to the entry if they are not present.
     */
    private void addMandatoryOpAt( Entry entry ) throws LdapException
    {
        // entryCSN
        if ( entry.get( SchemaConstants.ENTRY_CSN_AT ) == null )
        {
            entry.add( SchemaConstants.ENTRY_CSN_AT, defaultCSNFactory.newInstance().toString() );
        }

        // entryUUID
        if ( entry.get( SchemaConstants.ENTRY_UUID_AT ) == null )
        {
            String uuid = UUID.randomUUID().toString();
            entry.add( SchemaConstants.ENTRY_UUID_AT, uuid );
        }
    }


    //---------------------------------------------------------------------------------------------
    // Operations
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext arg0 ) throws LdapException
    {
        // Does nothing (Read-Only)
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry delete( String id ) throws LdapException
    {
        // Does nothing (Read-Only)
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        // Does nothing (Read-Only)
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext arg0 ) throws LdapException
    {
        // Does nothing (Read-Only)
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext arg0 ) throws LdapException
    {
        // Does nothing (Read-Only)
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext arg0 ) throws LdapException
    {
        // Does nothing (Read-Only)
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext arg0 ) throws LdapException
    {
        // Does nothing (Read-Only)
    }
}
