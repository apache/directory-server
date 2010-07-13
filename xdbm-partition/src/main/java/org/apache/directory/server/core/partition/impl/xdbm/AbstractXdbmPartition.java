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
package org.apache.directory.server.core.partition.impl.xdbm;


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
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * Base class for XDBM partitions that use an {@link Store}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractXdbmPartition<ID extends Comparable<ID>> extends BTreePartition<ID>
{

    protected boolean optimizerEnabled = true;

    /** The store. */
    protected Store<Entry, ID> store;


    protected AbstractXdbmPartition( Store<Entry, ID> store )
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

    public final void addIndexOn( Index<? extends Object, Entry, ID> index ) throws Exception
    {
        store.addIndex( index );
    }


    public final Index<ID, Entry, ID> getOneLevelIndex()
    {
        return store.getOneLevelIndex();
    }


    public final Index<String, Entry, ID> getAliasIndex()
    {
        return store.getAliasIndex();
    }


    public final Index<ID, Entry, ID> getOneAliasIndex()
    {
        return store.getOneAliasIndex();
    }


    public final Index<ID, Entry, ID> getSubAliasIndex()
    {
        return store.getSubAliasIndex();
    }


    public final Iterator<String> getUserIndices()
    {
        return store.userIndices();
    }


    public final Iterator<String> getSystemIndices()
    {
        return store.systemIndices();
    }


    public final boolean hasUserIndexOn( AttributeType attributeType ) throws Exception
    {
        return store.hasUserIndexOn( attributeType );
    }


    public final boolean hasSystemIndexOn( AttributeType attributeType ) throws Exception
    {
        return store.hasSystemIndexOn( attributeType );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.BTreePartition#getUserIndex(String)
     */
    public final Index<? extends Object, Entry, ID> getUserIndex( AttributeType attributeType ) throws IndexNotFoundException
    {
        return store.getUserIndex( attributeType );
    }


    /**
     * @see BTreePartition#getEntryId(String)
     */
    public final Index<? extends Object, Entry, ID> getSystemIndex( AttributeType attributeType ) throws IndexNotFoundException
    {
        return store.getSystemIndex( attributeType );
    }


    public final ID getEntryId( DN dn ) throws LdapException
    {
        try
        {
            return store.getEntryId( dn );
        }
        catch ( Exception e )
        {
            throw new LdapException( e.getMessage() );
        }
    }


    public final DN getEntryDn( ID id ) throws Exception
    {
        return store.getEntryDn( id );
    }


    public final int count() throws Exception
    {
        return store.count();
    }


    public final void add( AddOperationContext addContext ) throws LdapException
    {
        try
        {
            store.add( ( Entry ) ( ( ClonedServerEntry ) addContext.getEntry() ).getClonedEntry() );
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
    }


    public final ClonedServerEntry lookup( ID id ) throws LdapException
    {
        try
        {
            return new ClonedServerEntry( store.lookup( id ) );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    public final void delete( ID id ) throws LdapException
    {
        try
        {
            store.delete( id );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    public final IndexCursor<ID, Entry, ID> list( ID id ) throws LdapException
    {
        try
        {
            return store.list( id );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    public final int getChildCount( ID id ) throws LdapException
    {
        try
        {
            return store.getChildCount( id );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    public final void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        store.setProperty( propertyName, propertyValue );
    }


    public final String getProperty( String propertyName ) throws Exception
    {
        return store.getProperty( propertyName );
    }


    public final void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        try
        {
            Entry modifiedEntry = store.modify( modifyContext.getDn(), modifyContext.getModItems() );
            modifyContext.setAlteredEntry( modifiedEntry );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    public final void rename( RenameOperationContext renameContext ) throws LdapException
    {
        try
        {
            if ( renameContext.getEntry() != null )
            {
                store.rename( renameContext.getDn(), renameContext.getNewRdn(), renameContext.getDeleteOldRdn(), renameContext.getEntry().getClonedEntry() );
            }
            else
            {
                store.rename( renameContext.getDn(), renameContext.getNewRdn(), renameContext.getDeleteOldRdn() );
            }
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    public final void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        if ( moveAndRenameContext.getNewSuperiorDn().isChildOf( moveAndRenameContext.getDn() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                "cannot place an entry below itself" );
        }

        try
        {
        	DN oldDn = moveAndRenameContext.getDn();
        	DN newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();
        	RDN newRdn = moveAndRenameContext.getNewRdn();
        	boolean deleteOldRdn = moveAndRenameContext.getDeleteOldRdn();
        	
            store.moveAndRename( oldDn, newSuperiorDn, newRdn, null, deleteOldRdn );
        }
        catch ( LdapException le )
        {
        	// In case we get an LdapException, just rethrow it as is to 
        	// avoid having it lost
        	throw le;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    public final void move( MoveOperationContext moveContext ) throws LdapException
    {
        if ( moveContext.getNewSuperior().isChildOf( moveContext.getDn() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                "cannot place an entry below itself" );
        }

        try
        {
            DN oldDn = moveContext.getDn();
            DN newSuperior = moveContext.getNewSuperior();
            DN newDn = moveContext.getNewDn();
            Entry modifiedEntry = moveContext.getModifiedEntry();
            
            store.move( oldDn, newSuperior, newDn, modifiedEntry );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    public final void bind( DN bindDn, byte[] credentials, List<String> mechanisms, String saslAuthId )
        throws LdapException
    {
        // does nothing
        throw new LdapAuthenticationNotSupportedException( ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED, I18n
            .err( I18n.ERR_702 ) );
    }


    public final void bind( BindOperationContext bindContext ) throws LdapException
    {
        // does nothing
        throw new LdapAuthenticationNotSupportedException( ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED, I18n
            .err( I18n.ERR_702 ) );
    }


    public final void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
    }


    public final Index<String, Entry, ID> getPresenceIndex()
    {
        return store.getPresenceIndex();
    }


    public final Index<ID, Entry, ID> getSubLevelIndex()
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
