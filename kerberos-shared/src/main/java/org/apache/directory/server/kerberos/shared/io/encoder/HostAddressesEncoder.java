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
package org.apache.directory.server.kerberos.shared.io.encoder;


import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HostAddressesEncoder
{
    /**
     * HostAddresses ::=   SEQUENCE OF SEQUENCE {
     *                     addr-type[0]             INTEGER,
     *                     address[1]               OCTET STRING
     * }
     * 
     * 0x30 L1 hostAddresses sequence of HostAddresses
     *  |
     *  +--> 0x30 L2[1] Hostaddress[1]
     *  |
     *  +--> 0x30 L2[2] Hostaddress[2]
     *  |
     *  ...
     *  |
     *  +--> 0x30 L2[n] Hostaddress[n]
     */
    protected static DERSequence encodeSequence( HostAddresses hosts )
    {
        HostAddress[] addresses = hosts.getAddresses();
        DERSequence sequence = new DERSequence();

        for ( int ii = 0; ii < addresses.length; ii++ )
        {
            sequence.add( encode( addresses[ii] ) );
        }

        return sequence;
    }


    /**
     *  HostAddress ::=     SEQUENCE  {
     *                     addr-type[0]             INTEGER,
     *                     address[1]               OCTET STRING
     * }
     * 
     * 0x30 L1 hostAddress sequence
     *  |
     *  +--> 0xA0 L2 addressType tag
     *  |     |
     *  |     +--> 0x02 L2-1 addressType (int)
     *  |
     *  +--> 0xA1 L3 address tag
     *        |
     *        +--> 0x04 L3-1 address (OCTET STRING)
     * 
     */
    protected static DERSequence encode( HostAddress host )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( host.getAddrType().getOrdinal() ) ) );
        sequence.add( new DERTaggedObject( 1, new DEROctetString( host.getAddress() ) ) );

        return sequence;
    }
}
