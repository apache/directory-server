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
package org.apache.directory.shared.kerberos.codec.asReq;


import java.nio.ByteBuffer;

import org.apache.directory.shared.kerberos.codec.kdcReq.KdcReqContainer;
import org.apache.directory.shared.kerberos.messages.AsReq;


/**
 * The AS-REQ container stores the KdcReq decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AsReqContainer extends KdcReqContainer
{
    /** An AS-REQ container */
    private AsReq asReq;


    /**
     * Creates a new AsReqContainer object.
     * @param stream The stream containing the data to decode
     */
    public AsReqContainer( ByteBuffer stream )
    {
        super( stream );
        setGrammar( AsReqGrammar.getInstance() );
        setTransition( AsReqStatesEnum.START_STATE );
    }


    /**
     * @return Returns the AsReq.
     */
    public AsReq getAsReq()
    {
        return asReq;
    }


    /**
     * Set an AsReq Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param asReq The AsReq to set.
     */
    public void setAsReq( AsReq asReq )
    {
        this.asReq = asReq;
    }
}
