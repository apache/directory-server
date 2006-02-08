/*
 *   Copyright 2004 The Apache Software Foundation
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;
import org.apache.kerberos.messages.Encodable;
import org.apache.kerberos.messages.components.EncApRepPart;

public class EncApRepPartEncoder implements Encoder, EncoderFactory
{
	public static final int APPLICATION_CODE = 27;

    public Encoder getEncoder()
    {
        return new EncApRepPartEncoder();
    }

	public byte[] encode( Encodable apRepPart ) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ASN1OutputStream aos = new ASN1OutputStream( baos );
		
		DERSequence privPartSequence = encodeApRepPartSequence( (EncApRepPart) apRepPart );
		aos.writeObject( DERApplicationSpecific.valueOf( APPLICATION_CODE, privPartSequence ) );
		aos.close();
		
		return baos.toByteArray();
	}

	private DERSequence encodeApRepPartSequence( EncApRepPart message )
	{
		DERSequence sequence = new DERSequence();
		
		sequence.add( new DERTaggedObject(0, KerberosTimeEncoder.encode( message.getClientTime() ) ) );
		sequence.add( new DERTaggedObject(1, DERInteger.valueOf( message.getClientMicroSecond() ) ) );
		
		if ( message.getSubSessionKey() != null)
		{
			sequence.add( new DERTaggedObject( 2, EncryptionKeyEncoder.encode( message.getSubSessionKey() ) ) );
		}
		
		if ( message.getSequenceNumber() != null )
		{
			sequence.add( new DERTaggedObject( 3, DERInteger.valueOf( message.getSequenceNumber().intValue() ) ) );
		}
		
		return sequence;
	}
}
