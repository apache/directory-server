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
package org.apache.directory.server.ldap.handlers;


import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITControl;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.exception.LdapOperationException;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.internal.InternalLdapResult;
import org.apache.directory.shared.ldap.message.internal.InternalReferral;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponseRequest;
import org.apache.directory.shared.ldap.message.internal.InternalSearchRequest;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A based class for handlers which deal with SingleReplyRequests.  This class 
 * provides various capabilities out of the box for these kinds of requests so
 * common handling code is not duplicated.  Namely, exception handling and 
 * referral handling code common to most SingleReplyRequests (minus 
 * ExtendedRequests) are handled thanks to this class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class ReferralAwareRequestHandler<T extends InternalResultResponseRequest> extends LdapRequestHandler<T>
{
    private static final Logger LOG = LoggerFactory.getLogger( ReferralAwareRequestHandler.class );
    
    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    
    /* (non-Javadoc)
     * @see org.apache.directory.server.ldap.handlers.LdapRequestHandler#handle(org.apache.directory.server.ldap.LdapSession, org.apache.directory.shared.ldap.message.Request)
     */
    @Override
    public final void handle( LdapSession session, T req ) throws Exception
    {
        LOG.debug( "Handling single reply request: {}", req );
        
        // First, if we have the ManageDSAIt control, go directly
        // to the handling without pre-processing the request
        if ( req.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
        {
            // If the ManageDsaIT control is present, we will
            // consider that the user wants to get entry which
            // are referrals as plain entry. We have to return
            // SearchResponseEntry elements instead of 
            // SearchResponseReference elements.
            LOG.debug( "ManageDsaITControl detected." );
            handleIgnoringReferrals( session, req );
        }
        else
        {
            // No ManageDsaIT control. If the found entries is a referral,
            // we will return SearchResponseReference elements.
            LOG.debug( "ManageDsaITControl NOT detected." );
    
            switch ( req.getType() )
            {
                case SEARCH_REQUEST:
                    handleWithReferrals( session, ( ( InternalSearchRequest ) req ).getBase(), req );
                    break;

                case EXTENDED_REQUEST:
                    throw new IllegalStateException( I18n.err( I18n.ERR_684 ) );
                    
                default:
                    throw new IllegalStateException( I18n.err( I18n.ERR_685, req ) );
            }
            
        }

    }

    
    public static final boolean isEntryReferral( ClonedServerEntry entry ) throws Exception
    {
        return entry.getOriginalEntry().contains( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.REFERRAL_OC );
    }
    
    
    /**
     * Searches up the ancestry of a DN searching for the farthest referral 
     * ancestor.  This is required to properly handle referrals.  Note that 
     * this function is quite costly since it attempts to lookup all the 
     * ancestors up the hierarchy just to see if they represent referrals. 
     * Techniques can be employed later to improve this performance hit by
     * having an intelligent referral cache.
     *
     * @return the farthest referral ancestor or null
     * @throws Exception if there are problems during this search
     */
    public static final ClonedServerEntry getFarthestReferralAncestor( LdapSession session, DN target ) 
        throws Exception
    {
        ClonedServerEntry entry;
        ClonedServerEntry farthestReferralAncestor = null;
        DN dn = ( DN ) target.clone();
        
        try
        {
            dn.remove( dn.size() - 1 );
        }
        catch ( LdapInvalidDnException e2 )
        {
            // never thrown
        }
        
        while ( ! dn.isEmpty() )
        {
            LOG.debug( "Walking ancestors of {} to find referrals.", dn );
            
            try
            {
                entry = session.getCoreSession().lookup( dn );

                if ( isEntryReferral( entry ) )
                {
                    farthestReferralAncestor = entry;
                }

                dn.remove( dn.size() - 1 );
            }
            catch ( LdapException e )
            {
                LOG.debug( "Entry for {} not found.", dn );

                // update the DN as we strip last component 
                try
                {
                    dn.remove( dn.size() - 1 );
                }
                catch ( LdapInvalidDnException e1 )
                {
                    // never happens
                }
            }
        }
        
        return farthestReferralAncestor;
    }
    
    
    /**
     * Handles processing with referrals without ManageDsaIT control and with 
     * an ancestor that is a referral.  The original entry was not found and 
     * the walk of the ancestry returned a referral.
     * 
     * @param referralAncestor the farthest referral ancestor of the missing 
     * entry  
     */
    public InternalReferral getReferralOnAncestor( LdapSession session, DN reqTargetDn, T req, 
        ClonedServerEntry referralAncestor ) throws Exception
    {
        LOG.debug( "Inside getReferralOnAncestor()" );
        
        EntryAttribute refAttr =referralAncestor.getOriginalEntry()
            .get( SchemaConstants.REF_AT );
        InternalReferral referral = new ReferralImpl();

        for ( Value<?> value : refAttr )
        {
            String ref = value.getString();

            LOG.debug( "Calculating LdapURL for referrence value {}", ref );

            // need to add non-ldap URLs as-is
            if ( ! ref.startsWith( "ldap" ) )
            {
                referral.addLdapUrl( ref );
                continue;
            }
            
            // parse the ref value and normalize the DN  
            LdapURL ldapUrl = new LdapURL();
            try
            {
                ldapUrl.parse( ref.toCharArray() );
            }
            catch ( LdapURLEncodingException e )
            {
                LOG.error( I18n.err( I18n.ERR_165, ref, referralAncestor ) );
            }
            
            DN urlDn = new DN( ldapUrl.getDn().getName() );
            urlDn.normalize( session.getCoreSession().getDirectoryService().getSchemaManager()
                .getNormalizerMapping() ); 
            
            if ( urlDn.getNormName().equals( referralAncestor.getDn().getNormName() ) )
            {
                // according to the protocol there is no need for the dn since it is the same as this request
                StringBuilder buf = new StringBuilder();
                buf.append( ldapUrl.getScheme() );
                buf.append( ldapUrl.getHost() );

                if ( ldapUrl.getPort() > 0 )
                {
                    buf.append( ":" );
                    buf.append( ldapUrl.getPort() );
                }

                referral.addLdapUrl( buf.toString() );
                continue;
            }
            
            /*
             * If we get here then the DN of the referral was not the same as the 
             * DN of the ref LDAP URL.  We must calculate the remaining (difference)
             * name past the farthest referral DN which the target name extends.
             */
            int diff = reqTargetDn.size() - referralAncestor.getDn().size();
            DN extra = new DN();

            // TODO - fix this by access unormalized RDN values
            // seems we have to do this because get returns normalized rdns
            DN reqUnnormalizedDn = new DN( reqTargetDn.getName() );
            for ( int jj = 0; jj < diff; jj++ )
            {
                extra.add( reqUnnormalizedDn.get( referralAncestor.getDn().size() + jj ) );
            }

            urlDn.addAll( extra );

            StringBuilder buf = new StringBuilder();
            buf.append( ldapUrl.getScheme() );
            buf.append( ldapUrl.getHost() );

            if ( ldapUrl.getPort() > 0 )
            {
                buf.append( ":" );
                buf.append( ldapUrl.getPort() );
            }

            buf.append( "/" );
            buf.append( LdapURL.urlEncode( urlDn.getName(), false ) );
            referral.addLdapUrl( buf.toString() );
        }
        
        return referral;
    }
    
    
    /**
     * Handles processing with referrals without ManageDsaIT control and with 
     * an ancestor that is a referral.  The original entry was not found and 
     * the walk of the ancestry returned a referral.
     * 
     * @param referralAncestor the farthest referral ancestor of the missing 
     * entry  
     */
    public InternalReferral getReferralOnAncestorForSearch( LdapSession session, InternalSearchRequest req, 
        ClonedServerEntry referralAncestor ) throws Exception
    {
        LOG.debug( "Inside getReferralOnAncestor()" );
     
        EntryAttribute refAttr = referralAncestor.getOriginalEntry()
            .get( SchemaConstants.REF_AT );
        InternalReferral referral = new ReferralImpl();

        for ( Value<?> value : refAttr )
        {
            String ref = value.getString();

            LOG.debug( "Calculating LdapURL for referrence value {}", ref );

            // need to add non-ldap URLs as-is
            if ( ! ref.startsWith( "ldap" ) )
            {
                referral.addLdapUrl( ref );
                continue;
            }
            
            // Parse the ref value   
            LdapURL ldapUrl = new LdapURL();
            try
            {
                ldapUrl.parse( ref.toCharArray() );
            }
            catch ( LdapURLEncodingException e )
            {
                LOG.error( I18n.err( I18n.ERR_165, ref, referralAncestor ) );
            }
            
            // Normalize the DN to check for same dn
            DN urlDn = new DN( ldapUrl.getDn().getName() );
            urlDn.normalize( session.getCoreSession().getDirectoryService().getSchemaManager()
                .getNormalizerMapping() ); 
            
            if ( urlDn.getNormName().equals( req.getBase().getNormName() ) )
            {
                ldapUrl.setForceScopeRendering( true );
                ldapUrl.setAttributes( req.getAttributes() );
                ldapUrl.setScope( req.getScope().getScope() );
                referral.addLdapUrl( ldapUrl.toString() );
                continue;
            }
            
            /*
             * If we get here then the DN of the referral was not the same as the 
             * DN of the ref LDAP URL.  We must calculate the remaining (difference)
             * name past the farthest referral DN which the target name extends.
             */
            int diff = req.getBase().size() - referralAncestor.getDn().size();
            DN extra = new DN();

            // TODO - fix this by access unormalized RDN values
            // seems we have to do this because get returns normalized rdns
            DN reqUnnormalizedDn = new DN( req.getBase().getName() );
            for ( int jj = 0; jj < diff; jj++ )
            {
                extra.add( reqUnnormalizedDn.get( referralAncestor.getDn().size() + jj ) );
            }

            ldapUrl.getDn().addAll( extra );
            ldapUrl.setForceScopeRendering( true );
            ldapUrl.setAttributes( req.getAttributes() );
            ldapUrl.setScope( req.getScope().getScope() );
            referral.addLdapUrl( ldapUrl.toString() );
        }
        
        return referral;
    }
    
    
    /**
     * Handles processing with referrals without ManageDsaIT control.
     */
    public void handleException( LdapSession session, InternalResultResponseRequest req, Exception e )
    {
        InternalLdapResult result = req.getResultResponse().getLdapResult();

        /*
         * Set the result code or guess the best option.
         */
        ResultCodeEnum code;
        
        if ( e instanceof LdapOperationException )
        {
            code = ( ( LdapOperationException ) e ).getResultCode();
        }
        else
        {
            code = ResultCodeEnum.getBestEstimate( e, req.getType() );
        }
        
        result.setResultCode( code );

        /*
         * Setup the error message to put into the request and put entire
         * exception into the message if we are in debug mode.  Note we 
         * embed the result code name into the message.
         */
        String msg = code.toString() + ": failed for " + req + ": " + e.getLocalizedMessage();
        LOG.debug( msg, e );
        
        if ( IS_DEBUG )
        {
            msg += ":\n" + ExceptionUtils.getStackTrace( e );
        }
        
        result.setErrorMessage( msg );

        if ( e instanceof LdapOperationException )
        {
            LdapOperationException ne = ( LdapOperationException ) e;

            // Add the matchedDN if necessary
            boolean setMatchedDn = 
                code == ResultCodeEnum.NO_SUCH_OBJECT             || 
                code == ResultCodeEnum.ALIAS_PROBLEM              ||
                code == ResultCodeEnum.INVALID_DN_SYNTAX          || 
                code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM;
            
            if ( ( ne.getResolvedDn() != null ) && setMatchedDn )
            {
                result.setMatchedDn( ( DN ) ne.getResolvedDn() );
            }
        }

        session.getIoSession().write( req.getResultResponse() );
    }

    
    /**
     * Handles processing without referral handling in effect: either with the
     * ManageDsaIT control or when the entry or all of it's ancestors are non-
     * referral entries.
     * 
     * Implementors
     * 
     * @param session the LDAP session under which processing occurs
     * @param reqTargetDn the target entry DN associated with the request
     * @param entry the target entry if it exists and has been looked up, may 
     * be null even if the entry exists, offered in case the entry is looked
     * up to avoid repeat lookups.  Implementations should check if the entry
     * is null and attempt a lookup instead of presuming the entry does not 
     * exist.
     * @param req the request to be handled
     */
    public abstract void handleIgnoringReferrals( LdapSession session, T req );


    /**
     * Handles processing with referrals without ManageDsaIT control.
     */
    public abstract void handleWithReferrals( LdapSession session, DN reqTargetDn, T req ) throws LdapException;
}
