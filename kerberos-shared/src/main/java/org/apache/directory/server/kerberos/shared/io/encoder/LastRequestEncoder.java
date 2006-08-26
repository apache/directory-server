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
package org.apache.directory.server.kerberos.shared.io.encoder;


import org.apache.directory.server.kerberos.shared.messages.value.LastRequest;
import org.apache.directory.server.kerberos.shared.messages.value.LastRequestEntry;
import org.apache.directory.shared.asn1.der.DERGeneralizedTime;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class LastRequestEncoder
{
    /**
     * LastReq ::=   SEQUENCE OF SEQUENCE {
     * lr-type[0]               INTEGER,
     * lr-value[1]              KerberosTime
     * }
     */
    protected static DERSequence encode( LastRequest lastReq )
    {
        LastRequestEntry[] entries = lastReq.getEntries();
        DERSequence outerSequence = new DERSequence();

        for ( int ii = 0; ii < entries.length; ii++ )
        {
            DERSequence sequence = new DERSequence();
            sequence
                .add( new DERTaggedObject( 0, DERInteger.valueOf( entries[ii].getLastRequestType().getOrdinal() ) ) );
            sequence.add( new DERTaggedObject( 1, DERGeneralizedTime.valueOf( entries[ii].getLastRequestValue()
                .toDate() ) ) );
            outerSequence.add( sequence );
        }

        return outerSequence;
    }
}
