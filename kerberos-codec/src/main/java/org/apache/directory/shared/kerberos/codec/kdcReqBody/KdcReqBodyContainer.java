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
package org.apache.directory.shared.kerberos.codec.kdcReqBody;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.KdcReqBody;


/**
 * The KdcReqBody container stores the KDC-REQ-BODY decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcReqBodyContainer extends AbstractContainer
{
    /** An KDC-REQ-BODY container */
    private KdcReqBody kdcReqBody;


    /**
     * Creates a new KdcReqBodyContainer object.
     */
    public KdcReqBodyContainer( ByteBuffer stream )
    {
        super( stream );
        setGrammar( KdcReqBodyGrammar.getInstance() );
        setTransition( KdcReqBodyStatesEnum.START_STATE );
    }


    /**
     * @return Returns the KdcReqBody.
     */
    public KdcReqBody getKdcReqBody()
    {
        return kdcReqBody;
    }


    /**
     * Set a KdcReqBody Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param kdcReqBody The KdcReqBody to set.
     */
    public void setKdcReqBody( KdcReqBody kdcReqBody )
    {
        this.kdcReqBody = kdcReqBody;
    }
}
