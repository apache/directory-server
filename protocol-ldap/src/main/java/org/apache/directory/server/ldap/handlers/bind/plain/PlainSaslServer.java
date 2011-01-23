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
package org.apache.directory.server.ldap.handlers.bind.plain;


import java.io.IOException;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.bind.AbstractSaslServer;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.schema.PrepareString;
import org.apache.directory.shared.util.StringConstants;
import org.apache.directory.shared.util.Strings;

import javax.naming.InvalidNameException;
import javax.security.sasl.SaslException;


/**
 * A SaslServer implementation for PLAIN based SASL mechanism.  This is
 * required unfortunately because the JDK's SASL provider does not support
 * this mechanism.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PlainSaslServer extends AbstractSaslServer
{
    /** The authzid property stored into the LdapSession instance */
    public static final String SASL_PLAIN_AUTHZID = "authzid";
    
    /** The authcid property stored into the LdapSession instance */
    public static final String SASL_PLAIN_AUTHCID = "authcid";

    /** The password property stored into the LdapSession instance */
    public static final String SASL_PLAIN_PASSWORD = "password";
    
    
    /**
     * The possible states for the negotiation of a PLAIN mechanism. 
     */
    private enum NegotiationState 
    {
        INITIALIZED,    // Negotiation has just started 
        MECH_RECEIVED,  // We have received the PLAIN mechanism
        COMPLETED       // The user/password have been received
    }
    
    
    /**
     * The different state used by the iInitialResponse decoding
     */
    private enum InitialResponse
    {
        AUTHZID_EXPECTED,    // We are expecting a authzid element
        AUTHCID_EXPECTED,    // We are expecting a authcid element 
        PASSWORD_EXPECTED    // We are expecting a password element
    }

    /** The current negotiation state */
    private NegotiationState state;
    
    
    /**
     * 
     * Creates a new instance of PlainSaslServer.
     *
     * @param bindRequest The associated BindRequest object
     * @param ldapSession The associated LdapSession instance 
     */
    public PlainSaslServer( LdapSession ldapSession, CoreSession adminSession, BindRequest bindRequest )
    {
        super( ldapSession, adminSession, bindRequest );
        state = NegotiationState.INITIALIZED;
        
        // Reinitialize the SASL properties
        getLdapSession().removeSaslProperty( SASL_PLAIN_AUTHZID );
        getLdapSession().removeSaslProperty( SASL_PLAIN_AUTHCID );
        getLdapSession().removeSaslProperty( SASL_PLAIN_PASSWORD );
    }


    /**
     * {@inheritDoc}
     */
    public String getMechanismName()
    {
        return SupportedSaslMechanisms.PLAIN;
    }


    /**
     * {@inheritDoc}
     */
    public byte[] evaluateResponse( byte[] initialResponse ) throws SaslException
    {
        if ( Strings.isEmpty(initialResponse) )
        {
            state = NegotiationState.MECH_RECEIVED;
            return null;
        }
        else
        {
            // Split the credentials in three parts :
            // - the optional authzId
            // - the authId
            // - the password
            InitialResponse element = InitialResponse.AUTHZID_EXPECTED;
            String authzId = null;
            String authcId = null;
            String password = null;
            
            int start = 0;
            int end = 0;
            
            try
            {
                for ( byte b:initialResponse )
                {
                    if ( b == '\0' )
                    {
                        if ( start - end == 0 )
                        {
                            // We don't have any value
                            if ( element == InitialResponse.AUTHZID_EXPECTED )
                            {
                                // This is optional : do nothing, but change
                                // the element type
                                element = InitialResponse.AUTHCID_EXPECTED;
                                continue;
                            }
                            else
                            {
                                // This not allowed
                                throw new IllegalArgumentException( I18n.err( I18n.ERR_671 ) );
                            }
                        }
                        else
                        {
                            start++;
                            String value = new String( initialResponse, start, end - start + 1, "UTF-8" );
                            
                            switch ( element )
                            {
                                case AUTHZID_EXPECTED :
                                    element = InitialResponse.AUTHCID_EXPECTED;
                                    authzId = PrepareString.normalize( value, PrepareString.StringType.CASE_EXACT_IA5 );
                                    end++;
                                    start = end;
                                    break;
                                    
                                case AUTHCID_EXPECTED :
                                    element = InitialResponse.PASSWORD_EXPECTED;
                                    authcId = PrepareString.normalize( value, PrepareString.StringType.DIRECTORY_STRING );
                                    end++;
                                    start = end;
                                    break;
                                    
                                    
                                default :
                                    // This is an error !
                                    throw new IllegalArgumentException( I18n.err( I18n.ERR_672 ) );
                            }
                        }
                    }
                    else
                    {
                        end++;
                    }
                }
            
                if ( start == end )
                {
                    throw new IllegalArgumentException( I18n.err( I18n.ERR_671 ) );
                }
                
                start++;
                String value = Strings.utf8ToString(initialResponse, start, end - start + 1);
                
                password = PrepareString.normalize( value, PrepareString.StringType.CASE_EXACT_IA5 );
                
                if ( ( authcId == null ) || ( password == null ) )
                {
                    throw new IllegalArgumentException( I18n.err( I18n.ERR_671 ) );
                }
                
                // Now that we have the authcid and password, try to authenticate.
                CoreSession userSession = authenticate( authcId, password );
                
                getLdapSession().setCoreSession( userSession );
                
                state = NegotiationState.COMPLETED;
            }
            catch ( IOException ioe )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_674 ) );
            }
            catch ( InvalidNameException ine )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_675 ) );
            }
            catch ( Exception e )
            {
                throw new SaslException( I18n.err( I18n.ERR_676, authcId ) );
            }
        }

        return StringConstants.EMPTY_BYTES;
    }


    public boolean isComplete()
    {
        return state == NegotiationState.COMPLETED;
    }
    
    
    /**
     * Try to authenticate the usr against the underlying LDAP server.
     */
    private CoreSession authenticate( String user, String password ) throws InvalidNameException, Exception
    {
        BindOperationContext bindContext = new BindOperationContext( getLdapSession().getCoreSession() );
        bindContext.setDn( new Dn( user ) );
        bindContext.setCredentials( Strings.getBytesUtf8(password) );
        
        getAdminSession().getDirectoryService().getOperationManager().bind( bindContext );
        
        return bindContext.getSession();
    }
}
