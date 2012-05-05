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
package org.apache.directory.shared.kerberos.codec.krbError;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.messages.KrbError;


/**
 * The KrbError container stores the KRB-ERROR decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbErrorContainer extends AbstractContainer
{
    /** An KDC-ERROR container */
    private KrbError krbError;


    /**
     * Creates a new KrbErrorContainer object.
     * @param stream The stream containing the data to decode
     */
    public KrbErrorContainer( ByteBuffer stream )
    {
        super( stream );
        this.stateStack = new int[1];
        this.grammar = KrbErrorGrammar.getInstance();
        setTransition( KrbErrorStatesEnum.START_STATE );
    }


    /**
     * @return Returns the KrbError.
     */
    public KrbError getKrbError()
    {
        return krbError;
    }


    /**
     * Set a KrbError Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param krbError The KrbError to set.
     */
    public void setKrbError( KrbError krbError )
    {
        this.krbError = krbError;
    }
}
