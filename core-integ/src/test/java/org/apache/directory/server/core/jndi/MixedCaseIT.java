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


import static org.apache.directory.server.core.integ.IntegrationUtils.getContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.api.ldap.model.ldif.LdifUtils;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests various operations against a partition whose suffix contains both upper and lower case letters.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "MixedCaseITest-class",
    partitions =
        {
            @CreatePartition(
                name = "apache",
                suffix = "dc=Apache,dc=Org",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=Apache,dc=Org\n" +
                        "dc: Apache\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "ou"),
                        @CreateIndex(attribute = "uid")
                })
    })
public class MixedCaseIT extends AbstractLdapTestUnit
{

    private static final String SUFFIX_DN = "dc=Apache,dc=Org";


    @Test
    public void testSearch() throws Exception
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", getService(), SUFFIX_DN );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.SUBTREE_SCOPE );

        NamingEnumeration<SearchResult> ne = ctxRoot.search( "", "(objectClass=*)", sc );
        assertTrue( ne.hasMore(), "Search should return at least one entry." );

        SearchResult sr = ne.next();
        assertEquals( SUFFIX_DN, sr.getName(), "The entry returned should be the root entry." );
        assertFalse( ne.hasMore(), "Search should return no more entries." );
    }


    @Test
    public void testAdd() throws Exception
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", getService(), SUFFIX_DN );

        String dn = "ou=Test";

        Attributes attributes = LdifUtils.createJndiAttributes( "objectClass: top", "objectClass: organizationalUnit",
            "ou: Test" );

        DirContext ctx = ctxRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration<SearchResult> ne = ctxRoot.search( dn, "(objectClass=*)", sc );
        assertTrue( ne.hasMore(), "Search should return at least one entry." );

        SearchResult sr = ne.next();
        assertEquals( dn + "," + SUFFIX_DN, sr.getName(), "The entry returned should be the entry added earlier." );
        assertFalse( ne.hasMore(), "Search should return no more entries." );
    }


    @Test
    public void testModify() throws Exception
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", getService(), SUFFIX_DN );

        String dn = "ou=Test";
        String description = "New Value";

        Attributes attributes = LdifUtils.createJndiAttributes( "objectClass: top", "objectClass: organizationalUnit",
            "ou: Test", "description: Old Value" );

        DirContext ctx = ctxRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        ModificationItem[] mods = new ModificationItem[1];
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, new BasicAttribute( "description", description ) );

        ctxRoot.modifyAttributes( dn, mods );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        NamingEnumeration<SearchResult> ne = ctxRoot.search( dn, "(objectClass=*)", sc );
        assertTrue( ne.hasMore(), "Search should return at least one entry." );

        SearchResult sr = ( SearchResult ) ne.next();
        assertEquals( dn + "," + SUFFIX_DN, sr.getName(), "The entry returned should be the entry added earlier." );

        attributes = sr.getAttributes();
        Attribute attribute = attributes.get( "description" );

        assertEquals( description, attribute.get(), "The description attribute should contain the new value." );
        assertFalse( ne.hasMore(), "Search should return no more entries." );
    }


    @Test
    public void testDelete() throws Exception
    {
        LdapContext ctxRoot = getContext( "uid=admin,ou=system", getService(), SUFFIX_DN );

        String dn = "ou=Test";

        Attributes attributes = LdifUtils.createJndiAttributes( "objectClass: top", "objectClass: organizationalUnit",
            "ou: Test" );

        DirContext ctx = ctxRoot.createSubcontext( dn, attributes );
        assertNotNull( ctx );

        ctxRoot.destroySubcontext( dn );

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.OBJECT_SCOPE );

        try
        {
            ctxRoot.search( dn, "(objectClass=*)", sc );
            fail( "Search should throw exception." );
        }
        catch ( NamingException e )
        {
            // ignore
        }
    }
}
