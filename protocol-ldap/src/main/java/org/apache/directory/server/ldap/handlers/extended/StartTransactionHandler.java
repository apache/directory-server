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

import org.apache.directory.api.ldap.extras.extended.startTransaction.StartTransactionRequest;
import org.apache.directory.api.ldap.extras.extended.startTransaction.StartTransactionResponse;
import org.apache.directory.api.ldap.extras.extended.startTransaction.StartTransactionResponseImpl;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An handler to manage the StartTransaction extended request operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StartTransactionHandler implements ExtendedOperationHandler<StartTransactionRequest, StartTransactionResponse>
{
    private static final Logger LOG = LoggerFactory.getLogger( StartTransactionHandler.class );
    public static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> set = new HashSet<>( 2 );
        set.add( StartTransactionRequest.EXTENSION_OID );
        set.add( StartTransactionResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    /**
     * {@inheritDoc}
     */
    public String getOid()
    {
        return StartTransactionRequest.EXTENSION_OID;
    }


    /**
     * {@inheritDoc}
     */
    public void handleExtendedOperation( LdapSession requestor, StartTransactionRequest req ) throws Exception
    {
        LOG.debug( "StartTransaction requested" );
        
        // We need to create a new transaction ID for the current session.
        // If the current session is already processing a transaction, we will return an error
        StartTransactionResponse startTransactionResponse = new StartTransactionResponseImpl();
        
        requestor.getCoreSession().beginSessionTransaction();

        // write the response
        requestor.getIoSession().write( startTransactionResponse );
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
