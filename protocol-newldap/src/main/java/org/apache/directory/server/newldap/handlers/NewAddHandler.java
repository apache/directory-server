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


import javax.naming.NamingException;
import javax.naming.ReferralException;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.newldap.LdapSession;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.directory.server.newldap.LdapProtocolUtils.*;


/**
 * An LDAP add operation {@link AddRequest} handler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NewAddHandler extends LdapRequestHandler<AddRequest>
{
    private static final Logger LOG = LoggerFactory.getLogger( NewAddHandler.class );
    
    
    /**
     * (non-Javadoc)
     * @see org.apache.directory.server.newldap.handlers.LdapRequestHandler#
     * handle(org.apache.directory.server.newldap.LdapSession, org.apache.directory.shared.ldap.message.Request)
     */
    public void handle( LdapSession session, AddRequest request ) throws Exception
    {
        LdapResult result = request.getResultResponse().getLdapResult();

        try
        {
            ServerEntry entry = ServerEntryUtils.toServerEntry( request.getAttributes(), request.getEntry(), 
                session.getCoreSession().getDirectoryService().getRegistries() );
            AddOperationContext opContext = new AddOperationContext( session.getCoreSession(), entry );
            setRequestControls( opContext, request );
            session.getCoreSession().getDirectoryService().getOperationManager().add( opContext );
            setResponseControls( opContext, request.getResultResponse() );
        }
        catch( ReferralException e )
        {
            ReferralImpl refs = new ReferralImpl();
            result.setReferral( refs );
            result.setResultCode( ResultCodeEnum.REFERRAL );
            result.setErrorMessage( "Encountered referral attempting to handle add request." );
            
            if ( e.getResolvedName() != null )
            {
                result.setMatchedDn( new LdapDN( e.getResolvedName().toString() ) );
            }
            
            do
            {
                refs.addLdapUrl( ( String ) e.getReferralInfo() );
            }
            while ( e.skipReferral() );
            
            session.getIoSession().write( request.getResultResponse() );
        }
        catch ( Throwable t )
        {
            ResultCodeEnum resultCode = ResultCodeEnum.OTHER;
            
            if ( t instanceof LdapException )
            {
                resultCode = ( ( LdapException ) t ).getResultCode();
            }
            else
            {
                resultCode = ResultCodeEnum.getBestEstimate( t, request.getType() );
            }
            
            result.setResultCode( resultCode );
            
            String msg = session + "failed to add entry " + request.getEntry() + ": " + t.getMessage();
            if ( LOG.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( t );
            }

            result.setErrorMessage( msg );
            result.setErrorMessage( msg );

            boolean setMatchedDn = 
                resultCode == ResultCodeEnum.NO_SUCH_OBJECT             || 
                resultCode == ResultCodeEnum.ALIAS_PROBLEM              ||
                resultCode == ResultCodeEnum.INVALID_DN_SYNTAX          || 
                resultCode == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM;
            
            if ( setMatchedDn )
            {
                if ( t instanceof NamingException )
                {
                    NamingException ne = ( NamingException ) t;
                    if ( ne.getResolvedName() != null )
                    {
                        result.setMatchedDn( ( LdapDN ) ne.getResolvedName() );
                    }
                }
                else
                {
                    // TODO - add ability to get the matched DN from the core via the session
//                  coreSession.getMatchedDn( request.getEntry() );
                }
            }

            session.getIoSession().write( request.getResultResponse() );
        }
    }
}
