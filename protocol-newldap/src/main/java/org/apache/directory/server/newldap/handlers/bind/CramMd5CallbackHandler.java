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
package org.apache.directory.server.newldap.handlers.bind;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.newldap.LdapSession;
import org.apache.directory.shared.ldap.message.BindRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.security.sasl.AuthorizeCallback;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CramMd5CallbackHandler extends AbstractSaslCallbackHandler
{
    private static final Logger LOG = LoggerFactory.getLogger( CramMd5CallbackHandler.class );

    private LdapSession session;

    private String bindDn;
    private String userPassword;


    /**
     * Creates a new instance of CramMd5CallbackHandler.
     *
     * @param session the mina IoSession
     * @param bindRequest the bind message
     * @param directoryService the directory service core
     */
    public CramMd5CallbackHandler( DirectoryService directoryService,  LdapSession session, BindRequest bindRequest )
    {
        super( directoryService );
        this.session = session;
    }


    protected String lookupPassword( String username, String realm )
    {
        GetBindDn getDn = new GetBindDn( username );

        // Don't actually want the entry, rather the hacked in dn.
        getDn.execute( session, null );
        bindDn = getDn.getBindDn();
        userPassword = getDn.getUserPassword();

        return userPassword;
    }


    protected void authorize( AuthorizeCallback authorizeCB )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "Converted username " + getUsername() + " to DN " + bindDn + " with password " + userPassword );
        }

        session.getIoSession().setAttribute( Context.SECURITY_PRINCIPAL, bindDn );

        authorizeCB.setAuthorizedID( bindDn );
        authorizeCB.setAuthorized( true );
    }
}
