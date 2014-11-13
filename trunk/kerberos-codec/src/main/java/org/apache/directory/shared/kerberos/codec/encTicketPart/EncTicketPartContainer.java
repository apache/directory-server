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
package org.apache.directory.shared.kerberos.codec.encTicketPart;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.EncTicketPart;


/**
 * The EncTicketPart container stores the EncTicketPart decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncTicketPartContainer extends AbstractContainer
{
    /** holds EncTicketPart */
    private EncTicketPart encTicketPart = new EncTicketPart();


    /**
     * Creates a new EncTicketPartContainer object.
     * @param stream The stream containing the data to decode
     */
    public EncTicketPartContainer( ByteBuffer stream )
    {
        super( stream );
        this.grammar = EncTicketPartGrammar.getInstance();
        setTransition( EncTicketPartStatesEnum.START_STATE );
    }


    /**
     * @return Returns the EncTicketPart.
     */
    public EncTicketPart getEncTicketPart()
    {
        return encTicketPart;
    }


    /**
     * Set a EncTicketPart Object into the container
     * 
     * @param encTicketPart The EncTicketPart to set.
     */
    public void setEncTicketPart( EncTicketPart encTicketPart )
    {
        this.encTicketPart = encTicketPart;
    }
}
