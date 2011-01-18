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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Testcases for the SubentryInterceptor for Triggers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@Ignore("Reverts are failing to delete marked entries. Fixing this " +
        "problem in testEntryAdd() will fix it all over.")
public class SubentryServiceForTriggersIT extends AbstractLdapTestUnit
{

    public Attributes getTestEntry( String cn )
    {
        Attributes subentry = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "person" );
        subentry.put( objectClass );
        subentry.put( "cn", cn );
        subentry.put( "sn", "testentry" );
        return subentry;
    }


    public Attributes getTestSubentry()
    {
        Attributes subentry = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( SchemaConstants.SUBENTRY_OC );
        objectClass.add( "triggerExecutionSubentry" );
        subentry.put( objectClass );
        subentry.put( "subtreeSpecification", "{ base \"ou=configuration\" }" );
        subentry.put( "prescriptiveTriggerSpecification", "AFTER Delete CALL \"LogUtils.logDelete\"($name);" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }
    
    public Attributes getTestSubentryWithExclusion()
    {
        Attributes subentry = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( SchemaConstants.SUBENTRY_OC );
        objectClass.add( "triggerExecutionSubentry" );
        subentry.put( objectClass );
        String spec = "{ base \"ou=configuration\", specificExclusions { chopBefore:\"cn=unmarked\" } }";
        subentry.put( "subtreeSpecification", spec );
        subentry.put( "prescriptiveTriggerSpecification", "AFTER Delete CALL \"LogUtils.logDelete\"($name);" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }


    public void addTheAdministrativeRole() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        Attribute attribute = new BasicAttribute( "administrativeRole" );
        attribute.add( "autonomousArea" );
        attribute.add( "triggerSpecificArea" );
        ModificationItem item = new ModificationItem( DirContext.ADD_ATTRIBUTE, attribute );
        sysRoot.modifyAttributes( "", new ModificationItem[] { item } );
    }


    public Map<String, Attributes> getAllEntries() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        Map<String, Attributes> resultMap = new HashMap<String, Attributes>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+", "*" } );
        NamingEnumeration<SearchResult> results = sysRoot.search( "", "(objectClass=*)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            resultMap.put( result.getName(), result.getAttributes() );
        }
        return resultMap;
    }


    @Test
    public void testEntryAdd() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addTheAdministrativeRole();        
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        Attribute triggerSubentries = marked.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        // @todo attempts to delete this entry cause an StringIndexOutOfBoundsException
        sysRoot.destroySubcontext( "cn=marked,ou=configuration" );
    }


    @Test
    public void testSubentryAdd() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );

        //noinspection EmptyCatchBlock
        try
        {
            sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
            fail( "should never get here: cannot create subentry under regular entries" );
        }
        catch ( Exception e )
        {
        }

        addTheAdministrativeRole();
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );
    }


    @Test
    public void testSubentryModify() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addTheAdministrativeRole();
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        // --------------------------------------------------------------------
        // Now modify the subentry by introducing an exclusion
        // --------------------------------------------------------------------

        Attribute subtreeSpecification = new BasicAttribute( "subtreeSpecification" );
        subtreeSpecification.add( "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=interceptors\" } }" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, subtreeSpecification );
        sysRoot.modifyAttributes( "cn=testsubentry", new ModificationItem[] { item } );
        results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        configuration = results.get( "ou=configuration,ou=system" );
        triggerSubentries = configuration.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        if ( triggerSubentries != null )
        {
            assertEquals( "ou=interceptors,ou=configuration,ou=system should not be marked", 0, triggerSubentries.size() );
        }
    }


    @Test
    public void testSubentryDelete() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addTheAdministrativeRole();
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        sysRoot.destroySubcontext( "cn=testsubentry" );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        if ( triggerSubentries != null )
        {
            assertEquals( "ou=configuration,ou=system should not be marked", 0, triggerSubentries.size() );
        }

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        if ( triggerSubentries != null )
        {
            assertEquals( "ou=interceptors,ou=configuration,ou=system should not be marked", 0, triggerSubentries.size() );
        }

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );
    }


    @Test
    public void testSubentryModifyRdn() throws Exception
    {
        addTheAdministrativeRole();
        LdapContext sysRoot = getSystemContext( service );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        sysRoot.rename( "cn=testsubentry", "cn=newname" );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );
    }


    @Test
    public void testEntryModifyRdn() throws Exception
    {
        addTheAdministrativeRole();
        LdapContext sysRoot = getSystemContext( service );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        sysRoot.createSubcontext( "cn=unmarked,ou=configuration", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        triggerSubentries = marked.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        sysRoot.destroySubcontext( "cn=unmarked,ou=configuration" );
        sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked,ou=configuration" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        sysRoot.rename( "cn=unmarked,ou=configuration", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=configuration,ou=system" ) );
        marked = results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        triggerSubentries = marked.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );
    }


    @Test
    public void testEntryMoveWithRdnChange() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addTheAdministrativeRole();
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        triggerSubentries = marked.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        sysRoot.destroySubcontext( "cn=unmarked" );
        sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        sysRoot.rename( "cn=unmarked", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=system" ) );
        marked = results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        triggerSubentries = marked.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );
    }


    @Test
    public void testEntryMove() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addTheAdministrativeRole();
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        triggerSubentries = marked.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        sysRoot.destroySubcontext( "cn=unmarked" );
        sysRoot.rename( "cn=marked,ou=configuration", "cn=marked,ou=interceptors,ou=configuration" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        marked = results.get( "cn=marked,ou=interceptors,ou=configuration,ou=system" );
        assertNotNull( marked );
        triggerSubentries = marked.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=interceptors,ou=configuration should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );
    }

}
