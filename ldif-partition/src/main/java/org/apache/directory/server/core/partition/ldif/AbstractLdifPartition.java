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

package org.apache.directory.server.core.partition.ldif;


import java.util.Iterator;

import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.xdbm.AbstractStore;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * A common base class for LDIF file based Partition implementations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractLdifPartition extends BTreePartition<Long>
{
    /** We use a partition to manage searches on this partition */
    protected AvlPartition wrappedPartition;

    /** The extension used for LDIF entry files */
    protected static final String CONF_FILE_EXTN = ".ldif";

    /** A default CSN factory */
    protected static CsnFactory defaultCSNFactory;


    public AbstractLdifPartition()
    {
        // Create the CsnFactory with a invalid ReplicaId
        // @TODO : inject a correct ReplicaId
        defaultCSNFactory = new CsnFactory( 0 );
    }


    /**
     * @return the contextEntry
     */
    public Entry getContextEntry()
    {
        return contextEntry;
    }


    /**
     * @return the wrappedPartition
     */
    public Partition getWrappedPartition()
    {
        return wrappedPartition;
    }


    /**
     * @param wrappedPartition the wrappedPartition to set
     */
    public void setWrappedPartition( AvlPartition wrappedPartition )
    {
        this.wrappedPartition = wrappedPartition;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void addIndexOn( Index<?, Entry, Long> index ) throws Exception
    {
        wrappedPartition.addIndexOn( index );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int count() throws Exception
    {
        return wrappedPartition.count();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDestroy() throws Exception
    {
        wrappedPartition.destroy();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<String, Entry, Long> getAliasIndex()
    {
        return wrappedPartition.getAliasIndex();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getChildCount( Long id ) throws LdapException
    {
        return wrappedPartition.getChildCount( id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Dn getEntryDn( Long id ) throws Exception
    {
        return wrappedPartition.getEntryDn( id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Long getEntryId( Dn dn ) throws LdapException
    {
        return wrappedPartition.getEntryId( dn );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<Long, Entry, Long> getOneAliasIndex()
    {
        return wrappedPartition.getOneAliasIndex();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<Long, Entry, Long> getOneLevelIndex()
    {
        return wrappedPartition.getOneLevelIndex();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<String, Entry, Long> getPresenceIndex()
    {
        return wrappedPartition.getPresenceIndex();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<Long, Entry, Long> getSubAliasIndex()
    {
        return wrappedPartition.getSubAliasIndex();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<Long, Entry, Long> getSubLevelIndex()
    {
        return wrappedPartition.getSubLevelIndex();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<?, Entry, Long> getSystemIndex( AttributeType attributeType ) throws Exception
    {
        return wrappedPartition.getSystemIndex( attributeType );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getSystemIndices()
    {
        return wrappedPartition.getSystemIndices();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<?, Entry, Long> getUserIndex( AttributeType attributeType ) throws Exception
    {
        return wrappedPartition.getUserIndex( attributeType );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getUserIndices()
    {
        return wrappedPartition.getUserIndices();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasSystemIndexOn( AttributeType attributeType ) throws Exception
    {
        return wrappedPartition.hasSystemIndexOn( attributeType );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasUserIndexOn( AttributeType attributeType ) throws Exception
    {
        return wrappedPartition.hasUserIndexOn( attributeType );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInitialized()
    {
        return wrappedPartition != null && wrappedPartition.isInitialized();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IndexCursor<Long, Entry, Long> list( Long id ) throws LdapException
    {
        return wrappedPartition.list( id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( Long id ) throws LdapException
    {
        return wrappedPartition.lookup( id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSchemaManager( SchemaManager schemaManager )
    {
        super.setSchemaManager( schemaManager );
        wrappedPartition.setSchemaManager( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void sync() throws Exception
    {
        wrappedPartition.sync();
    }


    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        wrappedPartition.unbind( unbindContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getId()
    {
        return super.getId();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setId( String id )
    {
        super.setId( id );
        wrappedPartition.setId( id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSuffix( Dn suffix ) throws LdapInvalidDnException
    {
        super.setSuffix( suffix );
        wrappedPartition.setSuffix( suffix );
    }

    /**
     * @see AbstractStore#isCheckHasEntryDuringAdd()
     */
    public boolean isCheckHasEntryDuringAdd()
    {
        return wrappedPartition.getStore().isCheckHasEntryDuringAdd();
    }

    
    /**
     * @see AbstractStore#setCheckHasEntryDuringAdd(boolean)
     */
    public void setCheckHasEntryDuringAdd( boolean checkHasEntryDuringAdd )
    {
        wrappedPartition.getStore().setCheckHasEntryDuringAdd( checkHasEntryDuringAdd );
    }
}
