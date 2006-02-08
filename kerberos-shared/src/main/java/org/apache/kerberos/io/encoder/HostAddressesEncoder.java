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
package org.apache.kerberos.io.encoder;

import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;
import org.apache.kerberos.messages.value.HostAddress;
import org.apache.kerberos.messages.value.HostAddresses;

public class HostAddressesEncoder
{
    /**
     * HostAddresses ::=   SEQUENCE OF SEQUENCE {
     *                     addr-type[0]             INTEGER,
     *                     address[1]               OCTET STRING
     * }
     */
    protected static DERSequence encodeSequence( HostAddresses hosts )
    {
        HostAddress[] addresses = hosts.getAddresses();
        DERSequence sequence = new DERSequence();

        for ( int ii = 0; ii < addresses.length; ii++ )
        {
            sequence.add( encode( addresses[ ii ] ) );
        }

        return sequence;
    }

    /**
     *  HostAddress ::=     SEQUENCE  {
     *                     addr-type[0]             INTEGER,
     *                     address[1]               OCTET STRING
     * }
     */
    protected static DERSequence encode( HostAddress host )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( host.getAddressType().getOrdinal() ) ) );
        sequence.add( new DERTaggedObject( 1, new DEROctetString( host.getAddress() ) ) );

        return sequence;
    }
}
