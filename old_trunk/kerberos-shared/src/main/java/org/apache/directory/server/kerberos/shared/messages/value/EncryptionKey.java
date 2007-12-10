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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.util.Arrays;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;


/**
 * A Kerberos symmetric encryption key, which includes metadata support for
 * the associated key type and key version number.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncryptionKey
{
    private EncryptionType keyType;
    private byte[] keyValue;
    private int keyVersion;


    /**
     * Creates a new instance of EncryptionKey.
     *
     * @param keyType
     * @param keyValue
     */
    public EncryptionKey( EncryptionType keyType, byte[] keyValue )
    {
        this.keyType = keyType;
        this.keyValue = keyValue;
    }


    /**
     * Creates a new instance of EncryptionKey.  This constructor supports 'keyVersion',
     * which is sent over the wire as part of EncryptedData but makes more sense
     * in the domain model to have here as part of the key itself.  Therefore, the
     * keyVersion should only be constructor-injected when EncryptionKey's are
     * retrieved from persisted storage.
     *
     * @param keyType
     * @param keyValue
     * @param keyVersion
     */
    public EncryptionKey( EncryptionType keyType, byte[] keyValue, int keyVersion )
    {
        this.keyType = keyType;
        this.keyValue = keyValue;
        this.keyVersion = keyVersion;
    }


    /**
     * Destroys this key by overwriting the symmetric key material with zeros.
     */
    public synchronized void destroy()
    {
        if ( keyValue != null )
        {
            for ( int ii = 0; ii < keyValue.length; ii++ )
            {
                keyValue[ii] = 0;
            }
        }
    }


    /**
     * Returns the key type.
     *
     * @return The key type.
     */
    public EncryptionType getKeyType()
    {
        return keyType;
    }


    /**
     * Returns the key value.
     *
     * @return The key value.
     */
    public byte[] getKeyValue()
    {
        return keyValue;
    }


    /**
     * Returns the key version.
     *
     * @return The key version.
     */
    public int getKeyVersion()
    {
        return keyVersion;
    }


    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( !( o instanceof EncryptionKey ) )
        {
            return false;
        }

        EncryptionKey that = ( EncryptionKey ) o;
        return ( this.keyType == that.keyType ) && ( Arrays.equals( this.keyValue, that.keyValue ) );
    }


    public String toString()
    {
        return keyType.toString() + " (" + keyType.getOrdinal() + ")";
    }
}
