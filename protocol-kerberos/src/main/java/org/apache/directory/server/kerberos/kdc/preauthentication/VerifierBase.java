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
package org.apache.directory.server.kerberos.kdc.preauthentication;


import java.io.IOException;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.io.encoder.EncryptionTypeInfoEncoder;
import org.apache.directory.server.kerberos.shared.io.encoder.PreAuthenticationDataEncoder;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionTypeInfoEntry;
import org.apache.directory.server.kerberos.shared.messages.value.PaData;
import org.apache.directory.server.kerberos.shared.messages.value.types.PaDataType;
import org.apache.mina.handler.chain.IoHandlerCommand;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class VerifierBase implements IoHandlerCommand
{
    private String contextKey = "context";


    /**
     * Prepares a pre-authentication error message containing required
     * encryption types.
     *
     * @param encryptionTypes
     * @return The error message as bytes.
     */
    public byte[] preparePreAuthenticationError( EncryptionType[] encryptionTypes )
    {
        PaData[] paDataSequence = new PaData[2];

        PaData paData = new PaData();
        paData.setPaDataType( PaDataType.PA_ENC_TIMESTAMP );
        paData.setPaDataValue( new byte[0] );

        paDataSequence[0] = paData;

        EncryptionTypeInfoEntry[] entries = new EncryptionTypeInfoEntry[encryptionTypes.length];
        for ( int ii = 0; ii < encryptionTypes.length; ii++ )
        {
            entries[ii] = new EncryptionTypeInfoEntry( encryptionTypes[ii], null );
        }

        byte[] encTypeInfo = null;

        try
        {
            encTypeInfo = EncryptionTypeInfoEncoder.encode( entries );
        }
        catch ( IOException ioe )
        {
            return null;
        }

        PaData encType = new PaData();
        encType.setPaDataType( PaDataType.PA_ENCTYPE_INFO );
        encType.setPaDataValue( encTypeInfo );

        paDataSequence[1] = encType;

        try
        {
            return PreAuthenticationDataEncoder.encode( paDataSequence );
        }
        catch ( IOException ioe )
        {
            return null;
        }
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
