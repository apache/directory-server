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
package org.apache.directory.shared.kerberos.codec.krbSafeBody;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.KrbSafeBody;


/**
 * The KrbSafeBody container stores the KRB-SAFE-BODY decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbSafeBodyContainer extends AbstractContainer
{
    /** An KRB-SAFE-BODY container */
    private KrbSafeBody krbSafeBody;


    /**
     * Creates a new KrbSafeBodyContainer object.
     */
    public KrbSafeBodyContainer()
    {
        super();
        this.stateStack = new int[1];
        this.grammar = KrbSafeBodyGrammar.getInstance();
        setTransition( KrbSafeBodyStatesEnum.START_STATE );
    }


    /**
     * @return Returns the KrbSafeBody.
     */
    public KrbSafeBody getKrbSafeBody()
    {
        return krbSafeBody;
    }


    /**
     * Set a KrbSafeBody Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param krbSafeBody The KrbSafeBody to set.
     */
    public void setKrbSafeBody( KrbSafeBody krbSafeBody )
    {
        this.krbSafeBody = krbSafeBody;
    }
}
