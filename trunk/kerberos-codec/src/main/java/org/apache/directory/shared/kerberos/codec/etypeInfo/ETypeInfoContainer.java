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
package org.apache.directory.shared.kerberos.codec.etypeInfo;


import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.ETypeInfo;
import org.apache.directory.shared.kerberos.components.ETypeInfoEntry;


/**
 * The ETypeInfo container stores the ETYPE-INFO decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeInfoContainer extends AbstractContainer
{
    /** holds ETypeInfo */
    private ETypeInfo etypeInfo = new ETypeInfo();


    /**
     * Creates a new ETypeInfoContainer object.
     */
    public ETypeInfoContainer()
    {
        super();
        this.stateStack = new int[1];
        this.grammar = ETypeInfoGrammar.getInstance();
        setTransition( ETypeInfoStatesEnum.START_STATE );
    }


    /**
     * @return Returns the ETypeInfo.
     */
    public ETypeInfo getETypeInfo()
    {
        return etypeInfo;
    }


    /**
     * Set a ETypeInfo Object into the container
     * 
     * @param etypeInfo The ETypeInfo to set.
     */
    public void setETypeInfo( ETypeInfo etypeInfo )
    {
        this.etypeInfo = etypeInfo;
    }


    /**
     * Add a EtypeInfoEntry Object into the list. It will be completed by the
     * KerberosDecoder.
     * 
     * @param etypeInfoEntry The ETypeInfoEntry to add.
     */
    public void addEtypeInfoEntry( ETypeInfoEntry etypeInfoEntry )
    {
        etypeInfo.addETypeInfoEntry( etypeInfoEntry );
    }
}
