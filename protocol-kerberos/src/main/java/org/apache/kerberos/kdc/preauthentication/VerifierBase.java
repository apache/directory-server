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
package org.apache.kerberos.kdc.preauthentication;

import java.io.IOException;

import org.apache.kerberos.crypto.encryption.EncryptionType;
import org.apache.kerberos.io.encoder.EncryptionTypeInfoEncoder;
import org.apache.kerberos.io.encoder.PreAuthenticationDataEncoder;
import org.apache.kerberos.messages.value.EncryptionTypeInfoEntry;
import org.apache.kerberos.messages.value.PreAuthenticationData;
import org.apache.kerberos.messages.value.PreAuthenticationDataModifier;
import org.apache.kerberos.messages.value.PreAuthenticationDataType;
import org.apache.protocol.common.chain.impl.CommandBase;

public abstract class VerifierBase extends CommandBase
{
    public byte[] preparePreAuthenticationError()
    {
        PreAuthenticationData[] paDataSequence = new PreAuthenticationData[ 2 ];

        PreAuthenticationDataModifier modifier = new PreAuthenticationDataModifier();
        modifier.setDataType( PreAuthenticationDataType.PA_ENC_TIMESTAMP );
        modifier.setDataValue( new byte[ 0 ] );

        paDataSequence[ 0 ] = modifier.getPreAuthenticationData();

        EncryptionTypeInfoEntry[] entries = new EncryptionTypeInfoEntry[ 1 ];
        entries[ 0 ] = new EncryptionTypeInfoEntry( EncryptionType.DES_CBC_MD5, null );

        byte[] encTypeInfo = null;

        try
        {
            encTypeInfo = EncryptionTypeInfoEncoder.encode( entries );
        }
        catch ( IOException ioe )
        {
            return null;
        }

        PreAuthenticationDataModifier encTypeModifier = new PreAuthenticationDataModifier();
        encTypeModifier.setDataType( PreAuthenticationDataType.PA_ENCTYPE_INFO );
        encTypeModifier.setDataValue( encTypeInfo );

        paDataSequence[ 1 ] = encTypeModifier.getPreAuthenticationData();

        try
        {
            return PreAuthenticationDataEncoder.encode( paDataSequence );
        }
        catch ( IOException ioe )
        {
            return null;
        }
    }
}
