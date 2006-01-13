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

import org.apache.kerberos.crypto.checksum.ChecksumEngine;
import org.apache.kerberos.crypto.checksum.ChecksumType;
import org.apache.kerberos.crypto.checksum.RsaMd5Checksum;
import org.apache.kerberos.exceptions.ErrorType;
import org.apache.kerberos.exceptions.KerberosException;
import org.apache.kerberos.messages.value.Checksum;
import org.apache.protocol.common.chain.Context;
import org.apache.protocol.common.chain.impl.CommandBase;

public class VerifyBodyChecksum extends CommandBase
{
    public boolean execute( Context context ) throws Exception
    {
        TicketGrantingContext tgsContext = (TicketGrantingContext) context;
        byte[] bodyBytes = tgsContext.getRequest().getBodyBytes();
        Checksum checksum = tgsContext.getAuthenticator().getChecksum();

        verifyChecksum( checksum, bodyBytes );

        return CONTINUE_CHAIN;
    }

    private void verifyChecksum( Checksum checksum, byte[] bytes ) throws KerberosException
    {
        if ( checksum == null )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_INAPP_CKSUM );
        }

        if ( !checksum.getChecksumType().equals( ChecksumType.RSA_MD5 ) )
        {
            throw new KerberosException( ErrorType.KDC_ERR_SUMTYPE_NOSUPP );
        }

        ChecksumEngine digester = new RsaMd5Checksum();
        Checksum newChecksum = new Checksum( digester.checksumType(), digester.calculateChecksum( bytes ) );

        if ( !newChecksum.equals( checksum ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_MODIFIED );
        }
    }
}
