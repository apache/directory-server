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


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.ldap.LdapContext;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.sasl.AuthorizeCallback;

import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GssapiCallbackHandler extends AbstractSaslCallbackHandler
{
    private static final Logger log = LoggerFactory.getLogger( GssapiCallbackHandler.class );

    private IoSession session;
    private Object message;


    /**
     * Creates a new instance of GssapiCallbackHandler.
     *
     * @param session
     * @param message
     */
    public GssapiCallbackHandler( IoSession session, Object message )
    {
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
        log.debug( "Processing conversion of principal name to DN." );

        Hashtable env = getEnvironment( session, message );

        LdapContext ctx = getContext( session, message, env );

        String username = authorizeCB.getAuthorizationID();

        GetPrincipal getPrincipal = new GetPrincipal( new KerberosPrincipal( username ) );
        // Don't actually want the entry, rather the hacked in dn.
        getPrincipal.execute( ctx, null );
        String bindDn = getPrincipal.getDn();

        log.debug( "Converted username " + username + " to DN " + bindDn + "." );
        session.setAttribute( Context.SECURITY_PRINCIPAL, bindDn );

        authorizeCB.setAuthorizedID( bindDn );
        authorizeCB.setAuthorized( true );
    }
}
