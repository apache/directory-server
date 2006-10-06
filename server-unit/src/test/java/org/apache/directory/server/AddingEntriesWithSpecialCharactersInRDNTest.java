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
package org.apache.directory.server;


import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.unit.AbstractServerTest;


/**
 * Test case to demonstrate DIRSERVER-631 ("Creation of entry with special (and
 * escaped) character in RDN leads to wrong attribute value").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddingEntriesWithSpecialCharactersInRDNTest extends AbstractServerTest {
    private DirContext ctx = null;


    /**
     * Create an entry for a person.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialDirContext( env );
    }

    /**
     * Remove the person.
     */
    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        super.tearDown();
    }

   protected Attributes getPersonAttributes(String sn, String cn) {

       Attributes attrs = new BasicAttributes();
       Attribute ocls = new BasicAttribute("objectClass");
       ocls.add("top");
       ocls.add("person");
       attrs.put(ocls);
       attrs.put("cn", cn);
       attrs.put("sn", sn);

       return attrs;
   }

   protected Attributes getOrgUnitAttributes(String ou) {

       Attributes attrs = new BasicAttributes();
       Attribute ocls = new BasicAttribute("objectClass");
       ocls.add("top");
       ocls.add("organizationalUnit");
       attrs.put(ocls);
       attrs.put("ou", ou);

       return attrs;
   }

   /**
    * adding an entry with hash sign (#) in RDN.
    */
   public void testAddingWithHashRdn() throws NamingException {
       Attributes attrs = getPersonAttributes("Bush", "Kate#Bush");
       String rdn = "cn=Kate\\#Bush";
       ctx.createSubcontext(rdn, attrs);

       SearchControls sctls = new SearchControls();
       sctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

       NamingEnumeration enm = ctx.search("", "(cn=Kate\\#Bush)", sctls);
       assertEquals("entry found", true, enm.hasMore());
       while (enm.hasMore()) {
           SearchResult sr = (SearchResult) enm.next();
           attrs = sr.getAttributes();
           Attribute cn = sr.getAttributes().get("cn");
           assertNotNull(cn);
           assertTrue(cn.contains("Kate#Bush"));
       }
       ctx.destroySubcontext(rdn);
   }

   /**
    * adding an entry with comma sign (,) in RDN.
    */
//   public void testAddingWithCommaInRdn() throws NamingException {
//
//       Attributes attrs = getPersonAttributes("Bush", "Bush, Kate");
//       String rdn = "cn=Bush\\, Kate";
//       ctx.createSubcontext(rdn, attrs);
//
//       SearchControls sctls = new SearchControls();
//       sctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
//
//       NamingEnumeration enm = ctx.search("", "(cn=Bush, Kate)", sctls);
//       assertEquals("entry found", true, enm.hasMore());
//       while (enm.hasMore()) {
//           SearchResult sr = (SearchResult) enm.next();
//           attrs = sr.getAttributes();
//           Attribute cn = sr.getAttributes().get("cn");
//           assertNotNull(cn);
//           assertTrue(cn.contains("Bush, Kate"));
//           assertEquals( "cn=Bush\\, Kate", sr.getName() );
//       }
//
//       ctx.destroySubcontext(rdn);
//   }

   /**
    * adding an entry with quotes (") in RDN.
    */
/*   public void testAddingWithQuotesInRdn() throws NamingException {

       Attributes attrs = getPersonAttributes("Messer",
               "Mackie \\\\\\\"The Knife\" Messer");
       String rdn = "cn=Mackie \\\"The Knife\" Messer";
       ctx.createSubcontext(rdn, attrs);

       SearchControls sctls = new SearchControls();
       sctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

       NamingEnumeration enm = ctx.search("",
               "(cn=Mackie \"The Knife\" Messer)", sctls);
       assertEquals("entry found", true, enm.hasMore());
       while (enm.hasMore()) {
           SearchResult sr = (SearchResult) enm.next();
           attrs = sr.getAttributes();
           Attribute cn = sr.getAttributes().get("cn");
           assertNotNull(cn);
           assertTrue(cn.contains("Mackie \"The Knife\" Messer"));
       }

       ctx.destroySubcontext(rdn);
   }
*/
   /**
    * adding an entry with backslash (\) in RDN.
    */
/*   public void testAddingWithBackslashInRdn() throws NamingException {

       Attributes attrs = getOrgUnitAttributes("AC\\DC");
       String rdn = "ou=AC\\\\DC";
       ctx.createSubcontext(rdn, attrs);

       SearchControls sctls = new SearchControls();
       sctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

       NamingEnumeration enm = ctx.search("", "(ou=AC\\DC)", sctls);
       assertEquals("entry found", true, enm.hasMore());
       while (enm.hasMore()) {
           SearchResult sr = (SearchResult) enm.next();
           attrs = sr.getAttributes();
           Attribute ou = sr.getAttributes().get("ou");
           assertNotNull(ou);
           assertTrue(ou.contains("AC\\DC"));
       }

       ctx.destroySubcontext(rdn);
   }
*/
   /**
    * adding an entry with greater sign (>) in RDN.
    */
   public void testAddingWithGreaterSignInRdn() throws NamingException {

       Attributes attrs = getOrgUnitAttributes("East -> West");
       String rdn = "ou=East -\\> West";
       ctx.createSubcontext(rdn, attrs);

       SearchControls sctls = new SearchControls();
       sctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

       NamingEnumeration enm = ctx.search("", "(ou=East -> West)", sctls);
       assertEquals("entry found", true, enm.hasMore());
       while (enm.hasMore()) {
           SearchResult sr = (SearchResult) enm.next();
           attrs = sr.getAttributes();
           Attribute ou = sr.getAttributes().get("ou");
           assertNotNull(ou);
           assertTrue(ou.contains("East -> West"));
       }

       ctx.destroySubcontext(rdn);
   }

   /**
    * adding an entry with less sign (<) in RDN.
    */
   public void testAddingWithLessSignInRdn() throws NamingException {

       Attributes attrs = getOrgUnitAttributes("Scissors 8<");
       String rdn = "ou=Scissors 8\\<";
       ctx.createSubcontext(rdn, attrs);

       SearchControls sctls = new SearchControls();
       sctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

       NamingEnumeration enm = ctx.search("", "(ou=Scissors 8<)", sctls);
       assertEquals("entry found", true, enm.hasMore());
       while (enm.hasMore()) {
           SearchResult sr = (SearchResult) enm.next();
           attrs = sr.getAttributes();
           Attribute ou = sr.getAttributes().get("ou");
           assertNotNull(ou);
           assertTrue(ou.contains("Scissors 8<"));
       }

       ctx.destroySubcontext(rdn);
   }

   /**
    * adding an entry with semicolon (;) in RDN.
    */
   public void testAddingWithSemicolonInRdn() throws NamingException {

       Attributes attrs = getOrgUnitAttributes("semicolon group;");
       String rdn = "ou=semicolon group\\;";
       ctx.createSubcontext(rdn, attrs);

       SearchControls sctls = new SearchControls();
       sctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

       NamingEnumeration enm = ctx.search("", "(ou=semicolon group;)", sctls);
       assertEquals("entry found", true, enm.hasMore());
       while (enm.hasMore()) {
           SearchResult sr = (SearchResult) enm.next();
           attrs = sr.getAttributes();
           Attribute ou = sr.getAttributes().get("ou");
           assertNotNull(ou);
           assertTrue(ou.contains("semicolon group;"));
       }

       ctx.destroySubcontext(rdn);
   }

   /**
    * adding an entry with equals sign (=) in RDN.
    */
   public void testAddingWithEqualsInRdn() throws NamingException {

       Attributes attrs = getOrgUnitAttributes("nomen=omen");
       String rdn = "ou=nomen\\=omen";
       ctx.createSubcontext(rdn, attrs);

       SearchControls sctls = new SearchControls();
       sctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

       NamingEnumeration enm = ctx.search("", "(ou=nomen=omen)", sctls);
       assertEquals("entry found", true, enm.hasMore());
       while (enm.hasMore()) {
           SearchResult sr = (SearchResult) enm.next();
           attrs = sr.getAttributes();
           Attribute ou = sr.getAttributes().get("ou");
           assertNotNull(ou);
           assertTrue(ou.contains("nomen=omen"));
       }

       ctx.destroySubcontext(rdn);
   }
}
