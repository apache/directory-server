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
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;


/**
 * Base class for XDBM partitions that use an {@link Store}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractXdbmPartition<ID> extends BTreePartition<ID>
{

    protected boolean optimizerEnabled = true;

    /** The store. */
    protected Store<ServerEntry, ID> store;


    protected AbstractXdbmPartition( Store<ServerEntry, ID> store )
    {
        this.store = store;
    }


    /**
     * {@inheritDoc}
     */
    protected void doDestroy() throws Exception
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

    public final void addIndexOn( Index<? extends Object, ServerEntry, ID> index ) throws Exception
    {
        store.addIndex( index );
    }


    public final Index<String, ServerEntry, ID> getExistenceIndex()
    {
        return store.getPresenceIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setPresenceIndexOn( Index<String, ServerEntry, ID> index ) throws Exception
    {
        store.setPresenceIndex( index );
    }


    public final Index<ID, ServerEntry, ID> getOneLevelIndex()
    {
        return store.getOneLevelIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setOneLevelIndexOn( Index<ID, ServerEntry, ID> index ) throws Exception
    {
        store.setOneLevelIndex( index );
    }


    public final Index<String, ServerEntry, ID> getAliasIndex()
    {
        return store.getAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setAliasIndexOn( Index<String, ServerEntry, ID> index ) throws Exception
    {
        store.setAliasIndex( index );
    }


    public final Index<ID, ServerEntry, ID> getOneAliasIndex()
    {
        return store.getOneAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setOneAliasIndexOn( Index<ID, ServerEntry, ID> index ) throws Exception
    {
        store.setOneAliasIndex( index );
    }


    public final Index<ID, ServerEntry, ID> getSubAliasIndex()
    {
        return store.getSubAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setSubAliasIndexOn( Index<ID, ServerEntry, ID> index ) throws Exception
    {
        store.setSubAliasIndex( index );
    }


    public final Index<String, ServerEntry, ID> getUpdnIndex()
    {
        return store.getUpdnIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setUpdnIndexOn( Index<String, ServerEntry, ID> index ) throws Exception
    {
        store.setUpdnIndex( index );
    }


    public final Index<String, ServerEntry, ID> getNdnIndex()
    {
        return store.getNdnIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setNdnIndexOn( Index<String, ServerEntry, ID> index ) throws Exception
    {
        store.setNdnIndex( index );
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
    public final Index<? extends Object, ServerEntry, ID> getUserIndex( String id ) throws IndexNotFoundException
    {
        return store.getUserIndex( id );
    }


    /**
     * @see BTreePartition#getEntryId(String)
     */
    public final Index<? extends Object, ServerEntry, ID> getSystemIndex( String id ) throws IndexNotFoundException
    {
        return store.getSystemIndex( id );
    }


    public final ID getEntryId( String dn ) throws Exception
    {
        return store.getEntryId( dn );
    }


    public final String getEntryDn( ID id ) throws Exception
    {
        return store.getEntryDn( id );
    }


    public final ID getParentId( String dn ) throws Exception
    {
        return store.getParentId( dn );
    }


    public final ID getParentId( ID childId ) throws Exception
    {
        return store.getParentId( childId );
    }


    public final String getEntryUpdn( ID id ) throws Exception
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


    public final ClonedServerEntry lookup( ID id ) throws Exception
    {
        return new ClonedServerEntry( store.lookup( id ) );
    }


    public final void delete( ID id ) throws Exception
    {
        store.delete( id );
    }


    public final IndexCursor<ID, ServerEntry, ID> list( ID id ) throws Exception
    {
        return store.list( id );
    }


    public final int getChildCount( ID id ) throws Exception
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
        checkIsValidMove( moveAndRenameContext.getDn(), moveAndRenameContext.getParent() );
        store.move( moveAndRenameContext.getDn(), moveAndRenameContext.getParent(), moveAndRenameContext.getNewRdn(),
            moveAndRenameContext.getDelOldDn() );
    }


    public final void move( MoveOperationContext moveContext ) throws Exception
    {
        checkIsValidMove( moveContext.getDn(), moveContext.getParent() );
        store.move( moveContext.getDn(), moveContext.getParent() );
    }


    /**
     * 
     * checks whether the moving of given entry is valid
     *
     * @param oldChildDn the entry's DN to be moved
     * @param newParentDn new parent entry's DN
     * @throws Exception
     */
    private void checkIsValidMove( DN oldChildDn, DN newParentDn ) throws Exception
    {
        boolean invalid = false;

        DN newParentDNClone = ( DN ) newParentDn.clone();
        newParentDNClone.remove( newParentDNClone.size() - 1 );

        if ( newParentDn.size() >= oldChildDn.size() )
        {
            for ( int i = 0; i < oldChildDn.size(); i++ )
            {
                RDN nameRdn = oldChildDn.getRdn( i );
                RDN ldapRdn = newParentDn.getRdn( i );

                if ( nameRdn.compareTo( ldapRdn ) == 0 )
                {
                    invalid = true;
                }
                else
                {
                    invalid = false;
                    break;
                }
            }
        }

        if ( invalid )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                "cannot place an entry below itself" );
        }

    }


    public final void bind( DN bindDn, byte[] credentials, List<String> mechanisms, String saslAuthId )
        throws Exception
    {
        if ( bindDn == null || credentials == null || mechanisms == null || saslAuthId == null )
        {
            // do nothing just using variables to prevent yellow lights : bad :)
        }

        // does nothing
        throw new LdapAuthenticationNotSupportedException( ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED,
            I18n.err( I18n.ERR_702 ) );
    }


    public final void bind( BindOperationContext bindContext ) throws Exception
    {
        // does nothing
        throw new LdapAuthenticationNotSupportedException( ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED,
            I18n.err( I18n.ERR_702 ) );
    }


    public final void unbind( UnbindOperationContext unbindContext ) throws Exception
    {
    }


    public final Index<String, ServerEntry, ID> getPresenceIndex()
    {
        return store.getPresenceIndex();
    }


    public final Index<ID, ServerEntry, ID> getSubLevelIndex()
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
