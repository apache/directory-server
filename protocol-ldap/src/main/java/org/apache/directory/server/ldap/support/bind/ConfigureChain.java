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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.sasl.Sasl;

import org.apache.directory.server.core.configuration.ConfigurationException;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.ldap.LdapConfiguration;
import org.apache.directory.server.ldap.constants.SupportedSASLMechanisms;
import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ConfigureChain implements IoHandlerCommand
{
    private static final Logger log = LoggerFactory.getLogger( ConfigureChain.class );

    private DirContext ctx;


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        LdapConfiguration config = ( LdapConfiguration ) session.getAttribute( LdapConfiguration.class.toString() );

        Map<String, String> saslProps = new HashMap<String, String>();
        saslProps.put( Sasl.QOP, getActiveQop( config ) );
        saslProps.put( "com.sun.security.sasl.digest.realm", getActiveRealms( config ) );
        session.setAttribute( "saslProps", saslProps );

        session.setAttribute( "saslHost", config.getSaslHost() );
        session.setAttribute( "baseDn", config.getSearchBaseDn() );

        Set activeMechanisms = getActiveMechanisms( config );

        if ( activeMechanisms.contains( "GSSAPI" ) )
        {
            try
            {
                Subject saslSubject = getSubject( config );
                session.setAttribute( "saslSubject", saslSubject );
            }
            catch ( ConfigurationException ce )
            {
                activeMechanisms.remove( "GSSAPI" );
                log.warn( ce.getMessage(), ce );
            }
        }

        session.setAttribute( "supportedMechanisms", activeMechanisms );

        next.execute( session, message );
    }


    private Set getActiveMechanisms( LdapConfiguration config )
    {
        List<String> supportedMechanisms = new ArrayList<String>();
        supportedMechanisms.add( SupportedSASLMechanisms.SIMPLE );
        supportedMechanisms.add( SupportedSASLMechanisms.CRAM_MD5 );
        supportedMechanisms.add( SupportedSASLMechanisms.DIGEST_MD5 );
        supportedMechanisms.add( SupportedSASLMechanisms.GSSAPI );

        Set<String> activeMechanisms = new HashSet<String>();

        Iterator it = config.getSupportedMechanisms().iterator();
        while ( it.hasNext() )
        {
            String desiredMechanism = ( String ) it.next();
            if ( supportedMechanisms.contains( desiredMechanism ) )
            {
                activeMechanisms.add( desiredMechanism );
            }
        }

        return activeMechanisms;
    }


    private String getActiveQop( LdapConfiguration config )
    {
        List<String> supportedQop = new ArrayList<String>();
        supportedQop.add( "auth" );
        supportedQop.add( "auth-int" );
        supportedQop.add( "auth-conf" );

        StringBuilder saslQop = new StringBuilder();

        Iterator it = config.getSaslQop().iterator();
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


    private String getActiveRealms( LdapConfiguration config )
    {
        StringBuilder realms = new StringBuilder();

        Iterator it = config.getSaslRealms().iterator();
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


    private Subject getSubject( LdapConfiguration config ) throws ConfigurationException
    {
        String servicePrincipalName = config.getSaslPrincipal();

        KerberosPrincipal servicePrincipal = new KerberosPrincipal( servicePrincipalName );
        GetPrincipal getPrincipal = new GetPrincipal( servicePrincipal );

        PrincipalStoreEntry entry;

        try
        {
            entry = ( PrincipalStoreEntry ) execute( config, getPrincipal );
        }
        catch ( Exception e )
        {
            String message = "Service principal " + servicePrincipalName + " not found at search base DN "
                + config.getSearchBaseDn() + ".";
            throw new ConfigurationException( message, e );
        }

        if ( entry == null )
        {
            String message = "Service principal " + servicePrincipalName + " not found at search base DN "
                + config.getSearchBaseDn() + ".";
            throw new ConfigurationException( message );
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


    private Object execute( LdapConfiguration config, ContextOperation operation ) throws Exception
    {
        Hashtable<String, Object> env = getEnvironment( config );

        if ( ctx == null )
        {
            try
            {
                ctx = new InitialLdapContext( env, null );
            }
            catch ( NamingException ne )
            {
                String message = "Failed to get initial context " + ( String ) env.get( Context.PROVIDER_URL );
                throw new ConfigurationException( message, ne );
            }
        }

        return operation.execute( ctx, null );
    }


    private Hashtable<String, Object> getEnvironment( LdapConfiguration config )
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>( config.toJndiEnvironment() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory() );
        env.put( Context.PROVIDER_URL, config.getSearchBaseDn() );
        env.put( Context.SECURITY_AUTHENTICATION, config.getSecurityAuthentication() );
        env.put( Context.SECURITY_CREDENTIALS, config.getSecurityCredentials() );
        env.put( Context.SECURITY_PRINCIPAL, config.getSecurityPrincipal() );

        return env;
    }
}
