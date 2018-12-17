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
package org.apache.directory.server.ldap.handlers.extended;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.api.ldap.extras.extended.ads_impl.endTransaction.EndTransactionRequestDecorator;
import org.apache.directory.api.ldap.extras.extended.endTransaction.EndTransactionRequest;
import org.apache.directory.api.ldap.extras.extended.endTransaction.EndTransactionResponse;
import org.apache.directory.api.ldap.extras.extended.endTransaction.EndTransactionResponseImpl;
import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.ExtendedResponse;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An handler to manage the EndTransaction extended request operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EndTransactionHandler implements ExtendedOperationHandler<ExtendedRequest, ExtendedResponse>
{
    private static final Logger LOG = LoggerFactory.getLogger( EndTransactionHandler.class );
    public static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> set = new HashSet<>( 2 );
        set.add( EndTransactionRequest.EXTENSION_OID );
        set.add( EndTransactionResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    /**
     * {@inheritDoc}
     */
    public String getOid()
    {
        return EndTransactionRequest.EXTENSION_OID;
    }


    /**
     * {@inheritDoc}
     */
    public void handleExtendedOperation( LdapSession session, ExtendedRequest req ) throws Exception
    {
        LOG.debug( "EndTransaction requested" );
        
        // We need to create a new transaction ID for the current session.
        // If the current session is already processing a transaction, we will return an error
        CoreSession coreSession = session.getCoreSession();
        coreSession.endSessionTransaction( ( ( EndTransactionRequestDecorator ) req ).getCommit() );

        EndTransactionResponse endTransactionResponse = new EndTransactionResponseImpl( req.getMessageId() );

        // write the response
        session.getIoSession().write( endTransactionResponse );
    }


    /**
     * {@inheritDoc}
     */
    public Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    /**
     * {@inheritDoc}
     */
    public void setLdapServer( LdapServer ldapServer )
    {
    }
}
