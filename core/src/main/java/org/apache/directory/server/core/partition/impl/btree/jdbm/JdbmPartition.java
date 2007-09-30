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
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.*;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;

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

    
    protected void initRegistries1( Registries registries ) 
    {
        super.initRegistries1( registries );
        store.initRegistries( registries );
    }
    

    public final void init( DirectoryServiceConfiguration factoryCfg, PartitionConfiguration cfg )
        throws NamingException
    {
        // setup optimizer and registries for parent
        initOptimizerAndConfiguration0( cfg );
        initRegistries1( factoryCfg.getRegistries() );
        
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

                userIndices.add( index );
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

    public final void addIndexOn( AttributeType spec, int cacheSize, int numDupLimit ) throws NamingException
    {
        JdbmIndex index = new JdbmIndex();
        index.setAttributeId( spec.getName() );
        index.setCacheSize( cacheSize );
        index.setNumDupLimit( numDupLimit );
        store.addIndex( index );
    }


    public final Index getExistanceIndex()
    {
        return store.getExistanceIndex();
    }


    public final void setExistanceIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        JdbmIndex index = new JdbmIndex();
        index.setAttributeId( attrType.getName() );
        index.setCacheSize( cacheSize );
        index.setNumDupLimit( numDupLimit );
        store.setExistanceIndex( index );
    }


    public final Index getHierarchyIndex()
    {
        return store.getHierarchyIndex();
    }


    public final void setHierarchyIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        JdbmIndex index = new JdbmIndex();
        index.setAttributeId( attrType.getName() );
        index.setCacheSize( cacheSize );
        index.setNumDupLimit( numDupLimit );
        store.setHierarchyIndex( index );
    }


    public final Index getAliasIndex()
    {
        return store.getAliasIndex();
    }


    public final void setAliasIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        JdbmIndex index = new JdbmIndex();
        index.setAttributeId( attrType.getName() );
        index.setCacheSize( cacheSize );
        index.setNumDupLimit( numDupLimit );
        store.setAliasIndex( index );
    }


    public final Index getOneAliasIndex()
    {
        return store.getOneAliasIndex();
    }


    public final void setOneAliasIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        JdbmIndex index = new JdbmIndex();
        index.setAttributeId( attrType.getName() );
        index.setCacheSize( cacheSize );
        index.setNumDupLimit( numDupLimit );
        store.setOneAliasIndex( index );
    }


    public final Index getSubAliasIndex()
    {
        return store.getSubAliasIndex();
    }


    public final void setSubAliasIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        JdbmIndex index = new JdbmIndex();
        index.setAttributeId( attrType.getName() );
        index.setCacheSize( cacheSize );
        index.setNumDupLimit( numDupLimit );
        store.setSubAliasIndex( index );
    }


    public final Index getUpdnIndex()
    {
        return store.getUpdnIndex();
    }


    public final void setUpdnIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        JdbmIndex index = new JdbmIndex();
        index.setAttributeId( attrType.getName() );
        index.setCacheSize( cacheSize );
        index.setNumDupLimit( numDupLimit );
        store.setUpdnIndex( index );
    }


    public final Index getNdnIndex()
    {
        return store.getNdnIndex();
    }


    public final void setNdnIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        JdbmIndex index = new JdbmIndex();
        index.setAttributeId( attrType.getName() );
        index.setCacheSize( cacheSize );
        index.setNumDupLimit( numDupLimit );
        store.setNdnIndex( index );
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
