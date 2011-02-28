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


import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.exception.LdapNoPermissionException;


/**
 * An {@link Authenticator} that handles anonymous connections
 * (type <tt>'none'</tt>).
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AnonymousAuthenticator extends AbstractAuthenticator
{
    /**
     * Creates a new instance.
     */
    public AnonymousAuthenticator()
    {
        super( AuthenticationLevel.NONE );
    }


    /**
     * If the context is not configured to allow anonymous connections,
     * this method throws a {@link javax.naming.NoPermissionException}.
     */
    public LdapPrincipal authenticate( BindOperationContext bindContext ) throws LdapNoPermissionException
    {
        // We only allow Anonymous binds if the service allows them
        if ( getDirectoryService().isAllowAnonymousAccess() )
        {
            return getDirectoryService().getAdminSession().getAnonymousPrincipal();
        }
        else
        {
            LOG.info( "Cannot authenticate as anonymous, the server does not allow it" );
            throw new LdapNoPermissionException( I18n.err( I18n.ERR_228 ) );
        }
    }
}
