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
package org.apache.directory.shared.kerberos.codec.encAsRepPart;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.messages.EncAsRepPart;


/**
 * The EncAsRepPart container stores the EncAsRepPart decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncAsRepPartContainer extends AbstractContainer
{
    /** An EncAsRepPart container */
    private EncAsRepPart encAsRepPart = new EncAsRepPart();


    /**
     * Creates a new EncAsRepPartContainer object.
     * @param stream The stream containing the data to decode
     */
    public EncAsRepPartContainer( ByteBuffer stream )
    {
        super( stream );
        this.grammar = EncAsRepPartGrammar.getInstance();
        setTransition( EncAsRepPartStatesEnum.START_STATE );
    }


    /**
     * @return Returns the EncAsRepPart.
     */
    public EncAsRepPart getEncAsRepPart()
    {
        return encAsRepPart;
    }


    /**
     * Set an EncAsRepPart Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param encAsRepPart The EncAsRepPart to set.
     */
    public void setEncAsRepPart( EncAsRepPart encAsRepPart )
    {
        this.encAsRepPart = encAsRepPart;
    }
}
