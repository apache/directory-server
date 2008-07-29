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
package org.apache.directory.server.integ;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.newldap.LdapServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServerIntegrationUtils extends IntegrationUtils
{
    /** The class logger */
    private static final Logger LOG = LoggerFactory.getLogger( ServerIntegrationUtils.class );
    private static final String CTX_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";


    /**
     * Creates a JNDI LdapContext with a connection over the wire using the 
     * SUN LDAP provider.  The connection is made using the administrative 
     * user as the principalDN.  The context is to the rootDSE.
     *
     * @param ldapServer the LDAP server to get the connection to
     * @return an LdapContext as the administrative user to the RootDSE
     * @throws Exception if there are problems creating the context
     */
    public static LdapContext getWiredContext( LdapServer ldapServer ) throws Exception
    {
        return getWiredContext( ldapServer, null );
    }


    /**
     * Creates a JNDI LdapContext with a connection over the wire using the 
     * SUN LDAP provider.  The connection is made using the administrative 
     * user as the principalDN.  The context is to the rootDSE.
     *
     * @param ldapServer the LDAP server to get the connection to
     * @return an LdapContext as the administrative user to the RootDSE
     * @throws Exception if there are problems creating the context
     */
    public static LdapContext getWiredContext( LdapServer ldapServer, Control[] controls ) throws Exception
    {
        LOG.debug( "Creating a wired context to local LDAP server on port {}", ldapServer.getIpPort() );
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CTX_FACTORY );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getIpPort() );
        env.put( Context.SECURITY_PRINCIPAL, ServerDNConstants.ADMIN_SYSTEM_DN );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        return new InitialLdapContext( env, controls );
    }
}
