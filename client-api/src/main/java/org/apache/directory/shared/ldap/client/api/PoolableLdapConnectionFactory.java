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

package org.apache.directory.shared.ldap.client.api;


import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A factory for creating LdapConnection objects managed by LdapConnectionPool.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PoolableLdapConnectionFactory implements PoolableObjectFactory
{

    /** The selected LDAP port, default value is 10389 */
    private int ldapPort = 10389;

    /** the remote LDAP host, default value is localhost */
    private String ldapHost = "127.0.0.1";

    /** A flag indicating if we are using SSL or not, default is false */
    private boolean useSsl = false;

    /** the user name used for binding, default value is uid=admin,ou=system*/
    private String userName;

    /** credentials of the user, default value is secret*/
    private byte[] credentials = StringTools.getBytesUtf8( "secret" );

    private static final Logger LOG = LoggerFactory.getLogger( PoolableLdapConnectionFactory.class );


    public PoolableLdapConnectionFactory()
    {
    }


    /**
     * 
     * Creates a new instance of PoolableLdapConnectionFactory for the
     * server running on localhost at the port 10389
     *
     * @param userName the DN of the user
     * @param credentials user's credential
     */
    public PoolableLdapConnectionFactory( String userName, byte[] credentials )
    {
        this.userName = userName;
        this.credentials = credentials;
    }


    /**
     * 
     * Creates a new instance of PoolableLdapConnectionFactory.
     *
     * @param host hostname where the LDAP server is running 
     * @param port port of the LDAP server
     * @param userName the DN of the user
     * @param credentials user's credential
     */
    public PoolableLdapConnectionFactory( String host, int port, String userName, byte[] credentials )
    {
        this.ldapHost = host;
        this.ldapPort = port;
        this.userName = userName;
        this.credentials = credentials;
    }


    /**
     * {@inheritDoc}
     */
    public void activateObject( Object obj ) throws Exception
    {
        LOG.debug( "activating {}", obj );
    }


    /**
     * {@inheritDoc}
     */
    public void destroyObject( Object obj ) throws Exception
    {
        LOG.debug( "destroying {}", obj );
        LdapConnection connection = ( LdapConnection ) obj;
        connection.unBind();
        connection.close();
    }


    /**
     * {@inheritDoc}
     */
    public Object makeObject() throws Exception
    {
        LOG.debug( "creating a LDAP connection" );

        LdapConnection connection = new LdapConnection( ldapHost, ldapPort, useSsl );
        connection.bind( userName, credentials );
        return connection;
    }


    /**
     * {@inheritDoc}
     */
    public void passivateObject( Object obj ) throws Exception
    {
        LOG.debug( "passivating {}", obj );
    }


    /**
     * {@inheritDoc}
     */
    public boolean validateObject( Object obj )
    {
        LOG.debug( "validating {}", obj );

        LdapConnection connection = ( LdapConnection ) obj;
        return connection.isSessionValid();
    }


    public void setLdapPort( int ldapPort )
    {
        this.ldapPort = ldapPort;
    }


    public void setLdapHost( String ldapHost )
    {
        this.ldapHost = ldapHost;
    }


    public void setUseSsl( boolean useSsl )
    {
        this.useSsl = useSsl;
    }


    public void setUserName( String userName )
    {
        this.userName = userName;
    }


    public void setCredentials( byte[] credentials )
    {
        this.credentials = credentials;
    }
}
