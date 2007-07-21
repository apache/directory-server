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


import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
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


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        TicketGrantingContext tgsContext = ( TicketGrantingContext ) session.getAttribute( getContextKey() );
        KdcConfiguration config = tgsContext.getConfig();

        if ( config.isBodyChecksumVerified() )
        {
            byte[] bodyBytes = tgsContext.getRequest().getBodyBytes();
            Checksum authenticatorChecksum = tgsContext.getAuthenticator().getChecksum();

            if ( authenticatorChecksum == null || authenticatorChecksum.getChecksumType() == null
                || authenticatorChecksum.getChecksumValue() == null || bodyBytes == null )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_INAPP_CKSUM );
            }

            log.debug( "Verifying body checksum type '{}'.", authenticatorChecksum.getChecksumType() );

            checksumHandler.verifyChecksum( authenticatorChecksum, bodyBytes, null, KeyUsage.NUMBER8 );
        }

        next.execute( session, message );
    }


    private String getContextKey()
    {
        return ( this.contextKey );
    }
}
