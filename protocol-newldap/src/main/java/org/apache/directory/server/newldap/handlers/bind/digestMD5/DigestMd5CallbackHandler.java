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
package org.apache.directory.server.newldap.handlers.bind.digestMD5;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.newldap.handlers.bind.AbstractSaslCallbackHandler;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.security.sasl.AuthorizeCallback;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DigestMd5CallbackHandler extends AbstractSaslCallbackHandler
{
    private static final Logger LOG = LoggerFactory.getLogger( DigestMd5CallbackHandler.class );

    private IoSession session;

    private String bindDn;
    private String userPassword;


    /**
     * Creates a new instance of DigestMd5CallbackHandler.
     *
     * @param session the mina IoSession
     * @param bindRequest the bind message
     * @param directoryService the directory service core
     */
    public DigestMd5CallbackHandler( DirectoryService directoryService, BindRequest bindRequest )
    {
        super( directoryService, bindRequest );
    }


    // TODO - should return not be a byte[]
    protected EntryAttribute lookupPassword( String username, String realm )
    {
        // TODO - Use realm with multi-realm support.
        throw new NotImplementedException();
    }


    protected void authorize( AuthorizeCallback authorizeCB )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "Converted username " + getUsername() + " to DN " + bindDn + " with password " + userPassword + "." );
        }

        session.setAttribute( Context.SECURITY_PRINCIPAL, bindDn );

        authorizeCB.setAuthorizedID( bindDn );
        authorizeCB.setAuthorized( true );
    }
}
