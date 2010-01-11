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


import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-21 17:00:43 -0700 (Mon, 21 May 2007) $
 */
public class EncryptionTypeInfo2Entry
{
    private EncryptionType encryptionType;
    private String salt;
    private byte[] s2kparams;


    /**
     * Creates a new instance of {@link EncryptionTypeInfo2Entry}.
     *
     * @param encryptionType
     * @param salt
     * @param s2kparams
     */
    public EncryptionTypeInfo2Entry( EncryptionType encryptionType, String salt, byte[] s2kparams )
    {
        this.encryptionType = encryptionType;
        this.salt = salt;
        this.s2kparams = s2kparams;
    }


    /**
     * Returns the {@link EncryptionType}.
     *
     * @return The {@link EncryptionType}.
     */
    public EncryptionType getEncryptionType()
    {
        return encryptionType;
    }


    /**
     * Returns the salt.
     *
     * @return The salt.
     */
    public String getSalt()
    {
        return salt;
    }


    /**
     * Returns the s2kparams.
     * 
     * @return The s2kparams.
     */
    public byte[] getS2kParams()
    {
        return s2kparams;
    }
}
