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
package org.apache.directory.server.operations.modify;


import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import org.apache.directory.server.newldap.LdapServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


/**
 * Test case for all modify replace operations.
 * 
 * Demonstrates DIRSERVER-646 ("Replacing an unknown attribute with
 * no values (deletion) causes an error").
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
public class ModifyReplaceIT 
{
    private static final String BASE = "ou=system";

    public static LdapServer ldapServer;
    
    
    protected Attributes getPersonAttributes( String sn, String cn ) 
    {
        Attributes attrs = new BasicAttributes();
        Attribute ocls = new BasicAttribute("objectClass");
        ocls.add("top");
        ocls.add("person");
        attrs.put(ocls);
        attrs.put("cn", cn);
        attrs.put("sn", sn);

        return attrs;
    }

    
    /**
     * Create a person entry and try to remove a not present attribute
     */
    @Test
    public void testReplaceNotPresentAttribute() throws Exception 
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush";
        sysRoot.createSubcontext( rdn, attrs );

        Attribute attr = new BasicAttribute( "description" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        sysRoot.modifyAttributes( rdn, new ModificationItem[] { item } );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );
        while ( enm.hasMore() ) 
        {
            SearchResult sr = ( SearchResult ) enm.next();
            attrs = sr.getAttributes();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains("Kate Bush") );
        }

        sysRoot.destroySubcontext( rdn );
    }

    
    /**
     * Create a person entry and try to remove a non existing attribute
     */
    @Test
    public void testReplaceNonExistingAttribute() throws Exception 
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush";
        sysRoot.createSubcontext( rdn, attrs );

        Attribute attr = new BasicAttribute( "numberOfOctaves" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        sysRoot.modifyAttributes(rdn, new ModificationItem[] { item });

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );
        while ( enm.hasMore() ) 
        {
            SearchResult sr = enm.next();
            attrs = sr.getAttributes();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
        }

        sysRoot.destroySubcontext( rdn );
    }


    /**
     * Create a person entry and try to remove a non existing attribute
     */
    @Test
    public void testReplaceNonExistingAttributeManyMods() throws Exception 
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush";
        sysRoot.createSubcontext( rdn, attrs );

        Attribute attr = new BasicAttribute( "numberOfOctaves" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        Attribute attr2 = new BasicAttribute( "description", "blah blah blah" );
        ModificationItem item2 = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr2 );

        sysRoot.modifyAttributes(rdn, new ModificationItem[] { item, item2 });

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );
        while ( enm.hasMore() ) 
        {
            SearchResult sr = enm.next();
            attrs = sr.getAttributes();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
        }

        sysRoot.destroySubcontext( rdn );
    }
}
