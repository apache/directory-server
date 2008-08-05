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
package org.apache.directory.server.operations.compare;


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.ServerIntegrationUtils;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.newldap.LdapServer;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;


/**
 * Tests with compare operations on attributes which use different matching
 * rules. Created to demonstrate JIRA DIREVE-243 ("Compare operation does not
 * adhere to some matching rules").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@ApplyLdifs( {
    // Entry # 1
    "dn: cn=Tori Amos,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "telephoneNumber: 1234567890\n" +
    "userPassword: Secret1!\n" +
    "cn: Tori Amos\n" +
    "sn: Amos\n\n" + 
    // Entry # 2
    "dn: cn=Artists,ou=system\n" +
    "objectClass: groupOfNames\n" +
    "objectClass: top\n" +
    "cn: Artists\n" +
    "member: cn=Tori Amos,ou=system\n\n"
    }
)
public class MatchingRuleCompareTest
{
    public static LdapServer ldapServer;

    public static final String PERSON_CN = "Tori Amos";
    public static final String PERSON_SN = "Amos";
    public static final String PERSON_RDN = "cn=" + PERSON_CN;
    public static final String PERSON_TELEPHONE = "1234567890";
    public static final String PERSON_PWD = "Secret1!";

    public static final String GROUP_CN = "Artists";
    public static final String GROUP_RDN = "cn=" + GROUP_CN;


    /**
     * Compare with caseIgnoreMatch matching rule.
     * 
     * @throws NamingException
     */
    @Test
    public void testCaseIgnoreMatch() throws Exception
    {
        DirContext ctx = ( DirContext ) ServerIntegrationUtils.getWiredContext( ldapServer ).lookup( "ou=system" );
        
        // Setting up search controls for compare op
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes( new String[]
            {} ); // no attributes
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );

        String[] values =
            { PERSON_SN, PERSON_SN.toUpperCase(), PERSON_SN.toLowerCase(), PERSON_SN + "X" };
        boolean[] expected =
            { true, true, true, false };

        for ( int i = 0; i < values.length; i++ )
        {
            String value = values[i];

            NamingEnumeration<SearchResult> enumeration = ctx.search( PERSON_RDN, "sn={0}", new String[]
                { value }, ctls );
            boolean result = enumeration.hasMore();

            assertEquals( "compare sn value '" + PERSON_SN + "' with '" + value + "'", expected[i], result );

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

// Cannot search the directory using binary attributes.  I don't know if this 
// is valid according to specifications but this is the case with respect to apacheds
// and userPassword is a binary attribute type.  This is why we get class cast 
// exceptions for this search which fails.  To make this succeed some simple changes
// are needed.
    
//    public void testOctetStringMatch() throws NamingException
//    {
//        // Setting up search controls for compare op
//        SearchControls ctls = new SearchControls();
//        ctls.setReturningAttributes( new String[]
//            {} ); // no attributes
//        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
//
//        String[] values =
//            { "", PERSON_PWD, PERSON_PWD.toUpperCase(), PERSON_PWD.toLowerCase(), PERSON_PWD + "X" };
//        boolean[] expected =
//            { false, true, false, false, false };
//
//        for ( int i = 0; i < values.length; i++ )
//        {
//            String value = values[i];
//
//            NamingEnumeration enumeration = ctx.search( PERSON_RDN, "userPassword={0}", new String[]
//                { value }, ctls );
//            boolean result = enumeration.hasMore();
//
//            assertEquals( "compare '" + PERSON_PWD + "' with '" + value + "'", expected[i], result );
//
//            enumeration.close();
//        }
//    }


    /**
     * Compare with distinguishedNameMatch matching rule.
     * 
     * @throws NamingException
     */
    @Test
    public void testDistinguishedNameMatch() throws Exception
    {
        DirContext ctx = ( DirContext ) ServerIntegrationUtils.getWiredContext( ldapServer ).lookup( "ou=system" );
        
        // determine member DN of person
        DirContext member = ( DirContext ) ctx.lookup( PERSON_RDN );
        String memberDN = member.getNameInNamespace();

        // Setting up search controls for compare op
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes( new String[]
            {} ); // no attributes
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );

        String[] values =
            { "", memberDN, "cn=nobody", memberDN, PERSON_RDN + " , " + ctx.getNameInNamespace() };
        boolean[] expected =
            { false, true, false, true, true };

        for ( int i = 0; i < values.length; i++ )
        {
            String value = values[i];

            NamingEnumeration<SearchResult> enumeration = ctx.search( GROUP_RDN, "member={0}", new Object[]
                { value }, ctls );
            boolean result = enumeration.hasMore();

            assertEquals( "compare '" + memberDN + "' with '" + value + "'", expected[i], result );

            enumeration.close();
        }
    }
}
