/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.ldap.server.jndi;


import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;

import org.apache.ldap.server.AbstractAdminTestCase;
import org.apache.ldap.server.configuration.MutableDirectoryPartitionConfiguration;
import org.apache.ldap.common.exception.LdapNameNotFoundException;

import java.util.HashSet;
import java.util.Set;


/**
 * Tests various operations against a partition whose suffix contains both upper and lower case letters.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MixedCaseTest extends AbstractAdminTestCase
{
    String suffix = "dc=Apache,dc=Org";

    public void setUp() throws Exception
    {

        MutableDirectoryPartitionConfiguration partition = new MutableDirectoryPartitionConfiguration();
        partition.setName( "apache" );
        partition.setSuffix( suffix );

        Set indexedAttributes = new HashSet();
        indexedAttributes.add( "objectClass" );
        indexedAttributes.add( "ou" );
        indexedAttributes.add( "uid" );
        partition.setIndexedAttributes( indexedAttributes );

        Attributes attrs = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "domain" );
        objectClass.add( "extensibleObject" );
        attrs.put( objectClass );
        attrs.put( "dc", "Apache" );

        partition.setContextEntry( attrs );

        Set partitions = new HashSet();
        partitions.add( partition );

        configuration.setContextPartitionConfigurations( partitions );
        super.overrideEnvironment( Context.PROVIDER_URL, suffix );

        super.setUp();
    }

    public void testSearch() throws NamingException
    {
        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.SUBTREE_SCOPE );

        NamingEnumeration ne = sysRoot.search( "", "(objectClass=*)", sc );

        assertTrue( "Search should return at least one entry.", ne.hasMore() );

        SearchResult sr = (SearchResult) ne.next();

        assertEquals( "The entry returned should be the root entry.", suffix, sr.getName() );

        assertFalse( "Search should return no more entries.", ne.hasMore() );
    }

    public void testAdd() throws NamingException
    {
        String dn = "ou=Test";

        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "Test" );

        DirContext ctx = sysRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration ne = sysRoot.search( dn, "(objectClass=*)", sc );

        assertTrue( "Search should return at least one entry.", ne.hasMore() );

        SearchResult sr = (SearchResult) ne.next();

        assertEquals( "The entry returned should be the entry added earlier.", dn+","+suffix, sr.getName() );

        assertFalse( "Search should return no more entries.", ne.hasMore() );
    }

    public void testModify() throws NamingException
    {
        String dn = "ou=Test";
        String description = "New Value";

        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "Test" );
        attributes.put( "description", "Old Value" );

        DirContext ctx = sysRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        ModificationItem[] mods = new ModificationItem[1];
        mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute( "description", description ));

        sysRoot.modifyAttributes( dn, mods );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration ne = sysRoot.search( dn, "(objectClass=*)", sc );

        assertTrue( "Search should return at least one entry.", ne.hasMore() );

        SearchResult sr = (SearchResult) ne.next();

        assertEquals( "The entry returned should be the entry added earlier.", dn+","+suffix, sr.getName() );

        attributes = sr.getAttributes();
        attribute = attributes.get( "description" );

        assertEquals( "The description attribute should contain the new value.", description, attribute.get() );

        assertFalse( "Search should return no more entries.", ne.hasMore() );
    }

    public void testDelete() throws NamingException
    {
        String dn = "ou=Test";

        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "Test" );

        DirContext ctx = sysRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        sysRoot.destroySubcontext( dn );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        try {
            sysRoot.search( dn, "(objectClass=*)", sc );

            fail( "Search should throw exception.");

        } catch (LdapNameNotFoundException e) {
            // ignore
        }
    }

}

