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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.interceptor.context.*;
import org.apache.directory.server.core.partition.Oid;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.*;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * A {@link Partition} that stores entries in
 * <a href="http://jdbm.sourceforge.net/">JDBM</a> database.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmPartition extends BTreePartition
{
    private JdbmStore store;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a store based on JDBM B+Trees.
     */
    public JdbmPartition()
    {
        store = new JdbmStore();
    }


    public void setRegistries( Registries registries )
    {
        initRegistries( registries );
    }


    protected void initRegistries( Registries registries )
    {
        attributeTypeRegistry = registries.getAttributeTypeRegistry();
        oidRegistry = registries.getOidRegistry();
        ExpressionEvaluator evaluator = new ExpressionEvaluator( this, oidRegistry, attributeTypeRegistry );
        ExpressionEnumerator enumerator = new ExpressionEnumerator( this, attributeTypeRegistry, evaluator );
        this.searchEngine = new DefaultSearchEngine( this, evaluator, enumerator, optimizer );
        store.initRegistries( registries );
    }


    public final void init( DirectoryServiceConfiguration factoryCfg, PartitionConfiguration cfg )
        throws NamingException
    {
        // setup optimizer and registries for parent
        if ( cfg instanceof BTreePartitionConfiguration )
        {
            this.cfg = ( BTreePartitionConfiguration ) cfg;
            if ( ! this.cfg.isOptimizerEnabled() )
            {
                optimizer = new NoOpOptimizer();
            }
            else
            {
                optimizer = new DefaultOptimizer( this );
            }
        }
        else
        {
            this.cfg = BTreePartitionConfiguration.convert( cfg );
            optimizer = new DefaultOptimizer( this );
        }
        initRegistries( factoryCfg.getRegistries() );
        
        // initialize the store
        store.setCacheSize( cfg.getCacheSize() );
        store.setContextEntry( cfg.getContextEntry() );
        store.setName( cfg.getName() );
        store.setSuffixDn( cfg.getSuffix() );
        store.setWorkingDirectory( new File(
            factoryCfg.getStartupConfiguration().getWorkingDirectory().getPath()
            + File.separator + cfg.getName() ) );

        Set<JdbmIndex> userIndices = new HashSet<JdbmIndex>();
        if ( cfg instanceof BTreePartitionConfiguration )
        {
            BTreePartitionConfiguration btpconf = ( BTreePartitionConfiguration ) cfg;
            for ( Index obj : btpconf.getIndexedAttributes() )
            {
                JdbmIndex index;

                if ( obj instanceof JdbmIndex )
                {
                    index = ( JdbmIndex ) obj;
                }
                else
                {
                    index = new JdbmIndex();
                    index.setAttributeId( obj.getAttributeId() );
                    index.setCacheSize( obj.getCacheSize() );
                    index.setWkDirPath( obj.getWkDirPath() );
                }

                String oid = oidRegistry.getOid( index.getAttributeId() );
                if ( SYS_INDEX_OIDS.contains( oidRegistry.getOid( index.getAttributeId() ) ) )
                {
                    if ( oid.equals( Oid.ALIAS ) )
                    {
                        store.setAliasIndex( index );
                    }
                    else if ( oid.equals( Oid.EXISTANCE ) )
                    {
                        store.setExistanceIndex( index );
                    }
                    else if ( oid.equals( Oid.HIERARCHY ) )
                    {
                        store.setHierarchyIndex( index );
                    }
                    else if ( oid.equals( Oid.NDN ) )
                    {
                        store.setNdnIndex( index );
                    }
                    else if ( oid.equals( Oid.ONEALIAS ) )
                    {
                        store.setOneAliasIndex( index );
                    }
                    else if ( oid.equals( Oid.SUBALIAS ) )
                    {
                        store.setSubAliasIndex( index );
                    }
                    else if ( oid.equals( Oid.UPDN ) )
                    {
                        store.setUpdnIndex( index );
                    }
                    else
                    {
                        throw new IllegalStateException( "Unrecognized system index " + oid );
                    }
                }
                else
                {
                    userIndices.add( index );
                }
            }
            store.setUserIndices( userIndices );
            store.setSyncOnWrite( ( ( BTreePartitionConfiguration ) cfg ).isSynchOnWrite() );
            store.setEnableOptimizer( ( ( BTreePartitionConfiguration ) cfg ).isOptimizerEnabled() );
        }

        store.init( oidRegistry, attributeTypeRegistry );
    }


    public final void destroy()
    {
        store.destroy();
    }


    public final boolean isInitialized()
    {
        return store.isInitialized();
    }


    public final void sync() throws NamingException
    {
        store.sync();
    }


    // ------------------------------------------------------------------------
    // I N D E X   M E T H O D S
    // ------------------------------------------------------------------------


    public final void addIndexOn( Index index ) throws NamingException
    {
        if ( index instanceof JdbmIndex)
        {
            store.addIndex( ( JdbmIndex ) index );
        }
    }


    public final Index getExistanceIndex()
    {
        return store.getExistanceIndex();
    }


    public final void setExistanceIndexOn( Index index ) throws NamingException
    {
        if ( index instanceof JdbmIndex )
        {
            store.setExistanceIndex( ( JdbmIndex ) index );
        }
    }


    public final Index getHierarchyIndex()
    {
        return store.getHierarchyIndex();
    }


    public final void setHierarchyIndexOn( Index index ) throws NamingException
    {
        if ( index instanceof JdbmIndex )
        {
            store.setHierarchyIndex( ( JdbmIndex ) index );
        }
    }


    public final Index getAliasIndex()
    {
        return store.getAliasIndex();
    }


    public final void setAliasIndexOn( Index index ) throws NamingException
    {
        if ( index instanceof JdbmIndex )
        {
            store.setAliasIndex( ( JdbmIndex ) index );
        }
    }


    public final Index getOneAliasIndex()
    {
        return store.getOneAliasIndex();
    }


    public final void setOneAliasIndexOn( Index index ) throws NamingException
    {
        if ( index instanceof JdbmIndex )
        {
            store.setOneAliasIndex( ( JdbmIndex ) index );
        }
    }


    public final Index getSubAliasIndex()
    {
        return store.getSubAliasIndex();
    }


    public final void setSubAliasIndexOn( Index index ) throws NamingException
    {
        if ( index instanceof JdbmIndex )
        {
            store.setSubAliasIndex( ( JdbmIndex ) index );
        }
    }


    public final Index getUpdnIndex()
    {
        return store.getUpdnIndex();
    }


    public final void setUpdnIndexOn( Index index ) throws NamingException
    {
        if ( index instanceof JdbmIndex )
        {
            store.setUpdnIndex( ( JdbmIndex ) index );
        }
    }


    public final Index getNdnIndex()
    {
        return store.getNdnIndex();
    }


    public final void setNdnIndexOn( Index index ) throws NamingException
    {
        if ( index instanceof JdbmIndex )
        {
            store.setNdnIndex( ( JdbmIndex ) index );
        }
    }


    public final Iterator getUserIndices()
    {
        return store.userIndices();
    }


    public final Iterator getSystemIndices()
    {
        return store.systemIndices();
    }


    public final boolean hasUserIndexOn( String id ) throws NamingException
    {
        return store.hasUserIndexOn( id );
    }


    public final boolean hasSystemIndexOn( String id ) throws NamingException
    {
        return store.hasSystemIndexOn( id );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.BTreePartition#getUserIndex(String)
     */
    public final Index getUserIndex( String id ) throws IndexNotFoundException
    {
        return store.getUserIndex( id );
    }


    /**
     * @see BTreePartition#getEntryId(String)
     */
    public final Index getSystemIndex( String id ) throws IndexNotFoundException
    {
        return store.getSystemIndex( id );
    }


    public final Long getEntryId( String dn ) throws NamingException
    {
        return store.getEntryId( dn );
    }


    public final String getEntryDn( Long id ) throws NamingException
    {
        return store.getEntryDn( id );
    }


    public final Long getParentId( String dn ) throws NamingException
    {
        return store.getParentId( dn );
    }


    public final Long getParentId( Long childId ) throws NamingException
    {
        return store.getParentId( childId );
    }


    public final String getEntryUpdn( Long id ) throws NamingException
    {
        return store.getEntryUpdn( id );
    }


    public final String getEntryUpdn( String dn ) throws NamingException
    {
        return store.getEntryUpdn( dn );
    }


    public final int count() throws NamingException
    {
        return store.count();
    }

    
    public final void add( AddOperationContext addContext ) throws NamingException
    {
        store.add( addContext.getDn(), addContext.getEntry() );
    }


    public final Attributes lookup( Long id ) throws NamingException
    {
        return store.lookup( id );
    }


    public final void delete( Long id ) throws NamingException
    {
        store.delete( id );
    }


    public final NamingEnumeration list( Long id ) throws NamingException
    {
        return store.list( id );
    }


    public final int getChildCount( Long id ) throws NamingException
    {
        return store.getChildCount( id );
    }


    public final LdapDN getSuffix()
    {
        return store.getSuffix();
    }

    public final LdapDN getUpSuffix()
    {
        return store.getUpSuffix();
    }


    public final Attributes getSuffixEntry() throws NamingException
    {
        return store.getSuffixEntry();
    }


    public final void setProperty( String propertyName, String propertyValue ) throws NamingException
    {
        store.setProperty( propertyName, propertyValue );
    }


    public final String getProperty( String propertyName ) throws NamingException
    {
        return store.getProperty( propertyName );
    }


    public final Attributes getIndices( Long id ) throws NamingException
    {
        return store.getIndices( id );
    }

    
    public final void modify( ModifyOperationContext modifyContext ) throws NamingException
    {
        store.modify( modifyContext.getDn(), modifyContext.getModItems() );
    }

    public final void rename( RenameOperationContext renameContext ) throws NamingException
    {
        store.rename( renameContext.getDn(), renameContext.getNewRdn(), renameContext.getDelOldDn() );
    }


    public final void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws NamingException
    {
        store.move( moveAndRenameContext.getDn(), moveAndRenameContext.getParent(), 
        		moveAndRenameContext.getNewRdn(), moveAndRenameContext.getDelOldDn() );
    }


    public final void move( MoveOperationContext moveContext ) throws NamingException
    {
        store.move( moveContext.getDn(), moveContext.getParent() );
    }


    public final void bind( LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId ) throws NamingException
    {
        if ( bindDn == null || credentials == null || mechanisms == null ||  saslAuthId == null )
        {
            // do nothing just using variables to prevent yellow lights : bad :)
        }
        // does nothing
        throw new LdapAuthenticationNotSupportedException(
                "Bind requests only tunnel down into partitions if there are no authenticators to handle the mechanism.\n"
                        + "Check to see if you have correctly configured authenticators for the server.",
                ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
    }
    

    public final void bind( BindOperationContext bindContext ) throws NamingException
    {
        // does nothing
        throw new LdapAuthenticationNotSupportedException(
            "Bind requests only tunnel down into partitions if there are no authenticators to handle the mechanism.\n"
                + "Check to see if you have correctly configured authenticators for the server.",
            ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
    }


    public final void unbind( UnbindOperationContext unbindContext ) throws NamingException
    {
    }
}
