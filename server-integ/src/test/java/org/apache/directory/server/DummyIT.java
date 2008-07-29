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
package org.apache.directory.server;


import static org.junit.Assert.*;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.ServerIntegrationUtils;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.newldap.LdapServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A bogus test to make sure the framework is working.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
public class DummyIT
{
    private static final Logger LOG = LoggerFactory.getLogger( DummyIT.class );

    // will be set by the framework
    public static LdapServer ldapServer;
    

    static
    {
        LOG.debug( "0 - {}", System.currentTimeMillis() );
    }
    
    
    @Test
    public void testInitialization() throws Exception
    {
        LOG.debug( "testInitialization()" );
        assertNotNull( ldapServer );
        
        LdapContext ldapContext = null;
        
        try
        {
            ldapContext = ServerIntegrationUtils.getWiredContext( ldapServer );
            assertNotNull( ldapContext );
            
            Attributes attrs = ldapContext.getAttributes( "uid=admin,ou=system" );
            assertNotNull( attrs );
            assertEquals( "administrator", attrs.get( "sn" ).get() );
            assertEquals( "admin", attrs.get( "uid" ).get() );
            assertEquals( "Directory Superuser", attrs.get( "displayName" ).get() );
        }
        finally
        {
            ldapContext.close();
        }

        LOG.debug( "1 - {}", System.currentTimeMillis() );
    }


    @Test
    public void test2() throws Exception
    {
        LOG.debug( "2 - {}", System.currentTimeMillis() );
    }


    @Test
    public void test3() throws Exception
    {
        LOG.debug( "3 - {}", System.currentTimeMillis() );
    }


    @Test
    public void test4() throws Exception
    {
        LOG.debug( "4 - {}", System.currentTimeMillis() );
    }


    @Test
    public void test5() throws Exception
    {
        LOG.debug( "5 - {}", System.currentTimeMillis() );
    }


    @Test
    public void test6() throws Exception
    {
        LOG.debug( "6 - {}", System.currentTimeMillis() );
    }


    @Test
    public void test7() throws Exception
    {
        LOG.debug( "7 - {}", System.currentTimeMillis() );
    }


    @Test
    public void test8() throws Exception
    {
        LOG.debug( "8 - {}", System.currentTimeMillis() );
    }


    @Test
    public void test9() throws Exception
    {
        LOG.debug( "9 - {}", System.currentTimeMillis() );
    }
}
