/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.client.api;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.PoolableLdapConnectionFactory;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.codec.api.DefaultBinaryAttributeDetector;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Class LdapApiIntegrationUtils.
 */
public class LdapApiIntegrationUtils
{

    /** The class logger. */
    private static final Logger LOG = LoggerFactory.getLogger( LdapApiIntegrationUtils.class );

    /** The Constant DEFAULT_HOST. */
    private static final String DEFAULT_HOST = "localhost";

    /** The Constant DEFAULT_ADMIN. */
    private static final String DEFAULT_ADMIN = ServerDNConstants.ADMIN_SYSTEM_DN;

    /** The Constant DEFAULT_PASSWORD. */
    private static final String DEFAULT_PASSWORD = "secret";

    /** The pools. */
    private static Map<Integer, LdapConnectionPool> pools = new HashMap<Integer, LdapConnectionPool>();


    /**
     * Creates a new {@link LdapNetworkConnection} and authenticates as admin user.
     * The caller is responsible for closing the connection, use closeConnection().
     *
     * @param ldapServer the LDAP server instance, used to obtain the port used
     * @return the created connection
     * @throws LdapException the LDAP exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static LdapNetworkConnection createAdminConnection( LdapServer ldapServer ) throws LdapException,
        IOException
    {
        LdapNetworkConnection conn = new LdapNetworkConnection( DEFAULT_HOST, ldapServer.getPort() );
        conn.bind( DEFAULT_ADMIN, DEFAULT_PASSWORD );
        return conn;
    }


    /**
     * Closes the {@link LdapNetworkConnection}.
     *
     * @param conn the connection to close
     * @throws LdapException the LDAP exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void closeConnection( LdapNetworkConnection conn ) throws LdapException, IOException
    {
        if ( conn != null )
        {
            conn.unBind();
            conn.close();
        }
    }


    /**
     * Gets the pooled {@link LdapConnectionPool}, authenticated as admin user.
     * The caller is reponsible for releasing the connection, use releasePooledConnection().
     *
     * @param ldapServer the LDAP server instance, used to obtain the port used
     * @return the pooled admin connection
     * @throws Exception the exception
     */
    public static LdapNetworkConnection getPooledAdminConnection( LdapServer ldapServer ) throws Exception
    {
        return getAdminPool( ldapServer ).getConnection();
    }


    /**
     * Releases a pooled connection back to the pool.
     *
     * @param conn the connection to release
     * @param ldapServer the LDAP server instance, used to obtain the port used
     * @throws Exception the exception
     */
    public static void releasePooledAdminConnection( LdapNetworkConnection conn, LdapServer ldapServer )
        throws Exception
    {
        getAdminPool( ldapServer ).releaseConnection( conn );
    }


    /**
     * Gets the admin pool.
     *
     * @param ldapServer the ldap server
     * @return the admin pool
     */
    private static LdapConnectionPool getAdminPool( LdapServer ldapServer )
    {
        int port = ldapServer.getPort();
        
        if ( !pools.containsKey( port ) )
        {
            LdapConnectionConfig config = new LdapConnectionConfig();
            config.setLdapHost( DEFAULT_HOST );
            config.setLdapPort( port );
            config.setName( DEFAULT_ADMIN );
            config.setCredentials( DEFAULT_PASSWORD );
            config.setBinaryAttributeDetector( new DefaultBinaryAttributeDetector(
                ldapServer.getDirectoryService().getSchemaManager() ) );
            PoolableLdapConnectionFactory factory = new PoolableLdapConnectionFactory( config );
            LdapConnectionPool pool = new LdapConnectionPool( factory );
            pool.setTestOnBorrow( true );
            pools.put( port, pool );
        }

        return pools.get( port );
    }

}
