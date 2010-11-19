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
package org.apache.directory.shared.kerberos.codec.krbError.actions;


import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.codec.actions.AbstractReadMsgType;
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
     * Instantiates a new StoreMsgType action.
     */
    public CheckMsgType()
    {
        super( "KRB-ERROR msg-type" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void verifyMsgType( KerberosMessageType krbMsgType, Asn1Container container ) throws DecoderException
    {
        // The message type must be the expected one
        if ( krbMsgType != KerberosMessageType.KRB_ERROR )
        {
            String msg = "The msg-type should be KRB-ERROR";
            LOG.error( I18n.err( I18n.ERR_04070, krbMsgType, msg ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( msg );
        }
    }
}
