package org.apache.directory.server.config.beans;

import java.util.ArrayList;
import java.util.List;

public class AuthenticationInterceptorBean extends InterceptorBean
{
    /** The list of authenticators */
    private List<AuthenticatorBean> authenticators = new ArrayList<AuthenticatorBean>();

    public AuthenticationInterceptorBean() {
        super();
    }
    /**
     * @param authenticators the authenticators to set
     */
    public void setAuthenticators( List<AuthenticatorBean> authenticators )
    {
        this.authenticators = authenticators;
    }

    
    /**
     * @param authenticators the authenticators to add
     */
    public void addAuthenticators( AuthenticatorBean... authenticators )
    {
        for ( AuthenticatorBean authenticator : authenticators )
        {   
            this.authenticators.add( authenticator );
        }
    }

    /**
     * @return the extendedOps
     */
    public List<AuthenticatorBean> getAuthenticators()
    {
        return authenticators;
    }

    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "AuthenticationInterceptor :\n" );
        sb.append( super.toString( tabs + "  " ) );
        if ((authenticators != null) && (authenticators.size() > 0))
        {
            sb.append( tabs ).append( "  authenticator :\n" );

            for (AuthenticatorBean authenticator : authenticators)
            {
                sb.append( authenticator.toString( tabs + "    " ) );
            }
        }
        return sb.toString();
    }

}
