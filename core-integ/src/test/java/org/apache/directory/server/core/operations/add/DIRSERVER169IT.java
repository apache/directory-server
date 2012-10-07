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
package org.apache.directory.server.core.operations.add;


import static org.apache.directory.server.core.integ.IntegrationUtils.getAdminConnection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.model.exception.LdapSchemaViolationException;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Contributed by Luke Taylor to fix DIRSERVER-169.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "AddIT")
public class DIRSERVER169IT extends AbstractLdapTestUnit
{
    /**
     * Test that attribute name case is preserved after adding an entry
     * in the case the user added them.  This is to test DIRSERVER-832.
     */
    @Test
    public void testAddCasePreservedOnAttributeNames() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( getService() );

        Entry entry = new DefaultEntry( "uID=kevin,ou=system",
            "ObjectClass: top",
            "ObjectClass: PERSON",
            "ObjectClass: organizationalPerson",
            "ObjectClass: inetORGperson",
            "Cn: Kevin Spacey",
            "sN: Spacey",
            "uID: kevin" );
            
        sysRoot.add( entry );
        
        Entry returned = sysRoot.lookup( "uID=kevin,ou=system" );
        
        assertTrue( returned.containsAttribute( "uID", "ObjectClass", "sN", "Cn" ) );
    }

    /**
     * Test that we can't add an entry with an attribute type not within
     * any of the MUST or MAY of any of its objectClasses
     * 
     * @throws Exception on error
     */
    @Test( expected=LdapSchemaViolationException.class )
    public void testAddAttributesNotInObjectClasses() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( getService() );

        String base = "uid=kevin, ou=system";

        Entry entry = new DefaultEntry( base,
            "ObjectClass: top",
            "cn: kevin Spacey",
            "dc: ke" );

        //create subcontext
        sysRoot.add( entry );
        fail( "Should not reach this state" );
    }


    /**
     * Test that we can't add an entry with an attribute with a bad syntax
     *
     * @throws Exception on error
     */
    @Test( expected=LdapInvalidAttributeValueException.class )
    public void testAddAttributesBadSyntax() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( getService() );

        String base = "sn=kevin, ou=system";

        Entry entry = new DefaultEntry( base,
            "ObjectClass: top",
            "ObjectClass: person",
            "cn: kevin Spacey",
            "sn: ke",
            "telephoneNumber: 0123456abc" );

        // create subcontext
        sysRoot.add( entry );
    }


    /**
     * test case for DIRSERVER-1442
     */
    @Test
    public void testAddAttributeWithEscapedPlusCharacter() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( getService() );

        String base = "cn=John\\+Doe, ou=system";

        Entry entry = new DefaultEntry( base,
            "ObjectClass: top",
            "ObjectClass: inetorgperson",
            "cn: John+Doe",
            "sn: +Name+" );

        sysRoot.add( entry );

        try
        {
            Entry obj = sysRoot.lookup( "cn=John\\+Doe,ou=system" );
            assertNotNull( obj );
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        Entry result = sysRoot.lookup( "cn=John\\+Doe,ou=system" );
        assertNotNull( result );

        assertTrue( result.containsAttribute( "cn" ) );
        Attribute cn = result.get( "cn" );
        assertEquals( 1, cn.size() );
    }
}
