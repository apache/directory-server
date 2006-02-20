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
package org.apache.directory.server.kerberos.shared.io.encoder;


import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class TicketEncoder
{
    /**
     * Ticket ::=                    [APPLICATION 1] SEQUENCE {
     *     tkt-vno[0]                   INTEGER,
     *     realm[1]                     Realm,
     *     sname[2]                     PrincipalName,
     *     enc-part[3]                  EncryptedData
     * }
     */
    protected static DERApplicationSpecific encode( Ticket ticket )
    {
        DERSequence vector = new DERSequence();

        vector.add( new DERTaggedObject( 0, DERInteger.valueOf( ticket.getVersionNumber() ) ) );
        vector.add( new DERTaggedObject( 1, DERGeneralString.valueOf( ticket.getRealm() ) ) );
        vector.add( new DERTaggedObject( 2, PrincipalNameEncoder.encode( ticket.getServerPrincipal() ) ) );
        vector.add( new DERTaggedObject( 3, EncryptedDataEncoder.encodeSequence( ticket.getEncPart() ) ) );

        DERApplicationSpecific ticketSequence = null;

        try
        {
            ticketSequence = DERApplicationSpecific.valueOf( 1, vector );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return ticketSequence;
    }


    protected static DERSequence encodeSequence( Ticket[] tickets )
    {
        DERSequence outerVector = new DERSequence();

        for ( int ii = 0; ii < tickets.length; ii++ )
        {
            DERSequence vector = new DERSequence();
            vector.add( encode( tickets[ii] ) );
            outerVector.add( vector );
        }

        return outerVector;
    }
}
