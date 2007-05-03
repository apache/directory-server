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
package org.apache.directory.server.kerberos.kdc.ticketgrant;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumHandler;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.Checksum;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class VerifyBodyChecksum implements IoHandlerCommand
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( VerifyBodyChecksum.class );

    private ChecksumHandler checksumHandler = new ChecksumHandler();
    private String contextKey = "context";

    /** a map of the default encryption types to the encryption engine class names */
    private static final Map<EncryptionType, ChecksumType> DEFAULT_CHECKSUMS;

    static
    {
        Map<EncryptionType, ChecksumType> map = new HashMap<EncryptionType, ChecksumType>();

        map.put( EncryptionType.DES_CBC_MD5, ChecksumType.RSA_MD5 );
        map.put( EncryptionType.DES3_CBC_SHA1_KD, ChecksumType.HMAC_SHA1_DES3_KD );
        map.put( EncryptionType.RC4_HMAC, ChecksumType.HMAC_MD5 );
        map.put( EncryptionType.AES128_CTS_HMAC_SHA1_96, ChecksumType.HMAC_SHA1_96_AES128 );
        map.put( EncryptionType.AES256_CTS_HMAC_SHA1_96, ChecksumType.HMAC_SHA1_96_AES256 );

        DEFAULT_CHECKSUMS = Collections.unmodifiableMap( map );
    }


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        TicketGrantingContext tgsContext = ( TicketGrantingContext ) session.getAttribute( getContextKey() );
        byte[] bodyBytes = tgsContext.getRequest().getBodyBytes();
        Checksum authenticatorChecksum = tgsContext.getAuthenticator().getChecksum();

        EncryptionType encryptionType = tgsContext.getEncryptionType();
        ChecksumType allowedChecksumType = DEFAULT_CHECKSUMS.get( encryptionType );

        if ( !allowedChecksumType.equals( authenticatorChecksum.getChecksumType() ) )
        {
            log.warn( "Allowed checksum type '" + allowedChecksumType + "' did not match authenticator checksum type '"
                + authenticatorChecksum.getChecksumType() + "'." );
        }

        checksumHandler.verifyChecksum( authenticatorChecksum, bodyBytes, null );

        next.execute( session, message );
    }


    private String getContextKey()
    {
        return ( this.contextKey );
    }
}
