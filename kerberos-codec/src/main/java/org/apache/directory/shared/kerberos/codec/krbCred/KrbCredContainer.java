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
package org.apache.directory.shared.kerberos.codec.krbCred;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.messages.KrbCred;


/**
 * The KrbCred container stores the KrbCred decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbCredContainer extends AbstractContainer
{
    /** An KrbCred container */
    private KrbCred krbCred;


    /**
     * Creates a new KrbErrorContainer object.
     * @param stream The stream containing the data to decode
     */
    public KrbCredContainer( ByteBuffer stream )
    {
        super( stream );
        this.stateStack = new int[1];
        this.grammar = KrbCredGrammar.getInstance();
        setTransition( KrbCredStatesEnum.START_STATE );
    }


    /**
     * @return Returns the KrbCred.
     */
    public KrbCred getKrbCred()
    {
        return krbCred;
    }


    /**
     * Set a KrbCred Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param krbCred The KrbCred to set.
     */
    public void setKrbCred( KrbCred krbCred )
    {
        this.krbCred = krbCred;
    }
}
