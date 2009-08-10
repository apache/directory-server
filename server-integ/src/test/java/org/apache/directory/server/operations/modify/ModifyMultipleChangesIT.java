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
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import org.apache.directory.server.ldap.LdapServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Test case with multiple modifications on a person entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@ApplyLdifs( {
    // Entry # 1
    "dn: cn=Tori Amos,ou=system\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "description: an American singer-songwriter\n" +
    "cn: Tori Amos\n" +
    "sn: Amos\n\n" + 
    // Entry # 2
    "dn: cn=Debbie Harry,ou=system\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "cn: Debbie Harry\n" +
    "sn: Harry\n\n" 
    }
)
public class ModifyMultipleChangesIT 
{
    private static final String BASE = "ou=system";
    private static final String RDN_TORI_AMOS = "cn=Tori Amos";
    private static final String PERSON_DESCRIPTION = "an American singer-songwriter";
    private static final String RDN_DEBBIE_HARRY = "cn=Debbie Harry";

    public static LdapServer ldapServer;
    

    /**
     * Creation of required attributes of a person entry.
     */
    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attribute.add( "organizationalperson" );
        attribute.add( "inetorgperson" );
        attributes.put( attribute );
        attributes.put( "cn", cn );
        attributes.put( "sn", sn );

        return attributes;
    }


    /**
     * Add a new attribute with two values.
     */
    @Test
    public void testAddNewAttributeValues() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        // Add telephoneNumber attribute
        String[] newValues =
            { "1234567890", "999999999" };
        Attribute attr = new BasicAttribute( "telephoneNumber" );
        attr.add( newValues[0] );
        attr.add( newValues[1] );
        Attributes attrs = new BasicAttributes( true );
        attrs.put( attr );
        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify, that
        // - case of attribute description is correct
        // - attribute values are present
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        attr = attrs.get( "telephoneNumber" );
        assertNotNull( attr );
        assertEquals( "telephoneNumber", attr.getID() );
        assertTrue( attr.contains( newValues[0] ) );
        assertTrue( attr.contains( newValues[1] ) );
        assertEquals( newValues.length, attr.size() );
    }

    
    /**
     * Create a person entry and perform a modify op, in which
     * we modify an attribute two times.
     */
    @Test
    public void testAttributeValueMultiMofificationDIRSERVER_636() throws Exception 
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        // Create a person entry
        Attributes attrs = getPersonAttributes("Bush", "Kate Bush");
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext(rdn, attrs);

        // Add a description with two values
        String[] descriptions = {
                "Kate Bush is a British singer-songwriter.",
                "She has become one of the most influential female artists of the twentieth century." };
        Attribute desc1 = new BasicAttribute("description");
        desc1.add(descriptions[0]);
        desc1.add(descriptions[1]);

        ModificationItem addModOp = new ModificationItem(
                DirContext.ADD_ATTRIBUTE, desc1);

        Attribute desc2 = new BasicAttribute("description");
        desc2.add(descriptions[1]);
        ModificationItem delModOp = new ModificationItem(
                DirContext.REMOVE_ATTRIBUTE, desc2);

        ctx.modifyAttributes(rdn, new ModificationItem[] { addModOp,
                        delModOp });

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String filter = "(cn=*Bush)";
        String base = "";

        // Check entry
        NamingEnumeration<SearchResult> enm = ctx.search(base, filter, sctls);
        assertTrue(enm.hasMore());
        
        while (enm.hasMore()) {
            SearchResult sr = enm.next();
            attrs = sr.getAttributes();
            Attribute desc = sr.getAttributes().get("description");
            assertEquals(1, desc.size());
            assertTrue(desc.contains(descriptions[0]));
        }

        // Remove the person entry
        ctx.destroySubcontext(rdn);
    }
}
