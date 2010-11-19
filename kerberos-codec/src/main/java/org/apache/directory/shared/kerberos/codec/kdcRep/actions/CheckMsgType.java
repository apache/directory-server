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


import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.codec.actions.AbstractReadMsgType;
import org.apache.directory.shared.kerberos.codec.kdcRep.KdcRepContainer;
import org.apache.directory.shared.kerberos.components.KdcRep;
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
        super( "KDC-REP msg-type" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void verifyMsgType( KerberosMessageType krbMsgType, Asn1Container container ) throws DecoderException
    {
        KdcRepContainer kdcRepContainer = ( KdcRepContainer ) container;
        KdcRep kdcRep = kdcRepContainer.getKdcRep();

        // The message type must be the expected one
        if ( krbMsgType != kdcRep.getMessageType() )
        {
            LOG.error( I18n.err( I18n.ERR_04070, krbMsgType, "The msg-type should be AS-REQ or TGS-REQ" ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( "The msg-type should be AS-REQ or TGS-REQ" );
        }
    }
}
