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
package org.apache.directory.server.ldap.support.bind;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.operations.GetPrincipal;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.ldap.LdapContext;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.sasl.AuthorizeCallback;
import java.util.Hashtable;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GssapiCallbackHandler extends AbstractSaslCallbackHandler
{
    private static final Logger LOG = LoggerFactory.getLogger( GssapiCallbackHandler.class );

    private IoSession session;
    private Object message;


    /**
     * Creates a new instance of GssapiCallbackHandler.
     *
     * @param session the mina IO session
     * @param message the bind message
     * @param directoryService the directory service core
     */
    public GssapiCallbackHandler( DirectoryService directoryService, IoSession session, Object message )
    {
        super( directoryService );
        this.session = session;
        this.message = message;
    }


    protected String lookupPassword( String username, String password )
    {
        // do nothing, password not used by GSSAPI
        return null;
    }


    protected void authorize( AuthorizeCallback authorizeCB )
    {
        LOG.debug( "Processing conversion of principal name to DN." );

        Hashtable<String, Object> env = getEnvironment( session );

        LdapContext ctx = getContext( session, message, env );

        String username = authorizeCB.getAuthorizationID();

        GetPrincipal getPrincipal = new GetPrincipal( new KerberosPrincipal( username ) );
        PrincipalStoreEntry entry = ( PrincipalStoreEntry ) getPrincipal.execute( ctx, null );
        String bindDn = entry.getDistinguishedName();

        LOG.debug( "Converted username {} to DN {}.", username, bindDn );
        session.setAttribute( Context.SECURITY_PRINCIPAL, bindDn );

        authorizeCB.setAuthorizedID( bindDn );
        authorizeCB.setAuthorized( true );
    }
}
