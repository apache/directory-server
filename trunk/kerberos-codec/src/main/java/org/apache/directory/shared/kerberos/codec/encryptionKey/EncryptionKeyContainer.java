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
package org.apache.directory.shared.kerberos.codec.encryptionKey;


import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.EncryptionKey;


/**
 * The EncryptionKey container stores the EncryptionKey decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncryptionKeyContainer extends AbstractContainer
{
    /** holds EncryptionKey */
    private EncryptionKey encryptionKey;


    /**
     * Creates a new EncryptionKeyContainer object.
     */
    public EncryptionKeyContainer()
    {
        super();
        this.stateStack = new int[1];
        this.grammar = EncryptionKeyGrammar.getInstance();
        setTransition( EncryptionKeyStatesEnum.START_STATE );
    }


    /**
     * @return Returns the EncryptionKey.
     */
    public EncryptionKey getEncryptionKey()
    {
        return encryptionKey;
    }


    /**
     * Set a EncryptionKey Object into the container
     * 
     * @param encryptionKey The EncryptionKey to set.
     */
    public void setEncryptionKey( EncryptionKey encryptionKey )
    {
        this.encryptionKey = encryptionKey;
    }
}
