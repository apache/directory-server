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
package org.apache.directory.server.kerberos.shared.io.decoder;


import java.io.IOException;
import java.util.Enumeration;

import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.components.TicketModifier;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class TicketDecoder
{
    public static Ticket[] decodeSequence( DERSequence sequence ) throws IOException
    {
        Ticket[] tickets = new Ticket[sequence.size()];

        int ii = 0;
        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERApplicationSpecific object = ( DERApplicationSpecific ) e.nextElement();
            tickets[ii] = decode( object );
        }

        return tickets;
    }


    /**
     * Ticket ::=                    [APPLICATION 1] SEQUENCE {
     *     tkt-vno[0]                   INTEGER,
     *     realm[1]                     Realm,
     *     sname[2]                     PrincipalName,
     *     enc-part[3]                  EncryptedData
     * }
     */
    protected static Ticket decode( DERApplicationSpecific app ) throws IOException
    {
        DERSequence sequence = ( DERSequence ) app.getObject();

        TicketModifier modifier = new TicketModifier();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERInteger tag0 = ( DERInteger ) derObject;
                    modifier.setTicketVersionNumber( tag0.intValue() );
                    break;
                case 1:
                    DERGeneralString tag1 = ( DERGeneralString ) derObject;
                    modifier.setServerRealm( tag1.getString() );
                    break;
                case 2:
                    DERSequence tag2 = ( DERSequence ) derObject;
                    modifier.setServerName( PrincipalNameDecoder.decode( tag2 ) );
                    break;
                case 3:
                    DERSequence tag3 = ( DERSequence ) derObject;
                    modifier.setEncPart( EncryptedDataDecoder.decode( tag3 ) );
                    break;
            }
        }

        return modifier.getTicket();
    }
}
