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
package org.apache.directory.server.kerberos.kdc;


import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.Krb5LoginConfiguration;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosTestUtils
{
    public static char[] getControlDocument( String resource ) throws IOException
    {
        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream( resource );

        CharArrayWriter writer = new CharArrayWriter();

        try ( Reader reader = new InputStreamReader( new BufferedInputStream( is ) ) )
        {
            char[] buf = new char[2048];
            int len = 0;
            while ( len >= 0 )
            {
                len = reader.read( buf );
                if ( len > 0 )
                {
                    writer.write( buf, 0, len );
                }
            }
        }

        char[] isca = writer.toCharArray();
        return isca;
    }


    public static byte[] getBytesFromResource( String resource ) throws IOException
    {
        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream( resource );

        try ( BufferedInputStream stream = new BufferedInputStream( is ) )
        {
            int len = stream.available();
            byte[] bytes = new byte[len];
            stream.read( bytes, 0, len );
            return bytes;
        }
    }


    public static void hexdump( byte[] data )
    {
        hexdump( data, true );
    }


    public static void hexdump( byte[] data, boolean delimit )
    {
        String delimiter = new String( "-------------------------------------------------" );

        if ( delimit )
        {
            System.out.println( delimiter );
        }

        int lineLength = 0;
        for ( int ii = 0; ii < data.length; ii++ )
        {
            System.out.print( byte2hexString( data[ii] ) + " " );
            lineLength++;

            if ( lineLength == 8 )
            {
                System.out.print( "  " );
            }

            if ( lineLength == 16 )
            {
                System.out.println();
                lineLength = 0;
            }
        }

        if ( delimit )
        {
            System.out.println();
            System.out.println( delimiter );
        }
    }

    public static final String[] hex_digit =
        { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };


    public static String byte2hexString( byte x )
    {
        String s = "";
        for ( int ii = 0; ii < 2; ii++ )
        {
            s = hex_digit[( ( ( x ) & 0xff ) & ( 15 << ( ii * 4 ) ) ) >>> ( ii * 4 )] + s;
        }

        return s;
    }


    public static String int2hexString( int x )
    {
        String s = "";
        for ( int ii = 0; ii < 8; ii++ )
        {
            s = hex_digit[( x & ( 15 << ( ii * 4 ) ) ) >>> ( ii * 4 )] + s;
        }

        return s;
    }


    public static String int2binString( int x )
    {
        String s = "";
        for ( int ii = 0; ii < 32; ii++ )
        {
            if ( ( ii > 0 ) && ( ii % 4 == 0 ) )
            {
                s = " " + s;
            }

            s = hex_digit[( x & ( 1 << ii ) ) >>> ii] + s;
        }

        return s;
    }


    public static String long2hexString( long x )
    {
        String s = "";
        for ( int ii = 0; ii < 16; ii++ )
        {
            s = hex_digit[( int ) ( ( x & ( 15L << ( ii * 4 ) ) ) >>> ( ii * 4 ) )] + s;
        }

        return s;
    }


    public static String long2binString( long x )
    {
        String s = "";
        for ( int ii = 0; ii < 64; ii++ )
        {
            if ( ( ii > 0 ) && ( ii % 4 == 0 ) )
            {
                s = " " + s;
            }

            s = hex_digit[( int ) ( ( x & ( 1L << ii ) ) >>> ii )] + s;
        }

        return s;
    }


    public static String byte2hexString( byte[] input )
    {
        return byte2hexString( input, 0, input.length );
    }


    public static String byte2hexString( byte[] input, int offset )
    {
        return byte2hexString( input, offset, input.length );
    }


    public static String byte2hexString( byte[] input, int offset, int length )
    {
        String result = "";
        for ( int ii = 0; ii < length; ii++ )
        {
            if ( ii + offset < input.length )
            {
                result += byte2hexString( input[ii + offset] );
            }
        }

        return result;
    }


    /**
     * Gets the host name for 'localhost' used for Kerberos tests.
     * On Windows 7 and Server 2008 the loopback address 127.0.0.1
     * isn't resolved to localhost by default. In that case we need
     * to use the IP address for the service principal.
     *
     * @return the hostname
     */
    public static String getHostName()
    {
        return Network.LOOPBACK_HOSTNAME;
    }


    /**
     * Obtains a new TGT from KDC.
     * 
     * Possible errors:
     * Bad username:  Client not found in Kerberos database
     * Bad password:  Integrity check on decrypted field failed
     * 
     * @param subject the empty subject
     * @param userName the user name 
     * @param password the password
     * @throws LoginException
     * 
     */
    public static void obtainTGT( Subject subject, String userName, String password ) throws LoginException
    {
        // Use our custom configuration to avoid reliance on external config
        Configuration.setConfiguration( new Krb5LoginConfiguration() );

        // Obtain TGT
        LoginContext lc = new LoginContext( KerberosUdpITest.class.getName(), subject, new
            CallbackHandlerBean( userName, password ) );
        lc.login();
    }

    private static class CallbackHandlerBean implements CallbackHandler
    {
        private String name;
        private String password;


        /**
         * Creates a new instance of CallbackHandlerBean.
         *
         * @param name
         * @param password
         */
        public CallbackHandlerBean( String name, String password )
        {
            this.name = name;
            this.password = password;
        }


        public void handle( Callback[] callbacks ) throws UnsupportedCallbackException, IOException
        {
            for ( Callback callback : callbacks )
            {
                if ( callback instanceof NameCallback )
                {
                    NameCallback nameCallback = ( NameCallback ) callback;
                    nameCallback.setName( name );
                }
                else if ( callback instanceof PasswordCallback )
                {
                    PasswordCallback passwordCallback = ( PasswordCallback ) callback;
                    passwordCallback.setPassword( password.toCharArray() );
                }
                else
                {
                    throw new UnsupportedCallbackException( callback, I18n.err( I18n.ERR_617 ) );
                }
            }
        }
    }


    /**
     * Obtains a Service Ticket from KDC.
     *
     * @param subject the subject, must contain a valid TGT
     * @param userName the user name
     * @param serviceName the service name
     * @param hostName the host name of the service
     * @throws GSSException
     */
    public static void obtainServiceTickets( Subject subject, String userName, String serviceName, String hostName )
        throws GSSException
    {
        ObtainServiceTicketAction action = new ObtainServiceTicketAction( userName, serviceName, hostName );
        GSSException exception = Subject.doAs( subject, action );
        if ( exception != null )
        {
            throw exception;
        }
    }

    private static class ObtainServiceTicketAction implements PrivilegedAction<GSSException>
    {
        private String userName;
        private String serviceName;
        private String hostName;


        public ObtainServiceTicketAction( String userName, String serviceName, String hostName )
        {
            this.userName = userName;
            this.serviceName = serviceName;
            this.hostName = hostName;
        }


        public GSSException run()
        {
            try
            {
                GSSManager manager = GSSManager.getInstance();
                GSSName clientName = manager.createName( userName, GSSName.NT_USER_NAME );
                GSSCredential clientCred = manager.createCredential( clientName,
                    8 * 3600,
                    createKerberosOid(),
                    GSSCredential.INITIATE_ONLY );

                GSSName serverName = manager.createName( serviceName + "@" + hostName, GSSName.NT_HOSTBASED_SERVICE );
                GSSContext context = manager.createContext( serverName,
                    createKerberosOid(),
                    clientCred,
                    GSSContext.DEFAULT_LIFETIME );
                context.requestMutualAuth( true );
                context.requestConf( true );
                context.requestInteg( true );

                context.initSecContext( Strings.EMPTY_BYTES, 0, 0 );

                // byte[] outToken = context.initSecContext( Strings.EMPTY_BYTES, 0, 0 );
                // System.out.println(new BASE64Encoder().encode(outToken));
                context.dispose();

                return null;
            }
            catch ( GSSException gsse )
            {
                return gsse;
            }
        }


        private Oid createKerberosOid() throws GSSException
        {
            return new Oid( "1.2.840.113554.1.2.2" );
        }
    }


    /**
     * Within the KerberosPrincipal/PrincipalName class a DNS lookup is done 
     * to get the canonical name of the host. So the principal name
     * may be extended to the form "ldap/localhost.example.com@EXAMPLE.COM".
     * This method fixes the SASL principal name of the service entry 
     * within the LDAP server.
     * 
     * @param servicePrincipalName the "original" service principal name
     * @param serviceEntryDn the service entry in LDAP
     * @param ldapServer the LDAP server instance
     * @return the fixed service principal name
     * @throws LdapException
     */
    public static String fixServicePrincipalName( String servicePrincipalName, Dn serviceEntryDn, LdapServer ldapServer )
        throws LdapException
    {
        KerberosPrincipal servicePrincipal = new KerberosPrincipal( servicePrincipalName,
            KerberosPrincipal.KRB_NT_SRV_HST );
        servicePrincipalName = servicePrincipal.getName();

        ldapServer.setSaslHost( servicePrincipalName.substring( servicePrincipalName.indexOf( "/" ) + 1,
            servicePrincipalName.indexOf( "@" ) ) );
        ldapServer.setSaslPrincipal( servicePrincipalName );

        if ( serviceEntryDn != null )
        {
            ModifyRequest modifyRequest = new ModifyRequestImpl();
            modifyRequest.setName( serviceEntryDn );
            modifyRequest.replace( "userPassword", "randall" );
            modifyRequest.replace( "krb5PrincipalName", servicePrincipalName );
            ldapServer.getDirectoryService().getAdminSession().modify( modifyRequest );
        }

        return servicePrincipalName;
    }
}
