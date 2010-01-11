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
package org.apache.directory.server.kerberos.shared;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.ApOptions;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.shared.ldap.util.StringTools;

/**
 * An utility class for Kerberos.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosUtils
{
    /** A constant for integer optional values */
    public static final int NULL = -1;

    /** An empty list of principal names */
    public static final List<String> EMPTY_PRINCIPAL_NAME = new ArrayList<String>();
    
    /**
     * Parse a KerberosPrincipal instance and return the names. The Principal name
     * is described in RFC 1964 : <br/>
     * <br/>
     * This name type corresponds to the single-string representation of a<br/>
     * Kerberos name.  (Within the MIT Kerberos V5 implementation, such<br/>
     * names are parseable with the krb5_parse_name() function.)  The<br/>
     * elements included within this name representation are as follows,<br/>
     * proceeding from the beginning of the string:<br/>
     * <br/>
     *  (1) One or more principal name components; if more than one<br/>
     *  principal name component is included, the components are<br/>
     *  separated by `/`.  Arbitrary octets may be included within<br/>
     *  principal name components, with the following constraints and<br/>
     *  special considerations:<br/>
     * <br/>
     *     (1a) Any occurrence of the characters `@` or `/` within a<br/>
     *     name component must be immediately preceded by the `\`<br/>
     *     quoting character, to prevent interpretation as a component<br/>
     *     or realm separator.<br/>
     * <br/>
     *     (1b) The ASCII newline, tab, backspace, and null characters<br/>
     *     may occur directly within the component or may be<br/>
     *     represented, respectively, by `\n`, `\t`, `\b`, or `\0`.<br/>
     * <br/>
     *     (1c) If the `\` quoting character occurs outside the contexts<br/>
     *     described in (1a) and (1b) above, the following character is<br/>
     *     interpreted literally.  As a special case, this allows the<br/>
     *     doubled representation `\\` to represent a single occurrence<br/>
     *     of the quoting character.<br/>
     * <br/>
     *     (1d) An occurrence of the `\` quoting character as the last<br/>
     *     character of a component is illegal.<br/>
     * <br/>
     *  (2) Optionally, a `@` character, signifying that a realm name<br/>
     *  immediately follows. If no realm name element is included, the<br/>
     *  local realm name is assumed.  The `/` , `:`, and null characters<br/>
     *  may not occur within a realm name; the `@`, newline, tab, and<br/>
     *  backspace characters may be included using the quoting<br/>
     *  conventions described in (1a), (1b), and (1c) above.<br/>
     * 
     * @param principal The principal to be parsed
     * @return The names as a List of nameComponent
     * 
     * @throws ParseException if the name is not valid
     */
    public static List<String> getNames( KerberosPrincipal principal ) throws ParseException
    {
        if ( principal == null )
        {
            return EMPTY_PRINCIPAL_NAME;
        }
        
        String names = principal.getName();
        
        if ( StringTools.isEmpty( names ) )
        {
            // Empty name...
            return EMPTY_PRINCIPAL_NAME;
        }
        
        return getNames( names );
    }

    /**
     * Parse a PrincipalName and return the names.
     */
    public static List<String> getNames( String principalNames ) throws ParseException
    {
        if ( principalNames == null )
        {
            return EMPTY_PRINCIPAL_NAME;
        }
        
        List<String> nameComponents = new ArrayList<String>();
        
        // Start the parsing. Another State Machine :)
        char[] chars = principalNames.toCharArray();
        
        boolean escaped = false;
        boolean done = false;
        int start = 0;
        int pos = 0;
        
        for ( int i = 0; i < chars.length; i++ )
        {
            pos = i;
            
            switch ( chars[i] )
            {
                case '\\' :
                    escaped = !escaped;
                    break;
                    
                case '/'  :
                    if ( escaped )
                    {
                        escaped = false;
                    }
                    else 
                    {
                        // We have a new name component
                        if ( i - start > 0 )
                        {
                            String nameComponent = new String( chars, start, i - start );
                            nameComponents.add( nameComponent );
                            start = i + 1;
                        }
                        else
                        {
                            throw new ParseException( "An empty name is not valid in a kerberos name", i );
                        }
                    }
                    
                    break;
                    
                case '@'  :
                    if ( escaped )
                    {
                        escaped = false;
                    }
                    else
                    {
                        // We have reached the realm : let's get out
                        done = true;
                        // We have a new name component

                        if ( i - start > 0 )
                        {
                            String nameComponent = new String( chars, start, i - start );
                            nameComponents.add( nameComponent );
                            start = i + 1;
                        }
                        else
                        {
                            throw new ParseException( "An empty name is not valid in a kerberos name", i );
                        }
                    }
                    
                    break;
                    
                default :
            }
            
            if ( done )
            {
                break;
            }
        } 
        
        if ( escaped )
        {
            throw new ParseException( "A '/' at the end of a Kerberos Name is not valid.", pos );
        }
        
        return nameComponents;
    }
    
    
    /**
     * Constructs a KerberosPrincipal from a PrincipalName and an 
     * optional realm
     *
     * @param principal The principal name and type
     * @param realm The optional realm
     * 
     * @return A KerberosPrincipal
     */
    public static KerberosPrincipal getKerberosPrincipal( PrincipalName principal, String realm )
    {
        String name = principal.getNameString(); 
        
        if ( !StringTools.isEmpty( realm ) )
        {
            name += '@' + realm;
        }
        
        return new KerberosPrincipal( name, principal.getNameType().getOrdinal() );
    }


    /**
     * Get the matching encryption type from the configured types, searching
     * into the requested types. We returns the first we find.
     *
     * @param requestedTypes The client encryption types
     * @param configuredTypes The configured encryption types
     * @return The first matching encryption type.
     */
    public static EncryptionType getBestEncryptionType( Set<EncryptionType> requestedTypes, Set<EncryptionType> configuredTypes )
    {
        for ( EncryptionType encryptionType:requestedTypes )
        {
            if ( configuredTypes.contains( encryptionType ) )
            {
                return encryptionType;
            }
        }

        return null;
    }
    
    
    /**
     * Build a list of encryptionTypes
     *
     * @param encryptionTypes The encryptionTypes
     * @return A list comma separated of the encryptionTypes
     */
    public static String getEncryptionTypesString( Set<EncryptionType> encryptionTypes )
    {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        for ( EncryptionType etype:encryptionTypes )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }
            
            sb.append( etype );
        }

        return sb.toString();
    }


    /**
     * Get a PrincipalStoreEntry given a principal.  The ErrorType is used to indicate
     * whether any resulting error pertains to a server or client.
     *
     * @param principal
     * @param store
     * @param errorType
     * @return The PrincipalStoreEntry
     * @throws Exception
     */
    public static PrincipalStoreEntry getEntry( KerberosPrincipal principal, PrincipalStore store, ErrorType errorType )
        throws KerberosException
    {
        PrincipalStoreEntry entry = null;

        try
        {
            entry = store.getPrincipal( principal );
        }
        catch ( Exception e )
        {
            throw new KerberosException( errorType, e );
        }

        if ( entry == null )
        {
            throw new KerberosException( errorType );
        }

        if ( entry.getKeyMap() == null || entry.getKeyMap().isEmpty() )
        {
            throw new KerberosException( ErrorType.KDC_ERR_NULL_KEY );
        }

        return entry;
    }


    /**
     * Verifies an AuthHeader using guidelines from RFC 1510 section A.10., "KRB_AP_REQ verification."
     *
     * @param authHeader
     * @param ticket
     * @param serverKey
     * @param clockSkew
     * @param replayCache
     * @param emptyAddressesAllowed
     * @param clientAddress
     * @param lockBox
     * @param authenticatorKeyUsage
     * @param isValidate
     * @return The authenticator.
     * @throws KerberosException
     */
    public static Authenticator verifyAuthHeader( ApplicationRequest authHeader, Ticket ticket, EncryptionKey serverKey,
        long clockSkew, ReplayCache replayCache, boolean emptyAddressesAllowed, InetAddress clientAddress,
        CipherTextHandler lockBox, KeyUsage authenticatorKeyUsage, boolean isValidate ) throws KerberosException
    {
        if ( authHeader.getProtocolVersionNumber() != KerberosConstants.KERBEROS_V5 )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BADVERSION );
        }

        if ( authHeader.getMessageType() != KerberosMessageType.AP_REQ )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_MSG_TYPE );
        }

        if ( authHeader.getTicket().getTktVno() != KerberosConstants.KERBEROS_V5 )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BADVERSION );
        }

        EncryptionKey ticketKey = null;

        if ( authHeader.getOption( ApOptions.USE_SESSION_KEY ) )
        {
            ticketKey = authHeader.getTicket().getEncTicketPart().getSessionKey();
        }
        else
        {
            ticketKey = serverKey;
        }

        if ( ticketKey == null )
        {
            // TODO - check server key version number, skvno; requires store
            if ( false )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_BADKEYVER );
            }

            throw new KerberosException( ErrorType.KRB_AP_ERR_NOKEY );
        }

        EncTicketPart encPart = ( EncTicketPart ) lockBox.unseal( EncTicketPart.class, ticketKey, ticket.getEncPart(),
            KeyUsage.NUMBER2 );
        ticket.setEncTicketPart( encPart );

        Authenticator authenticator = ( Authenticator ) lockBox.unseal( Authenticator.class, ticket.getEncTicketPart().getSessionKey(),
            authHeader.getEncPart(), authenticatorKeyUsage );

        if ( !authenticator.getClientPrincipal().getName().equals( ticket.getEncTicketPart().getClientPrincipal().getName() ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BADMATCH );
        }

        if ( ticket.getEncTicketPart().getClientAddresses() != null )
        {
            if ( !ticket.getEncTicketPart().getClientAddresses().contains( new HostAddress( clientAddress ) ) )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_BADADDR );
            }
        }
        else
        {
            if ( !emptyAddressesAllowed )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_BADADDR );
            }
        }

        KerberosPrincipal serverPrincipal = ticket.getServerPrincipal();
        KerberosPrincipal clientPrincipal = authenticator.getClientPrincipal();
        KerberosTime clientTime = authenticator.getClientTime();
        int clientMicroSeconds = authenticator.getClientMicroSecond();

        if ( replayCache.isReplay( serverPrincipal, clientPrincipal, clientTime, clientMicroSeconds ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_REPEAT );
        }

        replayCache.save( serverPrincipal, clientPrincipal, clientTime, clientMicroSeconds );

        if ( !authenticator.getClientTime().isInClockSkew( clockSkew ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_SKEW );
        }

        /*
         * "The server computes the age of the ticket: local (server) time minus
         * the starttime inside the Ticket.  If the starttime is later than the
         * current time by more than the allowable clock skew, or if the INVALID
         * flag is set in the ticket, the KRB_AP_ERR_TKT_NYV error is returned."
         */
        KerberosTime startTime = ( ticket.getEncTicketPart().getStartTime() != null ) ? ticket.getEncTicketPart().getStartTime() : ticket.getEncTicketPart().getAuthTime();

        KerberosTime now = new KerberosTime();
        boolean isValidStartTime = startTime.lessThan( now );

        if ( !isValidStartTime || ( ticket.getEncTicketPart().getFlags().isInvalid() && !isValidate ) )
        {
            // it hasn't yet become valid
            throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_NYV );
        }

        // TODO - doesn't take into account skew
        if ( !ticket.getEncTicketPart().getEndTime().greaterThan( now ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_EXPIRED );
        }

        authHeader.setOption( ApOptions.MUTUAL_REQUIRED );

        return authenticator;
    }
}
