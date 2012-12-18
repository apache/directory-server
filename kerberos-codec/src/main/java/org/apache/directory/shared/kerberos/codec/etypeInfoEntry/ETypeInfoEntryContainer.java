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
package org.apache.directory.shared.kerberos.codec.etypeInfoEntry;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.ETypeInfoEntry;


/**
 * The ETypeInfoEntry container stores the ETYPE-INFO-ENTRY decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeInfoEntryContainer extends AbstractContainer
{
    /** holds ETypeInfoEntry */
    private ETypeInfoEntry etypeInfoEntry;


    /**
     * Creates a new ETypeInfoEntryContainer object.
     */
    public ETypeInfoEntryContainer()
    {
        super();
        this.stateStack = new int[1];
        this.grammar = ETypeInfoEntryGrammar.getInstance();
        setTransition( ETypeInfoEntryStatesEnum.START_STATE );
    }


    /**
     * @return Returns the ETypeInfoEntry.
     */
    public ETypeInfoEntry getETypeInfoEntry()
    {
        return etypeInfoEntry;
    }


    /**
     * Set a ETypeInfoEntry Object into the container
     * 
     * @param etypeInfoEntry The ETypeInfoEntry to set.
     */
    public void setETypeInfoEntry( ETypeInfoEntry etypeInfoEntry )
    {
        this.etypeInfoEntry = etypeInfoEntry;
    }
}
