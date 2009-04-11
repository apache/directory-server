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
package org.apache.directory.shared.ldap.util;


import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test the NameToolsTest class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NamespaceToolsTest
{
    @Test
    public void testNullRealm()
    {
        assertEquals( "", NamespaceTools.inferLdapName( null ) );
    }


    @Test
    public void testEmptyRealm()
    {
        assertEquals( "", NamespaceTools.inferLdapName( "" ) );
    }


    @Test
    public void testSingleElemRealm()
    {
        assertEquals( "dc=test", NamespaceTools.inferLdapName( "test" ) );
    }


    @Test
    public void testTwoElemsRealm()
    {
        assertEquals( "dc=test,dc=com", NamespaceTools.inferLdapName( "test.com" ) );
    }


    @Test
    public void testFullRealm()
    {
        assertEquals( "dc=CS,dc=UCL,dc=AC,dc=UK", NamespaceTools.inferLdapName( "CS.UCL.AC.UK" ) );
    }


    @Test
    public void testHasCompositeComponents() throws NamingException
    {
        assertTrue( NamespaceTools.hasCompositeComponents( "givenName=Alex+sn=Karasulu" ) );
        assertTrue( NamespaceTools.hasCompositeComponents( "givenName=Alex+sn=Karasulu+age=13" ) );
        assertFalse( NamespaceTools.hasCompositeComponents( "cn=One\\+Two" ) );
        assertFalse( NamespaceTools.hasCompositeComponents( "cn=Alex" ) );
    }


    @Test
    public void testGetCompositeComponents() throws NamingException
    {
        String[] args = NamespaceTools.getCompositeComponents( "givenName=Alex+sn=Karasulu" );
        assertEquals( "expecting two parts : ", 2, args.length );
        assertEquals( "givenName=Alex", args[0] );
        assertEquals( "sn=Karasulu", args[1] );

        args = NamespaceTools.getCompositeComponents( "givenName=Alex+sn=Karasulu+age=13" );
        assertEquals( "expecting two parts : ", 3, args.length );
        assertEquals( "givenName=Alex", args[0] );
        assertEquals( "sn=Karasulu", args[1] );
        assertEquals( "age=13", args[2] );

        args = NamespaceTools.getCompositeComponents( "cn=One\\+Two" );
        assertEquals( "expecting one part : ", 1, args.length );
        assertEquals( "cn=One\\+Two", args[0] );

        args = NamespaceTools.getCompositeComponents( "cn=Alex" );
        assertEquals( "expecting one part : ", 1, args.length );
        assertEquals( "cn=Alex", args[0] );
    }
    
    
    @Test
    public void testGetRelativeName() throws NamingException
    {
        // test the basis case first with the root
        LdapDN ancestor = new LdapDN( "" );
        LdapDN descendant = new LdapDN( "ou=system" );
        Name relativeName = NamespaceTools.getRelativeName( ancestor, descendant );
        assertEquals( relativeName.toString(), "ou=system" );
        
        ancestor = new LdapDN( "ou=system" );
        descendant = new LdapDN( "ou=users,ou=system" );
        relativeName = NamespaceTools.getRelativeName( ancestor, descendant );
        assertEquals( relativeName.toString(), "ou=users" );
    }
}
