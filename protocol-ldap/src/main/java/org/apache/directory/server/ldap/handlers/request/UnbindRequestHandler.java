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
package org.apache.directory.server.ldap.handlers.request;


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.LdapRequestHandler;
import org.apache.directory.shared.ldap.model.message.UnbindRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A no reply protocol handler implementation for LDAP {@link
 * org.apache.directory.shared.ldap.model.message.UnbindRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UnbindRequestHandler extends LdapRequestHandler<UnbindRequest>
{
    private static final Logger LOG = LoggerFactory.getLogger( UnbindRequestHandler.class );


    public void handle( LdapSession session, UnbindRequest unbindRequest ) throws Exception
    {
        LOG.debug( "Received: {}", unbindRequest );

        try
        {
            session.closeAllPagedSearches();
            session.getCoreSession().unbind( unbindRequest );
            session.getIoSession().close( true );
            ldapServer.getLdapSessionManager().removeLdapSession( session.getIoSession() );
        }
        catch ( Throwable t )
        {
            LOG.error( I18n.err( I18n.ERR_169 ), t );
        }
    }
}