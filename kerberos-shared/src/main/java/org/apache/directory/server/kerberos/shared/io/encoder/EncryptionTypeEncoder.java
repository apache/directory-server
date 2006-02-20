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


import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;


public class EncryptionTypeEncoder
{
    /**
     * etype[8]             SEQUENCE OF INTEGER, -- EncryptionEngine,
     *             -- in preference order
     */
    protected static DERSequence encode( EncryptionType[] eType )
    {
        DERSequence sequence = new DERSequence();

        for ( int ii = 0; ii < eType.length; ii++ )
        {
            sequence.add( DERInteger.valueOf( eType[ii].getOrdinal() ) );
        }

        return sequence;
    }
}
