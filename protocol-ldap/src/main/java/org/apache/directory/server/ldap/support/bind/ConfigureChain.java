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
package org.apache.directory.server.ldap.support.bind;


import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.operations.GetPrincipal;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.constants.SupportedSASLMechanisms;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;
import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.sasl.Sasl;
import java.util.*;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ConfigureChain implements IoHandlerCommand
{
    private static final Logger LOG = LoggerFactory.getLogger( ConfigureChain.class );

    private DirContext ctx;


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        LdapServer ldapServer = ( LdapServer )
                session.getAttribute( LdapServer.class.toString() );

        Map<String, String> saslProps = new HashMap<String, String>();
        saslProps.put( Sasl.QOP, getActiveQop( ldapServer ) );
        saslProps.put( "com.sun.security.sasl.digest.realm", getActiveRealms( ldapServer ) );
        session.setAttribute( "saslProps", saslProps );

        session.setAttribute( "saslHost", ldapServer.getSaslHost() );
        session.setAttribute( "baseDn", ldapServer.getSearchBaseDn() );

        Set activeMechanisms = getActiveMechanisms( ldapServer );

        if ( activeMechanisms.contains( "GSSAPI" ) )
        {
            try
            {
                Subject saslSubject = getSubject( ldapServer );
                session.setAttribute( "saslSubject", saslSubject );
            }
            catch ( ServiceConfigurationException sce )
            {
                activeMechanisms.remove( "GSSAPI" );
                LOG.warn( sce.getMessage() );
            }
        }

        session.setAttribute( "supportedMechanisms", activeMechanisms );

        next.execute( session, message );
    }


    private Set getActiveMechanisms( LdapServer ldapServer )
    {
        List<String> supportedMechanisms = new ArrayList<String>();
        supportedMechanisms.add( SupportedSASLMechanisms.SIMPLE );
        supportedMechanisms.add( SupportedSASLMechanisms.CRAM_MD5 );
        supportedMechanisms.add( SupportedSASLMechanisms.DIGEST_MD5 );
        supportedMechanisms.add( SupportedSASLMechanisms.GSSAPI );

        Set<String> activeMechanisms = new HashSet<String>();

        for ( String desiredMechanism : ldapServer.getSupportedMechanisms() )
        {
            if ( supportedMechanisms.contains( desiredMechanism ) )
            {
                activeMechanisms.add( desiredMechanism );
            }
        }

        return activeMechanisms;
    }


    private String getActiveQop( LdapServer ldapServer )
    {
        List<String> supportedQop = new ArrayList<String>();
        supportedQop.add( "auth" );
        supportedQop.add( "auth-int" );
        supportedQop.add( "auth-conf" );

        StringBuilder saslQop = new StringBuilder();

        Iterator it = ldapServer.getSaslQop().iterator();
        while ( it.hasNext() )
        {
            String desiredQopLevel = ( String ) it.next();
            if ( supportedQop.contains( desiredQopLevel ) )
            {
                saslQop.append( desiredQopLevel );
            }

            if ( it.hasNext() )
            {
                // QOP is comma-delimited
                saslQop.append( "," );
            }
        }

        return saslQop.toString();
    }


    private String getActiveRealms( LdapServer ldapServer )
    {
        StringBuilder realms = new StringBuilder();

        Iterator it = ldapServer.getSaslRealms().iterator();
        while ( it.hasNext() )
        {
            String realm = ( String ) it.next();
            realms.append( realm );

            if ( it.hasNext() )
            {
                // realms are space-delimited
                realms.append( " " );
            }
        }

        return realms.toString();
    }


    private Subject getSubject( LdapServer ldapServer ) throws ServiceConfigurationException
    {
        String servicePrincipalName = ldapServer.getSaslPrincipal();

        KerberosPrincipal servicePrincipal = new KerberosPrincipal( servicePrincipalName );
        GetPrincipal getPrincipal = new GetPrincipal( servicePrincipal );

        PrincipalStoreEntry entry;

        try
        {
            entry = ( PrincipalStoreEntry ) execute( ldapServer, getPrincipal );
        }
        catch ( Exception e )
        {
            String message = "Service principal " + servicePrincipalName + " not found at search base DN "
                + ldapServer.getSearchBaseDn() + ".";
            throw new ServiceConfigurationException( message, e );
        }

        if ( entry == null )
        {
            String message = "Service principal " + servicePrincipalName + " not found at search base DN "
                + ldapServer.getSearchBaseDn() + ".";
            throw new ServiceConfigurationException( message );
        }

        EncryptionKey key = entry.getKeyMap().get( EncryptionType.DES_CBC_MD5 );
        byte[] keyBytes = key.getKeyValue();
        int type = key.getKeyType().getOrdinal();
        int kvno = key.getKeyVersion();

        KerberosKey serviceKey = new KerberosKey( servicePrincipal, keyBytes, type, kvno );
        Subject subject = new Subject();
        subject.getPrivateCredentials().add( serviceKey );

        return subject;
    }


    private Object execute( LdapServer ldapServer, ContextOperation operation ) throws Exception
    {
        Hashtable<String, Object> env = getEnvironment( ldapServer );

        if ( ctx == null )
        {
            try
            {
                ctx = new InitialLdapContext( env, null );
            }
            catch ( NamingException ne )
            {
                String message = "Failed to get initial context " + env.get( Context.PROVIDER_URL );
                throw new ServiceConfigurationException( message, ne );
            }
        }

        return operation.execute( ctx, null );
    }


    private Hashtable<String, Object> getEnvironment( LdapServer ldapServer )
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, ldapServer.getInitialContextFactory() );
        env.put( Context.PROVIDER_URL, ldapServer.getSearchBaseDn() );
        env.put( Context.SECURITY_AUTHENTICATION, ldapServer.getSecurityAuthentication() );
        env.put( Context.SECURITY_CREDENTIALS, ldapServer.getSecurityCredentials() );
        env.put( Context.SECURITY_PRINCIPAL, ldapServer.getSecurityPrincipal() );

        return env;
    }
}
