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
package org.apache.directory.shared.kerberos.codec.checksum;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.Checksum;


/**
 * The Checksum container stores the Checksum decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChecksumContainer extends AbstractContainer
{
    /** holds Checksum */
    private Checksum checksum;


    /**
     * Creates a new ChecksumContainer object.
     */
    public ChecksumContainer()
    {
        super();
        setGrammar( ChecksumGrammar.getInstance() );
        setTransition( ChecksumStatesEnum.START_STATE );
    }


    /**
     * @return Returns the Checksum.
     */
    public Checksum getChecksum()
    {
        return checksum;
    }


    /**
     * Set a Checksum Object into the container
     * 
     * @param checksum The Checksum to set.
     */
    public void setChecksum( Checksum checksum )
    {
        this.checksum = checksum;
    }
}
