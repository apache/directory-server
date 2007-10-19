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


import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.operations.GetPrincipal;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SupportedSASLMechanisms;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.BindResponse;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.MutableControl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ConfigureChain implements IoHandlerCommand
{
    private static final Logger LOG = LoggerFactory.getLogger( ConfigureChain.class );

    private DirContext ctx;

    /**
     * A Hashed Adapter mapping SASL mechanisms to their handlers.
     */
    private final Map<String, MechanismHandler> handlers;

    private static final MutableControl[] EMPTY = new MutableControl[0];
    
    private final SessionRegistry registry;
    

    /**
     * Creates a new instance of ConfigureChain.
     * 
     * Initialize the mechanism handlers.
     *
     * @param directoryService
     */
    public ConfigureChain( DirectoryService directoryService, SessionRegistry registry )
    {
        Map<String, MechanismHandler> map = new HashMap<String, MechanismHandler>();
        map.put( SupportedSASLMechanisms.CRAM_MD5, new CramMd5MechanismHandler( directoryService ) );
        map.put( SupportedSASLMechanisms.DIGEST_MD5, new DigestMd5MechanismHandler( directoryService ) );
        map.put( SupportedSASLMechanisms.GSSAPI, new GssapiMechanismHandler( directoryService ) );
        handlers = Collections.unmodifiableMap( map );
        
        this.registry = registry;
    }

    
    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        LdapServer ldapServer = ( LdapServer )
                session.getAttribute( LdapServer.class.toString() );

        Map<String, String> saslProps = new HashMap<String, String>();
        saslProps.put( Sasl.QOP, ldapServer.getSaslQopString() );
        saslProps.put( "com.sun.security.sasl.digest.realm", getActiveRealms( ldapServer ) );
        session.setAttribute( "saslProps", saslProps );

        session.setAttribute( "saslHost", ldapServer.getSaslHost() );
        session.setAttribute( "baseDn", ldapServer.getSearchBaseDn() );

        Set<String> activeMechanisms = ldapServer.getSupportedMechanisms();

        if ( activeMechanisms.contains( SupportedSASLMechanisms.GSSAPI ) )
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

        BindRequest bindRequest = ( BindRequest ) message;

        // Guard clause:  Reject unsupported SASL mechanisms.
        if ( !ldapServer.getSupportedMechanisms().contains( bindRequest.getSaslMechanism() ) )
        {
            LOG.error( "Bind error : {} mechanism not supported. Please check the server.xml configuration file (supportedMechanisms field)", 
                bindRequest.getSaslMechanism() );

            LdapResult bindResult = bindRequest.getResultResponse().getLdapResult();
            bindResult.setResultCode( ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
            bindResult.setErrorMessage( bindRequest.getSaslMechanism() + " is not a supported mechanism." );
            session.write( bindRequest.getResultResponse() );
            return;
        }

        handleSasl( next, session, bindRequest );
    }

    
    /**
     * Deal with a SASL bind request
     */
    public void handleSasl( NextCommand next, IoSession session, BindRequest bindRequest ) throws Exception
    {
        String sessionMechanism = bindRequest.getSaslMechanism();

        if ( sessionMechanism.equals( SupportedSASLMechanisms.SIMPLE ) )
        {
            /*
             * This is the principal name that will be used to bind to the DIT.
             */
            session.setAttribute( Context.SECURITY_PRINCIPAL, bindRequest.getName() );

            /*
             * These are the credentials that will be used to bind to the DIT.
             * For the simple mechanism, this will be a password, possibly one-way hashed.
             */
            session.setAttribute( Context.SECURITY_CREDENTIALS, bindRequest.getCredentials() );

            getLdapContext( next, session, bindRequest );
        }
        else
        {
            MechanismHandler mechanismHandler = handlers.get( sessionMechanism );

            if ( mechanismHandler == null )
            {
                LOG.error( "Handler unavailable for " + sessionMechanism );
                throw new IllegalArgumentException( "Handler unavailable for " + sessionMechanism );
            }

            SaslServer ss = mechanismHandler.handleMechanism( session, bindRequest );
            
            LdapResult result = bindRequest.getResultResponse().getLdapResult();

            if ( !ss.isComplete() )
            {
                try
                {
                    /*
                     * SaslServer will throw an exception if the credentials are null.
                     */
                    if ( bindRequest.getCredentials() == null )
                    {
                        bindRequest.setCredentials( new byte[0] );
                    }

                    byte[] tokenBytes = ss.evaluateResponse( bindRequest.getCredentials() );

                    if ( ss.isComplete() )
                    {
                        /*
                         * There may be a token to return to the client.  We set it here
                         * so it will be returned in a SUCCESS message, after an LdapContext
                         * has been initialized for the client.
                         */
                        session.setAttribute( "saslCreds", tokenBytes );

                        /*
                         * If we got here, we're ready to try getting an initial LDAP context.
                         */
                        getLdapContext( next, session, bindRequest );
                    }
                    else
                    {
                        LOG.info( "Continuation token had length " + tokenBytes.length );
                        result.setResultCode( ResultCodeEnum.SASL_BIND_IN_PROGRESS );
                        BindResponse resp = ( BindResponse ) bindRequest.getResultResponse();
                        resp.setServerSaslCreds( tokenBytes );
                        session.write( resp );
                        LOG.debug( "Returning final authentication data to client to complete context." );
                    }
                }
                catch ( SaslException se )
                {
                    LOG.error( se.getMessage() );
                    result.setResultCode( ResultCodeEnum.INVALID_CREDENTIALS );
                    result.setErrorMessage( se.getMessage() );
                    session.write( bindRequest.getResultResponse() );
                }
            }
        }
    }

    
    /**
     * Create a list of all the configured realms.
     */
    private String getActiveRealms( LdapServer ldapServer )
    {
        StringBuilder realms = new StringBuilder();
        boolean isFirst = true;

        for ( String realm:ldapServer.getSaslRealms() )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                realms.append( ' ' );
            }
            
            realms.append( realm );
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
            entry = findPrincipal( ldapServer, getPrincipal );
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
    
    private PrincipalStoreEntry findPrincipal( LdapServer ldapServer, GetPrincipal getPrincipal ) throws Exception
    {
        if ( ctx == null )
        {
            try
            {
                LdapPrincipal principal = new LdapPrincipal(
                        new LdapDN( PartitionNexus.ADMIN_PRINCIPAL ), AuthenticationLevel.SIMPLE );
                ctx = ldapServer.getDirectoryService().getJndiContext( principal, ldapServer.getSearchBaseDn() );
            }
            catch ( NamingException ne )
            {
                String message = "Failed to get initial context " + ldapServer.getSearchBaseDn();
                throw new ServiceConfigurationException( message, ne );
            }
        }

        return (PrincipalStoreEntry)getPrincipal.execute( ctx, null );
    }    
    
    
    private Hashtable<String, Object> getEnvironment( IoSession session, BindRequest bindRequest )
    {
        Object principal = session.getAttribute( Context.SECURITY_PRINCIPAL );

        /**
         * For simple, this is a password.  For strong, this is unused.
         */
        Object credentials = session.getAttribute( Context.SECURITY_CREDENTIALS );

        String sessionMechanism = bindRequest.getSaslMechanism();
        String authenticationLevel = getAuthenticationLevel( sessionMechanism );

        LOG.debug( "{} {}", Context.SECURITY_PRINCIPAL, principal );
        LOG.debug( "{} {}", Context.SECURITY_CREDENTIALS, credentials );
        LOG.debug( "{} {}", Context.SECURITY_AUTHENTICATION, authenticationLevel );

        // clone the environment first then add the required security settings
        Hashtable<String, Object> env = registry.getEnvironmentByCopy();
        env.put( Context.SECURITY_PRINCIPAL, principal );

        if ( credentials != null )
        {
            env.put( Context.SECURITY_CREDENTIALS, credentials );
        }

        env.put( Context.SECURITY_AUTHENTICATION, authenticationLevel );

        if ( bindRequest.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
        {
            env.put( Context.REFERRAL, "ignore" );
        }
        else
        {
            env.put( Context.REFERRAL, "throw" );
        }

        return env;
    }
    
    
    private void getLdapContext( NextCommand next, IoSession session, BindRequest bindRequest ) throws Exception
    {
        Hashtable<String, Object> env = getEnvironment( session, bindRequest );
        LdapResult result = bindRequest.getResultResponse().getLdapResult();
        LdapContext ctx;

        try
        {
            MutableControl[] connCtls = bindRequest.getControls().values().toArray( EMPTY );
            ctx = new InitialLdapContext( env, connCtls );

            registry.setLdapContext( session, ctx );
            
            // add the bind response controls 
            bindRequest.getResultResponse().addAll( ctx.getResponseControls() );
            
            returnSuccess( next, session, bindRequest );
        }
        catch ( NamingException e )
        {
            ResultCodeEnum code;

            if ( e instanceof LdapException )
            {
                code = ( ( LdapException ) e ).getResultCode();
                result.setResultCode( code );
            }
            else
            {
                code = ResultCodeEnum.getBestEstimate( e, bindRequest.getType() );
                result.setResultCode( code );
            }

            String msg = "Bind failed: " + e.getMessage();

            if ( LOG.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( e );
                msg += "\n\nBindRequest = \n" + bindRequest.toString();
            }

            if ( ( e.getResolvedName() != null )
                && ( ( code == ResultCodeEnum.NO_SUCH_OBJECT ) || ( code == ResultCodeEnum.ALIAS_PROBLEM )
                    || ( code == ResultCodeEnum.INVALID_DN_SYNTAX ) || ( code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM ) ) )
            {
                result.setMatchedDn( ( LdapDN ) e.getResolvedName() );
            }

            result.setErrorMessage( msg );
            session.write( bindRequest.getResultResponse() );

            ctx = null;
        }
    }

    
    private void returnSuccess( NextCommand next, IoSession session, BindRequest bindRequest ) throws Exception
    {
        /*
         * We have now both authenticated the client and retrieved a JNDI context for them.
         * We can return a success message to the client.
         */
        LdapResult result = bindRequest.getResultResponse().getLdapResult();

        byte[] tokenBytes = ( byte[] ) session.getAttribute( "saslCreds" );

        result.setResultCode( ResultCodeEnum.SUCCESS );
        BindResponse response = ( BindResponse ) bindRequest.getResultResponse();
        response.setServerSaslCreds( tokenBytes );

        String sessionMechanism = bindRequest.getSaslMechanism();

        /*
         * If the SASL mechanism is DIGEST-MD5 or GSSAPI, we insert a SASLFilter.
         */
        if ( sessionMechanism.equals( SupportedSASLMechanisms.DIGEST_MD5 ) || 
             sessionMechanism.equals( SupportedSASLMechanisms.GSSAPI ) )
        {
            LOG.debug( "Inserting SaslFilter to engage negotiated security layer." );

            IoFilterChain chain = session.getFilterChain();
            if ( !chain.contains( "SASL" ) )
            {
                SaslServer saslContext = ( SaslServer ) session.getAttribute( MechanismHandler.SASL_CONTEXT );
                chain.addBefore( "codec", "SASL", new SaslFilter( saslContext ) );
            }

            /*
             * We disable the SASL security layer once, to write the outbound SUCCESS
             * message without SASL security layer processing.
             */
            session.setAttribute( SaslFilter.DISABLE_SECURITY_LAYER_ONCE, Boolean.TRUE );
        }

        session.write( response );
        LOG.debug( "Returned SUCCESS message." );

        next.execute( session, bindRequest );
    }

    
    /**
     * Convert a SASL mechanism to an Authentication level
     *
     * @param sessionMechanism The resquested mechanism
     * @return The corresponding authentication level
     */
    private String getAuthenticationLevel( String sessionMechanism )
    {
        if ( sessionMechanism.equals( SupportedSASLMechanisms.SIMPLE ) )
        {
            return AuthenticationLevel.SIMPLE.toString();
        }
        else
        {
            return AuthenticationLevel.STRONG.toString();
        }
    }
}
