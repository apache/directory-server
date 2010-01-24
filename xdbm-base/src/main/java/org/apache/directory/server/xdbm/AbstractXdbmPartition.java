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
package org.apache.directory.server.xdbm;


import java.util.Iterator;
import java.util.List;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Base class for XDBM partitions that use an {@link Store}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractXdbmPartition extends BTreePartition
{

    protected boolean optimizerEnabled = true;

    /** The store. */
    protected Store<ServerEntry> store;


    protected AbstractXdbmPartition( Store<ServerEntry> store )
    {
        this.store = store;
    }


    /**
     * {@inheritDoc}
     */
    public void destroy() throws Exception
    {
        store.destroy();
    }


    /**
     * {@inheritDoc}
     */
    public final boolean isInitialized()
    {
        return store.isInitialized();
    }


    /**
     * {@inheritDoc}
     */
    public final void sync() throws Exception
    {
        store.sync();
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------

    public boolean isOptimizerEnabled()
    {
        return optimizerEnabled;
    }


    public void setOptimizerEnabled( boolean optimizerEnabled )
    {
        this.optimizerEnabled = optimizerEnabled;
    }


    public void setSyncOnWrite( boolean syncOnWrite )
    {
        store.setSyncOnWrite( syncOnWrite );
    }


    public boolean isSyncOnWrite()
    {
        return store.isSyncOnWrite();
    }


    // ------------------------------------------------------------------------
    // I N D E X   M E T H O D S
    // ------------------------------------------------------------------------

    public final void addIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        store.addIndex( index );
    }


    public final Index<String, ServerEntry> getExistenceIndex()
    {
        return store.getPresenceIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setPresenceIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        store.setPresenceIndex( index );
    }


    public final Index<Long, ServerEntry> getOneLevelIndex()
    {
        return store.getOneLevelIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setOneLevelIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        store.setOneLevelIndex( index );
    }


    public final Index<String, ServerEntry> getAliasIndex()
    {
        return store.getAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setAliasIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        store.setAliasIndex( index );
    }


    public final Index<Long, ServerEntry> getOneAliasIndex()
    {
        return store.getOneAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setOneAliasIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        store.setOneAliasIndex( ( Index<Long, ServerEntry> ) index );
    }


    public final Index<Long, ServerEntry> getSubAliasIndex()
    {
        return store.getSubAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setSubAliasIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        store.setSubAliasIndex( ( Index<Long, ServerEntry> ) index );
    }


    public final Index<String, ServerEntry> getUpdnIndex()
    {
        return store.getUpdnIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setUpdnIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        store.setUpdnIndex( ( Index<String, ServerEntry> ) index );
    }


    public final Index<String, ServerEntry> getNdnIndex()
    {
        return store.getNdnIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setNdnIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        store.setNdnIndex( ( Index<String, ServerEntry> ) index );
    }


    public final Iterator<String> getUserIndices()
    {
        return store.userIndices();
    }


    public final Iterator<String> getSystemIndices()
    {
        return store.systemIndices();
    }


    public final boolean hasUserIndexOn( String id ) throws Exception
    {
        return store.hasUserIndexOn( id );
    }


    public final boolean hasSystemIndexOn( String id ) throws Exception
    {
        return store.hasSystemIndexOn( id );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.BTreePartition#getUserIndex(String)
     */
    public final Index<?, ServerEntry> getUserIndex( String id ) throws IndexNotFoundException
    {
        return store.getUserIndex( id );
    }


    /**
     * @see BTreePartition#getEntryId(String)
     */
    public final Index<?, ServerEntry> getSystemIndex( String id ) throws IndexNotFoundException
    {
        return store.getSystemIndex( id );
    }


    public final Long getEntryId( String dn ) throws Exception
    {
        return store.getEntryId( dn );
    }


    public final String getEntryDn( Long id ) throws Exception
    {
        return store.getEntryDn( id );
    }


    public final Long getParentId( String dn ) throws Exception
    {
        return store.getParentId( dn );
    }


    public final Long getParentId( Long childId ) throws Exception
    {
        return store.getParentId( childId );
    }


    public final String getEntryUpdn( Long id ) throws Exception
    {
        return store.getEntryUpdn( id );
    }


    public final String getEntryUpdn( String dn ) throws Exception
    {
        return store.getEntryUpdn( dn );
    }


    public final int count() throws Exception
    {
        return store.count();
    }


    public final void add( AddOperationContext addContext ) throws Exception
    {
        store.add( ( ServerEntry ) ( ( ClonedServerEntry ) addContext.getEntry() ).getClonedEntry() );
    }


    public final ClonedServerEntry lookup( Long id ) throws Exception
    {
        return new ClonedServerEntry( store.lookup( id ) );
    }


    public final void delete( Long id ) throws Exception
    {
        store.delete( id );
    }


    public final IndexCursor<Long, ServerEntry> list( Long id ) throws Exception
    {
        return store.list( id );
    }


    public final int getChildCount( Long id ) throws Exception
    {
        return store.getChildCount( id );
    }


    public final void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        store.setProperty( propertyName, propertyValue );
    }


    public final String getProperty( String propertyName ) throws Exception
    {
        return store.getProperty( propertyName );
    }


    public final void modify( ModifyOperationContext modifyContext ) throws Exception
    {
        store.modify( modifyContext.getDn(), modifyContext.getModItems() );
    }


    public final void rename( RenameOperationContext renameContext ) throws Exception
    {
        store.rename( renameContext.getDn(), renameContext.getNewRdn(), renameContext.getDelOldDn() );
    }


    public final void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws Exception
    {
        store.move( moveAndRenameContext.getDn(), moveAndRenameContext.getParent(), moveAndRenameContext.getNewRdn(),
            moveAndRenameContext.getDelOldDn() );
    }


    public final void move( MoveOperationContext moveContext ) throws Exception
    {
        store.move( moveContext.getDn(), moveContext.getParent() );
    }


    public final void bind( LdapDN bindDn, byte[] credentials, List<String> mechanisms, String saslAuthId )
        throws Exception
    {
        if ( bindDn == null || credentials == null || mechanisms == null || saslAuthId == null )
        {
            // do nothing just using variables to prevent yellow lights : bad :)
        }

        // does nothing
        throw new LdapAuthenticationNotSupportedException( I18n.err( I18n.ERR_702 ),
            ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
    }


    public final void bind( BindOperationContext bindContext ) throws Exception
    {
        // does nothing
        throw new LdapAuthenticationNotSupportedException( I18n.err( I18n.ERR_702 ),
            ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
    }


    public final void unbind( UnbindOperationContext unbindContext ) throws Exception
    {
    }


    public final Index<String, ServerEntry> getPresenceIndex()
    {
        return store.getPresenceIndex();
    }


    public final Index<Long, ServerEntry> getSubLevelIndex()
    {
        return store.getSubLevelIndex();
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "Partition<" + id + ">";
    }

}
