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

import java.util.Iterator;

import org.apache.asn1.der.DERInteger;
import org.apache.asn1.der.DEROctetString;
import org.apache.asn1.der.DERSequence;
import org.apache.asn1.der.DERTaggedObject;
import org.apache.kerberos.messages.value.AuthorizationData;
import org.apache.kerberos.messages.value.AuthorizationDataEntry;

public class AuthorizationDataEncoder
{
    /**
     * AuthorizationData ::=   SEQUENCE OF SEQUENCE {
     *     ad-type[0]               INTEGER,
     *     ad-data[1]               OCTET STRING
     * }
     */
    protected static DERSequence encode( AuthorizationData data )
    {
        Iterator it = data.iterator();
        DERSequence outerSequence = new DERSequence();

        while ( it.hasNext() )
        {
            AuthorizationDataEntry entry = (AuthorizationDataEntry) it.next();
            DERSequence sequence = new DERSequence();
            sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( entry.getAuthorizationDataType().getOrdinal() ) ) );
            sequence.add( new DERTaggedObject( 1, new DEROctetString( entry.getAuthorizationData() ) ) );
            outerSequence.add( sequence );
        }

        return outerSequence;
    }
}
