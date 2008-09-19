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


import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.CompareRequest;
import org.apache.directory.shared.ldap.message.DeleteRequest;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.ModifyRequest;
import org.apache.directory.shared.ldap.message.Referral;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.ResultResponseRequest;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.control.ManageDsaITControl;
import org.apache.directory.shared.ldap.name.LdapDN;
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
public abstract class ReferralAwareRequestHandler<T extends ResultResponseRequest> extends LdapRequestHandler<T>
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
        
        LdapDN reqTargetDn = null;
        
        switch ( req.getType() )
        {
            case ADD_REQUEST:
                reqTargetDn = ( ( AddRequest ) req ).getEntryDn();
                break;
            case BIND_REQUEST:
                // not used for bind but may be in future
                reqTargetDn = ( ( BindRequest ) req ).getName();
                break;
            case COMPARE_REQUEST:
                reqTargetDn = ( ( CompareRequest ) req ).getName();
                break;
            case DEL_REQUEST:
                reqTargetDn = ( ( DeleteRequest ) req ).getName();
                break;
            case EXTENDED_REQ:
                throw new IllegalStateException( 
                    "Although ExtendedRequests are SingleReplyRequests they're not handled" +
                    " using this base class.  They have no target entry unlike the rest of" +
                    " the SingleReplyRequests" );
            case MOD_DN_REQUEST:
                /*
                 * Special handling needed because of the new superior entry 
                 * as specified in RFC 3296 section 5.6.2 here: 
                 *    
                 *     http://www.faqs.org/rfcs/rfc3296.html
                 */
                if ( req.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
                {
                    LOG.debug( "ManageDsaITControl detected." );
                    handleIgnoringReferrals( session, ( ( ModifyDnRequest ) req ).getName(), null, req );
                }
                else
                {
                    LOG.debug( "ManageDsaITControl NOT detected." );
                    
                    if ( ( ( ModifyDnRequest ) req ).getNewSuperior() == null )
                    {
                        handleWithReferrals( session, ( ( ModifyDnRequest ) req ).getName(), req );
                    }
                    else
                    {
                        // NOTE: call is to overload just for the ModifyDnRequest
                        handleModifyDnWithReferrals( session, req );
                    }
                }
                return;
            case MODIFY_REQUEST:
                reqTargetDn = ( ( ModifyRequest ) req ).getName();
                break;
            case SEARCH_REQUEST:
                reqTargetDn = ( ( SearchRequest ) req ).getBase();
                break;
            default:
                throw new IllegalStateException( 
                    "Unidentified single reply request/response type: " + req );
        }
        
        if ( req.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
        {
            LOG.debug( "ManageDsaITControl detected." );
            handleIgnoringReferrals( session, reqTargetDn, null, req );
        }
        else
        {
            LOG.debug( "ManageDsaITControl NOT detected." );
            handleWithReferrals( session, reqTargetDn, req );
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
    public static final ClonedServerEntry getFarthestReferralAncestor( LdapSession session, LdapDN target ) 
        throws Exception
    {
        ClonedServerEntry entry;
        ClonedServerEntry farthestReferralAncestor = null;
        LdapDN dn = ( LdapDN ) target.clone();
        
        try
        {
            dn.remove( dn.size() - 1 );
        }
        catch ( InvalidNameException e2 )
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
            catch ( NameNotFoundException e )
            {
                LOG.debug( "Entry for {} not found.", dn );

                // update the DN as we strip last component 
                try
                {
                    dn.remove( dn.size() - 1 );
                }
                catch ( InvalidNameException e1 )
                {
                    // never happens
                }
            }
        }
        
        return farthestReferralAncestor;
    }
    
    
    private void handleModifyDnWithReferrals( LdapSession session, T modifyDnRequest )
    {
        ModifyDnRequest req = ( ModifyDnRequest ) modifyDnRequest;
        LdapResult result = req.getResultResponse().getLdapResult();
        ClonedServerEntry entry = null;
        ClonedServerEntry superiorEntry = null;

        // -------------------------------------------------------------------
        // Lookup Entry
        // -------------------------------------------------------------------
        
        // try to lookup the entry but ignore exceptions when it does not   
        // exist since entry may not exist but may have an ancestor that is a 
        // referral - would rather attempt a lookup that fails then do check 
        // for existence than have to do another lookup to get entry info

        try
        {
            entry = session.getCoreSession().lookup( req.getName() );
            LOG.debug( "Entry for {} was found: ", req.getName(), entry );
        }
        catch ( NameNotFoundException e )
        {
            /* ignore */
            LOG.debug( "Entry for {} not found.", req.getName() );
        }
        catch ( Exception e )
        {
            /* serious and needs handling */
            handleException( session, modifyDnRequest, e );
            return;
        }
        
        try
        {
            superiorEntry = session.getCoreSession().lookup( req.getNewSuperior() );
            LOG.debug( "New superior entry for {} was found: ", req.getName(), entry );
        }
        catch ( NameNotFoundException e )
        {
            /* ignore */
            LOG.debug( "New superior entry for {} not found.", req.getName() );
        }
        catch ( Exception e )
        {
            /* serious and needs handling */
            handleException( session, modifyDnRequest, e );
            return;
        }
        
        // -------------------------------------------------------------------
        // Handle Existing Entry
        // -------------------------------------------------------------------
        
        if ( entry != null )
        {
            try
            {
                if ( isEntryReferral( entry ) )
                {
                    LOG.debug( "Entry is a referral: {}", entry );
                    handleReferralEntry( session, req.getName(), modifyDnRequest, entry );
                    return;
                }
                else
                {
                    if ( superiorEntry != null && isEntryReferral( superiorEntry ) )
                    {
                        result.setErrorMessage( "Superior entry is a referral." );
                        result.setMatchedDn( req.getNewSuperior() );
                        result.setResultCode( ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
                        session.getIoSession().write( req.getResultResponse() );
                        return;
                    }
                    else if ( superiorEntry == null )
                    {
                        ClonedServerEntry referralAncestor = getFarthestReferralAncestor( session, 
                            req.getNewSuperior() );
                        
                        if ( referralAncestor != null )
                        {
                            result.setErrorMessage( "Superior entry does has referral ancestor." );
                            result.setResultCode( ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
                            session.getIoSession().write( req.getResultResponse() );
                            return;
                        }
                        else
                        {
                            result.setErrorMessage( "Superior entry does not exist." );
                            result.setResultCode( ResultCodeEnum.NO_SUCH_OBJECT );
                            session.getIoSession().write( req.getResultResponse() );
                            return;
                        }
                    }
                    
                    LOG.debug( "Entry is NOT a referral: {}", entry );
                    handleIgnoringReferrals( session, req.getName(), entry, modifyDnRequest );
                    return;
                }
            }
            catch ( Exception e )
            {
                handleException( session, modifyDnRequest, e );
            }
        }

        // -------------------------------------------------------------------
        // Handle Non-existing Entry
        // -------------------------------------------------------------------
        
        // if the entry is null we still have to check for a referral ancestor
        // also the referrals need to be adjusted based on the ancestor's ref
        // values to yield the correct path to the entry in the target DSAs
        
        if ( entry == null )
        {
            ClonedServerEntry referralAncestor = null;

            try
            {
                referralAncestor = getFarthestReferralAncestor( session, req.getName() );
            }
            catch ( Exception e )
            {
                handleException( session, modifyDnRequest, e );
                return;
            }

            if ( referralAncestor == null && ! ( req instanceof AddRequest ) )
            {
                result.setErrorMessage( "Entry not found." );
                result.setResultCode( ResultCodeEnum.NO_SUCH_OBJECT );
                session.getIoSession().write( req.getResultResponse() );
                return;
            }
            else if ( ( req instanceof AddRequest ) && referralAncestor == null )
            {
                handleIgnoringReferrals( session, req.getName(), entry, modifyDnRequest );
                return;
            }
              
            // if we get here then we have a valid referral ancestor
            try
            {
                Referral referral = getReferralOnAncestor( session, req.getName(), modifyDnRequest, referralAncestor );
                result.setResultCode( ResultCodeEnum.REFERRAL );
                result.setReferral( referral );
                session.getIoSession().write( req.getResultResponse() );
            }
            catch ( Exception e )
            {
                handleException( session, modifyDnRequest, e );
            }
        }
    }
    
    
    /**
     * Handles processing with referrals without ManageDsaIT control.
     */
    private void handleWithReferrals( LdapSession session, LdapDN reqTargetDn, T req )
    {
        LdapResult result = req.getResultResponse().getLdapResult();
        ClonedServerEntry entry = null;

        // -------------------------------------------------------------------
        // Lookup Entry
        // -------------------------------------------------------------------
        
        // try to lookup the entry but ignore exceptions when it does not   
        // exist since entry may not exist but may have an ancestor that is a 
        // referral - would rather attempt a lookup that fails then do check 
        // for existence than have to do another lookup to get entry info

        if ( ! ( req instanceof AddRequest ) )
        {
            try
            {
                entry = session.getCoreSession().lookup( reqTargetDn );
                LOG.debug( "Entry for {} was found: ", reqTargetDn, entry );
            }
            catch ( NameNotFoundException e )
            {
                /* ignore */
                LOG.debug( "Entry for {} not found.", reqTargetDn );
            }
            catch ( Exception e )
            {
                /* serious and needs handling */
                handleException( session, req, e );
                return;
            }
        }
        
        // -------------------------------------------------------------------
        // Handle Existing Entry
        // -------------------------------------------------------------------
        
        if ( entry != null )
        {
            try
            {
                if ( isEntryReferral( entry ) )
                {
                    LOG.debug( "Entry is a referral: {}", entry );
                    
                    if ( req instanceof SearchRequest )
                    {
                        handleReferralEntryForSearch( session, ( SearchRequest ) req, entry );
                    }
                    else
                    {
                        handleReferralEntry( session, reqTargetDn, req, entry );
                    }
                    return;
                }
                else
                {
                    LOG.debug( "Entry is NOT a referral: {}", entry );
                    handleIgnoringReferrals( session, reqTargetDn, entry, req );
                    return;
                }
            }
            catch ( Exception e )
            {
                handleException( session, req, e );
            }
        }

        // -------------------------------------------------------------------
        // Handle Non-existing Entry
        // -------------------------------------------------------------------
        
        // if the entry is null we still have to check for a referral ancestor
        // also the referrals need to be adjusted based on the ancestor's ref
        // values to yield the correct path to the entry in the target DSAs
        
        if ( entry == null )
        {
            ClonedServerEntry referralAncestor = null;

            try
            {
                referralAncestor = getFarthestReferralAncestor( session, reqTargetDn );
            }
            catch ( Exception e )
            {
                handleException( session, req, e );
                return;
            }

            if ( referralAncestor == null && ! ( req instanceof AddRequest ) )
            {
                result.setErrorMessage( "Entry not found." );
                result.setResultCode( ResultCodeEnum.NO_SUCH_OBJECT );
                session.getIoSession().write( req.getResultResponse() );
                return;
            }
            else if ( ( req instanceof AddRequest ) && referralAncestor == null )
            {
                handleIgnoringReferrals( session, reqTargetDn, entry, req );
                return;
            }
              
            // if we get here then we have a valid referral ancestor
            try
            {
                Referral referral = null;
                
                if ( req instanceof SearchRequest )
                {
                    referral = getReferralOnAncestorForSearch( session, ( SearchRequest ) req, referralAncestor );
                }
                else
                {
                    referral = getReferralOnAncestor( session, reqTargetDn, req, referralAncestor );
                }
                
                result.setResultCode( ResultCodeEnum.REFERRAL );
                result.setReferral( referral );
                session.getIoSession().write( req.getResultResponse() );
            }
            catch ( Exception e )
            {
                handleException( session, req, e );
            }
        }
    }

    
    /**
     * Handles processing a referral response on a target entry which is a 
     * referral.  It will for any request that returns an LdapResult in it's 
     * response.
     *
     * @param session the session to use for processing
     * @param reqTargetDn the dn of the target entry of the request
     * @param req the request
     * @param entry the entry associated with the request
     */
    private void handleReferralEntry( LdapSession session, LdapDN reqTargetDn, T req, ClonedServerEntry entry )
    {
        LdapResult result = req.getResultResponse().getLdapResult();
        ReferralImpl refs = new ReferralImpl();
        result.setReferral( refs );
        result.setResultCode( ResultCodeEnum.REFERRAL );
        result.setErrorMessage( "Encountered referral attempting to handle request." );
        result.setMatchedDn( reqTargetDn );

        EntryAttribute refAttr = entry.getOriginalEntry().get( SchemaConstants.REF_AT );
        for ( Value<?> refval : refAttr )
        {
            refs.addLdapUrl( ( String ) refval.get() );
        }

        session.getIoSession().write( req.getResultResponse() );
    }
    
    
    /**
     * Handles processing a referral response on a target entry which is a 
     * referral.  It will for any request that returns an LdapResult in it's 
     * response.
     *
     * @param session the session to use for processing
     * @param reqTargetDn the dn of the target entry of the request
     * @param req the request
     * @param entry the entry associated with the request
     */
    private void handleReferralEntryForSearch( LdapSession session, SearchRequest req, ClonedServerEntry entry )
        throws Exception
    {
        LdapResult result = req.getResultResponse().getLdapResult();
        ReferralImpl referral = new ReferralImpl();
        result.setReferral( referral );
        result.setResultCode( ResultCodeEnum.REFERRAL );
        result.setErrorMessage( "Encountered referral attempting to handle request." );
        result.setMatchedDn( req.getBase() );

        EntryAttribute refAttr = entry.getOriginalEntry().get( SchemaConstants.REF_AT );
        for ( Value<?> refval : refAttr )
        {
            String refstr = ( String ) refval.get();
            
            // need to add non-ldap URLs as-is
            if ( ! refstr.startsWith( "ldap" ) )
            {
                referral.addLdapUrl( refstr );
                continue;
            }
            
            // parse the ref value and normalize the DN  
            LdapURL ldapUrl = new LdapURL();
            try
            {
                ldapUrl.parse( refstr.toCharArray() );
            }
            catch ( LdapURLEncodingException e )
            {
                LOG.error( "Bad URL ({}) for ref in {}.  Reference will be ignored.", refstr, entry );
                continue;
            }
            
            ldapUrl.setForceScopeRendering( true );
            ldapUrl.setAttributes( req.getAttributes() );
            ldapUrl.setScope( req.getScope().getJndiScope() );
            referral.addLdapUrl( ldapUrl.toString() );
        }

        session.getIoSession().write( req.getResultResponse() );
    }
    
    
    /**
     * Handles processing with referrals without ManageDsaIT control and with 
     * an ancestor that is a referral.  The original entry was not found and 
     * the walk of the ancestry returned a referral.
     * 
     * @param referralAncestor the farthest referral ancestor of the missing 
     * entry  
     */
    public Referral getReferralOnAncestor( LdapSession session, LdapDN reqTargetDn, T req, 
        ClonedServerEntry referralAncestor ) throws Exception
    {
        LOG.debug( "Inside getReferralOnAncestor()" );
        
        ServerAttribute refAttr = ( ServerAttribute ) referralAncestor.getOriginalEntry()
            .get( SchemaConstants.REF_AT );
        Referral referral = new ReferralImpl();

        for ( Value<?> value : refAttr )
        {
            String ref = ( String ) value.get();

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
                LOG.error( "Bad URL ({}) for ref in {}.  Reference will be ignored.", ref, referralAncestor );
            }
            
            LdapDN urlDn = new LdapDN( ldapUrl.getDn().getUpName() );
            urlDn.normalize( session.getCoreSession().getDirectoryService().getRegistries()
                .getAttributeTypeRegistry().getNormalizerMapping() ); 
            
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
            LdapDN extra = new LdapDN();

            // TODO - fix this by access unormalized RDN values
            // seems we have to do this because get returns normalized rdns
            LdapDN reqUnnormalizedDn = new LdapDN( reqTargetDn.getUpName() );
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
            buf.append( LdapURL.urlEncode( urlDn.getUpName(), false ) );
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
    public Referral getReferralOnAncestorForSearch( LdapSession session, SearchRequest req, 
        ClonedServerEntry referralAncestor ) throws Exception
    {
        LOG.debug( "Inside getReferralOnAncestor()" );
     
        ServerAttribute refAttr = ( ServerAttribute ) referralAncestor.getOriginalEntry()
            .get( SchemaConstants.REF_AT );
        Referral referral = new ReferralImpl();

        for ( Value<?> value : refAttr )
        {
            String ref = ( String ) value.get();

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
                LOG.error( "Bad URL ({}) for ref in {}.  Reference will be ignored.", ref, referralAncestor );
            }
            
            // Normalize the DN to check for same dn
            LdapDN urlDn = new LdapDN( ldapUrl.getDn().getUpName() );
            urlDn.normalize( session.getCoreSession().getDirectoryService().getRegistries()
                .getAttributeTypeRegistry().getNormalizerMapping() ); 
            
            if ( urlDn.getNormName().equals( req.getBase().getNormName() ) )
            {
                ldapUrl.setForceScopeRendering( true );
                ldapUrl.setAttributes( req.getAttributes() );
                ldapUrl.setScope( req.getScope().getJndiScope() );
                referral.addLdapUrl( ldapUrl.toString() );
                continue;
            }
            
            /*
             * If we get here then the DN of the referral was not the same as the 
             * DN of the ref LDAP URL.  We must calculate the remaining (difference)
             * name past the farthest referral DN which the target name extends.
             */
            int diff = req.getBase().size() - referralAncestor.getDn().size();
            LdapDN extra = new LdapDN();

            // TODO - fix this by access unormalized RDN values
            // seems we have to do this because get returns normalized rdns
            LdapDN reqUnnormalizedDn = new LdapDN( req.getBase().getUpName() );
            for ( int jj = 0; jj < diff; jj++ )
            {
                extra.add( reqUnnormalizedDn.get( referralAncestor.getDn().size() + jj ) );
            }

            ldapUrl.getDn().addAll( extra );
            ldapUrl.setForceScopeRendering( true );
            ldapUrl.setAttributes( req.getAttributes() );
            ldapUrl.setScope( req.getScope().getJndiScope() );
            referral.addLdapUrl( ldapUrl.toString() );
        }
        
        return referral;
    }
    
    
    /**
     * Handles processing with referrals without ManageDsaIT control.
     */
    public void handleException( LdapSession session, T req, Exception e )
    {
        LdapResult result = req.getResultResponse().getLdapResult();

        /*
         * Set the result code or guess the best option.
         */
        ResultCodeEnum code;
        if ( e instanceof LdapException )
        {
            code = ( ( LdapException ) e ).getResultCode();
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
        String msg = code.toString() + ": failed for " + req + ": " + e.getMessage();
        LOG.debug( msg, e );
        
        if ( IS_DEBUG )
        {
            msg += ":\n" + ExceptionUtils.getStackTrace( e );
        }
        
        result.setErrorMessage( msg );

        if ( e instanceof NamingException )
        {
            NamingException ne = ( NamingException ) e;

            // Add the matchedDN if necessary
            boolean setMatchedDn = 
                code == ResultCodeEnum.NO_SUCH_OBJECT             || 
                code == ResultCodeEnum.ALIAS_PROBLEM              ||
                code == ResultCodeEnum.INVALID_DN_SYNTAX          || 
                code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM;
            
            if ( ( ne.getResolvedName() != null ) && setMatchedDn )
            {
                result.setMatchedDn( ( LdapDN ) ne.getResolvedName() );
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
    public abstract void handleIgnoringReferrals( LdapSession session, LdapDN reqTargetDn, 
        ClonedServerEntry entry, T req );
}
