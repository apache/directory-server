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
package org.apache.directory.server.core.partition;


import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.naming.InvalidNameException;

import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * A dummy do nothing partition that is useful for testing NullPartition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NullPartition extends AbstractPartition
{
    private String id;
    private Dn suffix;
    

    /**
     * Creates a new instance of NullPartition.
     *
     */
    public NullPartition()
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#add(org.apache.directory.server.core.interceptor.context.AddOperationContext)
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#bind(org.apache.directory.server.core.interceptor.context.BindOperationContext)
     */
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#delete(org.apache.directory.server.core.interceptor.context.DeleteOperationContext)
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#getId()
     */
    public String getId()
    {
        return id;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#list(org.apache.directory.server.core.interceptor.context.ListOperationContext)
     */
    public EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException
    {
        return new EntryFilteringCursor()
        {
            
            public Iterator<ClonedServerEntry> iterator()
            {
                return EmptyIterator.INSTANCE;
            }
            
        
            public void setClosureMonitor( ClosureMonitor monitor )
            {
            }
            
        
            public boolean previous() throws Exception
            {
                return false;
            }
            
        
            public boolean next() throws Exception
            {
                return false;
            }
            
        
            public boolean last() throws Exception
            {
                return false;
            }
            
        
            public boolean isElementReused()
            {
                return false;
            }
            
        
            public boolean isClosed() throws Exception
            {
                return true;
            }
            
        
            public ClonedServerEntry get() throws Exception
            {
                return null;
            }
            
        
            public boolean first() throws Exception
            {
                return false;
            }
            
        
            public void close( Exception reason ) throws Exception
            {
            }
            
        
            public void close() throws Exception
            {
            }
            
        
            public void beforeFirst() throws Exception
            {
            }
            
        
            public void before( ClonedServerEntry element ) throws Exception
            {
            }
            
        
            public boolean available()
            {
                return false;
            }
            
        
            public void afterLast() throws Exception
            {
            }
            
        
            public void after( ClonedServerEntry element ) throws Exception
            {
            }
            
        
            public void setAbandoned( boolean abandoned )
            {
            }
            
        
            public boolean removeEntryFilter( EntryFilter filter )
            {
                return false;
            }
            
        
            public boolean isAbandoned()
            {
                return true;
            }
            
        
            public SearchingOperationContext getOperationContext()
            {
                return null;
            }
            
        
            public List<EntryFilter> getEntryFilters()
            {
                return Collections.emptyList();
            }
            
        
            public boolean addEntryFilter( EntryFilter filter )
            {
                return false;
            }


            /**
             * {@inheritDoc}
             */
            public boolean isAfterLast() throws Exception
            {
                throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
                    .concat( "." ).concat( "isAfterLast()" ) ) );
            }


            /**
             * {@inheritDoc}
             */
            public boolean isBeforeFirst() throws Exception
            {
                throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
                    .concat( "." ).concat( "isBeforeFirst()" ) ) );
            }


            /**
             * {@inheritDoc}
             */
            public boolean isFirst() throws Exception
            {
                throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
                    .concat( "." ).concat( "isFirst()" ) ) );
            }


            /**
             * {@inheritDoc}
             */
            public boolean isLast() throws Exception
            {
                throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
                    .concat( "." ).concat( "isLast()" ) ) );
            }
        };
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#lookup(java.lang.Long)
     */
    public ClonedServerEntry lookup( Long id ) throws Exception
    {
        return null;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#modify(org.apache.directory.server.core.interceptor.context.ModifyOperationContext)
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#move(org.apache.directory.server.core.interceptor.context.MoveOperationContext)
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#moveAndRename(org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext)
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#rename(org.apache.directory.server.core.interceptor.context.RenameOperationContext)
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#search(org.apache.directory.server.core.interceptor.context.SearchOperationContext)
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        return null;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#setId(java.lang.String)
     */
    public void setId( String id )
    {
        this.id = id;
    }


    /**
     * {@inheritDoc}
     */
    public void setSuffix( Dn suffix ) throws LdapInvalidDnException
    {
        this.suffix = suffix;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#unbind(org.apache.directory.server.core.interceptor.context.UnbindOperationContext)
     */
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
    }


    public Dn getSuffix()
    {
        return suffix;
    }


    public SchemaManager getSchemaManager()
    {
        return null;
    }


    public void setSchemaManager( SchemaManager schemaManager )
    {
    }


    @Override
    protected void doDestroy() throws Exception
    {
    }


    @Override
    protected void doInit() throws InvalidNameException, Exception
    {
    }


    @Override
    public ClonedServerEntry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        return null;
    }


    @Override
    public void sync() throws Exception
    {
    }
}
