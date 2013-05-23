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
package org.apache.directory.shared.kerberos.codec.kdcReq;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.KdcReq;


/**
 * The KdcReq container stores the KDC-REQ decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcReqContainer extends AbstractContainer
{
    /** An KDC-REQ container */
    private KdcReq kdcReq;


    /**
     * Creates a new KdcReqContainer object.
     * @param stream The stream containing the data to decode
     */
    public KdcReqContainer( ByteBuffer stream )
    {
        super( stream );
        this.grammar = KdcReqGrammar.getInstance();
        setTransition( KdcReqStatesEnum.START_STATE );
    }


    /**
     * @return Returns the KdcReq.
     */
    public KdcReq getKdcReq()
    {
        return kdcReq;
    }


    /**
     * Set a KdcReq Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param kdcReq The KdcReq to set.
     */
    public void setKdcReq( KdcReq kdcReq )
    {
        this.kdcReq = kdcReq;
    }
}
