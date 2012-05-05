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
package org.apache.directory.shared.kerberos.codec.kdcRep.actions;


import org.apache.directory.shared.kerberos.codec.actions.AbstractReadEncryptedPart;
import org.apache.directory.shared.kerberos.codec.kdcRep.KdcRepContainer;
import org.apache.directory.shared.kerberos.components.EncryptedData;


/**
 * The action used to set the ticket EncodedPart
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreEncPart extends AbstractReadEncryptedPart<KdcRepContainer>
{

    /**
     * Instantiates a new TicketEncPart action.
     */
    public StoreEncPart()
    {
        super( "Kerberos Ticket EncodedPart" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEncryptedData( EncryptedData encryptedData, KdcRepContainer kdcRepContainer )
    {
        kdcRepContainer.getKdcRep().setEncPart( encryptedData );

        kdcRepContainer.setGrammarEndAllowed( true );
    }
}
