/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.ldap.util;


import junit.framework.Assert;
import junit.framework.TestCase;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.NamespaceTools;


/**
 * Test the NameToolsTest class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NamespaceToolsTest extends TestCase
{
    public void testNullRealm()
    {
        Assert.assertEquals( "", NamespaceTools.inferLdapName( null ) );
    }


    public void testEmptyRealm()
    {
        Assert.assertEquals( "", NamespaceTools.inferLdapName( "" ) );
    }


    public void testSingleElemRealm()
    {
        Assert.assertEquals( "dc=test", NamespaceTools.inferLdapName( "test" ) );
    }


    public void testTwoElemsRealm()
    {
        Assert.assertEquals( "dc=test,dc=com", NamespaceTools.inferLdapName( "test.com" ) );
    }


    public void testFullRealm()
    {
        Assert.assertEquals( "dc=CS,dc=UCL,dc=AC,dc=UK", NamespaceTools.inferLdapName( "CS.UCL.AC.UK" ) );
    }


    public void testHasCompositeComponents() throws NamingException
    {
        assertTrue( NamespaceTools.hasCompositeComponents( "givenName=Alex+sn=Karasulu" ) );
        assertTrue( NamespaceTools.hasCompositeComponents( "givenName=Alex+sn=Karasulu+age=13" ) );
        assertFalse( NamespaceTools.hasCompositeComponents( "cn=One\\+Two" ) );
        assertFalse( NamespaceTools.hasCompositeComponents( "cn=Alex" ) );
    }


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
