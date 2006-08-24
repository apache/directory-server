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
package org.apache.directory.server.changepw.service;


import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.changepw.exceptions.ChangePasswordException;
import org.apache.directory.server.changepw.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProcessPasswordChange extends CommandBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( ProcessPasswordChange.class );


    public boolean execute( Context context ) throws Exception
    {
        ChangePasswordContext changepwContext = ( ChangePasswordContext ) context;

        PrincipalStore store = changepwContext.getStore();
        Authenticator authenticator = changepwContext.getAuthenticator();
        String password = changepwContext.getPassword();

        // usec and seq-number must be present per MS but aren't in legacy kpasswd
        // seq-number must have same value as authenticator
        // ignore r-address

        // generate key from password
        KerberosPrincipal clientPrincipal = authenticator.getClientPrincipal();
        KerberosKey newKey = new KerberosKey( clientPrincipal, password.toCharArray(), "DES" );

        // store password in database
        try
        {
            String principalName = store.changePassword( clientPrincipal, newKey );
            log.debug( "Successfully modified principal {}", principalName );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_HARDERROR );
        }

        return CONTINUE_CHAIN;
    }
}
