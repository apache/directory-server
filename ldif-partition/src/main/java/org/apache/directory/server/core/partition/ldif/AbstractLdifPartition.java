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


import java.net.URI;

import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * A common base class for LDIF file based Partition implementations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractLdifPartition extends AvlPartition
{
    /** The extension used for LDIF entry files */
    protected static final String CONF_FILE_EXTN = ".ldif";

    /** A default CSN factory */
    protected static CsnFactory defaultCSNFactory;


    public AbstractLdifPartition( SchemaManager schemaManager )
    {
        super( schemaManager );
        
        // Create the CsnFactory with a invalid ReplicaId
        // @TODO : inject a correct ReplicaId
        defaultCSNFactory = new CsnFactory( 0 );
    }


    @Override
    protected void doDestroy() throws Exception
    {
        // Nothing to do : we don't have index
    }

    
    /**
     * @return the wrappedPartition
     *
    public Partition getWrappedPartition()
    {
        return wrappedPartition;
    }


    /**
     * @param wrappedPartition the wrappedPartition to set
     *
    public void setWrappedPartition( AvlPartition wrappedPartition )
    {
        this.wrappedPartition = wrappedPartition;
    }


    /**
     * {@inheritDoc}
     *
    public void addIndexOn( Index<?, Entry, Long> index ) throws Exception
    {
        wrappedPartition.addIndexOn( index );
    }


    /**
     * {@inheritDoc}
     *
    public int count() throws Exception
    {
        return wrappedPartition.count();
    }


    /**
     * {@inheritDoc}
     *
    @Override
    protected void doDestroy() throws Exception
    {
        wrappedPartition.destroy();
    }


    /**
     * {@inheritDoc}
     *
    public Index<String, Entry, Long> getAliasIndex()
    {
        return wrappedPartition.getAliasIndex();
    }


    /**
     * {@inheritDoc}
     *
    @Override
    public int getChildCount( Long id ) throws LdapException
    {
        return wrappedPartition.getChildCount( id );
    }


    /**
     * {@inheritDoc}
     *
    public Dn getEntryDn( Long id ) throws Exception
    {
        return wrappedPartition.getEntryDn( id );
    }


    /**
     * {@inheritDoc}
     *
    @Override
    public Long getEntryId( Dn dn ) throws LdapException
    {
        return wrappedPartition.getEntryId( dn );
    }


    /**
     * {@inheritDoc}
     *
    public Index<Long, Entry, Long> getOneAliasIndex()
    {
        return wrappedPartition.getOneAliasIndex();
    }


    /**
     * {@inheritDoc}
     *
    public Index<Long, Entry, Long> getOneLevelIndex()
    {
        return wrappedPartition.getOneLevelIndex();
    }


    /**
     * {@inheritDoc}
     *
    public Index<String, Entry, Long> getPresenceIndex()
    {
        return wrappedPartition.getPresenceIndex();
    }


    /**
     * {@inheritDoc}
     *
    public Index<Long, Entry, Long> getSubAliasIndex()
    {
        return wrappedPartition.getSubAliasIndex();
    }


    /**
     * {@inheritDoc}
     *
    public Index<Long, Entry, Long> getSubLevelIndex()
    {
        return wrappedPartition.getSubLevelIndex();
    }


    /**
     * {@inheritDoc}
     *
    public Index<?, Entry, Long> getSystemIndex( AttributeType attributeType ) throws IndexNotFoundException
    {
        return wrappedPartition.getSystemIndex( attributeType );
    }


    /**
     * {@inheritDoc}
     *
    public Iterator<String> getSystemIndices()
    {
        return wrappedPartition.getSystemIndices();
    }


    /**
     * {@inheritDoc}
     *
    public Index<?, Entry, Long> getUserIndex( AttributeType attributeType ) throws IndexNotFoundException
    {
        return wrappedPartition.getUserIndex( attributeType );
    }


    /**
     * {@inheritDoc}
     *
    public Iterator<String> getUserIndices()
    {
        return wrappedPartition.getUserIndices();
    }


    /**
     * {@inheritDoc}
     *
    public boolean hasSystemIndexOn( AttributeType attributeType ) throws LdapException
    {
        return wrappedPartition.hasSystemIndexOn( attributeType );
    }


    /**
     * {@inheritDoc}
     *
    public boolean hasUserIndexOn( AttributeType attributeType ) throws LdapException
    {
        return wrappedPartition.hasUserIndexOn( attributeType );
    }


    /**
     * {@inheritDoc}
     *
    @Override
    public boolean isInitialized()
    {
        return wrappedPartition != null && wrappedPartition.isInitialized();
    }


    /**
     * {@inheritDoc}
     *
    @Override
    public IndexCursor<Long, Entry, Long> list( Long id ) throws LdapException
    {
        return wrappedPartition.list( id );
    }


    /**
     * {@inheritDoc}
     *
    @Override
    public void setSchemaManager( SchemaManager schemaManager )
    {
        super.setSchemaManager( schemaManager );
        wrappedPartition.setSchemaManager( schemaManager );
    }


    /**
     * {@inheritDoc}
     *
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
     *
    @Override
    public void setId( String id )
    {
        super.setId( id );
        wrappedPartition.setId( id );
    }


    /**
     * {@inheritDoc}
     *
    @Override
    public void setSuffixDn( Dn suffix ) throws LdapInvalidDnException
    {
        super.setSuffixDn( suffix );
        wrappedPartition.setSuffixDn( suffix );
    }


    /**
     * {@inheritDoc}
     */
    public Long getDefaultId()
    {
        return 1L;
    }

    
    /**
     * {@inheritDoc}
     */
    public URI getPartitionPath()
    {
        return partitionPath;
    }
}
