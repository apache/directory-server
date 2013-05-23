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
package org.apache.directory.shared.kerberos.codec.apRep;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.messages.ApRep;


/**
 * The AP-REP container.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ApRepContainer extends AbstractContainer
{
    /** An AP-REP container */
    private ApRep apRep;


    /**
     * Creates a new ApRepContainer object.
     * @param stream The stream containing the data to decode
     */
    public ApRepContainer( ByteBuffer stream )
    {
        super( stream );
        this.grammar = ApRepGrammar.getInstance();
        setTransition( ApRepStatesEnum.START_STATE );
    }


    /**
     * @return Returns the ApRep.
     */
    public ApRep getApRep()
    {
        return apRep;
    }


    /**
     * Set an ApRep Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param apRep The ApRep to set.
     */
    public void setApRep( ApRep apRep )
    {
        this.apRep = apRep;
    }
}
