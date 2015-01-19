package org.apache.directory.shared.client.api;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;

public class TrackingLdapConnectionFactory extends DefaultLdapConnectionFactory {
    private int bindCalled = 0;
    
    public TrackingLdapConnectionFactory( LdapConnectionConfig config ) {
        super( config );
    }
    
    @Override
    public LdapConnection bindConnection( LdapConnection connection ) throws LdapException
    {
        bindCalled++;
        return super.bindConnection( connection );
    }

    @Override
    public LdapConnection configureConnection( LdapConnection connection )
    {
        return super.configureConnection( connection );
    }
    
    public int getBindCalled() {
        return bindCalled;
    }
}