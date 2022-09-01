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

package org.apache.directory.server.core.trigger;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Testcases for the SubentryInterceptor for Triggers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@Disabled("Reverts are failing to delete marked entries. Fixing this " +
    "problem in testEntryAdd() will fix it all over.")
public class SubentryServiceForTriggersIT extends AbstractLdapTestUnit
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

    
    public Entry getTestSubentry( String dn ) throws LdapException
    {
        return new DefaultEntry(  
            dn,
            "objectClass", "top",
            "objectClass", "subentry",
            "objectClass", "triggerExecutionSubentry",
            "subtreeSpecification", "{ base \"ou=configuration\" }",
            "prescriptiveTriggerSpecification", "AFTER Delete CALL \"LogUtils.logDelete\"($name);",
            "cn", "testsubentry" );
    }

    
    public Entry getTestSubentryWithExclusion( String dn ) throws LdapException
    {
        return new DefaultEntry(  
            dn,
            "objectClass", "top",
            "objectClass", "subentry",
            "objectClass", "triggerExecutionSubentry",
            "subtreeSpecification", "{ base \"ou=configuration\", specificExclusions { chopBefore:\"cn=unmarked\" } }",
            "prescriptiveTriggerSpecification", "AFTER Delete CALL \"LogUtils.logDelete\"($name);",
            "cn", "testsubentry" );
    }

    
    private void addAdministrativeRole( LdapConnection connection, String dn ) throws Exception
    {
        connection.modify( dn, 
            new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "administrativeRole", "autonomousArea", "triggerSpecificArea" ) );
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
    public void testEntryAdd() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system" );
            connection.add( getTestSubentry( "cn=testsubentry,ou=system" ) );
            connection.add( getTestEntry( "cn=unmarked,ou=system", "unmarked" ) );
            connection.add( getTestEntry( "cn=marked,ou=configuration,ou=system", "marked" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );

            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry marked = results.get( "cn=marked,ou=configuration,ou=system" );
            Attribute triggerSubentries = marked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry unmarked = results.get( "cn=unmarked,ou=system" );
            assertNull( unmarked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) , "cn=unmarked,ou=system should not be marked" );
    
            // @todo attempts to delete this entry cause an StringIndexOutOfBoundsException
            connection.delete( "cn=marked,ou=configuration,ou=system" );
        }
    }


    @Test
    public void testSubentryAdd() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            //noinspection EmptyCatchBlock
            try
            {
                connection.add( getTestSubentry( "cn=testsubentry" ) );
                fail( "should never get here: cannot create subentry under regular entries" );
            }
            catch ( Exception e )
            {
            }
    
            addAdministrativeRole( connection, "ou=system" );
            connection.add( getTestSubentry( "cn=testsubentry,ou=system" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute triggerSubentries = configuration.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            triggerSubentries = interceptors.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ), "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) ,  "ou=users,ou=system should not be marked" );
        }
    }


    @Test
    public void testSubentryModify() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system" );
            connection.add( getTestSubentry( "cn=testsubentry,ou=system" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute triggerSubentries = configuration.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            triggerSubentries = interceptors.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ), "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) ,  "ou=users,ou=system should not be marked" );
    
            // --------------------------------------------------------------------
            // Now modify the subentry by introducing an exclusion
            // --------------------------------------------------------------------
    
            connection.modify( "cn=testsubentry,ou=system", 
                new DefaultModification( 
                    ModificationOperation.REPLACE_ATTRIBUTE, 
                    "subtreeSpecification", 
                    "{ base \"ou=configuration\", specificExclusions { chopBefore: \"ou=interceptors\" } }" ) );
            
            results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            configuration = results.get( "ou=configuration,ou=system" );
            triggerSubentries = configuration.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            system = results.get( "ou=system" );
            assertNull( system.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ), 
                "ou=system should not be marked" );
    
            users = results.get( "ou=users,ou=system" );
            assertNull( users.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) ,  
                "ou=users,ou=system should not be marked" );
    
            interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            triggerSubentries = interceptors.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            
            if ( triggerSubentries != null )
            {
                assertEquals( 0, triggerSubentries.size(),
                    "ou=interceptors,ou=configuration,ou=system should not be marked" );
            }
        }
    }


    @Test
    public void testSubentryDelete() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system" );
            connection.add( getTestSubentry( "cn=testsubentry,ou=system" ) );
            connection.delete( "cn=testsubentry,ou=system" );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute triggerSubentries = configuration.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            if ( triggerSubentries != null )
            {
                assertEquals( 0, triggerSubentries.size(), "ou=configuration,ou=system should not be marked" );
            }
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            triggerSubentries = interceptors.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            if ( triggerSubentries != null )
            {
                assertEquals( 0, triggerSubentries.size(),
                    "ou=interceptors,ou=configuration,ou=system should not be marked" );
            }
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ), 
                "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "ou=users,ou=system should not be marked" );
        }
    }


    @Test
    public void testSubentryModifyRdn() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system" );
            connection.add( getTestSubentry( "cn=testsubentry,ou=system" ) );
            connection.rename( "cn=testsubentry,ou=system", "cn=newname" );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute triggerSubentries = configuration.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= newname ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            triggerSubentries = interceptors.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= newname ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "ou=users,ou=system should not be marked" );
        }
    }


    @Test
    public void testEntryModifyRdn() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry,ou=system" ) );
            connection.add( getTestEntry( "cn=unmarked,ou=configuration,ou=system", "unmarked" ) );
            connection.add( getTestEntry( "cn=marked,ou=configuration,ou=system", "marked" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute triggerSubentries = configuration.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            triggerSubentries = interceptors.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            Entry marked = results.get( "cn=marked,ou=configuration,ou=system" );
            triggerSubentries = marked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "ou=users,ou=system should not be marked" );
    
            Entry unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
            assertNull( unmarked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "cn=unmarked,ou=configuration,ou=system should not be marked" );
    
            // --------------------------------------------------------------------
            // Now destry one of the marked/unmarked and rename to deleted entry
            // --------------------------------------------------------------------
    
            connection.delete( "cn=unmarked,ou=configuration,ou=system" );
            connection.rename( "cn=marked,ou=configuration,ou=system", "cn=unmarked" );
            results = getAllEntries( connection, "ou=system" );
    
            unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
            assertNull( unmarked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "cn=unmarked,ou=configuration,ou=system should not be marked" );
            assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );
    
            // --------------------------------------------------------------------
            // Now rename unmarked to marked and see that subentry op attr is there
            // --------------------------------------------------------------------
    
            connection.rename( "cn=unmarked,ou=configuration,ou=system", "cn=marked" );
            results = getAllEntries( connection, "ou=system" );
            assertNull( results.get( "cn=unmarked,ou=configuration,ou=system" ) );
            marked = results.get( "cn=marked,ou=configuration,ou=system" );
            assertNotNull( marked );
            triggerSubentries = marked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
        }
    }


    @Test
    public void testEntryMoveWithRdnChange() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry,ou=system" ) );
            connection.add( getTestEntry( "cn=unmarked,ou=system", "unmarked" ) );
            connection.add( getTestEntry( "cn=marked,ou=configuration,ou=system", "marked" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute triggerSubentries = configuration.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            triggerSubentries = interceptors.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            Entry marked = results.get( "cn=marked,ou=configuration,ou=system" );
            triggerSubentries = marked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "ou=users,ou=system should not be marked" );
    
            Entry unmarked = results.get( "cn=unmarked,ou=system" );
            assertNull( unmarked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "cn=unmarked,ou=system should not be marked" );
    
            // --------------------------------------------------------------------
            // Now destry one of the marked/unmarked and rename to deleted entry
            // --------------------------------------------------------------------
    
            connection.delete( "cn=unmarked,ou=system" );
            connection.moveAndRename( "cn=marked,ou=configuration,ou=system", "cn=unmarked,ou=system" );
            results = getAllEntries( connection, "ou=system" );
    
            unmarked = results.get( "cn=unmarked,ou=system" );
            assertNull( unmarked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),
                "cn=unmarked,ou=system should not be marked" );
            assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );
    
            // --------------------------------------------------------------------
            // Now rename unmarked to marked and see that subentry op attr is there
            // --------------------------------------------------------------------
    
            connection.moveAndRename( "cn=unmarked,ou=system", "cn=marked,ou=configuration,ou=system" );
            results = getAllEntries( connection, "ou=system" );
            assertNull( results.get( "cn=unmarked,ou=system" ) );
            marked = results.get( "cn=marked,ou=configuration,ou=system" );
            assertNotNull( marked );
            triggerSubentries = marked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
       } 
    }


    @Test
    public void testEntryMove() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            addAdministrativeRole( connection, "ou=system" );
            connection.add( getTestSubentryWithExclusion( "cn=testsubentry,ou=system" ) );
            connection.add( getTestEntry( "cn=unmarked,ou=system", "unmarked" ) );
            connection.add( getTestEntry( "cn=marked,ou=configuration,ou=system", "marked" ) );
            Map<String, Entry> results = getAllEntries( connection, "ou=system" );
    
            // --------------------------------------------------------------------
            // Make sure entries selected by the subentry do have the mark
            // --------------------------------------------------------------------
    
            Entry configuration = results.get( "ou=configuration,ou=system" );
            Attribute triggerSubentries = configuration.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            Entry interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
            triggerSubentries = interceptors.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "ou=interceptors,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            Entry marked = results.get( "cn=marked,ou=configuration,ou=system" );
            triggerSubentries = marked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "cn=marked,ou=configuration,ou=system should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
    
            // --------------------------------------------------------------------
            // Make sure entries not selected by subentry do not have the mark
            // --------------------------------------------------------------------
    
            Entry system = results.get( "ou=system" );
            assertNull( system.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ), "ou=system should not be marked" );
    
            Entry users = results.get( "ou=users,ou=system" );
            assertNull( users.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ),  
                "ou=users,ou=system should not be marked" );
    
            Entry unmarked = results.get( "cn=unmarked,ou=system" );
            assertNull( unmarked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ), 
                "cn=unmarked,ou=system should not be marked" );
    
            // --------------------------------------------------------------------
            // Now destry one of the marked/unmarked and rename to deleted entry
            // --------------------------------------------------------------------
    
            connection.delete( "cn=unmarked,ou=system" );
            connection.move( "cn=marked,ou=configuration,ou=system", "ou=interceptors,ou=configuration,ou=system" );
            results = getAllEntries( connection, "ou=system" );
    
            unmarked = results.get( "cn=unmarked,ou=system" );
            assertNull( unmarked, "cn=unmarked,ou=system should not be marked"  );
            assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );
    
            marked = results.get( "cn=marked,ou=interceptors,ou=configuration,ou=system" );
            assertNotNull( marked );
            triggerSubentries = marked.get( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
            assertNotNull( triggerSubentries ,  "cn=marked,ou=interceptors,ou=configuration should be marked" );
            assertEquals( "2.5.4.3= testsubentry ,2.5.4.11= system ", triggerSubentries.get().getNormalized() );
            assertEquals( 1, triggerSubentries.size() );
        }
    }
}
