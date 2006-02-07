/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.kerberos.kdc.preauthentication;

import java.io.IOException;

import org.apache.kerberos.exceptions.ErrorType;
import org.apache.kerberos.exceptions.KerberosException;
import org.apache.kerberos.io.decoder.EncryptedDataDecoder;
import org.apache.kerberos.kdc.KdcConfiguration;
import org.apache.kerberos.kdc.authentication.AuthenticationContext;
import org.apache.kerberos.messages.KdcRequest;
import org.apache.kerberos.messages.value.EncryptedData;
import org.apache.kerberos.messages.value.EncryptedTimeStamp;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.messages.value.PreAuthenticationData;
import org.apache.kerberos.messages.value.PreAuthenticationDataType;
import org.apache.kerberos.service.LockBox;
import org.apache.kerberos.store.PrincipalStoreEntry;
import org.apache.protocol.common.chain.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerifyEncryptedTimestamp extends VerifierBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( VerifyEncryptedTimestamp.class );

    public boolean execute( Context ctx ) throws Exception
    {
        AuthenticationContext authContext = (AuthenticationContext) ctx;

        if ( authContext.getClientKey() != null )
        {
            return CONTINUE_CHAIN;
        }

        log.debug( "Verifying using encrypted timestamp." );
        KdcConfiguration config = authContext.getConfig();
        KdcRequest request = authContext.getRequest();
        LockBox lockBox = authContext.getLockBox();
        PrincipalStoreEntry clientEntry = authContext.getClientEntry();
        String clientName = clientEntry.getPrincipal().getName();

        EncryptionKey clientKey = null;

        if ( clientEntry.getSamType() == null )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "entry for client principal " + clientName
                        + " has no SAM type: proceeding with standard pre-authentication" );
            }

            clientKey = clientEntry.getEncryptionKey();

            if ( clientKey == null )
            {
                throw new KerberosException( ErrorType.KDC_ERR_NULL_KEY );
            }

            if ( config.isPaEncTimestampRequired() )
            {
                PreAuthenticationData[] preAuthData = request.getPreAuthData();

                if ( preAuthData == null )
                {
                    throw new KerberosException( ErrorType.KDC_ERR_PREAUTH_REQUIRED,
                            preparePreAuthenticationError() );
                }

                EncryptedTimeStamp timestamp = null;

                for ( int ii = 0; ii < preAuthData.length; ii++ )
                {
                    if ( preAuthData[ ii ].getDataType().equals(
                            PreAuthenticationDataType.PA_ENC_TIMESTAMP ) )
                    {
                        EncryptedData dataValue;

                        try
                        {
                            dataValue = EncryptedDataDecoder.decode( preAuthData[ ii ].getDataValue() );
                        }
                        catch ( IOException ioe )
                        {
                            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY );
                        }
                        catch ( ClassCastException cce )
                        {
                            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY );
                        }

                        timestamp = (EncryptedTimeStamp) lockBox.unseal( EncryptedTimeStamp.class, clientKey, dataValue );
                    }
                }

                if ( timestamp == null )
                {
                    throw new KerberosException( ErrorType.KDC_ERR_PREAUTH_REQUIRED,
                            preparePreAuthenticationError() );
                }

                if ( !timestamp.getTimeStamp().isInClockSkew( config.getClockSkew() ) )
                {
                    throw new KerberosException( ErrorType.KDC_ERR_PREAUTH_FAILED );
                }

                /*
                 if(decrypted_enc_timestamp and usec is replay)
                 error_out(KDC_ERR_PREAUTH_FAILED);
                 endif

                 add decrypted_enc_timestamp and usec to replay cache;
                 */
            }
        }

        authContext.setClientKey( clientKey );

        if ( log.isDebugEnabled() )
        {
            log.debug( "Pre-authentication by encrypted timestamp successful for " + clientName + "." );
        }

        return CONTINUE_CHAIN;
    }
}
