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
package org.apache.directory.shared.kerberos.codec.encTgsRepPart;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.messages.EncTgsRepPart;


/**
 * The EncTgsRepPart container stores the EncTgsRepPart decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncTgsRepPartContainer extends AbstractContainer
{
    /** An EncTgsRepPart container */
    private EncTgsRepPart encTgsRepPart = new EncTgsRepPart();


    /**
     * Creates a new EncTgsRepPartContainer object.
     * @param stream The stream containing the data to decode
     */
    public EncTgsRepPartContainer( ByteBuffer stream )
    {
        super( stream );
        this.stateStack = new int[1];
        this.grammar = EncTgsRepPartGrammar.getInstance();
        setTransition( EncTgsRepPartStatesEnum.START_STATE );
    }


    /**
     * @return Returns the EncTgsRepPart.
     */
    public EncTgsRepPart getEncTgsRepPart()
    {
        return encTgsRepPart;
    }


    /**
     * Set an EncTgsRepPart Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param encTgsRepPart The EncTgsRepPart to set.
     */
    public void setEncTgsRepPart( EncTgsRepPart encTgsRepPart )
    {
        this.encTgsRepPart = encTgsRepPart;
    }
}
