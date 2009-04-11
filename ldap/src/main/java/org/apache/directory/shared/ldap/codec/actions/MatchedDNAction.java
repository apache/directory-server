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


import javax.naming.InvalidNameException;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapResponseCodec;
import org.apache.directory.shared.ldap.codec.LdapResultCodec;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to set the LdapResult matched DN.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class MatchedDNAction extends GrammarAction
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( MatchedDNAction.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    public MatchedDNAction()
    {
        super( "Store matched DN" );
    }

    /**
     * The initialization action
     */
    public void action( IAsn1Container container ) throws DecoderException
    {
        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
        LdapMessageCodec ldapMessage = ldapMessageContainer.getLdapMessage();
        LdapResponseCodec response = ldapMessage.getLdapResponse();
        LdapResultCodec ldapResult = response.getLdapResult();

        // Get the Value and store it in the BindResponse
        TLV tlv = ldapMessageContainer.getCurrentTLV();

        // We have to handle the special case of a 0 length matched
        // DN
        if ( tlv.getLength() == 0 )
        {
            ldapResult.setMatchedDN( LdapDN.EMPTY_LDAPDN );
        }
        else
        {
            // A not null matchedDN is valid for resultCodes
            // NoSuchObject, AliasProblem, InvalidDNSyntax and
            // AliasDreferencingProblem.
            ResultCodeEnum resultCode = ldapResult.getResultCode();

            switch ( resultCode )
            {
                case NO_SUCH_OBJECT :
                case ALIAS_PROBLEM :
                case INVALID_DN_SYNTAX :
                case ALIAS_DEREFERENCING_PROBLEM :
                    byte[] dnBytes = tlv.getValue().getData();
                    
                    try
                    {
                        ldapResult.setMatchedDN( new LdapDN( dnBytes ) );
                    }
                    catch ( InvalidNameException ine )
                    {
                        // This is for the client side. We will never decode LdapResult on the server
                        String msg = "Incorrect DN given : " + StringTools.utf8ToString( dnBytes ) + 
                            " (" + StringTools.dumpBytes( dnBytes )
                            + ") is invalid";
                        log.error( "{} : {}", msg, ine.getMessage() );
                    
                        throw new DecoderException( "Incorrect DN given : " + ine.getMessage() );
                    }
                    
                    break;
                    
                default :
                    log.warn( "The matched DN should not be set when the result code is one of NoSuchObject," + 
                        " AliasProblem, InvalidDNSyntax or AliasDreferencingProblem" );

                    ldapResult.setMatchedDN( LdapDN.EMPTY_LDAPDN );
                    break;
            }
        }

        if ( IS_DEBUG )
        {
            log.debug( "The matchedDN is " + ldapResult.getMatchedDN() );
        }

        return;
    }
}
