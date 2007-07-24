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
package org.apache.directory.server.kerberos.kdc.authentication;


import java.net.InetAddress;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MonitorContext implements IoHandlerCommand
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( MonitorContext.class );

    private String serviceName;

    private String contextKey = "context";


    /**
     * Creates a new instance of MonitorContext.
     *
     * @param serviceName
     */
    public MonitorContext( String serviceName )
    {
        this.serviceName = serviceName;
    }


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        if ( log.isDebugEnabled() )
        {
            try
            {
                AuthenticationContext authContext = ( AuthenticationContext ) session.getAttribute( getContextKey() );

                long clockSkew = authContext.getConfig().getAllowableClockSkew();
                InetAddress clientAddress = authContext.getClientAddress();

                StringBuffer sb = new StringBuffer();

                sb.append( "Monitoring " + serviceName + " context:" );

                sb.append( "\n\t" + "clockSkew              " + clockSkew );
                sb.append( "\n\t" + "clientAddress          " + clientAddress );

                KerberosPrincipal clientPrincipal = authContext.getClientEntry().getPrincipal();
                PrincipalStoreEntry clientEntry = authContext.getClientEntry();

                sb.append( "\n\t" + "principal              " + clientPrincipal );
                sb.append( "\n\t" + "cn                     " + clientEntry.getCommonName() );
                sb.append( "\n\t" + "realm                  " + clientEntry.getRealmName() );
                sb.append( "\n\t" + "principal              " + clientEntry.getPrincipal() );
                sb.append( "\n\t" + "SAM type               " + clientEntry.getSamType() );

                KerberosPrincipal serverPrincipal = authContext.getRequest().getServerPrincipal();
                PrincipalStoreEntry serverEntry = authContext.getServerEntry();

                sb.append( "\n\t" + "principal              " + serverPrincipal );
                sb.append( "\n\t" + "cn                     " + serverEntry.getCommonName() );
                sb.append( "\n\t" + "realm                  " + serverEntry.getRealmName() );
                sb.append( "\n\t" + "principal              " + serverEntry.getPrincipal() );
                sb.append( "\n\t" + "SAM type               " + serverEntry.getSamType() );

                EncryptionType encryptionType = authContext.getEncryptionType();
                int clientKeyVersion = clientEntry.getKeyMap().get( encryptionType ).getKeyVersion();
                int serverKeyVersion = serverEntry.getKeyMap().get( encryptionType ).getKeyVersion();
                sb.append( "\n\t" + "Request key type        " + encryptionType );
                sb.append( "\n\t" + "Client key version    " + clientKeyVersion );
                sb.append( "\n\t" + "Server key version    " + serverKeyVersion );

                log.debug( sb.toString() );
            }
            catch ( Exception e )
            {
                // This is a monitor.  No exceptions should bubble up.
                log.error( "Error in context monitor", e );
            }
        }

        next.execute( session, message );
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
