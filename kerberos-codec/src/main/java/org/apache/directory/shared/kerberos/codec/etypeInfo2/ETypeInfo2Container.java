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
package org.apache.directory.shared.kerberos.codec.etypeInfo2;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.ETypeInfo2;
import org.apache.directory.shared.kerberos.components.ETypeInfo2Entry;


/**
 * The ETypeInfo container stores the ETYPE-INFO2 decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeInfo2Container extends AbstractContainer
{
    /** holds ETypeInfo2 */
    private ETypeInfo2 etypeInfo2 = new ETypeInfo2();


    /**
     * Creates a new ETypeInfo2Container object.
     */
    public ETypeInfo2Container()
    {
        super();
        this.grammar = ETypeInfo2Grammar.getInstance();
        setTransition( ETypeInfo2StatesEnum.START_STATE );
    }


    /**
     * @return Returns the ETypeInfo2.
     */
    public ETypeInfo2 getETypeInfo2()
    {
        return etypeInfo2;
    }


    /**
     * Set a ETypeInfo2 Object into the container
     * 
     * @param etypeInfo2 The ETypeInfo2 to set.
     */
    public void setETypeInfo2( ETypeInfo2 etypeInfo2 )
    {
        this.etypeInfo2 = etypeInfo2;
    }


    /**
     * Add a EtypeInfo2Entry Object into the list. It will be completed by the
     * KerberosDecoder.
     * 
     * @param etypeInfo2Entry The EtypeInfo2Entry to add.
     */
    public void addEtypeInfo2Entry( ETypeInfo2Entry etypeInfo2Entry )
    {
        etypeInfo2.addETypeInfo2Entry( etypeInfo2Entry );
    }
}
