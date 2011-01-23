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
package org.apache.directory.server.core.partition;


import javax.naming.InvalidNameException;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapOtherException;
import org.apache.directory.shared.ldap.schema.SchemaManager;


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

    /** The SchemaManager instance */
    protected SchemaManager schemaManager;
    
    /** The partition ContextEntry */
    protected Entry contextEntry;

    protected AbstractPartition()
    {
    }
    
    
    /**
     * Sets up (<tt>directoryService</tt> and calls {@link #doInit()} where you have to put your
     * initialization code in.  {@link #isInitialized()} will return <tt>true</tt> if
     * {@link #doInit()} returns without any errors.  {@link #destroy()} is called automatically
     * as a clean-up process if {@link #doInit()} throws an exception.
     */
    public final void initialize( ) throws LdapException
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
            throw new LdapOtherException( e.getMessage() );
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
                    throw new LdapOtherException( e.getMessage() );
                }
            }
        }
    }
    

    /**
     * Override this method to put your initialization code.
     * @throws Exception 
     */
    protected abstract void doInit() throws InvalidNameException, Exception;


    /**
     * Calls {@link #doDestroy()} where you have to put your destroy code in,
     * and clears default properties.  Once this method is invoked, {@link #isInitialized()}
     * will return <tt>false</tt>.
     */
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
     * Override this method to put your initialization code.
     */
    protected abstract void doDestroy() throws Exception;


    /**
     * Returns <tt>true</tt> if this context partition is initialized successfully.
     */
    public boolean isInitialized()
    {
        return initialized;
    }


    /**
     * This method does nothing by default.
     */
    public abstract void sync() throws Exception;


    /**
     * This method calls {@link Partition#lookup(LookupOperationContext)} and return <tt>true</tt>
     * if it returns an entry by default.  Please override this method if
     * there is more effective way for your implementation.
     */
    public boolean hasEntry( EntryOperationContext entryContext ) throws LdapException
    {
        try
        {
            return entryContext.lookup( entryContext.getDn(), ByPassConstants.LOOKUP_BYPASS ) != null; 
        }
        catch ( LdapException e )
        {
            return false;
        }
    }


    /**
     * This method calls {@link Partition#lookup(LookupOperationContext)}
     * with null <tt>attributeIds</tt> by default.  Please override
     * this method if there is more effective way for your implementation.
     */
    public abstract ClonedServerEntry lookup( LookupOperationContext lookupContext ) throws LdapException;

    
    /**
     * {@inheritDoc}
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    /**
     * {@inheritDoc}
     */
    public SchemaManager getSchemaManager()
    {
        return schemaManager;
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
}
