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
package org.apache.directory.shared.kerberos.codec.adAndOr;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.AdAndOr;


/**
 * The AdAndOr container stores the AD-AND-OR decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AdAndOrContainer extends AbstractContainer
{
    /** An AD-AND-OR container */
    private AdAndOr adAndOr = new AdAndOr();


    /**
     * Creates a new AdAndOrContainer object.
     */
    public AdAndOrContainer()
    {
        super();
        this.stateStack = new int[1];
        this.grammar = AdAndOrGrammar.getInstance();
        setTransition( AdAndOrStatesEnum.START_STATE );
    }


    /**
     * @return Returns the AdAndOr.
     */
    public AdAndOr getAdAndOr()
    {
        return adAndOr;
    }


    /**
     * Set an AdAndOr Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param adAndOr The AdAndOr to set.
     */
    public void setAdAndOr( AdAndOr adAndOr )
    {
        this.adAndOr = adAndOr;
    }
}
