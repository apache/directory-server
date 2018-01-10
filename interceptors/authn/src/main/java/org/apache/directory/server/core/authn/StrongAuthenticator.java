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
package org.apache.directory.server.core.authn;


import java.net.SocketAddress;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.mina.core.session.IoSession;


/**
 * An {@link Authenticator} that handles SASL connections (X.501 authentication
 * level <tt>'strong'</tt>).  The principal has been authenticated during SASL
 * negotiation; therefore, no additional authentication is necessary in this
 * {@link Authenticator}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StrongAuthenticator extends AbstractAuthenticator
{
    /**
     * Creates a new instance.
     */
    public StrongAuthenticator()
    {
        super( AuthenticationLevel.STRONG );
    }


    /**
     * Creates a new instance of SaslAuthenticator.
     */
    public StrongAuthenticator( Dn baseDn )
    {
        super( AuthenticationLevel.STRONG, baseDn );
    }


    /**
     * User has already been authenticated during SASL negotiation. Set the authentication level
     * to strong and return an {@link LdapPrincipal}.
     */
    @Override
    public LdapPrincipal authenticate( BindOperationContext bindContext ) throws LdapAuthenticationException
    {
        // Possibly check if user account is disabled, other account checks.
        LdapPrincipal principal = new LdapPrincipal( getDirectoryService().getSchemaManager(), bindContext.getDn(),
            AuthenticationLevel.STRONG );

        IoSession session = bindContext.getIoSession();

        if ( session != null )
        {
            SocketAddress clientAddress = session.getRemoteAddress();
            principal.setClientAddress( clientAddress );
            SocketAddress serverAddress = session.getServiceAddress();
            principal.setServerAddress( serverAddress );
        }

        return principal;
    }
}
