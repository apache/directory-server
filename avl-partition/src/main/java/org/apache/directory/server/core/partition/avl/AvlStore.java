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

package org.apache.directory.server.core.partition.avl;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Store implementation backed by in memory AVL trees.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlStore<E> implements Store<E>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( AvlStore.class );

    private OidRegistry oidRegistry;
    private AttributeTypeRegistry attributeTypeRegistry;
    
    /** Two static declaration to avoid lookup all over the code */
    private static AttributeType OBJECT_CLASS_AT;
    private static AttributeType ALIASED_OBJECT_NAME_AT;

    /** the master table storing entries by primary key */
    private AvlMasterTable<ServerEntry> master;

    /** the normalized distinguished name index */
    private AvlIndex<String,E> ndnIdx;
    /** the user provided distinguished name index */
    private AvlIndex<String,E> updnIdx;
    /** the attribute existence index */
    private AvlIndex<String,E> existenceIdx;
    /** a system index on aliasedObjectName attribute */
    private AvlIndex<String,E> aliasIdx;
    /** a system index on the entries of descendants of root DN*/
    private AvlIndex<Long,E> subLevelIdx;
    /** the parent child relationship index */
    private AvlIndex<Long,E> oneLevelIdx;
    /** the one level scope alias index */
    private AvlIndex<Long,E> oneAliasIdx;
    /** the subtree scope alias index */
    private AvlIndex<Long,E> subAliasIdx;
    
    /** a map of attributeType numeric ID to user userIndices */
    private Map<String, ? extends Index<?,E>> userIndices 
        = new HashMap<String, AvlIndex<?,E>>();
    
    /** a map of attributeType numeric ID to system userIndices */
    private Map<String, ? extends Index<?,E>> systemIndices 
        = new HashMap<String, AvlIndex<?,E>>();
    
    /** true if initialized */
    private boolean initialized;


    /**
     * {@inheritDoc}
     */
    public void add( LdapDN normName, ServerEntry entry ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void addIndex( Index<?, E> index ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public int count() throws Exception
    {
        // TODO Auto-generated method stub
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Long id ) throws Exception
    {
        // TODO Auto-generated method stub
    }


    /**
     * {@inheritDoc}
     */
    public void destroy() throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public Index<String, E> getAliasIndex()
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
     * {@inheritDoc}
     */
    public int getCacheSize()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    
    /**
     * {@inheritDoc}
     */
    public int getChildCount( Long id ) throws Exception
    {
        // TODO Auto-generated method stub
        return 0;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getEntryDn( Long id ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Long getEntryId( String dn ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getEntryUpdn( Long id ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public String getEntryUpdn( String dn ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Index<String, E> getNdnIndex()
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
     * {@inheritDoc}
     */
    public Index<Long, E> getOneAliasIndex()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Index<Long, E> getOneLevelIndex()
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
     * {@inheritDoc}
     */
    public Long getParentId( String dn ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
     * {@inheritDoc}
     */
    public Long getParentId( Long childId ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
     * {@inheritDoc}
     */
    public Index<String, E> getPresenceIndex()
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getProperty( String propertyName ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Index<Long, E> getSubAliasIndex()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Index<Long, E> getSubLevelIndex()
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
     * {@inheritDoc}
     */
    public LdapDN getSuffix()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public String getSuffixDn()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Index<?, E> getSystemIndex( String id ) throws IndexNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public LdapDN getUpSuffix()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Index<String, E> getUpdnIndex()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Index<?, E> getUserIndex( String id ) throws IndexNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Set<? extends Index<?, E>> getUserIndices()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public File getWorkingDirectory()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasSystemIndexOn( String id ) throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasUserIndexOn( String id ) throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }

    
    /**
     * {@inheritDoc}
     */
    public void init( Registries registries ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void initRegistries( Registries registries )
    {
        // TODO Auto-generated method stub
    }


    /**
     * {@inheritDoc}
     */
    public boolean isInitialized()
    {
        // TODO Auto-generated method stub
        return false;
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isSyncOnWrite()
    {
        // TODO Auto-generated method stub
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<Long, E> list( Long id ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
     * {@inheritDoc}
     */
    public ServerEntry lookup( Long id ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
     * {@inheritDoc}
     */
    public void modify( LdapDN dn, ModificationOperation modOp, ServerEntry mods ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void modify( LdapDN dn, List<Modification> mods ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void move( LdapDN oldChildDn, LdapDN newParentDn, Rdn newRdn, boolean deleteOldRdn ) throws Exception
    {
        // TODO Auto-generated method stub
    }


    /**
     * {@inheritDoc}
     */
    public void move( LdapDN oldChildDn, LdapDN newParentDn ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void rename( LdapDN dn, Rdn newRdn, boolean deleteOldRdn ) throws Exception
    {
        // TODO Auto-generated method stub
    }


    /**
     * {@inheritDoc}
     */
    public void setAliasIndex( Index<String, E> index ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void setCacheSize( int cacheSize )
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void setName( String name )
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void setNdnIndex( Index<String, E> index ) throws Exception
    {
        // TODO Auto-generated method stub
    }


    /**
     * {@inheritDoc}
     */
    public void setOneAliasIndex( Index<Long, E> index ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void setOneLevelIndex( Index<Long, E> index ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void setPresenceIndex( Index<String, E> index ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        // TODO Auto-generated method stub    
    }

    
    /**
     * {@inheritDoc}
     */
    public void setSubAliasIndex( Index<Long, E> index ) throws Exception
    {
        // TODO Auto-generated method stub    
    }

    
    /**
     * {@inheritDoc}
     */
    public void setSubLevelIndex( Index<Long, E> index ) throws Exception
    {
        // TODO Auto-generated method stub    
    }

    
    /**
     * {@inheritDoc}
     */
    public void setSuffixDn( String suffixDn )
    {
        // TODO Auto-generated method stub    
    }

    
    /**
     * {@inheritDoc}
     */
    public void setSyncOnWrite( boolean isSyncOnWrite )
    {
        // TODO Auto-generated method stub    
    }

    
    /**
     * {@inheritDoc}
     */
    public void setUpdnIndex( Index<String, E> index ) throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public void setUserIndices( Set<? extends Index<?, E>> userIndices )
    {
        protect( "setUserIndices" );
        
        for ( Index<?, E> index : userIndices )
        {
            this.userIndices.put( index.getAttributeId(), convertIndex( index ) );
        }
    }

    
    private void protect( String method )
    {
        if ( initialized )
        {
            throw new IllegalStateException( "Cannot call store method: " + method );
        }
    }
    
    
    private<K> AvlIndex<K, E> convertIndex( Index<K,E> index ) throws Exception
    {
        if ( index instanceof AvlIndex )
        {
            return ( AvlIndex<K,E> ) index;
        }

        LOG.warn( "Supplied index {} is not a AvlIndex.  " +
            "Will create new AvlIndex using copied configuration parameters.", index );
        AvlIndex<K,E> avlIndex = 
            new AvlIndex<K, E>( index.getAttributeId(), index.getAttribute() );
        return avlIndex;
    }


    /**
     * {@inheritDoc}
     */
    public void setWorkingDirectory( File workingDirectory )
    {
    }

    
    /**
     * {@inheritDoc}
     */
    public void sync() throws Exception
    {
    }

    
    /**
     * {@inheritDoc}
     */
    public Iterator<String> systemIndices()
    {
        return systemIndices.keySet().iterator();
    }

    
    /**
     * {@inheritDoc}
     */
    public Iterator<String> userIndices()
    {
        return userIndices.keySet().iterator();
    }
}
