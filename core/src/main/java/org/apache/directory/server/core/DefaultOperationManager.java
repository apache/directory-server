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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.OperationManager;
import org.apache.directory.server.core.api.ReferralManager;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.InterceptorChain;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.invocation.InvocationStack;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapAffectMultipleDsaException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.model.exception.LdapPartialResultException;
import org.apache.directory.shared.ldap.model.exception.LdapReferralException;
import org.apache.directory.shared.ldap.model.exception.LdapServiceUnavailableException;
import org.apache.directory.shared.ldap.model.exception.LdapURLEncodingException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.url.LdapUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The default implementation of an OperationManager.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultOperationManager implements OperationManager
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultOperationManager.class );

    /** A logger specifically for change operations */
    private static final Logger LOG_CHANGES = LoggerFactory.getLogger( "LOG_CHANGES" );

    /** The directory service instance */
    private final DirectoryService directoryService;


    public DefaultOperationManager( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
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

            LookupOperationContext lookupContext = new LookupOperationContext( adminSession, opContext.getDn(), SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            Entry foundEntry = opContext.getSession().getDirectoryService().getPartitionNexus().lookup( lookupContext );

            if ( foundEntry != null )
            {
                opContext.setEntry( foundEntry );
            }
            else
            {
                // This is an error : we *must* have an entry if we want to be able to rename.
                LdapNoSuchObjectException ldnfe = new LdapNoSuchObjectException( I18n.err( I18n.ERR_256_NO_SUCH_OBJECT,
                    opContext.getDn() ) );

                throw ldnfe;
            }
        }
    }


    private Entry getOriginalEntry( OperationContext opContext ) throws LdapException
    {
        // We have to use the admin session here, otherwise we may have
        // trouble reading the entry due to insufficient access rights
        CoreSession adminSession = opContext.getSession().getDirectoryService().getAdminSession();

        Entry foundEntry = adminSession.lookup( opContext.getDn(), SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES );

        if ( foundEntry != null )
        {
            return foundEntry;
        }
        else
        {
            // This is an error : we *must* have an entry if we want to be able to rename.
            LdapNoSuchObjectException ldnfe = new LdapNoSuchObjectException( I18n.err( I18n.ERR_256_NO_SUCH_OBJECT,
                opContext.getDn() ) );

            throw ldnfe;
        }
    }


    private LdapReferralException buildReferralException( Entry parentEntry, Dn childDn ) throws LdapException
    {
        // Get the Ref attributeType
        Attribute refs = parentEntry.get( SchemaConstants.REF_AT );

        List<String> urls = new ArrayList<String>();

        try
        {
            // manage each Referral, building the correct URL for each of them
            for ( Value<?> url : refs )
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


    private LdapReferralException buildReferralExceptionForSearch( Entry parentEntry, Dn childDn, SearchScope scope ) throws LdapException
    {
        // Get the Ref attributeType
        Attribute refs = parentEntry.get( SchemaConstants.REF_AT );

        List<String> urls = new ArrayList<String>();

        // manage each Referral, building the correct URL for each of them
        for ( Value<?> url : refs )
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
        LdapPartialResultException lpre = new LdapPartialResultException( I18n.err( I18n.ERR_315 ) );

        lpre.setRemainingDn( childDn );
        lpre.setResolvedDn( Dn.EMPTY_DN );

        return lpre;
    }


    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        LOG.debug( ">> AddOperation : {}", addContext );
        LOG_CHANGES.debug( ">> AddOperation : {}", addContext );

        ensureStarted();
        push( addContext );

        try
        {
            // Normalize the addContext Dn
            Dn dn = addContext.getDn();
            dn.apply( directoryService.getSchemaManager() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

            if ( directoryService.getReferralManager().hasParentReferral( dn ) )
            {
                Entry parentEntry = directoryService.getReferralManager().getParentReferral( dn );
                Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                // Depending on the Context.REFERRAL property value, we will throw
                // a different exception.
                if ( addContext.isReferralIgnored() )
                {
                    directoryService.getReferralManager().unlock();

                    LdapPartialResultException exception = buildLdapPartialResultException( childDn );
                    throw exception;
                }
                else
                {
                    // Unlock the referral manager
                    directoryService.getReferralManager().unlock();

                    LdapReferralException exception = buildReferralException( parentEntry, childDn );
                    throw exception;
                }
            }
            else
            {
                // Unlock the ReferralManager
                directoryService.getReferralManager().unlock();

                // Call the Add method
                Interceptor head = directoryService.getInterceptor( addContext.getNextInterceptor() );

                head.add( addContext );
            }
        }
        finally
        {
            pop();
        }

        LOG.debug( "<< AddOperation successful" );
        LOG_CHANGES.debug( "<< AddOperation successful" );
    }


    /**
     * {@inheritDoc}
     */
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        LOG.debug( ">> BindOperation : {}", bindContext );

        ensureStarted();
        push( bindContext );

        try
        {
            // Call the Delete method
            Interceptor head = directoryService.getInterceptor( bindContext.getNextInterceptor() );

            head.bind( bindContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< BindOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        LOG.debug( ">> CompareOperation : {}", compareContext );

        ensureStarted();
        push( compareContext );

        try
        {
            // Normalize the compareContext Dn
            Dn dn = compareContext.getDn();
            dn.apply( directoryService.getSchemaManager() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

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
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( compareContext.isReferralIgnored() )
                    {
                        directoryService.getReferralManager().unlock();

                        LdapPartialResultException exception = buildLdapPartialResultException( childDn );
                        throw exception;
                    }
                    else
                    {
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
            }

            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();

            // populate the context with the old entry
            compareContext.setOriginalEntry( getOriginalEntry( compareContext ) );

            // Call the Compare method
            Interceptor head = directoryService.getInterceptor( compareContext.getNextInterceptor() );

            return head.compare( compareContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< CompareOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        LOG.debug( ">> DeleteOperation : {}", deleteContext );
        LOG_CHANGES.debug( ">> DeleteOperation : {}", deleteContext );

        ensureStarted();
        push( deleteContext );

        try
        {
            // Normalize the deleteContext Dn
            Dn dn = deleteContext.getDn();
            dn.apply( directoryService.getSchemaManager() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

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
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't delete an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( deleteContext.isReferralIgnored() )
                    {
                        directoryService.getReferralManager().unlock();

                        LdapPartialResultException exception = buildLdapPartialResultException( childDn );
                        throw exception;
                    }
                    else
                    {
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
            }

            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();

            // populate the context with the old entry
            eagerlyPopulateFields( deleteContext );

            // Call the Delete method
            Interceptor head = directoryService.getInterceptor( deleteContext.getNextInterceptor() );

            head.delete( deleteContext );
        }
        finally
        {
            pop();
        }

        LOG.debug( "<< DeleteOperation successful" );
        LOG_CHANGES.debug( "<< DeleteOperation successful" );
    }


    /**
     * {@inheritDoc}
     */
    public Entry getRootDSE( GetRootDSEOperationContext getRootDseContext ) throws LdapException
    {
        LOG.debug( ">> GetRootDSEOperation : {}", getRootDseContext );

        ensureStarted();
        push( getRootDseContext );

        try
        {
            Interceptor head = directoryService.getInterceptor( getRootDseContext.getNextInterceptor() );

            return head.getRootDSE( getRootDseContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< getRootDSEOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( EntryOperationContext hasEntryContext ) throws LdapException
    {
        LOG.debug( ">> hasEntryOperation : {}", hasEntryContext );

        ensureStarted();
        push( hasEntryContext );

        try
        {
            Interceptor head = directoryService.getInterceptor( hasEntryContext.getNextInterceptor() );

            return head.hasEntry( hasEntryContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< HasEntryOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException
    {
        LOG.debug( ">> ListOperation : {}", listContext );

        ensureStarted();
        push( listContext );

        try
        {
            Interceptor head = directoryService.getInterceptor( listContext.getNextInterceptor() );

            return head.list( listContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< ListOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        LOG.debug( ">> LookupOperation : {}", lookupContext );

        ensureStarted();
        push( lookupContext );

        try
        {
            Interceptor head = directoryService.getInterceptor( lookupContext.getNextInterceptor() );

            return head.lookup( lookupContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< LookupOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        LOG.debug( ">> ModifyOperation : {}", modifyContext );
        LOG_CHANGES.debug( ">> ModifyOperation : {}", modifyContext );

        ensureStarted();
        push( modifyContext );

        try
        {
            // Normalize the modifyContext Dn
            Dn dn = modifyContext.getDn();
            dn.apply( directoryService.getSchemaManager() );

            ReferralManager referralManager = directoryService.getReferralManager();

            // We have to deal with the referral first
            referralManager.lockRead();

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
                        // Unlock the referral manager
                        referralManager.unlock();

                        // We have found a parent referral for the current Dn
                        Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
                else if ( referralManager.hasParentReferral( dn ) )
                {
                    // We can't delete an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( modifyContext.isReferralIgnored() )
                    {
                        referralManager.unlock();

                        // We have found a parent referral for the current Dn
                        Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                        LdapPartialResultException exception = buildLdapPartialResultException( childDn );
                        throw exception;
                    }
                    else
                    {
                        // Unlock the referral manager
                        referralManager.unlock();

                        // We have found a parent referral for the current Dn
                        Dn childDn = dn.getDescendantOf( parentEntry.getDn() );

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
            }

            // Unlock the ReferralManager
            referralManager.unlock();

            // Call the Add method
            InterceptorChain interceptorChain = directoryService.getInterceptorChain();
            interceptorChain.modify( modifyContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< ModifyOperation successful" );
            LOG_CHANGES.debug( "<< ModifyOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        LOG.debug( ">> MoveOperation : {}", moveContext );
        LOG_CHANGES.debug( ">> MoveOperation : {}", moveContext );

        ensureStarted();
        push( moveContext );

        try
        {
            // Normalize the moveContext Dn
            Dn dn = moveContext.getDn();
            dn.apply( directoryService.getSchemaManager() );

            // Normalize the moveContext superior Dn
            Dn newSuperiorDn = moveContext.getNewSuperior();
            newSuperiorDn.apply( directoryService.getSchemaManager() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

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
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't delete an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( moveContext.isReferralIgnored() )
                    {
                        directoryService.getReferralManager().unlock();

                        LdapPartialResultException exception = buildLdapPartialResultException( childDn );
                        throw exception;
                    }
                    else
                    {
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
            }

            // Now, check the destination
            // If he parent Dn is a referral, or has a referral ancestor, we have to issue a AffectMultipleDsas result
            // as stated by RFC 3296 Section 5.6.2
            if ( directoryService.getReferralManager().isReferral( newSuperiorDn )
                || directoryService.getReferralManager().hasParentReferral( newSuperiorDn ) )
            {
                // Unlock the referral manager
                directoryService.getReferralManager().unlock();

                LdapAffectMultipleDsaException exception = new LdapAffectMultipleDsaException();
                //exception.setRemainingName( dn );

                throw exception;
            }

            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();

            Entry originalEntry = getOriginalEntry( moveContext );

            moveContext.setOriginalEntry( originalEntry );

            // Call the Move method
            Interceptor head = directoryService.getInterceptor( moveContext.getNextInterceptor() );

            head.move( moveContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< MoveOperation successful" );
            LOG_CHANGES.debug( "<< MoveOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        LOG.debug( ">> MoveAndRenameOperation : {}", moveAndRenameContext );
        LOG_CHANGES.debug( ">> MoveAndRenameOperation : {}", moveAndRenameContext );

        ensureStarted();
        push( moveAndRenameContext );

        try
        {
            // Normalize the moveAndRenameContext Dn
            Dn dn = moveAndRenameContext.getDn();
            dn.apply( directoryService.getSchemaManager() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

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
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't delete an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( moveAndRenameContext.isReferralIgnored() )
                    {
                        directoryService.getReferralManager().unlock();

                        LdapPartialResultException exception = buildLdapPartialResultException( childDn );
                        throw exception;
                    }
                    else
                    {
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
            }

            // Now, check the destination
            // Normalize the moveAndRenameContext Dn
            Dn newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();
            newSuperiorDn.apply( directoryService.getSchemaManager() );

            // If he parent Dn is a referral, or has a referral ancestor, we have to issue a AffectMultipleDsas result
            // as stated by RFC 3296 Section 5.6.2
            if ( directoryService.getReferralManager().isReferral( newSuperiorDn )
                || directoryService.getReferralManager().hasParentReferral( newSuperiorDn ) )
            {
                // Unlock the referral manager
                directoryService.getReferralManager().unlock();

                // The parent Dn is a referral, we have to issue a AffectMultipleDsas result
                // as stated by RFC 3296 Section 5.6.2
                LdapAffectMultipleDsaException exception = new LdapAffectMultipleDsaException();
                //exception.setRemainingName( dn );

                throw exception;
            }

            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();

            // Call the Add method
            InterceptorChain interceptorChain = directoryService.getInterceptorChain();
            interceptorChain.moveAndRename( moveAndRenameContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< MoveAndRenameOperation successful" );
            LOG_CHANGES.debug( "<< MoveAndRenameOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        LOG.debug( ">> RenameOperation : {}", renameContext );
        LOG_CHANGES.debug( ">> RenameOperation : {}", renameContext );

        ensureStarted();
        push( renameContext );

        try
        {
            // Normalize the renameContext Dn
            Dn dn = renameContext.getDn();
            dn.apply( directoryService.getSchemaManager() );

            // Inject the newDn into the operation context
            // Inject the new Dn into the context
            if ( !dn.isEmpty() )
            {
                Dn newDn = dn.getParent();
                newDn = newDn.add( renameContext.getNewRdn() );
                renameContext.setNewDn( newDn );
            }

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

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
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't delete an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( renameContext.isReferralIgnored() )
                    {
                        directoryService.getReferralManager().unlock();

                        LdapPartialResultException exception = buildLdapPartialResultException( childDn );
                        throw exception;
                    }
                    else
                    {
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralException( parentEntry, childDn );
                        throw exception;
                    }
                }
            }

            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();

            // Call the rename method
            // populate the context with the old entry
            eagerlyPopulateFields( renameContext );
            Entry originalEntry = getOriginalEntry( renameContext );
            renameContext.setOriginalEntry( originalEntry );
            renameContext.setModifiedEntry( originalEntry.clone() );

            // Call the Rename method
            Interceptor head = directoryService.getInterceptor( renameContext.getNextInterceptor() );

            head.rename( renameContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< RenameOperation successful" );
            LOG_CHANGES.debug( "<< RenameOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        LOG.debug( ">> SearchOperation : {}", searchContext );

        ensureStarted();
        push( searchContext );

        try
        {
            // Normalize the searchContext Dn
            Dn dn = searchContext.getDn();
            dn.apply( directoryService.getSchemaManager() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

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
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralExceptionForSearch( parentEntry, childDn,
                            searchContext.getScope() );
                        throw exception;
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't search an entry which has an ancestor referral

                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( searchContext.isReferralIgnored() )
                    {
                        directoryService.getReferralManager().unlock();

                        LdapPartialResultException exception = buildLdapPartialResultException( childDn );
                        throw exception;
                    }
                    else
                    {
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();

                        LdapReferralException exception = buildReferralExceptionForSearch( parentEntry, childDn,
                            searchContext.getScope() );
                        throw exception;
                    }
                }
            }

            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();

            // Call the Add method
            InterceptorChain interceptorChain = directoryService.getInterceptorChain();
            return interceptorChain.search( searchContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< SearchOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        LOG.debug( ">> UnbindOperation : {}", unbindContext );

        ensureStarted();
        push( unbindContext );

        try
        {
            // Call the Unbind method
            Interceptor head = directoryService.getInterceptor( unbindContext.getNextInterceptor() );

            head.unbind( unbindContext );
        }
        finally
        {
            pop();
        }

        LOG.debug( "<< UnbindOperation successful" );
    }


    private void ensureStarted() throws LdapServiceUnavailableException
    {
        if ( !directoryService.isStarted() )
        {
            throw new LdapServiceUnavailableException( ResultCodeEnum.UNAVAILABLE, I18n.err( I18n.ERR_316 ) );
        }
    }


    private void pop()
    {
        // TODO - need to remove Context caller and PartitionNexusProxy from Invocations
        InvocationStack stack = InvocationStack.getInstance();
        stack.pop();
    }


    private void push( OperationContext opContext )
    {
        // TODO - need to remove Context caller and PartitionNexusProxy from Invocations
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( opContext );
    }
}
