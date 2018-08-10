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
package org.apache.directory.server.core.shared;


import java.io.IOException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.directory.SearchControls;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.util.tree.DnNode;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.ReferralManager;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.api.partition.PartitionTxn;


/**
 * Implement a referral Manager, handling the requests from the LDAP protocol.
 * <br>
 * Referrals are stored in a tree, where leaves are the referrals. We are using
 * the very same structure than for the partition manager.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReferralManagerImpl implements ReferralManager
{
    /** The referrals tree */
    private DnNode<Entry> referrals;

    /** A lock to guarantee the manager consistency */
    private ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();

    /** A storage for the ObjectClass attributeType */
    private AttributeType objectClassAT;


    /**
     *
     * Creates a new instance of ReferralManagerImpl.
     *
     * @param directoryService The directory service
     * @throws LdapException If we can't initialize the manager
     */
    public ReferralManagerImpl( DirectoryService directoryService ) throws LdapException
    {
        lockWrite();

        try
        {
            referrals = new DnNode<>();
            PartitionNexus nexus = directoryService.getPartitionNexus();
    
            Set<String> suffixes = nexus.listSuffixes();
            objectClassAT = directoryService.getSchemaManager().getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
    
            init( directoryService, suffixes.toArray( new String[]
                {} ) );
        }
        finally
        {
            unlock();
        }
    }


    /**
     * Get a read-lock on the referralManager.
     * No read operation can be done on the referralManager if this
     * method is not called before.
     */
    @Override
    public void lockRead()
    {
        mutex.readLock().lock();
    }


    /**
     * Get a write-lock on the referralManager.
     * No write operation can be done on the referralManager if this
     * method is not called before.
     */
    @Override
    public void lockWrite()
    {
        mutex.writeLock().lock();
    }


    /**
     * Release the read-write lock on the referralManager.
     * This method must be called after having read or modified the
     * ReferralManager
     */
    @Override
    public void unlock()
    {
        if ( mutex.isWriteLockedByCurrentThread() )
        {
            mutex.writeLock().unlock();
        }
        else
        {
            mutex.readLock().unlock();
        }
    }


    /**
     * {@inheritDoc}
     */
    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Override
    public void addReferral( Entry entry )
    {
        try
        {
            referrals.add( entry.getDn(), entry );
        }
        catch ( LdapException ne )
        {
            // Do nothing
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( DirectoryService directoryService, String... suffixes ) throws LdapException
    {
        ExprNode referralFilter = new EqualityNode<String>( objectClassAT,
            new Value( objectClassAT, SchemaConstants.REFERRAL_OC ) );

        // Lookup for each entry with the ObjectClass = Referral value
        SearchControls searchControl = new SearchControls();
        searchControl.setReturningObjFlag( false );
        searchControl.setSearchScope( SearchControls.SUBTREE_SCOPE );

        CoreSession adminSession = directoryService.getAdminSession();
        PartitionNexus nexus = directoryService.getPartitionNexus();

        for ( String suffix : suffixes )
        {
            // We will store each entry's Dn into the Referral tree
            Dn suffixDn = directoryService.getDnFactory().create( suffix );

            SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, suffixDn,
                referralFilter, searchControl );
            
            Partition partition = nexus.getPartition( suffixDn );
            
            try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
            {
                searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );
                searchOperationContext.setTransaction( partitionTxn );
                searchOperationContext.setPartition( partition );
                EntryFilteringCursor cursor = nexus.search( searchOperationContext );
    
                try
                {
                    // Move to the first entry in the cursor
                    cursor.beforeFirst();
    
                    while ( cursor.next() )
                    {
                        Entry entry = cursor.get();
    
                        // Lock the referralManager
                        lockWrite();
    
                        try
                        {
                            // Add it at the right place
                            addReferral( entry );
                        }
                        finally
                        { 
                            // Unlock the referralManager
                            unlock();
                        }
                    }
    
                    cursor.close();
                }
                catch ( Exception e )
                {
                    throw new LdapOperationException( e.getMessage(), e );
                }
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void remove( DirectoryService directoryService, Dn suffix ) throws Exception
    {
        ExprNode referralFilter = new EqualityNode<String>( objectClassAT,
            new Value( objectClassAT, SchemaConstants.REFERRAL_OC ) );

        // Lookup for each entry with the ObjectClass = Referral value
        SearchControls searchControl = new SearchControls();
        searchControl.setReturningObjFlag( false );
        searchControl.setSearchScope( SearchControls.SUBTREE_SCOPE );

        CoreSession adminSession = directoryService.getAdminSession();
        PartitionNexus nexus = directoryService.getPartitionNexus();

        // We will store each entry's Dn into the Referral tree
        SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, suffix,
            referralFilter, searchControl );
        Partition partition = nexus.getPartition( suffix ); 
        searchOperationContext.setPartition( partition );
        searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            searchOperationContext.setTransaction( partitionTxn );
            EntryFilteringCursor cursor = nexus.search( searchOperationContext );
    
            // Move to the first entry in the cursor
            cursor.beforeFirst();
    
            while ( cursor.next() )
            {
                Entry entry = cursor.get();
    
                // Add it at the right place
                removeReferral( entry );
            }
        } 
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasParentReferral( Dn dn )
    {
        DnNode<Entry> referral = referrals.getNode( dn );

        return ( referral != null ) && referral.isLeaf();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry getParentReferral( Dn dn )
    {
        if ( !hasParentReferral( dn ) )
        {
            return null;
        }

        return referrals.getElement( dn );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReferral( Dn dn )
    {
        Entry parent = referrals.getElement( dn );

        if ( parent != null )
        {
            return dn.equals( parent.getDn() );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void removeReferral( Entry entry ) throws LdapException
    {
        referrals.remove( entry.getDn() );
    }
}
