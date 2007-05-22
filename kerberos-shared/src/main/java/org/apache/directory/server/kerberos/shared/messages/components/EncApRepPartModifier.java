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
package org.apache.directory.server.kerberos.shared.messages.components;


import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncApRepPartModifier
{
    private KerberosTime clientTime;
    private int cusec;
    private EncryptionKey subSessionKey; //optional
    private Integer sequenceNumber; //optional


    /**
     * Returns the {@link EncApRepPart}.
     *
     * @return The {@link EncApRepPart}.
     */
    public EncApRepPart getEncApRepPart()
    {
        return new EncApRepPart( clientTime, cusec, subSessionKey, sequenceNumber );
    }


    /**
     * Sets the client {@link KerberosTime}.
     *
     * @param clientTime
     */
    public void setClientTime( KerberosTime clientTime )
    {
        this.clientTime = clientTime;
    }


    /**
     * Sets the client microsecond.
     *
     * @param cusec
     */
    public void setClientMicroSecond( int cusec )
    {
        this.cusec = cusec;
    }


    /**
     * Sets the sub-session {@link EncryptionKey}.
     *
     * @param subSessionKey
     */
    public void setSubSessionKey( EncryptionKey subSessionKey )
    {
        this.subSessionKey = subSessionKey;
    }


    /**
     * Sets the sequence number.
     *
     * @param sequenceNumber
     */
    public void setSequenceNumber( Integer sequenceNumber )
    {
        this.sequenceNumber = sequenceNumber;
    }
}
