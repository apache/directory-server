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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SchemaViolationException;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A test case which demonstrates the three defects described in DIRSERVER-791.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true, name = "DIRSERVER791IT")
public class DIRSERVER791IT extends AbstractLdapTestUnit
{

    /**
     * Returns the attributes as depicted as test data in DIRSERVER-791
     * @return attributes for the test entry
     */
    protected Attributes getTestEntryAttributes()
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" );
        ocls.add( "organizationalPerson" );
        ocls.add( "inetOrgPerson" );
        attrs.put( ocls );

        Attribute cn = new BasicAttribute( "cn" );
        cn.add( "test" );
        cn.add( "aaa" );
        attrs.put( cn );

        attrs.put( "sn", "test" );

        return attrs;
    }


    /**
     * @todo  replace this with an ldif annotation
     *
     * @throws NamingException on error
     */
    protected void createData() throws Exception
    {
        Attributes entry = this.getTestEntryAttributes();
        getSystemContext( getService() ).createSubcontext( "cn=test", entry );
    }


    /**
     * Tests that it is possible to remove a value (in this case "cn=aaa") 
     * from the Rdn attribute which is not part of the Rdn
     * 
     * The defect was:
     * Removal of a value from Rdn attribute which is not part
     * of the Rdn is not possible.
     *
     * @throws NamingException on error
     */
    @Test
    public void testDefect1a() throws Exception
    {
        createData();
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( DirectoryService.JNDI_KEY, getService() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );
        DirContext ctx = new InitialDirContext( env );

        // remove "cn=aaa", which is not part of the Rdn
        Attribute attr = new BasicAttribute( "cn", "aaa" );
        ModificationItem modification = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, attr );
        ctx.modifyAttributes( "cn=test", new ModificationItem[]
            { modification } );

        Attributes attrs = ctx.getAttributes( "cn=test", new String[]
            { "cn" } );
        Attribute cn = attrs.get( "cn" );

        // cn=aaa must be removed, cn=test must exist
        assertEquals( "number of cn values", 1, cn.size() );
        assertTrue( cn.contains( "test" ) );
        assertFalse( cn.contains( "aaa" ) );
    }


    /**
     * Tests that it is possible to replace the Rdn attribute with
     * 
     * Checks whether it is possible to replace the cn attribute with a single
     * value. The JIRA issue states that this one works.
     *
     * @throws NamingException on error
     */
    @Test
    public void testDefect1b() throws Exception
    {
        createData();
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( DirectoryService.JNDI_KEY, getService() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );
        DirContext ctx = new InitialDirContext( env );

        // replace cn attribute with "cn=test", must remove the previous "cn=aaa"
        Attribute attr = new BasicAttribute( "cn", "test" );
        ModificationItem modification = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        ctx.modifyAttributes( "cn=test", new ModificationItem[]
            { modification } );

        Attributes attrs = ctx.getAttributes( "cn=test", new String[]
            { "cn" } );
        Attribute cn = attrs.get( "cn" );

        // cn=aaa must be removed, cn=test must exist
        assertEquals( "number of cn values", 1, cn.size() );
        assertTrue( cn.contains( "test" ) );
        assertFalse( cn.contains( "aaa" ) );
    }


    /**
     * Tests that the server rejects the addition of an non-existing objectClass.
     * Also checks that the non-existing isn't stored in the entry.
     * 
     * The defect was:
     * It is possible to add an value to objectclass, which isn't a valid
     * objectclass. The server returns an error, but nevertheless the invalid
     * value is stored. I think this should be rejected from server.
     *
     * @throws NamingException on error
     */
    @Test
    public void testDefect2() throws Exception
    {
        createData();
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( DirectoryService.JNDI_KEY, getService() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );
        DirContext ctx = new InitialDirContext( env );

        // try to add an non-existing objectClass "test", must be rejected
        Attribute attr = new BasicAttribute( "objectclass", "test" );
        ModificationItem modification = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        try
        {
            ctx.modifyAttributes( "cn=test", new ModificationItem[]
                { modification } );
            fail( "Exception expected" );
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

        // re-read the entry, the non-existing objectClass "test" must not be present
        Attributes attrs = ctx.getAttributes( "cn=test", new String[]
            { "objectClass" } );
        Attribute ocls = attrs.get( "objectClass" );
        assertEquals( "number of objectClasses", 4, ocls.size() );
        assertTrue( ocls.contains( "top" ) );
        assertTrue( ocls.contains( "person" ) );
        assertTrue( ocls.contains( "organizationalPerson" ) );
        assertTrue( ocls.contains( "inetOrgPerson" ) );
        assertFalse( ocls.contains( "test" ) );
    }


    /**
     * Tests that no unallowed attribute could be added to the entry.
     * 
     * The defect was:
     * It is possible to add an attribute to the entry that is not allowed
     * according the objectclasses. The server should reject this.
     *
     * @throws NamingException on error
     */
    @Test
    public void testDefect3() throws Exception
    {
        createData();
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( DirectoryService.JNDI_KEY, getService() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );
        DirContext ctx = new InitialDirContext( env );

        // try to add an unallowed attribute, must be rejected
        Attribute attr = new BasicAttribute( "javaDoc", "test" );
        ModificationItem modification = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        try
        {
            ctx.modifyAttributes( "cn=test", new ModificationItem[]
                { modification } );
            fail( "Exception expected" );
        }
        catch ( SchemaViolationException sve )
        {
            // Valid behavior
        }
        catch ( InvalidAttributeIdentifierException iaie )
        {
            // Valid behavior
        }
    }
}
