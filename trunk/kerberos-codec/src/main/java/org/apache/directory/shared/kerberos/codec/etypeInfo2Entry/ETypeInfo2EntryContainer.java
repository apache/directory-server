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
package org.apache.directory.shared.kerberos.codec.etypeInfo2Entry;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.ETypeInfo2Entry;


/**
 * The ETypeInfo2Entry container stores the ETYPE-INFO2-ENTRY decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeInfo2EntryContainer extends AbstractContainer
{
    /** holds ETypeInfo2Entry */
    private ETypeInfo2Entry etypeInfo2Entry;


    /**
     * Creates a new ETypeInfo2EntryContainer object.
     */
    public ETypeInfo2EntryContainer()
    {
        super();
        this.grammar = ETypeInfo2EntryGrammar.getInstance();
        setTransition( ETypeInfo2EntryStatesEnum.START_STATE );
    }


    /**
     * @return Returns the ETypeInfo2Entry.
     */
    public ETypeInfo2Entry getETypeInfo2Entry()
    {
        return etypeInfo2Entry;
    }


    /**
     * Set a ETypeInfo2Entry Object into the container
     * 
     * @param etypeInfo2Entry The ETypeInfo2Entry to set.
     */
    public void setETypeInfo2Entry( ETypeInfo2Entry etypeInfo2Entry )
    {
        this.etypeInfo2Entry = etypeInfo2Entry;
    }
}
