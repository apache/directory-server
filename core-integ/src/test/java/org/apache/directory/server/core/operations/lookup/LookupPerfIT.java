/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.server.core.operations.lookup;


import static org.apache.directory.server.core.authz.AutzIntegUtils.createAccessControlSubentry;
import static org.junit.Assert.assertNotNull;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.authz.AutzIntegUtils;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the lookup operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@ApplyLdifs(
    {
    // Entry # 1
        "dn: cn=test,ou=system", "objectClass: person", "cn: test", "sn: sn_test" })
public class LookupPerfIT extends AbstractLdapTestUnit
{
    /**
     * A lookup performance test
     */
    @Test
    public void testPerfLookupAllUserAttrs() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        Dn dn = new Dn( getService().getSchemaManager(), "cn=test,ou=system" );
        Entry entry = connection.lookup( dn, "*" );

        assertNotNull( entry );

        long nbIterations = 15000000L;
        long nbWarming =     5000000L;

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();

        for ( long i = 0L; i < nbIterations; i++ )
        {
            if ( i % 1000000L == 0L )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == nbWarming )
            {
                t00 = System.currentTimeMillis();
            }

            connection.lookup( dn, "*" );
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta all user attrs: " + deltaWarmed + "( " + ( ( ( ( nbIterations - nbWarming ) * 1000L ) / deltaWarmed ) )
            + " per s ) /" + ( t1 - t0 ) );
        connection.close();
    }


    @Before
    public void init()
    {
        AutzIntegUtils.service= getService();
    }


    /**
     * Test a lookup( Dn ) operation with the ACI subsystem enabled
     */
    @Test
    public void testPerfLookupACIEnabled() throws Exception
    {
        getService().setAccessControlEnabled( true );
        Dn dn = new Dn( "cn=test,ou=system" );
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        createAccessControlSubentry(
            "anybodySearch", "{ " +
            "  identificationTag \"searchAci\", " +
            "  precedence 14, " +
            "  authenticationLevel none, " +
            "  itemOrUserFirst userFirst: " +
            "  { " +
            "    userClasses { allUsers }, " +
            "    userPermissions " +
            "    { " +
            "      { " +
            "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
            "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " +
            "      } " +
            "    } " +
            "  } " +
            "}" );

        Entry entry = connection.lookup( dn, "*", "+" );

        assertNotNull( entry );

        int nbIterations = 1500000;

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();

        for ( int i = 0; i < nbIterations; i++ )
        {
            if ( i % 100000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 500000 )
            {
                t00 = System.currentTimeMillis();
            }

            connection.lookup( dn, "*", "+" );
        }

        assertNotNull( entry );

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta Authz : " + deltaWarmed + "( "
            + ( ( ( nbIterations - 500000 ) * 1000 ) / deltaWarmed ) + " per s ) /" + ( t1 - t0 ) );
        connection.close();
    }


    /**
     * A lookup performance test
     */
    @Test
    public void testPerfLookupRootDSE() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
    
        Dn dn = new Dn( getService().getSchemaManager(), "" );
        Entry entry = connection.lookup( dn, "*" );
    
        assertNotNull( entry );
    
        long nbIterations = 15000000L;
        long nbWarming =     5000000L;
    
        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();
    
        for ( long i = 0L; i < nbIterations; i++ )
        {
            if ( i % 1000000L == 0 )
            {
                long tt1 = System.currentTimeMillis();
    
                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }
    
            if ( i == nbWarming )
            {
                t00 = System.currentTimeMillis();
            }
    
            connection.lookup( dn, "*" );
        }
    
        long t1 = System.currentTimeMillis();
    
        long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta rootDSE all user attrs : " + deltaWarmed + "( " + ( ( ( ( nbIterations - nbWarming ) * 1000L ) / deltaWarmed ) )
            + " per s ) /" + ( t1 - t0 ) );
        connection.close();
    }


    /**
     * A lookup performance test
     */
    @Test
    public void testPerfLookupRootDSEAllAttrs() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
    
        Dn dn = new Dn( getService().getSchemaManager(), "" );
        Entry entry = connection.lookup( dn, "*", "+" );
    
        assertNotNull( entry );
    
        long nbIterations = 15000000L;
        long nbWarming =     5000000L;
    
        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();
    
        for ( long i = 0L; i < nbIterations; i++ )
        {
            if ( i % 1000000L == 0 )
            {
                long tt1 = System.currentTimeMillis();
    
                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }
    
            if ( i == nbWarming )
            {
                t00 = System.currentTimeMillis();
            }
    
            connection.lookup( dn, "*" );
        }
    
        long t1 = System.currentTimeMillis();
    
        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta rootDSE all attrs : " + deltaWarmed + "( " + ( ( ( ( nbIterations - nbWarming ) * 1000L ) / deltaWarmed ) )
            + " per s ) /" + ( t1 - t0 ) );
        connection.close();
    }


    /**
     * A lookup performance test
     */
    @Test
    public void testPerfLookupAllAtrs() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
    
        Dn dn = new Dn( getService().getSchemaManager(), "cn=test,ou=system" );
        Entry entry = connection.lookup( dn, "*", "+" );
    
        assertNotNull( entry );
    
        int nbIterations = 1500000;
    
        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();
    
        for ( int i = 0; i < nbIterations; i++ )
        {
            if ( i % 100000 == 0 )
            {
                long tt1 = System.currentTimeMillis();
    
                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }
    
            if ( i == 500000 )
            {
                t00 = System.currentTimeMillis();
            }
    
            connection.lookup( dn, "*" );
        }
    
        long t1 = System.currentTimeMillis();
    
        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta all attrs: " + deltaWarmed + "( " + ( ( ( nbIterations - 500000 ) * 1000 ) / deltaWarmed )
            + " per s ) /" + ( t1 - t0 ) );
        connection.close();
    }
}
