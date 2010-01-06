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

    /** configuration object for the connection */
    private LdapConnectionConfig config;

    private static final Logger LOG = LoggerFactory.getLogger( PoolableLdapConnectionFactory.class );


    /**
     * 
     * Creates a new instance of PoolableLdapConnectionFactory for the
     * server running on localhost at the port 10389
     *
     * @param config the configuration for creating LdapConnections
     */
    public PoolableLdapConnectionFactory( LdapConnectionConfig config )
    {
        this.config = config;
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

        LdapConnection connection = new LdapConnection( config );
        connection.bind( config.getName(), config.getCredentials() );
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

}
