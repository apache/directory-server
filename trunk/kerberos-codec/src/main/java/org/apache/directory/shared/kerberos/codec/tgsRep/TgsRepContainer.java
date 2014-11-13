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
package org.apache.directory.shared.kerberos.codec.tgsRep;


import java.nio.ByteBuffer;

import org.apache.directory.shared.kerberos.codec.kdcRep.KdcRepContainer;
import org.apache.directory.shared.kerberos.messages.TgsRep;


/**
 * The TGS-REP container stores the TgsRep decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TgsRepContainer extends KdcRepContainer
{
    /** An TGS-REP container */
    private TgsRep tgsRep;


    /**
     * Creates a new TgsRepContainer object.
     * @param stream The stream containing the data to decode
     */
    public TgsRepContainer( ByteBuffer stream )
    {
        super( stream );
        this.grammar = TgsRepGrammar.getInstance();
        setTransition( TgsRepStatesEnum.START_STATE );
    }


    /**
     * @return Returns the TgsRep.
     */
    public TgsRep getTgsRep()
    {
        return tgsRep;
    }


    /**
     * Set an TgsRep Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param tgsRep The TgsRep to set.
     */
    public void setTgsRep( TgsRep tgsRep )
    {
        this.tgsRep = tgsRep;
    }
}
