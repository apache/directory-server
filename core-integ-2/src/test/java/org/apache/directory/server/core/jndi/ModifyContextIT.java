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
package org.apache.directory.server.core.jndi;


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getUserAddLdif;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.integ.CiRunner;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the methods on JNDI contexts that are analogous to entry modify
 * operations in LDAP.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
@CleanupLevel( Level.CLASS )
public class ModifyContextIT
{
    public static DirectoryService service;


    /**
     * @todo put this into an ldif annotation
     *
     * @throws NamingException on error
     */
    protected void createData() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();
        service.getAdminSession().add( 
            new DefaultServerEntry( service.getSchemaManager(), akarasulu.getEntry() ) ); 

        LdapContext sysRoot = getSystemContext( service );

        /*
         * create ou=testing00,ou=system
         */
        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing00" );
        DirContext ctx = sysRoot.createSubcontext( "ou=testing00", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing00", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * create ou=testing01,ou=system
         */
        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing01" );
        ctx = sysRoot.createSubcontext( "ou=testing01", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing01", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * create ou=testing02,ou=system
         */
        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing02" );
        ctx = sysRoot.createSubcontext( "ou=testing02", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing02" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing02", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * create ou=subtest,ou=testing01,ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );

        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "subtest" );
        ctx = ctx.createSubcontext( "ou=subtest", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=subtest,ou=testing01" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "subtest", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );
    }


    /**
     * Add a new attribute without any value to a person entry: testcase for
     * http://issues.apache.org/jira/browse/DIRSERVER-630.
     * 
     * @throws NamingException on error
     */
    @Test
    public void testIllegalModifyAdd() throws Exception
    {
        createData();

        LdapContext sysRoot = getSystemContext( service );

        Attribute attr = new BasicAttribute( "description" );
        Attributes attrs = new BasicAttributes( true );
        attrs.put( attr );

        try
        {
            sysRoot.modifyAttributes( "uid=akarasulu,ou=users", DirContext.ADD_ATTRIBUTE, attrs );
            fail( "error expected due to empty attribute value" );
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            // expected
        }

        // Check whether entry is unmodified, i.e. no description
        Attributes entry = sysRoot.getAttributes( "uid=akarasulu,ou=users" );
        assertNull( entry.get( "description" ) );
    }



    @Test
    public void testModifyOperation() throws Exception
    {
        createData();

        LdapContext sysRoot = getSystemContext( service );
        Attributes attributes = new BasicAttributes( true );
        attributes.put( "ou", "testCases" );
        sysRoot.modifyAttributes( "ou=testing00", DirContext.ADD_ATTRIBUTE, attributes );

        DirContext ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
        attributes = ctx.getAttributes( "" );
        assertTrue( attributes.get( "ou" ).contains( "testCases" ) );

        Attribute attribute = attributes.get( "creatorsName" );
        assertNull( attribute );

        attribute = attributes.get( "createTimestamp" );
        assertNull( attribute );

        attribute = attributes.get( "modifiersName" );
        assertNull( attribute );

        attributes.get( "modifyTimestamp" );
        assertNull( attribute );
    }


    /**
     * Test that if we try to remove a non existing attribute,
     * we get a correct LdapNoSuchAttributeException
     * 
     * The test is currently disabled
     */
    //@Test
    public void testRemoveNonExistingValueException() throws Exception
    {
        createData();

        LdapContext sysRoot = getSystemContext( service );
        Attributes attributes = new BasicAttributes( true );
        attributes.put( "ou", "testCases" );
        
        try
        {
            sysRoot.modifyAttributes( "ou=testing00", DirContext.REMOVE_ATTRIBUTE, attributes );
            fail();
        }
        catch ( LdapNoSuchAttributeException lnsae )
        {
            // Expected
            assertTrue( true );
        }
    }

    
    @Test( expected = LdapNoSuchAttributeException.class )
    public void testRemoveNonExistingValue() throws Exception
    {
        createData();

        LdapContext sysRoot = getSystemContext( service );
        Attributes attributes = new BasicAttributes( true );
        attributes.put( "ou", "testCases" );
        sysRoot.modifyAttributes( "ou=testing00", DirContext.REMOVE_ATTRIBUTE, attributes );
    }
}
