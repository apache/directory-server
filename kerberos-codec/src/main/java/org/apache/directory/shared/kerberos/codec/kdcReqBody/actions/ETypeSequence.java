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
package org.apache.directory.shared.kerberos.codec.kdcReqBody.actions;


import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.kerberos.codec.kdcReqBody.KdcReqBodyContainer;


/**
 * The action used to store the KDC-REQ-BODY EType sequence.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeSequence extends GrammarAction<KdcReqBodyContainer>
{
    /**
     * Instantiates a new ETypeSequence action.
     */
    public ETypeSequence()
    {
        super( "KDC-REQ-BODY EType sequence" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( KdcReqBodyContainer kdcReqBodyContainer ) throws DecoderException
    {
        TLV tlv = kdcReqBodyContainer.getCurrentTLV();

        // The Length can be null, in this case, we can potentially exit from this grammar
        if ( tlv.getLength() == 0 )
        {
            kdcReqBodyContainer.setGrammarEndAllowed( true );
        }
    }
}
