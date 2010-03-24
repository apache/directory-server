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
import java.util.Set;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapAffectMultipleDsaException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.exception.LdapPartialResultException;
import org.apache.directory.shared.ldap.exception.LdapReferralException;
import org.apache.directory.shared.ldap.exception.LdapServiceUnavailableException;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The default implementation of an OperationManager.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
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
    
    
    private LdapReferralException buildReferralException( ServerEntry parentEntry, DN childDn ) 
        throws LdapInvalidDnException, LdapURLEncodingException
    {
        // Get the Ref attributeType
        EntryAttribute refs = parentEntry.get( SchemaConstants.REF_AT );
        
        List<String> urls = new ArrayList<String>();
        
        // manage each Referral, building the correct URL for each of them
        for ( Value<?> url:refs )
        {
            // we have to replace the parent by the referral
            LdapURL ldapUrl = new LdapURL( url.getString() );
            
            // We have a problem with the DN : we can't use the UpName,
            // as we may have some spaces around the ',' and '+'.
            // So we have to take the RDN one by one, and create a 
            // new DN with the type and value UP form
            
            DN urlDn = (DN)ldapUrl.getDn().addAll( childDn );
            
            ldapUrl.setDn( urlDn );
            urls.add( ldapUrl.toString() );
        }
        
        // Return with an exception
        LdapReferralException lre = new LdapReferralException( urls );
        lre.setRemainingDn( childDn );
        lre.setResolvedDn( parentEntry.getDn() );
        lre.setResolvedObject( parentEntry );
        
        return lre;
    }
    
    
    private LdapReferralException buildReferralExceptionForSearch( 
        ServerEntry parentEntry, DN childDn, SearchScope scope ) 
        throws LdapInvalidDnException, LdapURLEncodingException
    {
        // Get the Ref attributeType
        EntryAttribute refs = parentEntry.get( SchemaConstants.REF_AT );
        
        List<String> urls = new ArrayList<String>();
        
        // manage each Referral, building the correct URL for each of them
        for ( Value<?> url:refs )
        {
            // we have to replace the parent by the referral
            try
            {
                LdapURL ldapUrl = new LdapURL( url.getString() );
                
                StringBuilder urlString = new StringBuilder();
    
                if ( ( ldapUrl.getDn() == null ) || ( ldapUrl.getDn() == DN.EMPTY_DN) )
                {
                    ldapUrl.setDn( parentEntry.getDn() );
                }
                else
                {
                    // We have a problem with the DN : we can't use the UpName,
                    // as we may have some spaces around the ',' and '+'.
                    // So we have to take the RDN one by one, and create a 
                    // new DN with the type and value UP form
                    
                    DN urlDn = (DN)ldapUrl.getDn().addAll( childDn );
                    
                    ldapUrl.setDn( urlDn );
                }
                
                urlString.append( ldapUrl.toString() ).append( "??" );
                
                switch ( scope )
                {
                    case OBJECT :
                        urlString.append( "base" );
                        break;
                        
                    case SUBTREE :
                        urlString.append( "sub" );
                        break;
                        
                    case ONELEVEL :
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


    private LdapPartialResultException buildLdapPartialResultException( DN childDn )
    {
        LdapPartialResultException lpre = new LdapPartialResultException( I18n.err( I18n.ERR_315 ) );
        
        lpre.setRemainingDn( childDn );
        lpre.setResolvedDn( DN.EMPTY_DN );
        
        return lpre;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> AddOperation : {}", opContext );
        LOG_CHANGES.debug( ">> AddOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            // Normalize the opContext DN
            DN dn = opContext.getDn();
            dn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

            if ( directoryService.getReferralManager().hasParentReferral( dn ) )
            {
                ServerEntry parentEntry = directoryService.getReferralManager().getParentReferral( dn );
                DN childDn = (DN)dn.getSuffix( parentEntry.getDn().size() );

                // Depending on the Context.REFERRAL property value, we will throw
                // a different exception.
                if ( opContext.isReferralIgnored() )
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
                InterceptorChain interceptorChain = directoryService.getInterceptorChain();
                interceptorChain.add( opContext );
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
    public void bind( BindOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> BindOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            directoryService.getInterceptorChain().bind( opContext );
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
    public boolean compare( CompareOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> CompareOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            // Normalize the opContext DN
            DN dn = opContext.getDn();
            dn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

            // Check if we have an ancestor for this DN
            ServerEntry parentEntry = directoryService.getReferralManager().getParentReferral( dn );
            
            if ( parentEntry != null )
            {
                // We have found a parent referral for the current DN 
                DN childDn = (DN)dn.getSuffix( parentEntry.getDn().size() );
    
                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !opContext.isReferralIgnored() )
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
                    if ( opContext.isReferralIgnored() )
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

            // Call the Add method
            InterceptorChain interceptorChain = directoryService.getInterceptorChain();
            return interceptorChain.compare( opContext );
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
    public void delete( DeleteOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> DeleteOperation : {}", opContext );
        LOG_CHANGES.debug( ">> DeleteOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            // Normalize the opContext DN
            DN dn = opContext.getDn();
            dn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

            ServerEntry parentEntry = directoryService.getReferralManager().getParentReferral( dn );
            
            if ( parentEntry != null )
            {
                // We have found a parent referral for the current DN 
                DN childDn = (DN)dn.getSuffix( parentEntry.getDn().size() );
    
                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !opContext.isReferralIgnored() )
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
                    if ( opContext.isReferralIgnored() )
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

            // Call the Add method
            InterceptorChain interceptorChain = directoryService.getInterceptorChain();
            interceptorChain.delete( opContext );
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
    public DN getMatchedName( GetMatchedNameOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> GetMatchedNameOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            return directoryService.getInterceptorChain().getMatchedName( opContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< GetMatchedNameOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public ClonedServerEntry getRootDSE( GetRootDSEOperationContext opContext ) 
        throws Exception
    {
        LOG.debug( ">> GetRootDSEOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            InterceptorChain chain = directoryService.getInterceptorChain();
            return chain.getRootDSE( opContext );
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
    public DN getSuffix( GetSuffixOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> GetSuffixOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            return directoryService.getInterceptorChain().getSuffix( opContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< GetSuffixOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( EntryOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> hasEntryOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            return directoryService.getInterceptorChain().hasEntry( opContext );
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
    public EntryFilteringCursor list( ListOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> ListOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            return directoryService.getInterceptorChain().list( opContext );
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
    public Set<String> listSuffixes( ListSuffixOperationContext opContext ) 
        throws Exception
    {
        LOG.debug( ">> ListSuffixesOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            return directoryService.getInterceptorChain().listSuffixes( opContext );
        }
        finally
        {
            pop();

            LOG.debug( "<< ListSuffixesOperation successful" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public ClonedServerEntry lookup( LookupOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> LookupOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            return directoryService.getInterceptorChain().lookup( opContext );
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
    public void modify( ModifyOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> ModifyOperation : {}", opContext );
        LOG_CHANGES.debug( ">> ModifyOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            // Normalize the opContext DN
            DN dn = opContext.getDn();
            dn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

            // Check if we have an ancestor for this DN
            ServerEntry parentEntry = directoryService.getReferralManager().getParentReferral( dn );
            
            if ( parentEntry != null )
            {
                // We have found a parent referral for the current DN 
                DN childDn = (DN)dn.getSuffix( parentEntry.getDn().size() );
    
                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !opContext.isReferralIgnored() )
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
                    if ( opContext.isReferralIgnored() )
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

            // Call the Add method
            InterceptorChain interceptorChain = directoryService.getInterceptorChain();
            interceptorChain.modify( opContext );
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
    public void move( MoveOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> MoveOperation : {}", opContext );
        LOG_CHANGES.debug( ">> MoveOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            // Normalize the opContext DN
            DN dn = opContext.getDn();
            dn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

            // Check if we have an ancestor for this DN
            ServerEntry parentEntry = directoryService.getReferralManager().getParentReferral( dn );
            
            if ( parentEntry != null )
            {
                // We have found a parent referral for the current DN 
                DN childDn = (DN)dn.getSuffix( parentEntry.getDn().size() );
    
                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !opContext.isReferralIgnored() )
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
                    if ( opContext.isReferralIgnored() )
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
            // Normalize the opContext DN
            DN parentDn = opContext.getParent();
            parentDn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );

            // If he parent DN is a referral, or has a referral ancestor, we have to issue a AffectMultipleDsas result
            // as stated by RFC 3296 Section 5.6.2
            if ( directoryService.getReferralManager().isReferral( parentDn ) ||
                 directoryService.getReferralManager().hasParentReferral( parentDn ) )
            {
                // Unlock the referral manager
                directoryService.getReferralManager().unlock();

                LdapAffectMultipleDsaException exception = new LdapAffectMultipleDsaException();
                //exception.setRemainingName( dn );
                
                throw exception;
            }

            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();

            // Call the Add method
            InterceptorChain interceptorChain = directoryService.getInterceptorChain();
            interceptorChain.move( opContext );
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
    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> MoveAndRenameOperation : {}", opContext );
        LOG_CHANGES.debug( ">> MoveAndRenameOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            // Normalize the opContext DN
            DN dn = opContext.getDn();
            dn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

            // Check if we have an ancestor for this DN
            ServerEntry parentEntry = directoryService.getReferralManager().getParentReferral( dn );
            
            if ( parentEntry != null )
            {
                // We have found a parent referral for the current DN 
                DN childDn = (DN)dn.getSuffix( parentEntry.getDn().size() );
    
                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !opContext.isReferralIgnored() )
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
                    if ( opContext.isReferralIgnored() )
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
            // Normalize the opContext DN
            DN parentDn = opContext.getParent();
            parentDn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );

            // If he parent DN is a referral, or has a referral ancestor, we have to issue a AffectMultipleDsas result
            // as stated by RFC 3296 Section 5.6.2
            if ( directoryService.getReferralManager().isReferral( parentDn ) ||
                 directoryService.getReferralManager().hasParentReferral( parentDn ) )
            {
                // Unlock the referral manager
                directoryService.getReferralManager().unlock();

                // The parent DN is a referral, we have to issue a AffectMultipleDsas result
                // as stated by RFC 3296 Section 5.6.2
                LdapAffectMultipleDsaException exception = new LdapAffectMultipleDsaException();
                //exception.setRemainingName( dn );
                
                throw exception;
            }
            
            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();

            // Call the Add method
            InterceptorChain interceptorChain = directoryService.getInterceptorChain();
            interceptorChain.moveAndRename( opContext );
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
    public void rename( RenameOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> RenameOperation : {}", opContext );
        LOG_CHANGES.debug( ">> RenameOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            // Normalize the opContext DN
            DN dn = opContext.getDn();
            dn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );

            // Inject the newDn into the operation context
            // Inject the new DN into the context
            if ( !dn.isEmpty() )
            {
                DN newDn = (DN)dn.clone();
                newDn.remove( dn.size() - 1 );
                newDn.add( opContext.getNewRdn() );
                opContext.setNewDn( newDn );
            }
            
            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

            // Check if we have an ancestor for this DN
            ServerEntry parentEntry = directoryService.getReferralManager().getParentReferral( dn );
            
            if ( parentEntry != null )
            {
                // We have found a parent referral for the current DN 
                DN childDn = (DN)dn.getSuffix( parentEntry.getDn().size() );
    
                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can delete it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !opContext.isReferralIgnored() )
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
                    if ( opContext.isReferralIgnored() )
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

            // Call the Add method
            InterceptorChain interceptorChain = directoryService.getInterceptorChain();
            interceptorChain.rename( opContext );
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
    public EntryFilteringCursor search( SearchOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> SearchOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            // Normalize the opContext DN
            DN dn = opContext.getDn();
            dn.normalize( directoryService.getSchemaManager().getNormalizerMapping() );

            // We have to deal with the referral first
            directoryService.getReferralManager().lockRead();

            // Check if we have an ancestor for this DN
            ServerEntry parentEntry = directoryService.getReferralManager().getParentReferral( dn );
            
            if ( parentEntry != null )
            {
                // We have found a parent referral for the current DN 
                DN childDn = (DN)dn.getSuffix( parentEntry.getDn().size() );
    
                if ( directoryService.getReferralManager().isReferral( dn ) )
                {
                    // This is a referral. We can return it if the ManageDsaIt flag is true
                    // Otherwise, we just throw a LdapReferralException
                    if ( !opContext.isReferralIgnored() )
                    {
                        // Throw a Referral Exception
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();
                        
                        LdapReferralException exception = buildReferralExceptionForSearch( parentEntry, childDn, opContext.getScope() );
                        throw exception;
                    }
                }
                else if ( directoryService.getReferralManager().hasParentReferral( dn ) )
                {
                    // We can't search an entry which has an ancestor referral
    
                    // Depending on the Context.REFERRAL property value, we will throw
                    // a different exception.
                    if ( opContext.isReferralIgnored() )
                    {
                        directoryService.getReferralManager().unlock();
                        
                        LdapPartialResultException exception = buildLdapPartialResultException( childDn );
                        throw exception;
                    }
                    else
                    {
                        // Unlock the referral manager
                        directoryService.getReferralManager().unlock();
                        
                        LdapReferralException exception = buildReferralExceptionForSearch( parentEntry, childDn, opContext.getScope() );
                        throw exception;
                    }
                }
            }

            // Unlock the ReferralManager
            directoryService.getReferralManager().unlock();

            // Call the Add method
            InterceptorChain interceptorChain = directoryService.getInterceptorChain();
            return interceptorChain.search( opContext );
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
    public void unbind( UnbindOperationContext opContext ) throws Exception
    {
        LOG.debug( ">> UnbindOperation : {}", opContext );
        
        ensureStarted();
        push( opContext );
        
        try
        {
            directoryService.getInterceptorChain().unbind( opContext );
        }
        finally
        {
            pop();
        }

        LOG.debug( "<< UnbindOperation successful" );
    }


    private void ensureStarted() throws LdapServiceUnavailableException
    {
        if ( ! directoryService.isStarted() )
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
