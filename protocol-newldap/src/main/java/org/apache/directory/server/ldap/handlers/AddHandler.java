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
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An LDAP add operation {@link AddRequest} handler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AddHandler extends ReferralAwareRequestHandler<AddRequest>
{
	/** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AddHandler.class );
    
    
    /**
     * @see ReferralAwareRequestHandler#handleIgnoringReferrals(LdapSession, LdapDN, ClonedServerEntry, 
     * org.apache.directory.shared.ldap.message.SingleReplyRequest)
     */
    public void handleIgnoringReferrals( LdapSession session, LdapDN reqTargetDn, 
        ClonedServerEntry entry, AddRequest req ) 
    {
        LOG.debug( "Handling add request while ignoring referrals: {}", req );
        LdapResult result = req.getResultResponse().getLdapResult();

        try
        {
        	// Call the underlying layer to inject the new entry 
            session.getCoreSession().add( req );

            // If success, here now, otherwise, we would have an exception.
            result.setResultCode( ResultCodeEnum.SUCCESS );
            
            // Write the AddResponse message
            session.getIoSession().write( req.getResultResponse() );
        }
        catch ( Exception e )
        {
            handleException( session, req, e );
        }
    }
}
