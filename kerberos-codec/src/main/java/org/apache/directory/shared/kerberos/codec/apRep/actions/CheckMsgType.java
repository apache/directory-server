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
package org.apache.directory.shared.kerberos.codec.apRep.actions;


import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.codec.actions.AbstractReadMsgType;
import org.apache.directory.shared.kerberos.codec.apRep.ApRepContainer;
import org.apache.directory.shared.kerberos.messages.ApRep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store the msg-type
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CheckMsgType extends AbstractReadMsgType
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( CheckMsgType.class );

    /**
     * Instantiates a new CheckMsgType action.
     */
    public CheckMsgType()
    {
        super( "AP-REP msg-type" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void verifyMsgType( KerberosMessageType krbMsgType, Asn1Container container ) throws DecoderException
    {
        ApRepContainer apRepContainer = ( ApRepContainer ) container;
        ApRep apRep = apRepContainer.getApRep();

        // The message type must be the expected one
        if ( krbMsgType != apRep.getMessageType() )
        {
            LOG.error( I18n.err( I18n.ERR_04070, krbMsgType, "The msg-type should be AP-REP" ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( "The msg-type should be AP-REP" );
        }
    }
}
