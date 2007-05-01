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
package org.apache.directory.server.kerberos.shared.crypto.checksum;


import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherType;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class ChecksumEngine
{
    /**
     * Returns the checksum type of this checksum engine.
     *
     * @return The checksum type.
     */
    public abstract ChecksumType checksumType();


    /**
     * Returns the key type of this checksum engine.
     *
     * @return The key type.
     */
    public abstract CipherType keyType();


    /**
     * Calculate a checksum given raw bytes and an (optional) key.
     *
     * @param data
     * @param key
     * @return The checksum value.
     */
    public abstract byte[] calculateChecksum( byte[] data, byte[] key );
}
