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
package org.apache.directory.server.core.api.partition;


import java.io.IOException;
import java.io.OutputStream;

import javax.naming.InvalidNameException;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.i18n.I18n;


/**
 * A {@link Partition} that helps users to implement their own partition.
 * Most methods are implemented by default.  Please look at the description of
 * each methods for the detail of implementations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractPartition implements Partition
{
    /** <tt>true</tt> if and only if this partition is initialized. */
    protected boolean initialized;

    /** The partition ContextEntry */
    protected Entry contextEntry;

    /** The SchemaManager instance */
    protected SchemaManager schemaManager;

    /** The DnFactory to use to create DN */
    protected DnFactory dnFactory;

    /** The partition ID */
    protected String id;

    /** The root Dn for this partition */
    protected Dn suffixDn;

    /** the cache service */
    protected CacheService cacheService;

    /** the value of last successful add/update operation's CSN */
    private String contextCsn;
    
    /** a flag to detect the change in context CSN */
    protected volatile boolean ctxCsnChanged = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws LdapException
    {
        if ( initialized )
        {
            // Already initialized.
            return;
        }

        try
        {
            doInit();
            initialized = true;
        }
        catch ( Exception e )
        {
            throw new LdapOtherException( e.getMessage(), e );
        }
        finally
        {
            if ( !initialized )
            {
                try
                {
                    destroy();
                }
                catch ( Exception e )
                {
                    throw new LdapOtherException( e.getMessage(), e );
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void repair() throws Exception
    {
        // Do nothing. It will be handled by the implementation classes
    }


    /**
     * Override this method to put your initialization code.
     */
    protected abstract void doDestroy() throws Exception;


    /**
     * Override this method to put your initialization code.
     * 
     * @throws Exception If teh init failed
     */
    protected abstract void doInit() throws InvalidNameException, Exception;


    /**
     * Override this method to implement a repair method
     * 
     * @throws Exception If the repair failed
     */
    protected abstract void doRepair() throws InvalidNameException, Exception;


    /**
     * Calls {@link #doDestroy()} where you have to put your destroy code in,
     * and clears default properties.  Once this method is invoked, {@link #isInitialized()}
     * will return <tt>false</tt>.
     */
    @Override
    public final void destroy() throws Exception
    {
        try
        {
            doDestroy();
        }
        finally
        {
            initialized = false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isInitialized()
    {
        return initialized;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final SchemaManager getSchemaManager()
    {
        return schemaManager;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final String getId()
    {
        return id;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setId( String id )
    {
        checkInitialized( "id" );
        this.id = id;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final Dn getSuffixDn()
    {
        return suffixDn;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSuffixDn( Dn suffixDn ) throws LdapInvalidDnException
    {
        checkInitialized( "suffixDn" );

        if ( suffixDn.isSchemaAware() )
        {
            this.suffixDn = suffixDn;
        }
        else
        {
            this.suffixDn = new Dn( schemaManager, suffixDn );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void dumpIndex( OutputStream stream, String name ) throws IOException
    {
        stream.write( Strings.getBytesUtf8( "Nothing to dump for index " + name ) );
    }


    /**
     * Check that the operation is done on an initialized store
     * @param property
     */
    protected void checkInitialized( String property )
    {
        if ( initialized )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_576, property ) );
        }
    }


    /**
     * @return the contextEntry
     */
    public Entry getContextEntry()
    {
        return contextEntry;
    }


    /**
     * @param contextEntry the contextEntry to set
     */
    public void setContextEntry( Entry contextEntry )
    {
        this.contextEntry = contextEntry;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setCacheService( CacheService cacheService )
    {
        this.cacheService = cacheService;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getContextCsn()
    {
        return contextCsn;
    }

    
    /**
     * Replaces the current context CSN with the given CSN value if they are not same and
     * sets the ctxCsnChanged flag to true.
     * 
     * @param csn the CSN value
     */
    protected void setContextCsn( String csn )
    {
        if ( !csn.equals( contextCsn ) )
        {
            contextCsn = csn;
            ctxCsnChanged = true;
        }
    }
}
