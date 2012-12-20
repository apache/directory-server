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
package org.apache.directory.shared.kerberos.codec.padata.actions;


import org.apache.directory.api.asn1.actions.AbstractReadOctetString;
import org.apache.directory.shared.kerberos.codec.padata.PaDataContainer;


/**
 * The action used to store the PaData's padata-value
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StorePaDataValue extends AbstractReadOctetString<PaDataContainer>
{
    /**
     * Instantiates a new PaDataValue action.
     */
    public StorePaDataValue()
    {
        super( "PaData's padata-value", true ); // PaData value can be null 
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setOctetString( byte[] data, PaDataContainer paDataContainer )
    {
        paDataContainer.getPaData().setPaDataValue( data );
        paDataContainer.setGrammarEndAllowed( true );
    }
}
