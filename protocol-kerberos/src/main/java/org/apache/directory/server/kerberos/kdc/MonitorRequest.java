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
package org.apache.directory.server.kerberos.kdc;


import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MonitorRequest implements IoHandlerCommand
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( MonitorRequest.class );

    private String contextKey = "context";


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        KdcContext kdcContext = ( KdcContext ) session.getAttribute( getContextKey() );
        KdcRequest request = kdcContext.getRequest();
        String clientAddress = kdcContext.getClientAddress().getHostAddress();

        if ( log.isDebugEnabled() )
        {
            StringBuffer sb = new StringBuffer();

            sb.append( "Responding to authentication request:" );
            sb.append( "\n\t" + "realm:                 " + request.getRealm() );
            sb.append( "\n\t" + "serverPrincipal:       " + request.getServerPrincipal() );
            sb.append( "\n\t" + "clientPrincipal:       " + request.getClientPrincipal() );
            sb.append( "\n\t" + "clientAddress:         " + clientAddress );
            sb.append( "\n\t" + "hostAddresses:         " + request.getAddresses() );
            sb.append( "\n\t" + "encryptionType:        " + getEncryptionTypes( request ) );
            sb.append( "\n\t" + "from krb time:         " + request.getFrom() );
            sb.append( "\n\t" + "realm krb time:        " + request.getRtime() );
            sb.append( "\n\t" + "kdcOptions:            " + request.getKdcOptions() );
            sb.append( "\n\t" + "messageType:           " + request.getMessageType() );
            sb.append( "\n\t" + "nonce:                 " + request.getNonce() );
            sb.append( "\n\t" + "protocolVersionNumber: " + request.getProtocolVersionNumber() );
            sb.append( "\n\t" + "till:                  " + request.getTill() );

            log.debug( sb.toString() );
        }

        next.execute( session, message );
    }


    protected String getEncryptionTypes( KdcRequest request )
    {
        EncryptionType[] etypes = request.getEType();

        StringBuffer sb = new StringBuffer();

        for ( int ii = 0; ii < etypes.length; ii++ )
        {
            sb.append( etypes[ii].toString() );

            if ( ii < etypes.length - 1 )
            {
                sb.append( ", " );
            }
        }

        return sb.toString();
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
