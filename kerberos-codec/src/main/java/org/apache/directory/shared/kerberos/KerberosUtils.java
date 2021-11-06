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
package org.apache.directory.shared.kerberos;


import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES3_CBC_MD5;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES3_CBC_SHA1;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES3_CBC_SHA1_KD;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES_CBC_CRC;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES_CBC_MD4;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES_CBC_MD5;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES_EDE3_CBC_ENV_OID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DSAWITHSHA1_CMSOID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.MD5WITHRSAENCRYPTION_CMSOID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.RC2CBC_ENVOID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.RC4_HMAC;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.RSAENCRYPTION_ENVOID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.RSAES_OAEP_ENV_OID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.SHA1WITHRSAENCRYPTION_CMSOID;

import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.shared.kerberos.codec.KerberosDecoder;
import org.apache.directory.shared.kerberos.codec.options.ApOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.Ticket;


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
    public static final List<String> EMPTY_PRINCIPAL_NAME = new ArrayList<>();

    /** 
     * an order preserved map containing cipher names to the corresponding algorithm 
     * names in the descending order of strength
     */
    private static final Map<String, String> cipherAlgoMap = new LinkedHashMap<>();

    private static final Set<EncryptionType> oldEncTypes = new HashSet<>();

    static
    {
        cipherAlgoMap.put( "rc4", "ArcFourHmac" );
        cipherAlgoMap.put( "aes256", "AES256" );
        cipherAlgoMap.put( "aes128", "AES128" );
        cipherAlgoMap.put( "des3", "DESede" );
        cipherAlgoMap.put( "des", "DES" );

        oldEncTypes.add( DES_CBC_CRC );
        oldEncTypes.add( DES_CBC_MD4 );
        oldEncTypes.add( DES_CBC_MD5 );
        oldEncTypes.add( DES_EDE3_CBC_ENV_OID );
        oldEncTypes.add( DES3_CBC_MD5 );
        oldEncTypes.add( DES3_CBC_SHA1 );
        oldEncTypes.add( DES3_CBC_SHA1_KD );
        oldEncTypes.add( DSAWITHSHA1_CMSOID );
        oldEncTypes.add( MD5WITHRSAENCRYPTION_CMSOID );
        oldEncTypes.add( SHA1WITHRSAENCRYPTION_CMSOID );
        oldEncTypes.add( RC2CBC_ENVOID );
        oldEncTypes.add( RSAENCRYPTION_ENVOID );
        oldEncTypes.add( RSAES_OAEP_ENV_OID );
        oldEncTypes.add( RC4_HMAC );
    }


    /**
     * Parse a KerberosPrincipal instance and return the names. The Principal name
     * is described in RFC 1964 : <br>
     * <br>
     * This name type corresponds to the single-string representation of a<br>
     * Kerberos name.  (Within the MIT Kerberos V5 implementation, such<br>
     * names are parseable with the krb5_parse_name() function.)  The<br>
     * elements included within this name representation are as follows,<br>
     * proceeding from the beginning of the string:<br>
     * <br>
     *  (1) One or more principal name components; if more than one<br>
     *  principal name component is included, the components are<br>
     *  separated by `/`.  Arbitrary octets may be included within<br>
     *  principal name components, with the following constraints and<br>
     *  special considerations:<br>
     * <br>
     *     (1a) Any occurrence of the characters `@` or `/` within a<br>
     *     name component must be immediately preceded by the `\`<br>
     *     quoting character, to prevent interpretation as a component<br>
     *     or realm separator.<br>
     * <br>
     *     (1b) The ASCII newline, tab, backspace, and null characters<br>
     *     may occur directly within the component or may be<br>
     *     represented, respectively, by `\n`, `\t`, `\b`, or `\0`.<br>
     * <br>
     *     (1c) If the `\` quoting character occurs outside the contexts<br>
     *     described in (1a) and (1b) above, the following character is<br>
     *     interpreted literally.  As a special case, this allows the<br>
     *     doubled representation `\\` to represent a single occurrence<br>
     *     of the quoting character.<br>
     * <br>
     *     (1d) An occurrence of the `\` quoting character as the last<br>
     *     character of a component is illegal.<br>
     * <br>
     *  (2) Optionally, a `@` character, signifying that a realm name<br>
     *  immediately follows. If no realm name element is included, the<br>
     *  local realm name is assumed.  The `/` , `:`, and null characters<br>
     *  may not occur within a realm name; the `@`, newline, tab, and<br>
     *  backspace characters may be included using the quoting<br>
     *  conventions described in (1a), (1b), and (1c) above.<br>
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

        if ( Strings.isEmpty( names ) )
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

        List<String> nameComponents = new ArrayList<>();

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
                case '\\':
                    escaped = !escaped;
                    break;

                case '/':
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
                            throw new ParseException( I18n.err( I18n.ERR_628 ), i );
                        }
                    }

                    break;

                case '@':
                    if ( escaped )
                    {
                        escaped = false;
                    }
                    else
                    {
                        // We have reached the realm : let's get out
                        done = true;
                    }

                    break;

                default:
            }

            if ( done )
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
                    throw new ParseException( I18n.err( I18n.ERR_628 ), i );
                }

                break;
            }
            else if ( i + 1 == chars.length )
            {
                // We have a new name component
                String nameComponent = new String( chars, start, i - start + 1 );
                nameComponents.add( nameComponent );

                break;
            }
        }

        if ( escaped )
        {
            throw new ParseException( I18n.err( I18n.ERR_629 ), pos );
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

        if ( !Strings.isEmpty( realm ) )
        {
            name += '@' + realm;
        }

        return new KerberosPrincipal( name, principal.getNameType().getValue() );
    }


    /**
     * Get the matching encryption type from the configured types, searching
     * into the requested types. We returns the first we find.
     *
     * @param requestedTypes The client encryption types
     * @param configuredTypes The configured encryption types
     * @return The first matching encryption type.
     */
    public static EncryptionType getBestEncryptionType( Set<EncryptionType> requestedTypes,
        Set<EncryptionType> configuredTypes )
    {
        for ( EncryptionType encryptionType : configuredTypes )
        {
            if ( requestedTypes.contains( encryptionType ) )
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

        for ( EncryptionType etype : encryptionTypes )
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


    public static boolean isKerberosString( byte[] value )
    {
        if ( value == null )
        {
            return false;
        }

        for ( byte b : value )
        {
            if ( ( b < 0x20 ) || ( b > 0x7E ) )
            {
                return false;
            }
        }

        return true;
    }


    public static String getAlgoNameFromEncType( EncryptionType encType )
    {
        String cipherName = Strings.toLowerCaseAscii( encType.getName() );

        for ( Map.Entry<String, String> entry : cipherAlgoMap.entrySet() )
        {
            if ( cipherName.startsWith( entry.getKey() ) )
            {
                return entry.getValue();
            }
        }

        throw new IllegalArgumentException( "Unknown algorithm name for the encryption type " + encType );
    }


    /**
     * Order a list of EncryptionType in a decreasing strength order
     * 
     * @param etypes The ETypes to order
     * @return A list of ordered ETypes. he strongest is on the left.
     */
    public static Set<EncryptionType> orderEtypesByStrength( Set<EncryptionType> etypes )
    {
        Set<EncryptionType> ordered = new LinkedHashSet<>( etypes.size() );

        for ( String algo : cipherAlgoMap.values() )
        {
            for ( EncryptionType encType : etypes )
            {
                String foundAlgo = getAlgoNameFromEncType( encType );

                if ( algo.equals( foundAlgo ) )
                {
                    ordered.add( encType );
                }
            }
        }

        return ordered;
    }


    /**
     * Get a PrincipalStoreEntry given a principal.  The ErrorType is used to indicate
     * whether any resulting error pertains to a server or client.
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
    public static Authenticator verifyAuthHeader( ApReq authHeader, Ticket ticket, EncryptionKey serverKey,
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
            ticketKey = authHeader.getTicket().getEncTicketPart().getKey();
        }
        else
        {
            ticketKey = serverKey;
        }

        if ( ticketKey == null )
        {
            // TODO - check server key version number, skvno; requires store
            //            if ( false )
            //            {
            //                throw new KerberosException( ErrorType.KRB_AP_ERR_BADKEYVER );
            //            }

            throw new KerberosException( ErrorType.KRB_AP_ERR_NOKEY );
        }

        byte[] encTicketPartData = lockBox.decrypt( ticketKey, ticket.getEncPart(),
            KeyUsage.AS_OR_TGS_REP_TICKET_WITH_SRVKEY );
        EncTicketPart encPart = KerberosDecoder.decodeEncTicketPart( encTicketPartData );
        ticket.setEncTicketPart( encPart );

        byte[] authenticatorData = lockBox.decrypt( ticket.getEncTicketPart().getKey(), authHeader.getAuthenticator(),
            authenticatorKeyUsage );

        Authenticator authenticator = KerberosDecoder.decodeAuthenticator( authenticatorData );

        if ( !authenticator.getCName().getNameString().equals( ticket.getEncTicketPart().getCName().getNameString() ) )
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

        KerberosPrincipal serverPrincipal = getKerberosPrincipal( ticket.getSName(), ticket.getRealm() );
        KerberosPrincipal clientPrincipal = getKerberosPrincipal( authenticator.getCName(), authenticator.getCRealm() );
        KerberosTime clientTime = authenticator.getCtime();
        int clientMicroSeconds = authenticator.getCusec();

        if ( replayCache != null )
        {
            if ( replayCache.isReplay( serverPrincipal, clientPrincipal, clientTime, clientMicroSeconds ) )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_REPEAT );
            }

            replayCache.save( serverPrincipal, clientPrincipal, clientTime, clientMicroSeconds );
        }

        if ( !authenticator.getCtime().isInClockSkew( clockSkew ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_SKEW );
        }

        /*
         * "The server computes the age of the ticket: local (server) time minus
         * the starttime inside the Ticket.  If the starttime is later than the
         * current time by more than the allowable clock skew, or if the INVALID
         * flag is set in the ticket, the KRB_AP_ERR_TKT_NYV error is returned."
         */
        KerberosTime startTime = ( ticket.getEncTicketPart().getStartTime() != null ) ? ticket.getEncTicketPart()
            .getStartTime() : ticket.getEncTicketPart().getAuthTime();

        KerberosTime now = new KerberosTime();
        boolean isValidStartTime = startTime.compareTo( now ) <= 0;

        if ( !isValidStartTime || ( ticket.getEncTicketPart().getFlags().isInvalid() && !isValidate ) )
        {
            // it hasn't yet become valid
            throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_NYV );
        }

        // TODO - doesn't take into account skew
        if ( ticket.getEncTicketPart().getEndTime().compareTo( now ) < 0)
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_EXPIRED );
        }

        authHeader.getApOptions().set( ApOptions.MUTUAL_REQUIRED );

        return authenticator;
    }


    /**
     * checks if the given encryption type is *new* (ref sec#3.1.3 of rfc4120)
     *
     * @param eType the encryption type
     * @return true if the encryption type is new, false otherwise
     */
    public static boolean isNewEncryptionType( EncryptionType eType )
    {
        return !oldEncTypes.contains( eType );
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
     *
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
         *
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
    }*/
}
