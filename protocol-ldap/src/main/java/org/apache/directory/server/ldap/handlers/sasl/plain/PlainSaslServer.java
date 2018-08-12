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
package org.apache.directory.server.ldap.handlers.sasl.plain;


import java.io.IOException;

import javax.naming.InvalidNameException;
import javax.security.sasl.SaslException;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.schema.PrepareString;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.OperationManager;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.sasl.AbstractSaslServer;


/**
 * A SaslServer implementation for PLAIN based SASL mechanism.  This is
 * required unfortunately because the JDK's SASL provider does not support
 * this mechanism.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class PlainSaslServer extends AbstractSaslServer
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
        INITIALIZED, // Negotiation has just started 
        MECH_RECEIVED, // We have received the PLAIN mechanism
        COMPLETED // The user/password have been received
    }

    /**
     * The different state used by the iInitialResponse decoding
     */
    private enum InitialResponse
    {
        AUTHZID_EXPECTED, // We are expecting a authzid element
        AUTHCID_EXPECTED, // We are expecting a authcid element 
        PASSWORD_EXPECTED // We are expecting a password element
    }

    /** The current negotiation state */
    private NegotiationState state;

    /**
     * 
     * Creates a new instance of PlainSaslServer.
     *
     * @param ldapSession The associated LdapSession instance
     * @param adminSession The Administrator session 
     * @param bindRequest The associated BindRequest object
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
        if ( Strings.isEmpty( initialResponse ) )
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
            // The message should have this structure :
            // message   = [authzid] '0x00' authcid '0x00' passwd
            // authzid   = 1*SAFE ; MUST accept up to 255 octets
            // authcid   = 1*SAFE ; MUST accept up to 255 octets
            // passwd    = 1*SAFE ; MUST accept up to 255 octets
            InitialResponse element = InitialResponse.AUTHZID_EXPECTED;
            String authzId = null;
            String authcId = null;
            String password = null;

            int start = 0;
            int end = 0;

            try
            {
                for ( byte b : initialResponse )
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
                                case AUTHZID_EXPECTED:
                                    element = InitialResponse.AUTHCID_EXPECTED;
                                    authzId = PrepareString.normalize( value );
                                    end++;
                                    start = end;
                                    break;

                                case AUTHCID_EXPECTED:
                                    element = InitialResponse.PASSWORD_EXPECTED;
                                    authcId = PrepareString
                                        .normalize( value );
                                    end++;
                                    start = end;
                                    break;

                                default:
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
                String value = Strings.utf8ToString( initialResponse, start, end - start + 1 );

                password = PrepareString.normalize( value );

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

        return Strings.EMPTY_BYTES;
    }


    public boolean isComplete()
    {
        return state == NegotiationState.COMPLETED;
    }


    /**
     * Try to authenticate the user against the underlying LDAP server. The SASL PLAIN
     * authentication is based on the entry which uid is equal to the user name we received.
     */
    private CoreSession authenticate( String user, String password ) throws InvalidNameException, Exception
    {
        LdapSession ldapSession = getLdapSession();
        CoreSession adminSession = getAdminSession();
        DirectoryService directoryService = adminSession.getDirectoryService();
        LdapServer ldapServer = ldapSession.getLdapServer();
        OperationManager operationManager = directoryService.getOperationManager();

        // first, we have to find the entries which has the uid value
        EqualityNode<String> filter = new EqualityNode<String>(
            directoryService.getSchemaManager().getAttributeType( SchemaConstants.UID_AT ), new Value( user ) );

        SearchOperationContext searchContext = new SearchOperationContext( directoryService.getAdminSession() );
        searchContext.setDn( directoryService.getDnFactory().create( ldapServer.getSearchBaseDn() ) );
        searchContext.setScope( SearchScope.SUBTREE );
        searchContext.setFilter( filter );
        searchContext.setNoAttributes( true );

        EntryFilteringCursor cursor = operationManager.search( searchContext );
        Exception bindException = new LdapAuthenticationException( "Cannot authenticate user uid=" + user );

        while ( cursor.next() )
        {
            Entry entry = cursor.get();

            try
            {
                BindOperationContext bindContext = new BindOperationContext( ldapSession.getCoreSession() );
                bindContext.setDn( entry.getDn() );
                bindContext.setCredentials( Strings.getBytesUtf8( password ) );
                bindContext.setIoSession( ldapSession.getIoSession() );
                bindContext.setInterceptors( directoryService.getInterceptors( OperationEnum.BIND ) );

                operationManager.bind( bindContext );

                cursor.close();

                return bindContext.getSession();
            }
            catch ( Exception e )
            {
                bindException = e;// Nothing to do here : we will try to bind with the next user
            }
        }

        cursor.close();

        throw bindException;
    }
}
