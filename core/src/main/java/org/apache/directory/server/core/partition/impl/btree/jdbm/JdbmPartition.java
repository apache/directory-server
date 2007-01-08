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


import java.io.File;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.core.partition.impl.btree.BTreePartitionConfiguration;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.IndexNotFoundException;
import org.apache.directory.server.schema.registries.Registries;

import org.apache.directory.shared.ldap.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;


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
        initOptimizer0( cfg );
        initRegistries1( factoryCfg.getRegistries() );
        
        // initialize the store
        JdbmStoreConfiguration storeConfig = new JdbmStoreConfiguration();
        storeConfig.setAttributeTypeRegistry( attributeTypeRegistry );
        storeConfig.setCacheSize( cfg.getCacheSize() );
        storeConfig.setContextEntry( cfg.getContextEntry() );
        storeConfig.setIndexedAttributes( cfg.getIndexedAttributes() );
        storeConfig.setName( cfg.getName() );
        storeConfig.setOidRegistry( oidRegistry );
        storeConfig.setSuffixDn( cfg.getSuffix() );
        
        storeConfig.setWorkingDirectory( new File( 
            factoryCfg.getStartupConfiguration().getWorkingDirectory().getPath()
            + File.separator + cfg.getName() ) );
        
        if ( cfg instanceof BTreePartitionConfiguration )
        {
            storeConfig.setSyncOnWrite( ( ( BTreePartitionConfiguration ) cfg ).isSynchOnWrite() );
        }
        
        if ( cfg instanceof BTreePartitionConfiguration )
        {
            storeConfig.setEnableOptimizer( ( ( BTreePartitionConfiguration ) cfg ).isOptimizerEnabled() );
        }
        
        store.init( storeConfig );
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
        store.addIndexOn( spec, cacheSize, numDupLimit );
    }


    public final Index getExistanceIndex()
    {
        return store.getExistanceIndex();
    }


    public final void setExistanceIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        store.setExistanceIndexOn( attrType, cacheSize, numDupLimit );
    }


    public final Index getHierarchyIndex()
    {
        return store.getHierarchyIndex();
    }


    public final void setHierarchyIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        store.setHierarchyIndexOn( attrType, cacheSize, numDupLimit );
    }


    public final Index getAliasIndex()
    {
        return store.getAliasIndex();
    }


    public final void setAliasIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        store.setAliasIndexOn( attrType, cacheSize, numDupLimit );
    }


    public final Index getOneAliasIndex()
    {
        return store.getOneAliasIndex();
    }


    public final void setOneAliasIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        store.setOneAliasIndexOn( attrType, cacheSize, numDupLimit );
    }


    public final Index getSubAliasIndex()
    {
        return store.getSubAliasIndex();
    }


    public final void setSubAliasIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        store.setSubAliasIndexOn( attrType, cacheSize, numDupLimit );
    }


    public final Index getUpdnIndex()
    {
        return store.getUpdnIndex();
    }


    public final void setUpdnIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        store.setUpdnIndexOn( attrType, cacheSize, numDupLimit );
    }


    public final Index getNdnIndex()
    {
        return store.getNdnIndex();
    }


    public final void setNdnIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException
    {
        store.setNdnIndexOn( attrType, cacheSize, numDupLimit );
    }


    public final Iterator getUserIndices()
    {
        return store.getUserIndices();
    }


    public final Iterator getSystemIndices()
    {
        return store.getSystemIndices();
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


    public final BigInteger getEntryId( String dn ) throws NamingException
    {
        return store.getEntryId( dn );
    }


    public final String getEntryDn( BigInteger id ) throws NamingException
    {
        return store.getEntryDn( id );
    }


    public final BigInteger getParentId( String dn ) throws NamingException
    {
        return store.getParentId( dn );
    }


    public final BigInteger getParentId( BigInteger childId ) throws NamingException
    {
        return store.getParentId( childId );
    }


    public final String getEntryUpdn( BigInteger id ) throws NamingException
    {
        return store.getEntryUpdn( id );
    }


    public final String getEntryUpdn( String dn ) throws NamingException
    {
        return getEntryUpdn( dn );
    }


    public final int count() throws NamingException
    {
        return store.count();
    }

    
    public final void add( LdapDN normName, Attributes entry ) throws NamingException
    {
        store.add( normName, entry );
    }


    public final Attributes lookup( BigInteger id ) throws NamingException
    {
        return store.lookup( id );
    }


    public final void delete( BigInteger id ) throws NamingException
    {
        store.delete( id );
    }


    public final NamingEnumeration list( BigInteger id ) throws NamingException
    {
        return store.list( id );
    }


    public final int getChildCount( BigInteger id ) throws NamingException
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


    public final Attributes getIndices( BigInteger id ) throws NamingException
    {
        return store.getIndices( id );
    }

    
    public final void modify( LdapDN dn, int modOp, Attributes mods ) throws NamingException
    {
        store.modify( dn, modOp, mods );
    }


    public final void modify( LdapDN dn, ModificationItemImpl[] mods ) throws NamingException
    {
        store.modify( dn, mods );
    }


    public final void modifyRn( LdapDN dn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        store.modifyRn( dn, newRdn, deleteOldRdn );
    }


    public final void move( LdapDN oldChildDn, LdapDN newParentDn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        store.move( oldChildDn, newParentDn, newRdn, deleteOldRdn );
    }


    public final void move( LdapDN oldChildDn, LdapDN newParentDn ) throws NamingException
    {
        store.move( oldChildDn, newParentDn );
    }


    public final void bind( LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId ) throws NamingException
    {
        // does nothing
        throw new LdapAuthenticationNotSupportedException(
            "Bind requests only tunnel down into partitions if there are no authenticators to handle the mechanism.\n"
                + "Check to see if you have correctly configured authenticators for the server.",
            ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
    }


    public final void unbind( LdapDN bindDn ) throws NamingException
    {
    }
}
