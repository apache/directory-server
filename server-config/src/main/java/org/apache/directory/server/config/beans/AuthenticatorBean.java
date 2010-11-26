package org.apache.directory.server.config.beans;

public abstract class AuthenticatorBean extends AdsBaseBean
{
    /** The authenticator id */
    private String authenticatorId;
    /**
     * @return the authenticatorId
     */
    public String getAuthenticatorId()
    {
        return authenticatorId;
    }


    /**
     * @param authenticatorId the authenticatorId to set
     */
    public void setAuthenticatorId( String authenticatorId )
    {
        this.authenticatorId = authenticatorId;
    }

}
