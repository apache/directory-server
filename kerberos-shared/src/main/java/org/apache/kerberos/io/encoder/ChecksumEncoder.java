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
import org.apache.kerberos.messages.value.Checksum;

public class ChecksumEncoder
{
	/**
	 * Checksum ::=   SEQUENCE {
     *          cksumtype[0]   INTEGER,
     *          checksum[1]    OCTET STRING
     * }
	 */
	public static DERSequence encode( Checksum checksum )
	{
		DERSequence vector = new DERSequence();
		
		vector.add( new DERTaggedObject( 0, DERInteger.valueOf( checksum.getChecksumType().getOrdinal() ) ) );
		vector.add( new DERTaggedObject( 1, new DEROctetString( checksum.getChecksumValue() ) ) );
		
		return vector;
	}
}
