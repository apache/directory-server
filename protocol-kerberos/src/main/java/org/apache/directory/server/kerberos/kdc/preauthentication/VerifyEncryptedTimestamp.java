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


import java.io.IOException;
import java.util.List;

import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.kdc.authentication.AuthenticationContext;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.io.decoder.EncryptedDataDecoder;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthEncryptedTimestamp;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;
import org.apache.directory.server.kerberos.shared.messages.value.types.PreAuthenticationDataType;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class VerifyEncryptedTimestamp extends VerifierBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( VerifyEncryptedTimestamp.class );


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        AuthenticationContext authContext = ( AuthenticationContext ) session.getAttribute( getContextKey() );

        if ( authContext.getClientKey() != null )
        {
            next.execute( session, message );
        }

        log.debug( "Verifying using encrypted timestamp." );
        KdcConfiguration config = authContext.getConfig();
        KdcRequest request = authContext.getRequest();
        CipherTextHandler cipherTextHandler = authContext.getCipherTextHandler();
        PrincipalStoreEntry clientEntry = authContext.getClientEntry();
        String clientName = clientEntry.getPrincipal().getName();

        EncryptionKey clientKey = null;

        if ( clientEntry.getSamType() == null )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Entry for client principal {} has no SAM type.  Proceeding with standard pre-authentication.", clientName );
            }

            EncryptionType encryptionType = authContext.getEncryptionType();
            clientKey = clientEntry.getKeyMap().get( encryptionType );

            if ( clientKey == null )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_NULL_KEY );
            }

            if ( config.isPaEncTimestampRequired() )
            {
                List<PreAuthenticationData> preAuthDatas = request.getPreAuthData();

                if ( preAuthDatas == null )
                {
                    throw new KerberosException( KerberosErrorType.KDC_ERR_PREAUTH_REQUIRED,
                        preparePreAuthenticationError( config.getEncryptionTypes() ) );
                }

                PreAuthEncryptedTimestamp timestamp = null;

                for ( PreAuthenticationData preAuthData:preAuthDatas )
                {
                    if ( preAuthData.getDataType().equals( PreAuthenticationDataType.PA_ENC_TIMESTAMP ) )
                    {
                        EncryptedData dataValue;

                        try
                        {
                            dataValue = EncryptedDataDecoder.decode( preAuthData.getDataValue() );
                        }
                        catch ( IOException ioe )
                        {
                            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_BAD_INTEGRITY, ioe );
                        }
                        catch ( ClassCastException cce )
                        {
                            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_BAD_INTEGRITY, cce );
                        }

                        timestamp = ( PreAuthEncryptedTimestamp ) cipherTextHandler.unseal( PreAuthEncryptedTimestamp.class,
                            clientKey, dataValue, KeyUsage.NUMBER1 );
                    }
                }

                if ( timestamp == null )
                {
                    throw new KerberosException( KerberosErrorType.KDC_ERR_PREAUTH_REQUIRED,
                        preparePreAuthenticationError( config.getEncryptionTypes() ) );
                }

                if ( !timestamp.getTimeStamp().isInClockSkew( config.getAllowableClockSkew() ) )
                {
                    throw new KerberosException( KerberosErrorType.KDC_ERR_PREAUTH_FAILED );
                }
            }
        }

        authContext.setClientKey( clientKey );
        authContext.setPreAuthenticated( true );

        if ( log.isDebugEnabled() )
        {
            log.debug( "Pre-authentication by encrypted timestamp successful for {}.", clientName );
        }

        next.execute( session, message );
    }
}
