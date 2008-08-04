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
package org.apache.directory.server.newldap.handlers;


import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.newldap.LdapSession;
import org.apache.directory.shared.ldap.codec.add.AddRequest;
import org.apache.directory.shared.ldap.codec.bind.BindRequest;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequest;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequest;
import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.message.CompareRequest;
import org.apache.directory.shared.ldap.message.DeleteRequest;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.Referral;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.SingleReplyRequest;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
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
public abstract class SingleReplyRequestHandler<T extends SingleReplyRequest> extends LdapRequestHandler<T>
{
    private static final Logger LOG = LoggerFactory.getLogger( SingleReplyRequestHandler.class );
    
    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    
    /* (non-Javadoc)
     * @see org.apache.directory.server.newldap.handlers.LdapRequestHandler#handle(org.apache.directory.server.newldap.LdapSession, org.apache.directory.shared.ldap.message.Request)
     */
    @Override
    public final void handle( LdapSession session, T req ) throws Exception
    {
        LOG.debug( "Handling single reply request: {}", req );
        
        LdapDN reqTargetDn = null;
        
        switch ( req.getResponseType() )
        {
            case ADD_RESPONSE:
                reqTargetDn = ( ( AddRequest ) req ).getEntry();
                break;
            case BIND_RESPONSE:
                reqTargetDn = ( ( BindRequest ) req ).getName();
                break;
            case COMPARE_RESPONSE:
                reqTargetDn = ( ( CompareRequest ) req ).getName();
                break;
            case DEL_RESPONSE:
                reqTargetDn = ( ( DeleteRequest ) req ).getName();
                break;
            case EXTENDED_RESP:
                throw new IllegalStateException( 
                    "Although ExtendedRequests are SingleReplyRequests they're not handled" +
                    " using this base class.  They have no target entry unlike the rest of" +
                    " the SingleReplyRequests" );
            case MOD_DN_RESPONSE:
                reqTargetDn = ( ( ModifyDNRequest ) req ).getEntry();
                break;
            case MODIFY_RESPONSE:
                reqTargetDn = ( ( ModifyRequest ) req ).getObject();
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
        
        // -------------------------------------------------------------------
        // Handle Existing Entry
        // -------------------------------------------------------------------
        
        if ( entry != null )
        {
            try
            {
                if ( entry.getOriginalEntry().contains( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.REFERRAL_OC ) )
                {
                    LOG.debug( "Entry is a referral: {}", entry );
                    handleReferralEntry( session, reqTargetDn, req, entry );
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
            LdapDN lastMatchedDn = null;
            LdapDN dn = ( LdapDN ) reqTargetDn.clone();
            
            while ( ! dn.isEmpty() )
            {
                LOG.debug( "Walking ancestors of {} to find referrals.", dn );
                
                try
                {
                    entry = session.getCoreSession().lookup( dn );
                    EntryAttribute oc = entry.getOriginalEntry().get( SchemaConstants.OBJECT_CLASS_AT );
                    
                    if ( oc.contains( SchemaConstants.REFERRAL_OC ) )
                    {
                        referralAncestor = entry;
                    }

                    // set last matched DN only if not set yet because we want 
                    // the closest ancestor for this value regardless of what 
                    // kind of entry (referral or normal) it is.
                    
                    if ( lastMatchedDn == null )
                    {
                        lastMatchedDn = ( LdapDN ) dn.clone();
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
                catch ( Exception e )
                {
                    handleException( session, req, e );
                }
            }

            if ( referralAncestor == null )
            {
                result.setMatchedDn( lastMatchedDn );
                result.setErrorMessage( "Entry not found." );
                result.setResultCode( ResultCodeEnum.NO_SUCH_OBJECT );
                session.getIoSession().write( req.getResultResponse() );
                return;
            }
              
            // if we get here then we have a valid referral ancestor
            handleReferralOnAncestor( session, reqTargetDn, req, referralAncestor, lastMatchedDn );
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
     * Handles processing with referrals without ManageDsaIT control and with 
     * an ancestor that is a referral.  The original entry was not found and 
     * the walk of the ancestry returned a referral.
     * 
     * @param referralAncestor the farthest referral ancestor of the missing 
     * entry  
     */
    public void handleReferralOnAncestor( LdapSession session, LdapDN reqTargetDn, T req, 
        ClonedServerEntry referralAncestor, LdapDN lastMatchedDn )
    {
        LOG.debug( "Inside handleReferralOnAncestor()" );
        
        try
        {
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
            
            LdapResult result = req.getResultResponse().getLdapResult();
            result.setMatchedDn( lastMatchedDn );
            result.setResultCode( ResultCodeEnum.REFERRAL );
            result.setReferral( referral );
            
            session.getIoSession().write( req.getResultResponse() );
        }
        catch ( Exception e )
        {
            handleException( session, req, e );
        }
    }
    
    
    /**
     * Handles processing with referrals without ManageDsaIT control.
     */
    public void handleException( LdapSession session, T req, Exception e )
    {
        LdapResult result = req.getResultResponse().getLdapResult();

        String msg = "failed for " + req + ": " + e.getMessage();
        LOG.error( msg, e );

        if ( IS_DEBUG )
        {
            msg += ":\n" + ExceptionUtils.getStackTrace( e );
        }

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
        result.setErrorMessage( msg );

        if ( e instanceof NamingException )
        {
            NamingException ne = ( NamingException ) e;

            if ( ( ne.getResolvedName() != null )
                && ( ( code == ResultCodeEnum.NO_SUCH_OBJECT ) || ( code == ResultCodeEnum.ALIAS_PROBLEM )
                    || ( code == ResultCodeEnum.INVALID_DN_SYNTAX ) || ( code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM ) ) )
            {
                result.setMatchedDn( (LdapDN)ne.getResolvedName() );
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
