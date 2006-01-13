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
package org.apache.kerberos.kdc.ticketgrant;

import java.io.IOException;

import org.apache.kerberos.exceptions.ErrorType;
import org.apache.kerberos.exceptions.KerberosException;
import org.apache.kerberos.io.decoder.ApplicationRequestDecoder;
import org.apache.kerberos.messages.ApplicationRequest;
import org.apache.kerberos.messages.KdcRequest;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.messages.value.PreAuthenticationData;
import org.apache.kerberos.messages.value.PreAuthenticationDataType;
import org.apache.protocol.common.chain.Context;
import org.apache.protocol.common.chain.impl.CommandBase;

/*
 * differs from the changepw getAuthHeader by verifying the presence of TGS_REQ
 */
public class GetAuthHeader extends CommandBase
{
    public boolean execute( Context context ) throws Exception
    {
        TicketGrantingContext tgsContext = (TicketGrantingContext) context;
        KdcRequest request = tgsContext.getRequest();

        ApplicationRequest authHeader = getAuthHeader( request );
        Ticket tgt = authHeader.getTicket();

        tgsContext.setAuthHeader( authHeader );
        tgsContext.setTgt( tgt );

        return CONTINUE_CHAIN;
    }

    protected ApplicationRequest getAuthHeader( KdcRequest request ) throws KerberosException, IOException
    {
        byte[] undecodedAuthHeader = null;
        PreAuthenticationData[] preAuthData = request.getPreAuthData();

        for ( int ii = 0; ii < preAuthData.length; ii++ )
        {
            if ( preAuthData[ ii ].getDataType() == PreAuthenticationDataType.PA_TGS_REQ )
            {
                undecodedAuthHeader = preAuthData[ ii ].getDataValue();
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
