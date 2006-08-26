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


import java.io.IOException;

import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.io.decoder.ApplicationRequestDecoder;
import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationDataType;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;


/*
 * differs from the changepw getAuthHeader by verifying the presence of TGS_REQ
 */
public class GetAuthHeader implements IoHandlerCommand
{
    private String contextKey = "context";

    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        TicketGrantingContext tgsContext = ( TicketGrantingContext ) session.getAttribute( getContextKey() );
        KdcRequest request = tgsContext.getRequest();

        ApplicationRequest authHeader = getAuthHeader( request );
        Ticket tgt = authHeader.getTicket();

        tgsContext.setAuthHeader( authHeader );
        tgsContext.setTgt( tgt );

        next.execute( session, message );
    }


    public String getContextKey()
    {
        return ( this.contextKey );
    }


    protected ApplicationRequest getAuthHeader( KdcRequest request ) throws KerberosException, IOException
    {
        byte[] undecodedAuthHeader = null;
        PreAuthenticationData[] preAuthData = request.getPreAuthData();

        for ( int ii = 0; ii < preAuthData.length; ii++ )
        {
            if ( preAuthData[ii].getDataType() == PreAuthenticationDataType.PA_TGS_REQ )
            {
                undecodedAuthHeader = preAuthData[ii].getDataValue();
            }
        }

        if ( undecodedAuthHeader == null )
        {
            throw new KerberosException( ErrorType.KDC_ERR_PADATA_TYPE_NOSUPP );
        }

        ApplicationRequestDecoder decoder = new ApplicationRequestDecoder();
        ApplicationRequest authHeader = decoder.decode( undecodedAuthHeader );

        return authHeader;
    }
}
