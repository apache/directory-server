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
package org.apache.directory.server.core;

import java.util.Iterator;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.tree.DnBranchNode;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;

/**
 * Implement a referral Manager, handling the requests from the LDAP protocol.
 * <br>
 * Referrals are stored in a tree, where leaves are the referrals. We are using 
 * the very same structure than for the partition manager.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ReferralManagerImpl implements ReferralManager
{
    /** The referrals tree */
    private DnBranchNode<LdapDN> referrals;

    
    /**
     * {@inheritDoc}
     */
    public void addReferral( LdapDN dn )
    {
        synchronized ( referrals )
        {
            try
            {
                ((DnBranchNode<LdapDN>)referrals).add( dn, dn );
            }
            catch ( NamingException ne )
            {
                // Do noghing
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void init( DirectoryService directoryService ) throws Exception
    {
        referrals = new DnBranchNode<LdapDN>();
        ExprNode referralFilter = new EqualityNode<String>( SchemaConstants.OBJECT_CLASS_AT, 
            new ClientStringValue( SchemaConstants.REFERRAL_OC ) );

        // Lookup for each entry with the ObjectClass = Referral value
        SearchControls searchControl = new SearchControls();
        searchControl.setReturningObjFlag( false );
        searchControl.setSearchScope( SearchControls.SUBTREE_SCOPE );
        PartitionNexus nexus = directoryService.getPartitionNexus();
        
        CoreSession adminSession = directoryService.getAdminSession();
        
        Iterator<String> suffixes = nexus.listSuffixes( null );

        while ( suffixes.hasNext() )
        {
            // We will store each entry's DN into the Referral tree
            LdapDN suffix = new LdapDN( suffixes.next() );
            EntryFilteringCursor cursor = nexus.search( new SearchOperationContext( adminSession, suffix, AliasDerefMode.DEREF_ALWAYS,
                referralFilter, searchControl ) );
            
            // Move to the first entry in the cursor
            cursor.beforeFirst();
            
            while ( cursor.next() ) 
            {
                ServerEntry entry = cursor.get();
                
                // Add it at the right place
                addReferral( entry.getDn() );
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean isParentReferral( LdapDN dn )
    {
        return referrals.hasParentElement( dn );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isReferral( LdapDN dn )
    {
        LdapDN parent = referrals.getParentElement( dn );
        
        return dn.equals( parent );
    }


    /**
     * {@inheritDoc}
     */
    public void removeReferral( LdapDN dn )
    {
        referrals.remove( dn );
    }
}
