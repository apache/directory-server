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
package org.apache.directory.server.core;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.directory.api.ldap.extras.controls.ad.TreeDelete;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAffectMultipleDsaException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.exception.LdapPartialResultException;
import org.apache.directory.api.ldap.model.exception.LdapReferralException;
import org.apache.directory.api.ldap.model.exception.LdapServiceUnavailableException;
import org.apache.directory.api.ldap.model.exception.LdapURLEncodingException;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.url.LdapUrl;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.OperationManager;
import org.apache.directory.server.core.api.ReferralManager;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The default implementation of an OperationManager.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultOperationManager implements OperationManager
{
    /** A logger specifically for operations */
    private static final Logger OPERATION_LOG = LoggerFactory.getLogger( Loggers.OPERATION_LOG.getName() );

    /** A logger specifically for operations time */
    private static final Logger OPERATION_TIME = LoggerFactory.getLogger( Loggers.OPERATION_TIME.getName() );

    /** A logger specifically for operations statistics */
    private static final Logger OPERATION_STAT = LoggerFactory.getLogger( Loggers.OPERATION_STAT.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = OPERATION_LOG.isDebugEnabled();
    private static final boolean IS_TIME = OPERATION_TIME.isDebugEnabled();
    private static final boolean IS_STAT = OPERATION_STAT.isDebugEnabled();

    /** The directory service instance */
    private final DirectoryService directoryService;

    /** A lock used to protect against concurrent operations */
    private ReadWriteLock rwLock = new ReentrantReadWriteLock( true );

    /** A reference to the ObjectClass AT */
    protected AttributeType objectClassAT;
    
    /** The nbChildren count attributeType */
    protected AttributeType nbChildrenAT;
    
    public DefaultOperationManager( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }


    /**
     * {@inheritDoc}
     */
    public ReadWriteLock getRWLock()
    {
        return rwLock;
    }


    /**
     * Acquires a ReadLock
     */
    public void lockRead()
    {
        rwLock.readLock().lock();
    }


    /**
     * Acquires a WriteLock
     */
    public void lockWrite()
    {
        rwLock.writeLock().lock();
    }


    /**
     * Releases a WriteLock
     */
    public void unlockWrite()
    {
        rwLock.writeLock().unlock();
    }


    /**
     * Releases a ReadLock
     */
    public void unlockRead()
    {
        rwLock.readLock().unlock();
    }


    /**
     * Eagerly populates fields of operation contexts so multiple Interceptors
     * in the processing pathway can reuse this value without performing a
     * redundant lookup operation.
     *
     * @param opContext the operation context to populate with cached fields
     */
    private void eagerlyPopulateFields( OperationContext opContext ) throws LdapException
    {
        // If the entry field is not set for ops other than add for example
        // then we set the entry but don't freak if we fail to do so since it
        // may not exist in the first place

        if ( opContext.getEntry() == null )
        {
            // We have to use the admin session here, otherwise we may have
            // trouble reading the entry due to insufficient access rights
            CoreSession adminSession = opContext.getSession().getDirectoryService().getAdminSession();

            LookupOperationContext lookupContext = new LookupOperationContext( adminSession, opContext.getDn(),
                SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            lookupContext.setPartition( opContext.getPartition() );
            lookupContext.setTransaction( opContext.getTransaction() );
            Entry foundEntry = opContext.getSession().getDirectoryService().getPartitionNexus().lookup( lookupContext );

            if ( foundEntry != null )
            {
                opContext.setEntry( foundEntry );
            }
            else
            {
                // This is an error : we *must* have an entry if we want to be able to rename.
                throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_00017_NO_SUCH_OBJECT, opContext.getDn() ) );
            }
        }
    }


    private Entry getOriginalEntry( OperationContext opContext ) throws LdapException
    {
        // We have to use the admin session here, otherwise we may have
        // trouble reading the entry due to insufficient access rights
        CoreSession adminSession = opContext.getSession().getDirectoryService().getAdminSession();

        Entry foundEntry = adminSession.lookup( opContext.getDn(), SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES,
            SchemaConstants.ALL_USER_ATTRIBUTES );

        if ( foundEntry != null )
        {
            return foundEntry;
        }
        else
        {
            // This is an error : we *must* have an entry if we want to be able to rename.
            throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_00017_NO_SUCH_OBJECT,
                opContext.getDn() ) );
        }
    }


    private LdapReferralException buildReferralException( Entry parentEntry, Dn childDn ) throws LdapException
    {
        // Get the Ref attributeType
        Attribute refs = parentEntry.get( SchemaConstants.REF_AT );

        List<String> urls = new ArrayList<>();

        try
        {
            // manage each Referral, building the correct URL for each of them
            for ( Value url : refs )
            {
                // we have to replace the parent by the referral
                LdapUrl ldapUrl = new LdapUrl( url.getString() );

                // We have a problem with the Dn : we can't use the UpName,
                // as we may have some spaces around the ',' and '+'.
                // So we have to take the Rdn one by one, and create a
                // new Dn with the type and value UP form

                Dn urlDn = ldapUrl.getDn().add( childDn );

                ldapUrl.setDn( urlDn );
                urls.add( ldapUrl.toString() );
            }
        }
        catch ( LdapURLEncodingException luee )
        {
            throw new LdapOperationErrorException( luee.getMessage(), luee );
        }

        // Return with an exception
        LdapReferralException lre = new LdapReferralException( urls );
        lre.setRemainingDn( childDn );
        lre.setResolvedDn( parentEntry.getDn() );
        lre.setResolvedObject( parentEntry );

        return lre;
    }


    private LdapReferralException buildReferralExceptionForSearch( Entry parentEntry, Dn childDn, SearchScope scope )
        throws LdapException
    {
        // Get the Ref attributeType
        Attribute refs = parentEntry.get( SchemaConstants.REF_AT );

        List<String> urls = new ArrayList<>();

        // manage each Referral, building the correct URL for each of them
        for ( Value url : refs )
        {
            // we have to replace the parent by the referral
            try
            {
                LdapUrl ldapUrl = new LdapUrl( url.getString() );

                StringBuilder urlString = new StringBuilder();

                if ( ( ldapUrl.getDn() == null ) || ( ldapUrl.getDn() == Dn.ROOT_DSE ) )
                {
                    ldapUrl.setDn( parentEntry.getDn() );
                }
                else
                {
                    // We have a problem with the Dn : we can't use the UpName,
                    // as we may have some spaces around the ',' and '+'.
                    // So we have to take the Rdn one by one, and create a
                    // new Dn with the type and value UP form

                    Dn urlDn = ldapUrl.getDn().add( childDn );

                    ldapUrl.setDn( urlDn );
                }

                urlString.append( ldapUrl.toString() ).append( "??" );

                switch ( scope )
                {
                    case OBJECT:
                        urlString.append( "base" );
                        break;

                    case SUBTREE:
                        urlString.append( "sub" );
                        break;

                    case ONELEVEL:
                        urlString.append( "one" );
                        break;

                    default:
                        throw new IllegalArgumentException( "Unexpected scope " + scope );
                }

                urls.add( urlString.toString() );
            }
            catch ( LdapURLEncodingException luee )
            {
                // The URL is not correct, returns it as is
                urls.add( url.getString() );
            }
        }

        // Return with an exception
        LdapReferralException lre = new LdapReferralException( urls );
        lre.setRemainingDn( childDn );
        lre.setResolvedDn( parentEntry.getDn() );
        lre.setResolvedObject( parentEntry );

        return lre;
    }


    private LdapPartialResultException buildLdapPartialResultException( Dn childDn )
    {
        LdapPartialResultException lpre = new LdapPartialResultException( I18n.err( I18n.ERR_00018_CANNOT_CREATE_REFERRAL_WHEN_IGNORE ) );

        lpre.setRemainingDn( childDn );
        lpre.setResolvedDn( Dn.EMPTY_DN );

        return lpre;
    }


    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> AddOperation : {}", addContext );
        }

        long addStart = 0L;

        if ( IS_TIME )
        {
            addStart = System.nanoTime();
        }

        ensureStarted();

        // Normalize the addContext Dn
        Dn dn = addContext.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            addContext.setDn( dn );
        }
        
        // Find the working partition
        Partition partition = directoryService.getPartitionNexus().getPartition( dn );
        addContext.setPartition( partition );
        
        // We have to deal with the referral first
        directoryService.getReferralManager().lockRead();

        try
        {
            if ( directoryService.getReferralManager().hasParentReferral( dn ) )
            {
                Entry parentEntry = directoryService.getReferralManager().getParentReferral( dn );
                Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                // Depending on the Context.REFERRAL property value, we will throw
                // a different exception.
                if ( addContext.isReferralIgnored() )
                {
                    throw buildLdapPartialResultException( childDn );
                }
                else
                {
                    throw buildReferralException( parentEntry, childDn );
                }
            }
        }
        finally
        {
            // Unlock the referral manager
            directoryService.getReferralManager().unlock();
        }

        // Call the Add method
        Interceptor head = directoryService.getInterceptor( addContext.getNextInterceptor() );

        lockWrite();

        // Start a Write transaction right away
        PartitionTxn transaction = addContext.getSession().getTransaction( partition ); 
        
        try
        {
            if ( transaction == null )
            {
                transaction = partition.beginWriteTransaction();
                
                if ( addContext.getSession().hasSessionTransaction() )
                {
                    addContext.getSession().addTransaction( partition, transaction );
                }
            }
            
            addContext.setTransaction( transaction );

            head.add( addContext );
            
            if ( !addContext.getSession().hasSessionTransaction() )
            {
                transaction.commit();
            }
        }
        catch ( LdapException le )
        {
            try
            {
                if ( transaction != null )
                {
                    transaction.abort();
                }
                
                throw le;
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
        catch ( IOException ioe )
        {
            try
            {
                transaction.abort();
                
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
            catch ( IOException ioe2 )
            {
                throw new LdapOtherException( ioe2.getMessage(), ioe2 );
            }
        }
        finally
        {
            unlockWrite();
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< AddOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "Add operation took {} ns", ( System.nanoTime() - addStart ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> BindOperation : {}", bindContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        // Call the Delete method
        Interceptor head = directoryService.getInterceptor( bindContext.getNextInterceptor() );

        // Normalize the addContext Dn
        Dn dn = bindContext.getDn();
        
        if ( ( dn != null ) && !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            bindContext.setDn( dn );
        }

        lockRead();

        try
        {
            Partition partition = directoryService.getPartitionNexus().getPartition( dn );
            
            try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
            {
                bindContext.setPartition( partition );
                bindContext.setTransaction( partitionTxn );
                
                head.bind( bindContext );
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
        finally
        {
            unlockRead();
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< BindOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "Bind operation took {} ns", ( System.nanoTime() - opStart )  );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> CompareOperation : {}", compareContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();
        
        // Normalize the compareContext Dn
        Dn dn = compareContext.getDn();

        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            compareContext.setDn( dn );
        }

        // We have to deal with the referral first
        directoryService.getReferralManager().lockRead();

        try
        {
            // Check if we have an ancestor for this Dn
            Entry parentEntry = directoryService.getReferralManager().getParentReferral( dn );

            if ( parentEntry != null )
            {
                // We have found a parent referral for the current Dn
                Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !compareContext.isReferralIgnored() )
                    {
                        // Throw a Referral Exception
                        throw buildReferralException( parentEntry, childDn );
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( compareContext.isReferralIgnored() )
                    {
                        throw buildLdapPartialResultException( childDn );
                    }
                    else
                    {
                        throw buildReferralException( parentEntry, childDn );
                    }
                }
            }
        }
        finally
        {
            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();
        }

        // populate the context with the old entry
        compareContext.setOriginalEntry( getOriginalEntry( compareContext ) );

        // Call the Compare method
        Interceptor head = directoryService.getInterceptor( compareContext.getNextInterceptor() );

        boolean result = false;

        lockRead();

        try
        {
            Partition partition = directoryService.getPartitionNexus().getPartition( dn );
            
            try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
            {
                compareContext.setPartition( partition );
                compareContext.setTransaction( partitionTxn );
                
                result = head.compare( compareContext );
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
        finally
        {
            unlockRead();
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< CompareOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "Compare operation took {} ns", ( System.nanoTime() - opStart ) );
        }

        return result;
    }
    
    
    private void deleteEntry( DeleteOperationContext deleteContext, Dn dn ) throws LdapException
    {
        DeleteOperationContext entryDeleteContext = 
            new DeleteOperationContext( deleteContext.getSession(), dn );
        entryDeleteContext.setTransaction( deleteContext.getTransaction() );

        eagerlyPopulateFields( entryDeleteContext );
        
        // Call the Delete method
        Interceptor head = directoryService.getInterceptor( deleteContext.getNextInterceptor() );

        head.delete( entryDeleteContext );
    }
    
    
    private void processTreeDelete( DeleteOperationContext deleteContext, Dn dn ) throws LdapException, CursorException
    {
        objectClassAT = directoryService.getSchemaManager().getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        nbChildrenAT = directoryService.getSchemaManager().getAttributeType( ApacheSchemaConstants.NB_CHILDREN_OID );

        // This is a depth first recursive operation
        PresenceNode filter = new PresenceNode( objectClassAT );
        SearchOperationContext searchContext = new SearchOperationContext( 
            deleteContext.getSession(), 
            dn, 
            SearchScope.ONELEVEL, filter,
            ApacheSchemaConstants.NB_CHILDREN_OID );
        searchContext.setTransaction( deleteContext.getTransaction() );

        EntryFilteringCursor cursor = search( searchContext );
        
        cursor.beforeFirst();
        
        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            
            if ( Integer.parseInt( entry.get( nbChildrenAT ).getString() ) == 0 )
            {
                // We can delete the entry
                deleteEntry( deleteContext, entry.getDn() );
            }
            else
            {
                // Recurse
                processTreeDelete( deleteContext, entry.getDn() );
            }
        }
        
        // Done with the children, we can delete the entry
        // We can delete the entry
        deleteEntry( deleteContext, dn );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> DeleteOperation : {}", deleteContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        // Normalize the deleteContext Dn
        Dn dn = deleteContext.getDn();
        Partition partition = directoryService.getPartitionNexus().getPartition( dn );
        deleteContext.setPartition( partition );

        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            deleteContext.setDn( dn );
        }

        // We have to deal with the referral first
        directoryService.getReferralManager().lockRead();

        try
        {
            Entry parentEntry = directoryService.getReferralManager().getParentReferral( dn );

            if ( parentEntry != null )
            {
                // We have found a parent referral for the current Dn
                Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !deleteContext.isReferralIgnored() )
                    {
                        // Throw a Referral Exception
                        throw buildReferralException( parentEntry, childDn );
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't delete an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( deleteContext.isReferralIgnored() )
                    {
                        throw buildLdapPartialResultException( childDn );
                    }
                    else
                    {
                        throw buildReferralException( parentEntry, childDn );
                    }
                }
            }
        }
        finally
        {
            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();
        }

        // populate the context with the old entry
        lockWrite();

        // Start a Write transaction right away
        PartitionTxn transaction = deleteContext.getSession().getTransaction( partition ); 
        
        try
        {
            if ( transaction == null )
            {
                transaction = partition.beginWriteTransaction();
                
                if ( deleteContext.getSession().hasSessionTransaction() )
                {
                    deleteContext.getSession().addTransaction( partition, transaction );
                }
            }
            
            deleteContext.setTransaction( transaction );

            // Check if the TreeDelete control is used
            if ( deleteContext.hasRequestControl( TreeDelete.OID ) )
            {
                try
                {
                    processTreeDelete( deleteContext, deleteContext.getDn() );

                    if ( !deleteContext.getSession().hasSessionTransaction() )
                    {
                        transaction.commit();
                    }
                }
                catch ( CursorException ce )
                {
                    try
                    {
                        if ( transaction != null )
                        {
                            transaction.abort();
                        }
                        
                        throw new LdapOtherException( ce.getMessage(), ce );
                    }
                    catch ( IOException ioe )
                    {
                        throw new LdapOtherException( ioe.getMessage(), ioe );
                    }
                }
            }
            else
            {
                eagerlyPopulateFields( deleteContext );
    
                // Call the Delete method
                Interceptor head = directoryService.getInterceptor( deleteContext.getNextInterceptor() );
    
                head.delete( deleteContext );
    
                if ( !deleteContext.getSession().hasSessionTransaction() )
                {
                    transaction.commit();
                }
            }
        }
        catch ( LdapException le )
        {
            try
            {
                if ( transaction != null )
                {
                    transaction.abort();
                }
                
                throw le;
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
        catch ( IOException ioe )
        {
            try
            {
                transaction.abort();
                
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
            catch ( IOException ioe2 )
            {
                throw new LdapOtherException( ioe2.getMessage(), ioe2 );
            }
        }
        finally
        {
            unlockWrite();
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< DeleteOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "Delete operation took {} ns", ( System.nanoTime() - opStart ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public Entry getRootDse( GetRootDseOperationContext getRootDseContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> GetRootDseOperation : {}", getRootDseContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        Interceptor head = directoryService.getInterceptor( getRootDseContext.getNextInterceptor() );
        Entry root;

        try
        {
            lockRead();
            
            Partition partition = directoryService.getPartitionNexus().getPartition( Dn.ROOT_DSE );
            
            try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
            {
                getRootDseContext.setPartition( partition );
                getRootDseContext.setTransaction( partitionTxn );
                
                root = head.getRootDse( getRootDseContext );
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
        finally
        {
            unlockRead();
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< getRootDseOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "GetRootDSE operation took {} ns", ( System.nanoTime() - opStart ) );
        }

        return root;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> hasEntryOperation : {}", hasEntryContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        Interceptor head = directoryService.getInterceptor( hasEntryContext.getNextInterceptor() );

        boolean result = false;

        lockRead();

        // Normalize the addContext Dn
        Dn dn = hasEntryContext.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            hasEntryContext.setDn( dn );
        }

        try
        {
            Partition partition = directoryService.getPartitionNexus().getPartition( dn );

            try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
            {
                hasEntryContext.setPartition( partition );
                hasEntryContext.setTransaction( partitionTxn );

                result = head.hasEntry( hasEntryContext );
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
        finally
        {
            unlockRead();
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< HasEntryOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "HasEntry operation took {} ns", ( System.nanoTime() - opStart ) );
        }

        return result;
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> LookupOperation : {}", lookupContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        Interceptor head = directoryService.getInterceptor( lookupContext.getNextInterceptor() );

        Entry entry = null;

        // Normalize the modifyContext Dn
        Dn dn = lookupContext.getDn();

        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            lookupContext.setDn( dn );
        }
        
        Partition partition = directoryService.getPartitionNexus().getPartition( dn );
        lookupContext.setPartition( partition );
        
        // Start a read transaction right away
        try ( PartitionTxn transaction = partition.beginReadTransaction() )
        {
            lookupContext.setTransaction( transaction );

            lockRead();
    
            try
            {
                entry = head.lookup( lookupContext );
            }
            finally
            {
                unlockRead();
            }
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< LookupOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "Lookup operation took {} ns", ( System.nanoTime() - opStart ) );
        }

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> ModifyOperation : {}", modifyContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        // Normalize the modifyContext Dn
        Dn dn = modifyContext.getDn();

        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            modifyContext.setDn( dn );
        }

        ReferralManager referralManager = directoryService.getReferralManager();

        // We have to deal with the referral first
        referralManager.lockRead();

        try
        {
            // Check if we have an ancestor for this Dn
            Entry parentEntry = referralManager.getParentReferral( dn );

            if ( parentEntry != null )
            {
                if ( referralManager.isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !modifyContext.isReferralIgnored() )
                    {
                        // Throw a Referral Exception
                        // We have found a parent referral for the current Dn
                        Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                        throw buildReferralException( parentEntry, childDn );
                    }
                }
                else if ( referralManager.hasParentReferral( dn ) )
                {
                    // We can't delete an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( modifyContext.isReferralIgnored() )
                    {
                        // We have found a parent referral for the current Dn
                        Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                        throw buildLdapPartialResultException( childDn );
                    }
                    else
                    {
                        // We have found a parent referral for the current Dn
                        Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                        throw buildReferralException( parentEntry, childDn );
                    }
                }
            }
        }
        finally
        {
            // Unlock the ReferralManager
            referralManager.unlock();
        }
        
        Partition partition = directoryService.getPartitionNexus().getPartition( dn );
        modifyContext.setPartition( partition );
        
        lockWrite();
        
        // Start a Write transaction right away
        PartitionTxn transaction = modifyContext.getSession().getTransaction( partition ); 

        try
        {
            if ( transaction == null )
            {
                transaction = partition.beginWriteTransaction();
                
                if ( modifyContext.getSession().hasSessionTransaction() )
                {
                    modifyContext.getSession().addTransaction( partition, transaction );
                }
            }

            modifyContext.setTransaction( transaction );

            // populate the context with the old entry
            eagerlyPopulateFields( modifyContext );

            // Call the Modify method
            Interceptor head = directoryService.getInterceptor( modifyContext.getNextInterceptor() );

            head.modify( modifyContext );
            
            if ( !modifyContext.getSession().hasSessionTransaction() )
            {
                transaction.commit();
            }
        }
        catch ( LdapException le )
        {
            try 
            {
                if ( transaction != null )
                {
                    transaction.abort();
                }
                
                throw le;
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
        catch ( IOException ioe )
        {
            try 
            {
                transaction.abort();
                
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
            catch ( IOException ioe2 )
            {
                throw new LdapOtherException( ioe2.getMessage(), ioe2 );
            }
        }
        finally
        {
            unlockWrite();
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< ModifyOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "Modify operation took {} ns", ( System.nanoTime() - opStart ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> MoveOperation : {}", moveContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        // Normalize the moveContext Dn
        Dn dn = moveContext.getDn();

        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            moveContext.setDn( dn );
        }

        // Normalize the moveContext superior Dn
        Dn newSuperiorDn = moveContext.getNewSuperior();

        if ( !newSuperiorDn.isSchemaAware() )
        {
            newSuperiorDn = new Dn( directoryService.getSchemaManager(), newSuperiorDn );
            moveContext.setNewSuperior( newSuperiorDn );
        }

        // We have to deal with the referral first
        directoryService.getReferralManager().lockRead();

        try
        {
            // Check if we have an ancestor for this Dn
            Entry parentEntry = directoryService.getReferralManager().getParentReferral( dn );

            if ( parentEntry != null )
            {
                // We have found a parent referral for the current Dn
                Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !moveContext.isReferralIgnored() )
                    {
                        // Throw a Referral Exception
                        throw buildReferralException( parentEntry, childDn );
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't delete an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( moveContext.isReferralIgnored() )
                    {
                        throw buildLdapPartialResultException( childDn );
                    }
                    else
                    {
                        throw buildReferralException( parentEntry, childDn );
                    }
                }
            }

            // Now, check the destination
            // If he parent Dn is a referral, or has a referral ancestor, we have to issue a AffectMultipleDsas result
            // as stated by RFC 3296 Section 5.6.2
            if ( directoryService.getReferralManager().isReferral( newSuperiorDn )
                || directoryService.getReferralManager().hasParentReferral( newSuperiorDn ) )
            {
                throw new LdapAffectMultipleDsaException();
            }

        }
        finally
        {
            // Unlock the referral manager
            directoryService.getReferralManager().unlock();
        }

        lockWrite();
        
        // Find the working partition
        Partition partition = directoryService.getPartitionNexus().getPartition( dn );
        moveContext.setPartition( partition );

        // Start a Write transaction right away
        PartitionTxn transaction = moveContext.getSession().getTransaction( partition ); 
        
        try
        {
            if ( transaction == null )
            {
                transaction = partition.beginWriteTransaction();
                
                if ( moveContext.getSession().hasSessionTransaction() )
                {
                    moveContext.getSession().addTransaction( partition, transaction );
                }
            }
        
            moveContext.setTransaction( transaction );
            Entry originalEntry = getOriginalEntry( moveContext );

            moveContext.setOriginalEntry( originalEntry );

            // Call the Move method
            Interceptor head = directoryService.getInterceptor( moveContext.getNextInterceptor() );

            head.move( moveContext );
            
            if ( !moveContext.getSession().hasSessionTransaction() )
            {
                transaction.commit();
            }
        }
        catch ( LdapException le )
        {
            try
            {
                if ( transaction != null )
                {
                    transaction.abort();
                }
                
                throw le;
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
        catch ( IOException ioe )
        {
            try
            {
                if ( transaction != null )
                {
                    transaction.abort();
                }
                
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
            catch ( IOException ioe2 )
            {
                throw new LdapOtherException( ioe2.getMessage(), ioe2 );
            }
        }
        finally
        {
            unlockWrite();
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< MoveOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "Move operation took {} ns", ( System.nanoTime() - opStart ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> MoveAndRenameOperation : {}", moveAndRenameContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        // Normalize the moveAndRenameContext Dn
        Dn dn = moveAndRenameContext.getDn();

        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            moveAndRenameContext.setDn( dn );
        }

        // We have to deal with the referral first
        directoryService.getReferralManager().lockRead();

        try
        {
            // Check if we have an ancestor for this Dn
            Entry parentEntry = directoryService.getReferralManager().getParentReferral( dn );

            if ( parentEntry != null )
            {
                // We have found a parent referral for the current Dn
                Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !moveAndRenameContext.isReferralIgnored() )
                    {
                        // Throw a Referral Exception
                        throw buildReferralException( parentEntry, childDn );
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't delete an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( moveAndRenameContext.isReferralIgnored() )
                    {
                        throw buildLdapPartialResultException( childDn );
                    }
                    else
                    {
                        throw buildReferralException( parentEntry, childDn );
                    }
                }
            }

            // Now, check the destination
            // Normalize the moveAndRenameContext Dn
            Dn newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();

            if ( !newSuperiorDn.isSchemaAware() )
            {
                newSuperiorDn = new Dn( directoryService.getSchemaManager(), newSuperiorDn );
                moveAndRenameContext.setNewSuperiorDn( newSuperiorDn );
            }

            // If he parent Dn is a referral, or has a referral ancestor, we have to issue a AffectMultipleDsas result
            // as stated by RFC 3296 Section 5.6.2
            if ( directoryService.getReferralManager().isReferral( newSuperiorDn )
                || directoryService.getReferralManager().hasParentReferral( newSuperiorDn ) )
            {
                // The parent Dn is a referral, we have to issue a AffectMultipleDsas result
                // as stated by RFC 3296 Section 5.6.2
                throw new LdapAffectMultipleDsaException();
            }
        }
        finally
        {
            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();
        }

        // Find the working partition
        Partition partition = directoryService.getPartitionNexus().getPartition( dn );
        moveAndRenameContext.setPartition( partition );

        lockWrite();
        
        // Start a Write transaction right away
        PartitionTxn transaction = moveAndRenameContext.getSession().getTransaction( partition ); 
        
        try
        {
            if ( transaction == null )
            {
                transaction = partition.beginWriteTransaction();
                
                if ( moveAndRenameContext.getSession().hasSessionTransaction() )
                {
                    moveAndRenameContext.getSession().addTransaction( partition, transaction );
                }
            }

            moveAndRenameContext.setOriginalEntry( getOriginalEntry( moveAndRenameContext ) );
            moveAndRenameContext.setModifiedEntry( moveAndRenameContext.getOriginalEntry().clone() );
            moveAndRenameContext.setTransaction( transaction );

            // Call the MoveAndRename method
            Interceptor head = directoryService.getInterceptor( moveAndRenameContext.getNextInterceptor() );

            head.moveAndRename( moveAndRenameContext );

            if ( !moveAndRenameContext.getSession().hasSessionTransaction() )
            {
                transaction.commit();
            }
        }
        catch ( LdapException le )
        {
            try
            {
                if ( transaction != null )
                {
                    transaction.abort();
                }
                
                throw le;
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
        catch ( IOException ioe )
        {
            try
            {
                transaction.abort();
                
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
            catch ( IOException ioe2 )
            {
                throw new LdapOtherException( ioe2.getMessage(), ioe2 );
            }
        }
        finally
        {
            unlockWrite();
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< MoveAndRenameOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "MoveAndRename operation took {} ns", ( System.nanoTime() - opStart ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> RenameOperation : {}", renameContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        // Normalize the renameContext Dn
        Dn dn = renameContext.getDn();

        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            renameContext.setDn( dn );
        }

        // Inject the newDn into the operation context
        // Inject the new Dn into the context
        if ( !dn.isEmpty() )
        {
            Dn newDn = dn.getParent();
            Rdn newRdn = renameContext.getNewRdn();
            
            if ( !newRdn.isSchemaAware() )
            {
                newRdn = new Rdn( directoryService.getSchemaManager(), newRdn );
                renameContext.setNewRdn( newRdn );
            }
            
            newDn = newDn.add( renameContext.getNewRdn() );
            renameContext.setNewDn( newDn );
        }

        // We have to deal with the referral first
        directoryService.getReferralManager().lockRead();

        try
        {
            // Check if we have an ancestor for this Dn
            Entry parentEntry = directoryService.getReferralManager().getParentReferral( dn );

            if ( parentEntry != null )
            {
                // We have found a parent referral for the current Dn
                Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !renameContext.isReferralIgnored() )
                    {
                        // Throw a Referral Exception
                        throw buildReferralException( parentEntry, childDn );
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't delete an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( renameContext.isReferralIgnored() )
                    {
                        throw buildLdapPartialResultException( childDn );
                    }
                    else
                    {
                        throw buildReferralException( parentEntry, childDn );
                    }
                }
            }
        }
        finally
        {
            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();
        }

        lockWrite();

        Partition partition = directoryService.getPartitionNexus().getPartition( dn );

        // Start a Write transaction right away
        PartitionTxn transaction = renameContext.getSession().getTransaction( partition ); 
        
        // Call the rename method
        try
        {
            if ( transaction == null )
            {
                transaction = partition.beginWriteTransaction();
                
                if ( renameContext.getSession().hasSessionTransaction() )
                {
                    renameContext.getSession().addTransaction( partition, transaction );
                }
            }

            renameContext.setPartition( partition );

            // populate the context with the old entry
            PartitionTxn partitionTxn = null;
            
            try
            {
                partitionTxn = partition.beginReadTransaction();
                
                renameContext.setTransaction( partitionTxn );
                
                eagerlyPopulateFields( renameContext );
            }
            finally
            {
                try
                {
                    // Nothing to do
                    if ( partitionTxn != null )
                    {
                        partitionTxn.close();
                    }
                }
                catch ( IOException ioe )
                {
                    throw new LdapOtherException( ioe.getMessage(), ioe );
                }
            }

            Entry originalEntry = getOriginalEntry( renameContext );
            renameContext.setOriginalEntry( originalEntry );
            renameContext.setModifiedEntry( originalEntry.clone() );
            Interceptor head = directoryService.getInterceptor( renameContext.getNextInterceptor() );

            // Start a Write transaction right away
            transaction = renameContext.getSession().getTransaction( partition ); 
            
            // Call the Rename method
            try
            {
                if ( transaction == null )
                {
                    transaction = partition.beginWriteTransaction();
                    
                    if ( renameContext.getSession().hasSessionTransaction() )
                    {
                        renameContext.getSession().addTransaction( partition, transaction );
                    }
                }

                renameContext.setTransaction( transaction );

                head.rename( renameContext );
                
                if ( !renameContext.getSession().hasSessionTransaction() )
                {
                    transaction.commit();
                }
            }
            catch ( LdapException le )
            {
                try
                {
                    if ( transaction != null )
                    {
                        transaction.abort();
                    }
                    
                    throw le;
                }
                catch ( IOException ioe )
                {
                    throw new LdapOtherException( ioe.getMessage(), ioe );
                }
            }
            catch ( IOException ioe )
            {
                try
                {
                    if ( transaction != null )
                    {
                        transaction.abort();
                    }
                    
                    throw new LdapOtherException( ioe.getMessage(), ioe );
                }
                catch ( IOException ioe2 )
                {
                    throw new LdapOtherException( ioe2.getMessage(), ioe2 );
                }
            }
        }
        finally
        {
            unlockWrite();
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< RenameOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "Rename operation took {} ns", ( System.nanoTime() - opStart ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> SearchOperation : {}", searchContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        // Normalize the searchContext Dn
        Dn dn = searchContext.getDn();

        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( directoryService.getSchemaManager(), dn );
            searchContext.setDn( dn );
        }

        // We have to deal with the referral first
        directoryService.getReferralManager().lockRead();

        try
        {
            // Check if we have an ancestor for this Dn
            Entry parentEntry = directoryService.getReferralManager().getParentReferral( dn );

            if ( parentEntry != null )
            {
                // We have found a parent referral for the current Dn
                Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can return it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !searchContext.isReferralIgnored() )
                    {
                        // Throw a Referral Exception
                        throw buildReferralExceptionForSearch( parentEntry, childDn, searchContext.getScope() );
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't search an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( searchContext.isReferralIgnored() )
                    {
                        throw buildLdapPartialResultException( childDn );
                    }
                    else
                    {
                        throw buildReferralExceptionForSearch( parentEntry, childDn, searchContext.getScope() );
                    }
                }
            }
        }
        finally
        {
            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();
        }

        // Call the Search method
        Interceptor head = directoryService.getInterceptor( searchContext.getNextInterceptor() );

        EntryFilteringCursor cursor = null;
        Partition partition = directoryService.getPartitionNexus().getPartition( dn );
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            searchContext.setPartition( partition );
            searchContext.setTransaction( partitionTxn );
            lockRead();
    
            try
            {
                cursor = head.search( searchContext );
            }
            finally
            {
                unlockRead();
            }
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< SearchOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "Search operation took {} ns", ( System.nanoTime() - opStart ) );
        }

        return cursor;
    }


    /**
     * {@inheritDoc}
     */
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( ">> UnbindOperation : {}", unbindContext );
        }

        long opStart = 0L;

        if ( IS_TIME )
        {
            opStart = System.nanoTime();
        }

        ensureStarted();

        // Call the Unbind method
        Interceptor head = directoryService.getInterceptor( unbindContext.getNextInterceptor() );

        head.unbind( unbindContext );

        if ( IS_DEBUG )
        {
            OPERATION_LOG.debug( "<< UnbindOperation successful" );
        }

        if ( IS_TIME )
        {
            OPERATION_TIME.debug( "Unbind operation took {} ns", ( System.nanoTime() - opStart ) );
        }
    }


    private void ensureStarted() throws LdapServiceUnavailableException
    {
        if ( !directoryService.isStarted() )
        {
            throw new LdapServiceUnavailableException( ResultCodeEnum.UNAVAILABLE, I18n.err( I18n.ERR_00019_DIRECTORY_SERVICE_NOT_STARTED ) );
        }
    }
}
