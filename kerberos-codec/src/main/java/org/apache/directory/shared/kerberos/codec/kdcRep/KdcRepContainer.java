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
package org.apache.directory.shared.kerberos.codec.kdcRep;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.KdcRep;


/**
 * The KdcReq container stores the KDC-REP decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcRepContainer extends AbstractContainer
{
    /** An KDC-REP container */
    private KdcRep kdcRep;


    /**
     * Creates a new KdcReqContainer object.
     * @param stream The stream containing the data to decode
     */
    public KdcRepContainer( ByteBuffer stream )
    {
        super( stream );
        this.stateStack = new int[1];
        this.grammar = KdcRepGrammar.getInstance();
        setTransition( KdcRepStatesEnum.START_STATE );
    }


    /**
     * @return Returns the KdcRep.
     */
    public KdcRep getKdcRep()
    {
        return kdcRep;
    }


    /**
     * Set a KdcRep Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param kdcRep The KdcRep to set.
     */
    public void setKdcRep( KdcRep kdcRep )
    {
        this.kdcRep = kdcRep;
    }
}
