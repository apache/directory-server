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
package org.apache.directory.server.kerberos.shared.keytab;


import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * An entry within a keytab file.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KeytabEntry
{
    private String principalName;

    private long principalType;

    private KerberosTime timeStamp;

    private byte keyVersion;

    private EncryptionKey key;


    /**
     * Creates a new instance of Entry.
     *
     * @param principalName
     * @param principalType
     * @param timeStamp
     * @param keyVersion
     * @param key
     */
    public KeytabEntry( String principalName, long principalType, KerberosTime timeStamp, byte keyVersion,
        EncryptionKey key )
    {
        this.principalName = principalName;
        this.principalType = principalType;
        this.timeStamp = timeStamp;
        this.keyVersion = keyVersion;
        this.key = key;
    }


    /**
     * @return The key.
     */
    public EncryptionKey getKey()
    {
        return key;
    }


    /**
     * @return The keyVersion.
     */
    public byte getKeyVersion()
    {
        return keyVersion;
    }


    /**
     * @return The principalName.
     */
    public String getPrincipalName()
    {
        return principalName;
    }


    /**
     * @return The principalType.
     */
    public long getPrincipalType()
    {
        return principalType;
    }


    /**
     * @return The timeStamp.
     */
    public KerberosTime getTimeStamp()
    {
        return timeStamp;
    }
}
