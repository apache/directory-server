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
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.SchemaViolationException;

import java.util.Hashtable;


/**
 * A test case which demonstrates the three defects described in DIRSERVER-791.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( CiRunner.class )
public class DIRSERVER791IT
{
    public static DirectoryService service;


    /**
     * Returns the attributes as depicted as test data in DIRSERVER-791
     * @return attributes for the test entry
     */
    protected Attributes getTestEntryAttributes()
    {
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl("objectClass");
        ocls.add("top");
        ocls.add("person");
        ocls.add("organizationalPerson");
        ocls.add("inetOrgPerson");
        attrs.put(ocls);
        
        Attribute cn = new AttributeImpl("cn");
        cn.add("test");
        cn.add("aaa");
        attrs.put(cn);
        
        attrs.put("sn", "test");

        return attrs;
    }


    /**
     * @todo  replace this with an ldif annotation
     *
     * @throws NamingException on error
     */
    protected void createData() throws NamingException
    {
        Attributes entry = this.getTestEntryAttributes();
        getSystemContext( service ).createSubcontext("cn=test", entry);
    }

    
    /**
     * Demonstrates that removal of a value from RDN attribute which is not part
     * of the RDN is not possible.
     *
     * @throws NamingException on error
     */
    @Test
    public void testDefect1a() throws NamingException 
    {
        createData();
        Hashtable<String,Object> env = new Hashtable<String,Object>();
        env.put( DirectoryService.JNDI_KEY, service );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );

        DirContext ctx = new InitialDirContext( env );
        Attribute attr = new AttributeImpl("cn", "aaa");
        ModificationItemImpl modification = new ModificationItemImpl( DirContext.REMOVE_ATTRIBUTE, attr );
        ctx.modifyAttributes( "cn=test", new ModificationItemImpl[] { modification } );

        Attributes attrs = ctx.getAttributes("cn=test", new String[] { "cn" });
        Attribute cn = attrs.get("cn");

        assertEquals("number of cn values", 1, cn.size());
        assertTrue( cn.contains("test") );
        assertFalse( cn.contains("aaa") );
    }


    /**
     * Checks whether it is possible to replace the cn attribute with a single
     * value. The JIRA issue states that this one works.
     *
     * @throws NamingException on error
     */
    @Test
    public void testDefect1b() throws NamingException
    {
        createData();
        Hashtable<String,Object> env = new Hashtable<String,Object>();
        env.put( DirectoryService.JNDI_KEY, service );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );

        DirContext ctx = new InitialDirContext( env );

        Attribute attr = new AttributeImpl("cn", "test");
        ModificationItemImpl modification = new ModificationItemImpl(DirContext.REPLACE_ATTRIBUTE, attr);
        ctx.modifyAttributes("cn=test", new ModificationItemImpl[] { modification });

        Attributes attrs = ctx.getAttributes("cn=test", new String[] { "cn" });
        Attribute cn = attrs.get("cn");

        assertEquals("number of cn values", 1, cn.size());
        assertTrue(cn.contains("test"));
        assertFalse(cn.contains("aaa"));
    }


    /**
     * It is possible to add an value to objectclass, which isn't a valid
     * objectclass. The server returns an error, but nevertheless the invalid
     * value is stored. I think this should be rejected from server.
     *
     * @throws NamingException on error
     */
    @Test
    public void testDefect2() throws NamingException
    {
        createData();
        Hashtable<String,Object> env = new Hashtable<String,Object>();
        env.put( DirectoryService.JNDI_KEY, service );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );

        DirContext ctx = new InitialDirContext( env );


        Attribute attr = new AttributeImpl( "objectclass", "test" );
        ModificationItemImpl modification = new ModificationItemImpl(DirContext.ADD_ATTRIBUTE, attr);
        
        try 
        {
            ctx.modifyAttributes("cn=test", new ModificationItemImpl[] { modification });
            fail("Exception expected");
        } 
        catch ( SchemaViolationException sve ) 
        {
            // Valid behavior
        } 
        catch ( InvalidAttributeValueException iave ) 
        {
            // Valid behavior
        }
        catch ( NamingException ne )
        {
            // Valid behavior
        }

        Attributes attrs = ctx.getAttributes("cn=test", new String[] { "objectClass" });
        Attribute ocls = attrs.get("objectClass");

        assertEquals("number of objectClasses", 4, ocls.size());
        assertTrue(ocls.contains("top"));
        assertTrue(ocls.contains("person"));
        assertTrue(ocls.contains("organizationalPerson"));
        assertTrue(ocls.contains("inetOrgPerson"));
        assertFalse(ocls.contains("test"));
    }


    /**
     * It is possible to add an attribute to the entry that is not allowed
     * according the objectclasses. The server should reject this.
     *
     * @throws NamingException on error
     */
    @Test
    public void testDefect3() throws NamingException
    {
        createData();
        Hashtable<String,Object> env = new Hashtable<String,Object>();
        env.put( DirectoryService.JNDI_KEY, service );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );

        DirContext ctx = new InitialDirContext( env );


        Attribute attr = new AttributeImpl("bootParameter", "test");
        ModificationItemImpl modification = new ModificationItemImpl(DirContext.ADD_ATTRIBUTE, attr);
    
        try 
        {
            ctx.modifyAttributes("cn=test", new ModificationItemImpl[] { modification });
            fail("Exception expected");
        } 
        catch (SchemaViolationException sve) 
        {
            // Valid behavior
        } 
        catch (InvalidAttributeIdentifierException iaie) 
        {
            // Valid behavior
        }
    }
}
