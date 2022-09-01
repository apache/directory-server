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

package org.apache.directory.server.core.subtree;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Testcases for the SubentryInterceptor. Investigation on handling Subtree Refinement
 * Selection Membership upon objectClass attribute value changes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(factory = DefaultDirectoryServiceFactory.class, name = "SubentryServiceObjectClassChangeHandlingIT-class")
public class SubentryServiceObjectClassChangeHandlingIT extends AbstractLdapTestUnit
{

    public Entry getTestEntry( String dn, String cn ) throws LdapException
    {
        return new DefaultEntry(  
            dn,
            "objectClass", "top",
            "objectClass", "person",
            "cn", cn,
            "sn", cn );
    }


    public Entry getCollectiveAttributeTestSubentry( String dn, String cn ) throws LdapException
    {
        return new DefaultEntry(  
            dn,
            "objectClass", "top",
            "objectClass", "subentry",
            "objectClass", "collectiveAttributeSubentry",
            "subtreeSpecification", "{ specificationFilter item:organizationalPerson }",
            "c-o", "Test Org",
            "cn", cn );
    }

    private void addAdministrativeRole( LdapConnection connection, String dn ) throws Exception
    {
        connection.modify( dn, 
            new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "administrativeRole", "collectiveAttributeSpecificArea" ) );
    }
    

    public Map<String, Entry> getAllEntries( LdapConnection connection, String dn ) throws Exception
    {
        Map<String, Entry> resultMap = new HashMap<>();
        
        try ( EntryCursor cursor = connection.search( dn, "(objectClass=*)", SearchScope.SUBTREE, "*", "+" ) )
        {
            while ( cursor.next() )
            {
                Entry entry = cursor.get(); 
                
                resultMap.put( entry.getDn().getName(), entry );
            }
        }

        return resultMap;
    }

    
    @Test
    public void testTrackingOfOCChangesInSubentryServiceModifyRoutine() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system" );

            connection.add(
                getCollectiveAttributeTestSubentry( "cn=collectiveAttributeTestSubentry,ou=system", "collectiveAttributeTestSubentry" ) );
        
            Entry entry = getTestEntry( "cn=testEntry,ou=system", "testEntry" );
            connection.add( entry );
    
            //----------------------------------------------------------------------
    
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
            Entry testEntry = results.get( "cn=testEntry,ou=system" );
    
            Attribute collectiveAttributeSubentries = testEntry.get( "collectiveAttributeSubentries" );
    
            assertNull( collectiveAttributeSubentries );
    
            //----------------------------------------------------------------------
            connection.modify( "cn=testEntry,ou=system", 
                new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "ObjectClass", "organizationalPerson" ),
                new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "ou", "Test Organizational Unit" ) );
            
    
            results = getAllEntries( connection, "ou=system" );
            testEntry = results.get( "cn=testEntry,ou=system" );
    
            collectiveAttributeSubentries = testEntry.get( "collectiveAttributeSubentries" );
            assertNotNull( collectiveAttributeSubentries );
        }
    }
}
