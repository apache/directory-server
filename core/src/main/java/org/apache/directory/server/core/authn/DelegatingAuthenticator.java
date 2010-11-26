package org.apache.directory.server.core.authn;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionFactory;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.message.BindResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;

public class DelegatingAuthenticator extends AbstractAuthenticator
{
    /**
     * Creates a new instance.
     * @see AbstractAuthenticator
     */
    public DelegatingAuthenticator()
    {
        super( AuthenticationLevel.SIMPLE );
    }

    protected DelegatingAuthenticator(AuthenticationLevel type)
    {
        super( type );
    }

    /** A speedup for logger in debug mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();
    private String delegateHost;
    private int delegatePort;
    private List<String> dnPatterns = new ArrayList<String>();

    public String getDelegateHost()
    {
        return delegateHost;
    }

    public void setDelegateHost( String delegateHost )
    {
        this.delegateHost = delegateHost;
    }

    public int getDelegatePort()
    {
        return delegatePort;
    }

    public void setDelegatePort( int delegatePort )
    {
        this.delegatePort = delegatePort;
    }

    public List<String> getDnPatterns()
    {
        return dnPatterns;
    }

    public void setDnPatterns( List<String> dnPatterns )
    {
        this.dnPatterns = dnPatterns;
    }

    public LdapPrincipal authenticate( BindOperationContext bindContext )
            throws Exception
    {
        LdapPrincipal principal = null; 
        if ( IS_DEBUG )
        {
            LOG.debug( "Authenticating {}", bindContext.getDn() );
        }
        LdapConnection ldapConnection = LdapConnectionFactory.getNetworkConnection(delegateHost, delegatePort);
        try {
            BindResponse bindResponse = ldapConnection.bind(bindContext.getDn(), StringTools.utf8ToString( bindContext.getCredentials() ));
            if (bindResponse.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS) {
                String message = I18n.err( I18n.ERR_230, bindContext.getDn().getName() );
                LOG.info( message );
                throw new LdapAuthenticationException( message );
            }
            // Create the new principal before storing it in the cache
            principal = new LdapPrincipal( bindContext.getDn(), AuthenticationLevel.SIMPLE, bindContext.getCredentials() );
        } catch (LdapException e) {
            // Bad password ...
            String message = I18n.err( I18n.ERR_230, bindContext.getDn().getName() );
            LOG.info( message );
            throw new LdapAuthenticationException( message );
        }
        return principal;
    }

    public void checkPwdPolicy( Entry userEntry ) throws LdapException
    {
        // TODO Auto-generated method stub

    }


    public AuthenticationLevel getAuthenticatorType()
    {
        return AuthenticationLevel.SIMPLE;
    }


    public void invalidateCache( DN bindDn )
    {
        // TODO Auto-generated method stub

    }

}
