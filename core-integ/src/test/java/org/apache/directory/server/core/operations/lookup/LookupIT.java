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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.name.DN;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the lookup operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( FrameworkRunner.class )
@ApplyLdifs( {
    // Entry # 1
    "dn: cn=test,ou=system",
    "objectClass: person",
    "cn: test",
    "sn: sn_test" 
})
public class LookupIT extends AbstractLdapTestUnit
{

    /**
     * Test a lookup( DN, "*") operation
     */
    @Test
    public void testLookupStar() throws Exception
    {
        DN dn = new DN( "cn=test,ou=system" );
        Entry entry = service.getAdminSession().lookup( dn, new String[]{ "*" } );
        
        assertNotNull( entry );
        
        // Check that we don't have any operational attributes :
        // We should have only 3 attributes : objectClass, cn and sn
        assertEquals( 3, entry.size() ); 

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }


    /**
     * Test a lookup( DN, "+") operation
     */
    @Test
    public void testLookupPlus() throws Exception
    {
        service.setDenormalizeOpAttrsEnabled( true );
        DN dn = new DN( "cn=test,ou=system" );
        Entry entry = service.getAdminSession().lookup( dn, new String[]{ "+" } );
        
        assertNotNull( entry );
        
        // We should have 5 attributes
        assertEquals( 7, entry.size() ); 

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
        
        // Check that we have all the operational attributes :
        // We should have 3 users attributes : objectClass, cn and sn
        // and 2 operational attributes : createTime and createUser 
        assertNotNull( entry.get( "createTimestamp" ).getString() );
        assertEquals( "uid=admin,ou=system", entry.get( "creatorsName" ).getString() );
    }


    /**
     * Test a lookup( DN, []) operation
     */
    @Test
    public void testLookupEmptyAtrid() throws Exception
    {
        DN dn = new DN( "cn=test,ou=system" );
        Entry entry = service.getAdminSession().lookup( dn, new String[]{} );
        
        assertNotNull( entry );
        
        // We should have 3 attributes
        assertEquals( 3, entry.size() ); 

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }


    /**
     * Test a lookup( DN ) operation
     */
    @Test
    public void testLookup() throws Exception
    {
        DN dn = new DN( "cn=test,ou=system" );
        Entry entry = service.getAdminSession().lookup( dn );
        
        assertNotNull( entry );
        
        // We should have 3 attributes
        assertEquals( 3, entry.size() ); 

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }
}
