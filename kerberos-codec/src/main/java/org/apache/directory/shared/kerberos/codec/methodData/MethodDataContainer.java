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
package org.apache.directory.shared.kerberos.codec.methodData;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.MethodData;
import org.apache.directory.shared.kerberos.components.PaData;


/**
 * The METHOD-DATA container stores the METHOD-DATA decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MethodDataContainer extends AbstractContainer
{
    /** holds MethodData */
    private MethodData methodData = new MethodData();


    /**
     * Creates a new MethodDataContainer object.
     */
    public MethodDataContainer()
    {
        super();
        this.stateStack = new int[1];
        this.grammar = MethodDataGrammar.getInstance();
        setTransition( MethodDataStatesEnum.START_STATE );
    }


    /**
     * @return Returns the MethodData.
     */
    public MethodData getMethodData()
    {
        return methodData;
    }


    /**
     * Set a MethodData Object into the container
     * 
     * @param methodData The MethodData to set.
     */
    public void setMethodData( MethodData methodData )
    {
        this.methodData = methodData;
    }


    /**
     * Add a PaData Object into the list. It will be completed by the
     * KerberosDecoder.
     * 
     * @param paData The paData to add.
     */
    public void addPaData( PaData paData )
    {
        methodData.addPaData( paData );
    }
}
