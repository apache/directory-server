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
package org.apache.directory.server.core.referral;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.ReferralManager;
import org.apache.directory.server.core.ReferralManagerImpl;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An service which is responsible referral handling behavoirs.  It manages 
 * referral handling behavoir when the {@link Context#REFERRAL} is implicitly
 * or explicitly set to "ignore", when set to "throw" and when set to "follow". 
 * 
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReferralInterceptor extends BaseInterceptor
{
    private static final Logger LOG = LoggerFactory.getLogger( ReferralInterceptor.class );

    private PartitionNexus nexus;

    /** The global schemaManager */
    private SchemaManager schemaManager;

    /** The referralManager */
    private ReferralManager referralManager;

    /** A normalized form for the SubschemaSubentry DN */
    private String subschemaSubentryDnNorm;

    
    static private void checkRefAttributeValue( Value<?> value ) throws NamingException, LdapURLEncodingException
    {
        StringValue ref = ( StringValue ) value;

        String refVal = ref.getString();

        LdapURL ldapUrl = new LdapURL( refVal );

        // We have a LDAP URL, we have to check that :
        // - we don't have scope specifier
        // - we don't have filters
        // - we don't have attribute description list
        // - we don't have extensions
        // - the DN is not empty

        if ( ldapUrl.getScope() != SearchScope.OBJECT )
        {
            // This is the default value if we don't have any scope
            // Let's assume that it's incorrect if we get something
            // else in the LdapURL
            String message = I18n.err( I18n.ERR_36 );
            LOG.error( message );
            throw new NamingException( message );
        }

        if ( !StringTools.isEmpty( ldapUrl.getFilter() ) )
        {
            String message = I18n.err( I18n.ERR_37 );
            LOG.error( message );
            throw new NamingException( message );
        }

        if ( ( ldapUrl.getAttributes() != null ) && ( ldapUrl.getAttributes().size() != 0 ) )
        {
            String message = I18n.err( I18n.ERR_38 );
            LOG.error( message );
            throw new NamingException( message );
        }

        if ( ( ldapUrl.getExtensions() != null ) && ( ldapUrl.getExtensions().size() != 0 ) )
        {
            String message = I18n.err( I18n.ERR_39 );
            LOG.error( message );
            throw new NamingException( message );
        }

        if ( ( ldapUrl.getExtensions() != null ) && ( ldapUrl.getExtensions().size() != 0 ) )
        {
            String message = I18n.err( I18n.ERR_40 );
            LOG.error( message );
            throw new NamingException( message );
        }

        DN dn = ldapUrl.getDn();

        if ( ( dn == null ) || dn.isEmpty() )
        {
            String message = I18n.err( I18n.ERR_41 );
            LOG.error( message );
            throw new NamingException( message );
        }
    }

    
    static private boolean isReferral( ServerEntry entry ) throws NamingException
    {
        // Check that the entry is not null, otherwise return FALSE.
        // This is typically to cover the case where the entry has not 
        // been added into the context because it does not exists.
        if ( entry == null )
        {
            return false;
        }
        
        EntryAttribute oc = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( oc == null )
        {
            LOG.warn( "could not find objectClass attribute in entry: " + entry );
            return false;
        }

        if ( !oc.contains( SchemaConstants.REFERRAL_OC ) )
        {
            return false;
        }
        else
        {
            // We have a referral ObjectClass, let's check that the ref is
            // valid, accordingly to the RFC

            // Get the 'ref' attributeType
            EntryAttribute refAttr = entry.get( SchemaConstants.REF_AT );

            if ( refAttr == null )
            {
                // very unlikely, as we have already checked the entry in SchemaInterceptor
                String message = I18n.err( I18n.ERR_42 );
                LOG.error( message );
                throw new NamingException( message );
            }

            for ( Value<?> value : refAttr )
            {
                try
                {
                    checkRefAttributeValue( value );
                }
                catch ( LdapURLEncodingException luee )
                {
                    // Either the URL is invalid, or it's not a LDAP URL.
                    // we will just ignore this LdapURL.
                }
            }

            return true;
        }
    }


    public void init( DirectoryService directoryService ) throws Exception
    {
        nexus = directoryService.getPartitionNexus();
        schemaManager = directoryService.getSchemaManager();

        // Initialize the referralManager
        referralManager = new ReferralManagerImpl( directoryService );
        directoryService.setReferralManager( referralManager );

        Value<?> subschemaSubentry = nexus.getRootDSE( null ).get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).get();
        DN subschemaSubentryDn = new DN( subschemaSubentry.getString() );
        subschemaSubentryDn.normalize( schemaManager.getNormalizerMapping() );
        subschemaSubentryDnNorm = subschemaSubentryDn.getNormName();
    }


    /**
     * Add an entry into the server. We have 3 cases :
     * (1) The entry does not have any parent referral and is not a referral itself
     * (2) The entry does not have any parent referral and is a referral itself
     * (3) The entry has a parent referral
     * 
     * Case (1) is easy : we inject the entry into the server and we are done.
     * Case (2) is the same as case (1), but we have to update the referral manager.
     * Case (3) is handled by the LdapProcotol handler, as we have to return a 
     * LdapResult containing a list of this entry's parent's referrals URL, if the 
     * ManageDSAIT control is not present, or the parent's entry if the control 
     * is present. 
     * 
     * Of course, if the entry already exists, nothing will be done, as we will get an
     * entryAlreadyExists error.
     *  
     */
    public void add( NextInterceptor next, AddOperationContext opContext ) throws Exception
    {
        ServerEntry entry = opContext.getEntry();
        
        // Check if the entry is a referral itself
        boolean isReferral = isReferral( entry );

        // We add the entry into the server
        next.add( opContext );
        
        // If the addition is successful, we update the referralManager 
        if ( isReferral )
        {
            // We have to add it to the referralManager
            referralManager.lockWrite();

            referralManager.addReferral( entry );

            referralManager.unlock();
        }

    }


    /**
     * Delete an entry in the server. We have 4 cases :
     * (1) the entry is not a referral and does not have a parent referral
     * (2) the entry is not a referral but has a parent referral
     * (3) the entry is a referral
     * 
     * Case (1) is handled by removing the entry from the server
     * In case (2), we return an exception build using the parent referral 
     * For case(3), we remove the entry from the server and remove the referral
     * from the referral manager.
     * 
     * If the entry does not exist in the server, we will get a NoSuchObject error
     */
    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws Exception
    {
        ServerEntry entry = opContext.getEntry();

        // First delete the entry into the server
        next.delete( opContext );
        
        // Check if the entry exists and is a referral itself
        // If so, we have to update the referralManager
        if ( ( entry != null ) && isReferral( entry ) )
        {
            // We have to remove it from the referralManager
            referralManager.lockWrite();

            referralManager.removeReferral( entry );

            referralManager.unlock();
        }
    }


    /**
     * 
     **/
    public void move( NextInterceptor next, MoveOperationContext opContext ) throws Exception
    {
        DN oldName = opContext.getDn();

        DN newName = ( DN ) opContext.getParent().clone();
        newName.add( oldName.get( oldName.size() - 1 ) );

        // Check if the entry is a referral itself
        boolean isReferral = isReferral( opContext.getEntry() );

        next.move( opContext );
        
        
        if ( isReferral ) 
        {
            // Update the referralManager
            LookupOperationContext lookupContext = new LookupOperationContext( opContext.getSession(), newName );
            
            ServerEntry newEntry = nexus.lookup( lookupContext );
            
            referralManager.lockWrite();
            
            referralManager.addReferral( newEntry );
            referralManager.removeReferral( opContext.getEntry() );
            
            referralManager.unlock();
        }
    }


    /**
     * 
     **/
    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext ) throws Exception
    {
        DN newName = ( DN ) opContext.getParent().clone();
        newName.add( opContext.getNewRdn() );

        // Check if the entry is a referral itself
        boolean isReferral = isReferral( opContext.getEntry() );

        next.moveAndRename( opContext );
        
        if ( isReferral ) 
        {
            // Update the referralManager
            LookupOperationContext lookupContext = new LookupOperationContext( opContext.getSession(), newName );
            
            ServerEntry newEntry = nexus.lookup( lookupContext );
            
            referralManager.lockWrite();
            
            referralManager.addReferral( newEntry );
            referralManager.removeReferral( opContext.getEntry() );
            
            referralManager.unlock();
        }
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws Exception
    {
        // Check if the entry is a referral itself
        boolean isReferral = isReferral( opContext.getEntry() );

        next.rename( opContext );
        
        if ( isReferral ) 
        {
            // Update the referralManager
            LookupOperationContext lookupContext = new LookupOperationContext( opContext.getSession(), opContext.getNewDn() );
            
            ServerEntry newEntry = nexus.lookup( lookupContext );
            
            referralManager.lockWrite();
            
            referralManager.addReferral( newEntry );
            referralManager.removeReferral( opContext.getEntry().getOriginalEntry() );
            
            referralManager.unlock();
        }
    }
    

    /**
     * Modify an entry in the server.
     */
    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws Exception
    {
        DN name = opContext.getDn();
        
        // handle a normal modify without following referrals
        next.modify( opContext );

        // Check if we are trying to modify the schema or the rootDSE,
        // if so, we don't modify the referralManager
        if ( ( name == DN.EMPTY_DN ) || ( subschemaSubentryDnNorm.equals( name.getNormName() ) ) )
        {
            // Do nothing
            return;
        }

        // Update the referralManager. We have to read the entry again
        // as it has been modified, before updating the ReferralManager
        // TODO: this can be spare, as we build the entry later.
        // But we will have to store the modified entry into the opContext
        LookupOperationContext lookupContext = new LookupOperationContext( opContext.getSession(), name );
        
        ServerEntry newEntry = nexus.lookup( lookupContext );

        // Check that we have the entry, just in case
        // TODO : entries should be locked until the operation is done on it.
        if ( newEntry != null )
        {
            referralManager.lockWrite();

            if ( referralManager.isReferral( newEntry.getDn() ) )
            {
                referralManager.removeReferral( opContext.getEntry() );
                referralManager.addReferral( newEntry );
            }
            
            referralManager.unlock();
        }
    }


    /**
     * When adding a new context partition, we have to update the referralManager
     * by injecting all the new referrals into it. This is done using the init()
     * method of the referralManager.
     *
    public void addContextPartition( NextInterceptor next, AddContextPartitionOperationContext opContext )
        throws Exception
    {
        // First, inject the partition
        next.addContextPartition( opContext );

        Partition partition = opContext.getPartition();
        DN suffix = partition.getSuffixDn();
        
        // add referrals immediately after adding the new partition
        referralManager.init( directoryService, new String[]{ suffix.getNormName() } );
    }


    /**
     * Remove a partion's referrals from the server. We have to first
     * clear the referrals manager from all of this partition's referrals,
     * then we can delete the partition.
     *
    public void removeContextPartition( NextInterceptor next, RemoveContextPartitionOperationContext opContext )
        throws Exception
    {
        // get the partition suffix
        DN suffix = opContext.getDn();

        // remove referrals immediately before removing the partition
        referralManager.remove( directoryService, suffix );

        // And remove the partition from the server
        next.removeContextPartition( opContext );
    }*/
}
