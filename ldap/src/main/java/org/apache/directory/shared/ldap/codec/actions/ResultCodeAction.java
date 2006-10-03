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
package org.apache.directory.shared.ldap.codec.actions;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapResponse;
import org.apache.directory.shared.ldap.codec.LdapResult;
import org.apache.directory.shared.ldap.codec.util.LdapResultEnum;
import org.apache.directory.shared.ldap.util.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to set the LdapResult result code.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ResultCodeAction extends GrammarAction
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( ResultCodeAction.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    public ResultCodeAction()
    {
        super( "Store resultCode" );
    }

    /**
     * The initialization action
     */
    public void action( IAsn1Container container ) throws DecoderException
    {
        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
        LdapMessage message = ldapMessageContainer.getLdapMessage();
        LdapResponse response = message.getLdapResponse();
        LdapResult ldapResult = new LdapResult();
        response.setLdapResult( ldapResult );

        // We don't have to allocate a LdapResult first.

        // The current TLV should be a integer
        // We get it and store it in MessageId
        TLV tlv = ldapMessageContainer.getCurrentTLV();

        Value value = tlv.getValue();
        int resultCode = 0;

        try
        {
            resultCode = IntegerDecoder.parse( value, 0, 90 );
        }
        catch ( IntegerDecoderException ide )
        {
            log.error( "The result code " + StringTools.dumpBytes( value.getData() ) + " is invalid : "
                + ide.getMessage() + ". The result code must be between (0 .. 90)" );

            throw new DecoderException( ide.getMessage() );
        }

        // Treat the 'normal' cases !
        switch ( resultCode )
        {
            case LdapResultEnum.SUCCESS:
            case LdapResultEnum.OPERATIONS_ERROR:
            case LdapResultEnum.PROTOCOL_ERROR:
            case LdapResultEnum.TIME_LIMIT_EXCEEDED:
            case LdapResultEnum.SIZE_LIMIT_EXCEEDED:
            case LdapResultEnum.COMPARE_FALSE:
            case LdapResultEnum.COMPARE_TRUE:
            case LdapResultEnum.AUTH_METHOD_NOT_SUPPORTED:
            case LdapResultEnum.STRONG_AUTH_REQUIRED:
            case LdapResultEnum.REFERRAL:
            case LdapResultEnum.ADMIN_LIMIT_EXCEEDED:
            case LdapResultEnum.UNAVAILABLE_CRITICAL_EXTENSION:
            case LdapResultEnum.CONFIDENTIALITY_REQUIRED:
            case LdapResultEnum.SASL_BIND_IN_PROGRESS:
            case LdapResultEnum.NO_SUCH_ATTRIBUTE:
            case LdapResultEnum.UNDEFINED_ATTRIBUTE_TYPE:
            case LdapResultEnum.INAPPROPRIATE_MATCHING:
            case LdapResultEnum.CONSTRAINT_VIOLATION:
            case LdapResultEnum.ATTRIBUTE_OR_VALUE_EXISTS:
            case LdapResultEnum.INVALID_ATTRIBUTE_SYNTAX:
            case LdapResultEnum.NO_SUCH_OBJECT:
            case LdapResultEnum.ALIAS_PROBLEM:
            case LdapResultEnum.INVALID_DN_SYNTAX:
            case LdapResultEnum.ALIAS_DEREFERENCING_PROBLEM:
            case LdapResultEnum.INAPPROPRIATE_AUTHENTICATION:
            case LdapResultEnum.INVALID_CREDENTIALS:
            case LdapResultEnum.INSUFFICIENT_ACCESS_RIGHTS:
            case LdapResultEnum.BUSY:
            case LdapResultEnum.UNAVAILABLE:
            case LdapResultEnum.UNWILLING_TO_PERFORM:
            case LdapResultEnum.LOOP_DETECT:
            case LdapResultEnum.NAMING_VIOLATION:
            case LdapResultEnum.OBJECT_CLASS_VIOLATION:
            case LdapResultEnum.NOT_ALLOWED_ON_NON_LEAF:
            case LdapResultEnum.NOT_ALLOWED_ON_RDN:
            case LdapResultEnum.ENTRY_ALREADY_EXISTS:
            case LdapResultEnum.AFFECTS_MULTIPLE_DSAS:
                ldapResult.setResultCode( resultCode );
                break;

            default:
                log.warn( "The resultCode " + resultCode + " is unknown." );
                ldapResult.setResultCode( LdapResultEnum.OTHER );
        }

        if ( IS_DEBUG )
        {
            log.debug( "The result code is set to " + LdapResultEnum.errorCode( resultCode ) );
        }
    }
}
