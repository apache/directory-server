/*
 * Copyright (c) 2004 Solarsis Group LLC.
 *
 * Licensed under the Open Software License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://opensource.org/licenses/osl-2.1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ldap.server;

import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 * Tests with compare operations on attributes which use different matching
 * rules. Created to demonstrate JIRA DIREVE-243 ("Compare operation does not
 * adhere to some matching rules").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MatchingRuleCompareTest extends AbstractServerTest
{
    private LdapContext ctx = null;

    public static final String PERSON_CN = "Tori Amos";
    public static final String PERSON_SN = "Amos";
    public static final String PERSON_RDN = "cn=" + PERSON_CN;
    public static final String PERSON_TELEPHONE = "1234567890abc";
    public static final String PERSON_PWD = "Secret1!";

    public static final String GROUP_CN = "Artists";
    public static final String GROUP_RDN = "cn=" + GROUP_CN;

    protected Attributes getPersonAttributes(String sn, String cn)
    {
        Attributes attributes = new BasicAttributes();
        Attribute attribute = new BasicAttribute("objectClass");
        attribute.add("top");
        attribute.add("person");
        attributes.put(attribute);
        attributes.put("cn", cn);
        attributes.put("sn", sn);

        return attributes;
    }

    protected Attributes getGroupOfNamesAttributes(String cn, String member)
    {
        Attributes attributes = new BasicAttributes();
        Attribute attribute = new BasicAttribute("objectClass");
        attribute.add("top");
        attribute.add("groupOfNames");
        attributes.put(attribute);
        attributes.put("cn", cn);
        attributes.put("member", member);

        return attributes;
    }

    /**
     * Create context, a person entry and a group.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        Hashtable env = new Hashtable();
        env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("java.naming.provider.url", "ldap://localhost:" + port + "/ou=system");
        env.put("java.naming.security.principal", "uid=admin,ou=system");
        env.put("java.naming.security.credentials", "secret");
        env.put("java.naming.security.authentication", "simple");

        ctx = new InitialLdapContext(env, null);
        assertNotNull(ctx);

        // Create a person
        Attributes attributes = this.getPersonAttributes(PERSON_SN, PERSON_CN);
        attributes.put("telephoneNumber", PERSON_TELEPHONE);
        attributes.put("userPassword", PERSON_PWD);
        ctx.createSubcontext(PERSON_RDN, attributes);

        // Create a group
        DirContext member = (DirContext) ctx.lookup(PERSON_RDN);
        attributes = this.getGroupOfNamesAttributes(GROUP_CN, member.getNameInNamespace());
        ctx.createSubcontext(GROUP_RDN, attributes);
    }

    /**
     * Remove entries and close context.
     */
    public void tearDown() throws Exception
    {
        ctx.unbind(PERSON_RDN);
        ctx.unbind(GROUP_RDN);

        ctx.close();

        super.tearDown();
    }

    /**
     * Compare with caseIgnoreMatch matching rule.
     * 
     * @throws NamingException
     */
    public void testCaseIgnoreMatch() throws NamingException
    {
        // Setting up search controls for compare op
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(new String[] {}); // no attributes
        ctls.setSearchScope(SearchControls.OBJECT_SCOPE);

        String[] values = { PERSON_SN, PERSON_SN.toUpperCase(), PERSON_SN.toLowerCase(), PERSON_SN + "X" };
        boolean[] expected = { true, true, true, false };

        for (int i = 0; i < values.length; i++) {
            String value = values[i];

            NamingEnumeration enumeration = ctx.search(PERSON_RDN, "sn={0}", new String[] { value }, ctls);
            boolean result = enumeration.hasMore();

            assertEquals("compare sn value '" + PERSON_SN + "' with '" + value + "'", expected[i], result);

            enumeration.close();
        }
    }

    //

    /**
     * Compare with telephoneNumberMatch matching rule.
     * 
     * @throws NamingException
     */

// Comment this out until we have the telephone number match working.

//    public void testTelephoneNumberMatch() throws NamingException
//    {
//        // Setting up search controls for compare op
//        SearchControls ctls = new SearchControls();
//        ctls.setReturningAttributes(new String[] {}); // no attributes
//        ctls.setSearchScope(SearchControls.OBJECT_SCOPE);
//
//        String[] values = { "", "1234567890abc", "   1234567890 A B C", "123 456 7890 abc", "123-456-7890 abC",
//                "123456-7890 A bc" };
//        boolean[] expected = { false, true, true, true, true, true };
//
//        for (int i = 0; i < values.length; i++) {
//            String value = values[i];
//
//            NamingEnumeration enumeration = ctx.search(PERSON_RDN, "telephoneNumber={0}", new String[] { value }, ctls);
//            boolean result = enumeration.hasMore();
//
//            assertEquals("compare '" + PERSON_TELEPHONE + "' with '" + value + "'", expected[i], result);
//
//            enumeration.close();
//        }
//    }

    /**
     * Compare with octetStringMatch matching rule.
     * 
     * @throws NamingException
     */
    public void testOctetStringMatch() throws NamingException
    {
        // Setting up search controls for compare op
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(new String[] {}); // no attributes
        ctls.setSearchScope(SearchControls.OBJECT_SCOPE);

        String[] values = { "", PERSON_PWD, PERSON_PWD.toUpperCase(), PERSON_PWD.toLowerCase(), PERSON_PWD + "X" };
        boolean[] expected = { false, true, false, false, false };

        for (int i = 0; i < values.length; i++) {
            String value = values[i];

            NamingEnumeration enumeration = ctx.search(PERSON_RDN, "userPassword={0}", new String[] { value }, ctls);
            boolean result = enumeration.hasMore();

            assertEquals("compare '" + PERSON_PWD + "' with '" + value + "'", expected[i], result);

            enumeration.close();
        }
    }

    /**
     * Compare with distinguishedNameMatch matching rule.
     * 
     * @throws NamingException
     */
    public void testDistinguishedNameMatch() throws NamingException
    {
        // determine member DN of person
        DirContext member = (DirContext) ctx.lookup(PERSON_RDN);
        String memberDN = member.getNameInNamespace();

        // Setting up search controls for compare op
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(new String[] {}); // no attributes
        ctls.setSearchScope(SearchControls.OBJECT_SCOPE);

        String[] values = { "", memberDN, "cn=nobody", memberDN.toLowerCase(),
                PERSON_RDN + " , " + ctx.getNameInNamespace() };
        boolean[] expected = { false, true, false, true, true };

        for (int i = 0; i < values.length; i++) {
            String value = values[i];

            NamingEnumeration enumeration = ctx.search(GROUP_RDN, "member={0}", new Object[] { value }, ctls);
            boolean result = enumeration.hasMore();

            assertEquals("compare '" + memberDN + "' with '" + value + "'", expected[i], result);

            enumeration.close();
        }
    }

}
