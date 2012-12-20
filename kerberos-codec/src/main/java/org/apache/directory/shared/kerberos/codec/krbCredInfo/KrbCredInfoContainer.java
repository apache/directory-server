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
package org.apache.directory.shared.kerberos.codec.krbCredInfo;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.KrbCredInfo;


/**
 * The KrbCredInfo container stores the KrbCredInfo decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbCredInfoContainer extends AbstractContainer
{
    /** An KrbCredInfo container */
    private KrbCredInfo krbCredInfo;


    /**
     * Creates a new KrbCredInfoContainer object.
     */
    public KrbCredInfoContainer()
    {
        super();
        this.stateStack = new int[1];
        this.grammar = KrbCredInfoGrammar.getInstance();
        setTransition( KrbCredInfoStatesEnum.START_STATE );
    }


    /**
     * @return Returns the KrbCredInfo.
     */
    public KrbCredInfo getKrbCredInfo()
    {
        return krbCredInfo;
    }


    /**
     * Set a KrbCredInfo Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param krbCredInfo The KrbCredInfo to set.
     */
    public void setKrbCredInfo( KrbCredInfo krbCredInfo )
    {
        this.krbCredInfo = krbCredInfo;
    }
}
