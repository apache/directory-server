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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;


/**
 * A {@link Partition} that helps users to implement their own partition.
 * Most methods are implemented by default.  Please look at the description of
 * each methods for the detail of implementations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractPartition implements Partition
{
    /** {@link DirectoryService} specified at {@link #init(DirectoryService)}. */
    protected DirectoryService directoryService;
    /** <tt>true</tt> if and only if this partition is initialized. */
    protected boolean initialized;


    protected AbstractPartition()
    {
    }


    /**
     * Sets up (<tt>directoryService</tt> and calls {@link #doInit()} where you have to put your
     * initialization code in.  {@link #isInitialized()} will return <tt>true</tt> if
     * {@link #doInit()} returns without any errors.  {@link #destroy()} is called automatically
     * as a clean-up process if {@link #doInit()} throws an exception.
     */
    public final void init( DirectoryService directoryService ) throws NamingException
    {
        if ( initialized )
        {
            // Already initialized.
            return;
        }

        this.directoryService = directoryService;
        try
        {
            doInit();
            initialized = true;
        }
        finally
        {
            if ( !initialized )
            {
                destroy();
            }
        }
    }
    

    /**
     * Override this method to put your initialization code.
     */
    protected void doInit()
    {
    }


    /**
     * Calls {@link #doDestroy()} where you have to put your destroy code in,
     * and clears default properties.  Once this method is invoked, {@link #isInitialized()}
     * will return <tt>false</tt>.
     */
    public final void destroy()
    {
        try
        {
            doDestroy();
        }
        finally
        {
            initialized = false;
            directoryService = null;
        }
    }


    /**
     * Override this method to put your initialization code.
     */
    protected void doDestroy()
    {
    }


    /**
     * Returns <tt>true</tt> if this context partition is initialized successfully.
     */
    public final boolean isInitialized()
    {
        return initialized;
    }


    /**
     * Returns {@link DirectoryService} that is provided from
     * {@link #init(DirectoryService)}.
     * @return return the directory service core
     */
    public final DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    /**
     * This method does nothing by default.
     */
    public void sync() throws NamingException
    {
    }


    /**
     * This method calls {@link Partition#lookup(LookupOperationContext)} and return <tt>true</tt>
     * if it returns an entry by default.  Please override this method if
     * there is more effective way for your implementation.
     */
    public boolean hasEntry( EntryOperationContext entryContext ) throws NamingException
    {
        try
        {
            return lookup( new LookupOperationContext( entryContext.getDn() ) ) != null;
        }
        catch ( NameNotFoundException e )
        {
            return false;
        }
    }


    /**
     * This method calls {@link Partition#lookup(LookupOperationContext)}
     * with null <tt>attributeIds</tt> by default.  Please override
     * this method if there is more effective way for your implementation.
     */
    public Attributes lookup( LookupOperationContext lookupContext ) throws NamingException
    {
        return null;
    }
}
