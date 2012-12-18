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
package org.apache.directory.shared.kerberos.codec.EncKdcRepPart;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.EncKdcRepPart;


/**
 * The EncKdcRepPart container stores the EncKdcRepPart decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncKdcRepPartContainer extends AbstractContainer
{
    /** An EncKdcRepPart container */
    private EncKdcRepPart encKdcRepPart;


    /**
     * Creates a new EncKdcRepPartContainer object.
     * @param stream The stream containing the data to decode
     */
    public EncKdcRepPartContainer( ByteBuffer stream )
    {
        super( stream );
        this.stateStack = new int[1];
        this.grammar = EncKdcRepPartGrammar.getInstance();
        setTransition( EncKdcRepPartStatesEnum.START_STATE );
    }


    /**
     * @return Returns the EncKdcRepPart.
     */
    public EncKdcRepPart getEncKdcRepPart()
    {
        return encKdcRepPart;
    }


    /**
     * Set a EncKdcRepPart Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param encKdcRepPart The EncKdcRepPart to set.
     */
    public void setEncKdcRepPart( EncKdcRepPart encKdcRepPart )
    {
        this.encKdcRepPart = encKdcRepPart;
    }
}
