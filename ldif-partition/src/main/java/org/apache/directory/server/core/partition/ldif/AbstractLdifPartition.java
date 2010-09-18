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

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.xdbm.AbstractStore;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.shared.ldap.csn.CsnFactory;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * A common base class for LDIF file based Partition implementations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractLdifPartition extends BTreePartition<Long>
{
    /** The directory into which the partition is stored */
    protected String workingDirectory;

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
     * @return the workingDirectory
     */
    public String getWorkingDirectory()
    {
        return workingDirectory;
    }


    /**
     * @param workingDirectory the workingDirectory to set
     */
    public void setWorkingDirectory( String workingDirectory )
    {
        this.workingDirectory = workingDirectory;
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


    @Override
    public void addIndexOn( Index<? extends Object, Entry, Long> index ) throws Exception
    {
        wrappedPartition.addIndexOn( index );
    }


    @Override
    public int count() throws Exception
    {
        return wrappedPartition.count();
    }


    @Override
    protected void doDestroy() throws Exception
    {
        wrappedPartition.destroy();
    }


    @Override
    public Index<String, Entry, Long> getAliasIndex()
    {
        return wrappedPartition.getAliasIndex();
    }


    @Override
    public int getChildCount( Long id ) throws LdapException
    {
        return wrappedPartition.getChildCount( id );
    }


    @Override
    public DN getEntryDn( Long id ) throws Exception
    {
        return wrappedPartition.getEntryDn( id );
    }


    @Override
    public Long getEntryId( DN dn ) throws LdapException
    {
        return wrappedPartition.getEntryId( dn );
    }


    @Override
    public Index<Long, Entry, Long> getOneAliasIndex()
    {
        return wrappedPartition.getOneAliasIndex();
    }


    @Override
    public Index<Long, Entry, Long> getOneLevelIndex()
    {
        return wrappedPartition.getOneLevelIndex();
    }


    @Override
    public Index<String, Entry, Long> getPresenceIndex()
    {
        return wrappedPartition.getPresenceIndex();
    }


    @Override
    public String getProperty( String propertyName ) throws Exception
    {
        return wrappedPartition.getProperty( propertyName );
    }


    @Override
    public Index<Long, Entry, Long> getSubAliasIndex()
    {
        return wrappedPartition.getSubAliasIndex();
    }


    @Override
    public Index<Long, Entry, Long> getSubLevelIndex()
    {
        return wrappedPartition.getSubLevelIndex();
    }


    @Override
    public Index<?, Entry, Long> getSystemIndex( AttributeType attributeType ) throws Exception
    {
        return wrappedPartition.getSystemIndex( attributeType );
    }


    @Override
    public Iterator<String> getSystemIndices()
    {
        return wrappedPartition.getSystemIndices();
    }


    @Override
    public Index<? extends Object, Entry, Long> getUserIndex( AttributeType attributeType ) throws Exception
    {
        return wrappedPartition.getUserIndex( attributeType );
    }


    @Override
    public Iterator<String> getUserIndices()
    {
        return wrappedPartition.getUserIndices();
    }


    @Override
    public boolean hasSystemIndexOn( AttributeType attributeType ) throws Exception
    {
        return wrappedPartition.hasSystemIndexOn( attributeType );
    }


    @Override
    public boolean hasUserIndexOn( AttributeType attributeType ) throws Exception
    {
        return wrappedPartition.hasUserIndexOn( attributeType );
    }


    @Override
    public boolean isInitialized()
    {
        return wrappedPartition != null && wrappedPartition.isInitialized();
    }


    @Override
    public IndexCursor<Long, Entry, Long> list( Long id ) throws LdapException
    {
        return wrappedPartition.list( id );
    }


    @Override
    public ClonedServerEntry lookup( Long id ) throws LdapException
    {
        return wrappedPartition.lookup( id );
    }


    @Override
    public void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        wrappedPartition.setProperty( propertyName, propertyValue );
    }


    @Override
    public void setSchemaManager( SchemaManager schemaManager )
    {
        super.setSchemaManager( schemaManager );
    }


    @Override
    public void sync() throws Exception
    {
        wrappedPartition.sync();
    }


    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        wrappedPartition.unbind( unbindContext );
    }


    @Override
    public String getId()
    {
        return super.getId();
    }


    @Override
    public void setId( String id )
    {
        super.setId( id );
        wrappedPartition.setId( id );
    }


    @Override
    public void setSuffix( DN suffix ) throws LdapInvalidDnException
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
