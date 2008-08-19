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
package org.apache.directory.server.operations.modifydn;


import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


/**
 * Test case with different modify DN operations which move the entry under a 
 * new superior.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 679049 $
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@ApplyLdifs( {
    // Entry # 1
    "dn: uid=akarasulu,ou=users,ou=system\n" +
    "objectClass: uidObject\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "uid: akarasulu\n" +
    "cn: Alex Karasulu\n" +
    "sn: karasulu\n\n" + 
    // Entry # 2
    "dn: ou=NewSuperior,ou=system\n" +
    "objectClass: organizationalUnit\n" +
    "objectClass: top\n" +
    "ou: NewSuperior\n\n"
    }
)
public class MoveIT 
{
    private static final String DN = "uid=akarasulu,ou=users,ou=system";
    private static final String NEW_DN = "uid=akarasulu,ou=NewSuperior,ou=system";
    private static final String NEW_DN2 = "uid=elecharny,ou=NewSuperior,ou=system";
    
    public static LdapServer ldapServer;
    

    @Test
    public void testMoveNoRdnChange() throws Exception
    {
        LdapContext ctx = getWiredContext( ldapServer );
        ctx.rename( DN, NEW_DN );
        
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        
        NamingEnumeration<SearchResult> results = 
            ctx.search( NEW_DN, "(objectClass=*)", controls );
        assertNotNull( results );
        assertTrue( "Could not find entry after move.", results.hasMore() );
        SearchResult result = results.next();
        assertNotNull( result );
        assertEquals( NEW_DN, result.getNameInNamespace() );
        
        results.close();
        ctx.close();
    }
    

    @Test
    public void testMoveAndRdnChange() throws Exception
    {
        LdapContext ctx = getWiredContext( ldapServer );
        ctx.rename( DN, NEW_DN2 );
        
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        
        NamingEnumeration<SearchResult> results = 
            ctx.search( NEW_DN2, "(objectClass=*)", controls );
        assertNotNull( results );
        assertTrue( "Could not find entry after move.", results.hasMore() );
        SearchResult result = results.next();
        assertNotNull( result );
        assertEquals( NEW_DN2, result.getNameInNamespace() );
        
        results.close();
        ctx.close();
    }
    
    @Test
    public void testDummy()
    {
        
    }
}
