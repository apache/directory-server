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
package org.apache.directory.server.kerberos.kdc;


import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MonitorRequest extends CommandBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( MonitorRequest.class );


    public boolean execute( Context context ) throws Exception
    {
        KdcContext kdcContext = ( KdcContext ) context;
        KdcRequest request = kdcContext.getRequest();
        String clientAddress = kdcContext.getClientAddress().getHostAddress();

        if ( log.isDebugEnabled() )
        {
            log.debug( "Responding to authentication request:" + "\n\trealm:                 " + request.getRealm()
                + "\n\tserverPrincipal:       " + request.getServerPrincipal() + "\n\tclientPrincipal:       "
                + request.getClientPrincipal() + "\n\tclientAddress:         " + clientAddress
                + "\n\thostAddresses:         " + request.getAddresses() + "\n\tencryptionType:        "
                + getEncryptionTypes( request ) + "\n\tfrom krb time:         " + request.getFrom()
                + "\n\trealm krb time:        " + request.getRtime() + "\n\tkdcOptions:            "
                + request.getKdcOptions() + "\n\tmessageType:           " + request.getMessageType()
                + "\n\tnonce:                 " + request.getNonce() + "\n\tprotocolVersionNumber: "
                + request.getProtocolVersionNumber() + "\n\ttill:                  " + request.getTill() );
        }

        return CONTINUE_CHAIN;
    }


    public String getEncryptionTypes( KdcRequest request )
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
}
