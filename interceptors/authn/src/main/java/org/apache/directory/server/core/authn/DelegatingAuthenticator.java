/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.authn;


import java.io.IOException;
import java.net.SocketAddress;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.mina.core.session.IoSession;


/**
 * Authenticator delegating to another LDAP server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DelegatingAuthenticator extends AbstractAuthenticator
{
    /** A speedup for logger in debug mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The host in charge of delegated authentication */
    private String delegateHost;

    /** The associated port */
    private int delegatePort;

    /** Tells if we use SSL to connect */
    private boolean delegateSsl;

    /** Tells if we use StartTLS to connect */
    private boolean delegateTls;

    /** The SSL TrustManager FQCN to use */
    private String delegateSslTrustManagerFQCN;

    /** The startTLS TrustManager FQCN to use */
    private String delegateTlsTrustManagerFQCN;


    /**
     * Creates a new instance.
     */
    public DelegatingAuthenticator()
    {
        super( AuthenticationLevel.SIMPLE );
    }


    /**
     * Creates a new instance.
     * @see AbstractAuthenticator
     * @param baseDn The base Dn
     */
    public DelegatingAuthenticator( Dn baseDn )
    {
        super( AuthenticationLevel.SIMPLE, baseDn );
    }


    /**
     * Creates a new instance, for a specific authentication level.
     * @see AbstractAuthenticator
     * @param type The relevant AuthenticationLevel
     * @param baseDn The base Dn
     */
    protected DelegatingAuthenticator( AuthenticationLevel type, Dn baseDn )
    {
        super( type, baseDn );
    }


    /**
     * @return the delegateHost
     */
    public String getDelegateHost()
    {
        return delegateHost;
    }


    /**
     * @param delegateHost the delegateHost to set
     */
    public void setDelegateHost( String delegateHost )
    {
        this.delegateHost = delegateHost;
    }


    /**
     * @return the delegatePort
     */
    public int getDelegatePort()
    {
        return delegatePort;
    }


    /**
     * @param delegatePort the delegatePort to set
     */
    public void setDelegatePort( int delegatePort )
    {
        this.delegatePort = delegatePort;
    }


    /**
     * @return the delegateSsl
     */
    public boolean isDelegateSsl()
    {
        return delegateSsl;
    }


    /**
     * @param delegateSsl the delegateSsl to set
     */
    public void setDelegateSsl( boolean delegateSsl )
    {
        this.delegateSsl = delegateSsl;
    }


    /**
     * @return the delegateBaseDn
     */
    public String getDelegateBaseDn()
    {
        return getBaseDn().toString();
    }


    /**
     * @return the delegateTls
     */
    public boolean isDelegateTls()
    {
        return delegateTls;
    }


    /**
     * @param delegateTls the delegateTls to set
     */
    public void setDelegateTls( boolean delegateTls )
    {
        this.delegateTls = delegateTls;
    }


    /**
     * @return the delegateSslTrustManagerFQCN
     */
    public String getDelegateSslTrustManagerFQCN()
    {
        return delegateSslTrustManagerFQCN;
    }


    /**
     * @param delegateSslTrustManagerFQCN the delegateSslTrustManagerFQCN to set
     */
    public void setDelegateSslTrustManagerFQCN( String delegateSslTrustManagerFQCN )
    {
        this.delegateSslTrustManagerFQCN = delegateSslTrustManagerFQCN;
    }


    /**
     * @return the delegateTlsTrustManagerFQCN
     */
    public String getDelegateTlsTrustManagerFQCN()
    {
        return delegateTlsTrustManagerFQCN;
    }


    /**
     * @param delegateTlsTrustManagerFQCN the delegateTlsTrustManagerFQCN to set
     */
    public void setDelegateTlsTrustManagerFQCN( String delegateTlsTrustManagerFQCN )
    {
        this.delegateTlsTrustManagerFQCN = delegateTlsTrustManagerFQCN;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LdapPrincipal authenticate( BindOperationContext bindContext )
        throws LdapException
    {
        LdapPrincipal principal = null;

        if ( IS_DEBUG )
        {
            LOG.debug( "Authenticating {}", bindContext.getDn() );
        }

        // First, check that the Bind DN is under the delegateBaseDn
        Dn bindDn = bindContext.getDn();

        // Don't authenticate using this authenticator if the Bind ND is not a descendant of the
        // configured delegate base DN (or if it's null)
        if ( ( getBaseDn() == null ) || ( !bindDn.isDescendantOf( getBaseDn() ) ) )
        {
            return null;
        }

        LdapConnectionConfig connectionConfig;
        LdapNetworkConnection ldapConnection;

        // Create a connection on the remote host
        if ( delegateTls )
        {
            connectionConfig = new LdapConnectionConfig();
            connectionConfig.setLdapHost( delegateHost );
            connectionConfig.setLdapPort( delegatePort );
            connectionConfig.setTrustManagers( new NoVerificationTrustManager() );

            ldapConnection = new LdapNetworkConnection( connectionConfig );
            ldapConnection.connect();
            ldapConnection.startTls();
        }
        else if ( delegateSsl )
        {
            connectionConfig = new LdapConnectionConfig();
            connectionConfig.setLdapHost( delegateHost );
            connectionConfig.setUseSsl( true );
            connectionConfig.setLdapPort( delegatePort );
            connectionConfig.setTrustManagers( new NoVerificationTrustManager() );

            ldapConnection = new LdapNetworkConnection( connectionConfig );
            ldapConnection.connect();
        }
        else
        {
            connectionConfig = new LdapConnectionConfig();
            connectionConfig.setLdapHost( delegateHost );
            connectionConfig.setLdapPort( delegatePort );

            ldapConnection = new LdapNetworkConnection( delegateHost, delegatePort );
            ldapConnection.connect();
        }

        ldapConnection.setTimeOut( 0L );

        try
        {
            // Try to bind
            try
            {
                ldapConnection.bind( bindDn, Strings.utf8ToString( bindContext.getCredentials() ) );

                // no need to remain bound to delegate host
                ldapConnection.unBind();
            }
            catch ( LdapException le )
            {
                String message = I18n.err( I18n.ERR_230, bindDn.getName() );
                LOG.info( message );
                throw new LdapAuthenticationException( message );
            }

            // Create the new principal
            principal = new LdapPrincipal( getDirectoryService().getSchemaManager(), bindDn,
                AuthenticationLevel.SIMPLE,
                bindContext.getCredentials() );

            IoSession session = bindContext.getIoSession();

            if ( session != null )
            {
                SocketAddress clientAddress = session.getRemoteAddress();
                principal.setClientAddress( clientAddress );
                SocketAddress serverAddress = session.getServiceAddress();
                principal.setServerAddress( serverAddress );
            }

            return principal;
        }
        catch ( LdapException e )
        {
            // Bad password ...
            String message = I18n.err( I18n.ERR_230, bindDn.getName() );
            LOG.info( message );
            throw new LdapAuthenticationException( message );
        }
        finally
        {
            try
            {
                ldapConnection.close();
            }
            catch ( IOException ioe )
            {
                throw new LdapException( ioe.getMessage(), ioe );
            }
        }
    }


    /**
     * We don't handle any password policy when using a delegated authentication
     */
    @Override
    public void checkPwdPolicy( Entry userEntry ) throws LdapException
    {
        // no check for delegating authentication
    }


    /**
     * We don't handle any cache when using a delegated authentication
     */
    @Override
    public void invalidateCache( Dn bindDn )
    {
        // cache is not implemented here
    }
}
