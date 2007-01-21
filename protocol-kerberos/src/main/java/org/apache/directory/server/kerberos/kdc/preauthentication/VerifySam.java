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
package org.apache.directory.server.kerberos.kdc.preauthentication;


import javax.security.auth.kerberos.KerberosKey;

import org.apache.directory.server.kerberos.kdc.authentication.AuthenticationContext;
import org.apache.directory.server.kerberos.sam.SamException;
import org.apache.directory.server.kerberos.sam.SamSubsystem;
import org.apache.directory.server.kerberos.sam.TimestampChecker;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationDataType;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VerifySam extends VerifierBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( VerifySam.class );

    static
    {
        log.debug( "Initializing SAM subsystem" );
        SamSubsystem.getInstance().setIntegrityChecker( new TimestampChecker() );
    }


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        log.debug( "Verifying using SAM subsystem." );
        AuthenticationContext authContext = ( AuthenticationContext ) session.getAttribute( getContextKey() );
        KdcRequest request = authContext.getRequest();
        PrincipalStoreEntry clientEntry = authContext.getClientEntry();
        String clientName = clientEntry.getPrincipal().getName();

        EncryptionKey clientKey = null;

        if ( clientEntry.getSamType() != null )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "entry for client principal " + clientName
                    + " has a valid SAM type: invoking SAM subsystem for pre-authentication" );
            }

            PreAuthenticationData[] preAuthData = request.getPreAuthData();

            if ( preAuthData == null || preAuthData.length == 0 )
            {
                throw new KerberosException( ErrorType.KDC_ERR_PREAUTH_REQUIRED, preparePreAuthenticationError() );
            }

            try
            {
                for ( int ii = 0; ii < preAuthData.length; ii++ )
                {
                    if ( preAuthData[ii].getDataType().equals( PreAuthenticationDataType.PA_ENC_TIMESTAMP ) )
                    {
                        KerberosKey samKey = SamSubsystem.getInstance().verify( clientEntry,
                            preAuthData[ii].getDataValue() );
                        clientKey = new EncryptionKey( EncryptionType.getTypeByOrdinal( samKey.getKeyType() ), samKey
                            .getEncoded() );
                    }
                }
            }
            catch ( SamException se )
            {
                throw new KerberosException( ErrorType.KRB_ERR_GENERIC, se.getMessage() );
            }

            authContext.setClientKey( clientKey );

            if ( log.isDebugEnabled() )
            {
                log.debug( "Pre-authentication using SAM subsystem successful for " + clientName + "." );
            }
        }

        next.execute( session, message );
    }
}
