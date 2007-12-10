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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;


/**
 * Contributed by Luke Taylor to fix DIRSERVER-169.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class AddIT
{
    public static DirectoryService service;
    

//    /**
//     * Test that attribute name case is preserved after adding an entry
//     * in the case the user added them.  This is to test DIRSERVER-832.
//     */
//    public void testAddCasePreservedOnAttributeNames() throws Exception
//    {
//        Attributes attrs = new AttributesImpl( true );
//        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
//        oc.add( "PERSON" );
//        oc.add( "organizationalPerson" );
//        oc.add( "inetORGperson" );
//        Attribute cn = new AttributeImpl( "Cn", "Kevin Spacey" );
//        Attribute dc = new AttributeImpl( "sN", "Spacey" );
//        attrs.put( oc );
//        attrs.put( cn );
//        attrs.put( dc);
//        sysRoot.createSubcontext( "uID=kevin", attrs );
//        Attributes returned = sysRoot.getAttributes( "UID=kevin" );
//        
//        NamingEnumeration attrList = returned.getAll();
//        while( attrList.hasMore() )
//        {
//            Attribute attr = ( Attribute ) attrList.next();
//            
//            if ( attr.getID().equalsIgnoreCase( "uid" ) )
//            {
//                assertEquals( "uID", attr.getID() );
//            }
//            
//            if ( attr.getID().equalsIgnoreCase( "objectClass" ) )
//            {
//                assertEquals( "ObjectClass", attr.getID() );
//            }
//            
//            if ( attr.getID().equalsIgnoreCase( "sn" ) )
//            {
//                assertEquals( "sN", attr.getID() );
//            }
//            
//            if ( attr.getID().equalsIgnoreCase( "cn" ) )
//            {
//                assertEquals( "Cn", attr.getID() );
//            }
//        }
//    }
    
    
    /**
     * Test that we can't add an entry with an attribute type not within
     * any of the MUST or MAY of any of its objectClasses
     * 
     * @throws Exception on error
     */
    @Test
    public void testAddAttributesNotInObjectClasses() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );

        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey" );
        Attribute dc = new AttributeImpl( "dc", "ke" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc);

        String base = "uid=kevin";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
            fail( "Should not reach this state" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertTrue( true );
        }
    }


    /**
     * Test that we can't add an entry with an attribute with a bad syntax
     *
     * @throws Exception on error
     */
    @Test
    public void testAddAttributesBadSyntax() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );

        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "person" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey" );
        Attribute sn = new AttributeImpl( "sn", "ke" );
        Attribute telephone = new AttributeImpl( "telephoneNumber", "0123456abc" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( sn );
        attrs.put( telephone );

        String base = "sn=kevin";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
            fail( "Should not reach this state" );
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            assertTrue( true );
        }
    }
}
