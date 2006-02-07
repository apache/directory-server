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
package org.apache.ldap.server.schema;


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.ldap.server.unit.AbstractAdminTestCase;


/**
 * Test cases for the schema service.  This is for 
 * <a href="http://issues.apache.org/jira/browse/DIREVE-276">DIREVE-276</a>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaServiceTest extends AbstractAdminTestCase
{
    public void setUp() throws Exception
    {
        super.setLdifPath( "./nonspecific.ldif", getClass() );
        super.setUp();
    }
    
    
    public void testFillInObjectClasses() throws NamingException
    {
        Attribute ocs = sysRoot.getAttributes( "cn=person0" ).get( "objectClass" );
        assertEquals( 2, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );

        ocs = sysRoot.getAttributes( "cn=person1" ).get( "objectClass" );
        assertEquals( 3, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );

        ocs = sysRoot.getAttributes( "cn=person2" ).get( "objectClass" );
        assertEquals( 4, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        assertTrue( ocs.contains( "inetOrgPerson" ) );
    }


    public void testSearchForPerson() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map persons = new HashMap();
        NamingEnumeration results = sysRoot.search( "", "(objectClass=person)", controls );
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            persons.put( result.getName(), result.getAttributes() );
        }

        // admin is extra
        assertEquals( 4, persons.size() );
        
        Attributes person = null;
        Attribute ocs = null;
        
        person = ( Attributes ) persons.get( "cn=person0,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertEquals( 2, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        
        person = ( Attributes ) persons.get( "cn=person1,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertEquals( 3, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        
        person = ( Attributes ) persons.get( "cn=person2,ou=system" );
        assertNotNull( person );
        ocs = person.get( "objectClass" );
        assertEquals( 4, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        assertTrue( ocs.contains( "inetOrgPerson" ) );
    }


    public void testSearchForOrgPerson() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map orgPersons = new HashMap();
        NamingEnumeration results = sysRoot.search( "", "(objectClass=organizationalPerson)", controls );
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            orgPersons.put( result.getName(), result.getAttributes() );
        }
        
        // admin is extra
        assertEquals( 3, orgPersons.size() );
        
        Attributes orgPerson = null;
        Attribute ocs = null;
        
        orgPerson = ( Attributes ) orgPersons.get( "cn=person1,ou=system" );
        assertNotNull( orgPerson );
        ocs = orgPerson.get( "objectClass" );
        assertEquals( 3, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        
        orgPerson = ( Attributes ) orgPersons.get( "cn=person2,ou=system" );
        assertNotNull( orgPerson );
        ocs = orgPerson.get( "objectClass" );
        assertEquals( 4, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        assertTrue( ocs.contains( "inetOrgPerson" ) );
    }


    public void testSearchForInetOrgPerson() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        Map inetOrgPersons = new HashMap();
        NamingEnumeration results = sysRoot.search( "", "(objectClass=inetOrgPerson)", controls );
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            inetOrgPersons.put( result.getName(), result.getAttributes() );
        }
        
        // admin is extra
        assertEquals( 2, inetOrgPersons.size() );
        
        Attributes inetOrgPerson = null;
        Attribute ocs = null;
        
        inetOrgPerson = ( Attributes ) inetOrgPersons.get( "cn=person2,ou=system" );
        assertNotNull( inetOrgPerson );
        ocs = inetOrgPerson.get( "objectClass" );
        assertEquals( 4, ocs.size() );
        assertTrue( ocs.contains( "top" ) );
        assertTrue( ocs.contains( "person" ) );
        assertTrue( ocs.contains( "organizationalPerson" ) );
        assertTrue( ocs.contains( "inetOrgPerson" ) );
    }
}
