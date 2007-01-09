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


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.subtree.SubentryService;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;


/**
 * Testcases for the SubentryService for Triggers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class SubentryServiceForTriggersITest extends AbstractAdminTestCase
{
    public Attributes getTestEntry( String cn )
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "person" );
        subentry.put( objectClass );
        subentry.put( "cn", cn );
        subentry.put( "sn", "testentry" );
        return subentry;
    }


    public Attributes getTestSubentry()
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "triggerExecutionSubentry" );
        subentry.put( objectClass );
        subentry.put( "subtreeSpecification", "{ base \"ou=configuration\" }" );
        subentry.put( "prescriptiveTriggerSpecification", "AFTER Delete CALL \"LogUtils.logDelete\"($name)" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }
    
    public Attributes getTestSubentryWithExclusion()
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "triggerExecutionSubentry" );
        subentry.put( objectClass );
        String spec = "{ base \"ou=configuration\", specificExclusions { chopBefore:\"cn=unmarked\" } }";
        subentry.put( "subtreeSpecification", spec );
        subentry.put( "prescriptiveTriggerSpecification", "AFTER Delete CALL \"LogUtils.logDelete\"($name)" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }


    public void addTheAdministrativeRole() throws NamingException
    {
        Attribute attribute = new AttributeImpl( "administrativeRole" );
        attribute.add( "autonomousArea" );
        attribute.add( "triggerSpecificArea" );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attribute );
        super.sysRoot.modifyAttributes( "", new ModificationItemImpl[] { item } );
    }


    public Map<String, Attributes> getAllEntries() throws NamingException
    {
        Map<String, Attributes> resultMap = new HashMap<String, Attributes>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+", "*" } );
        NamingEnumeration results = super.sysRoot.search( "", "(objectClass=*)", controls );
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            resultMap.put( result.getName(), result.getAttributes() );
        }
        return resultMap;
    }
    
    public void testEntryAdd() throws NamingException
    {
        addTheAdministrativeRole();        
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        super.sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        Attribute triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );
    }


    public void testSubentryAdd() throws NamingException
    {
        try
        {
            super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
            fail( "should never get here: cannot create subentry under regular entries" );
        }
        catch ( LdapNoSuchAttributeException e )
        {
        }

        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );
    }


    public void testSubentryModify() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now modify the subentry by introducing an exclusion
        // --------------------------------------------------------------------

        Attribute subtreeSpecification = new AttributeImpl( "subtreeSpecification" );
        subtreeSpecification.add( "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=interceptors\" } }" );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, subtreeSpecification );
        super.sysRoot.modifyAttributes( "cn=testsubentry", new ModificationItemImpl[] { item } );
        results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        configuration = results.get( "ou=configuration,ou=system" );
        triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );

        interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        if ( triggerSubentries != null )
        {
            assertEquals( "ou=interceptors,ou=configuration,ou=system should not be marked", 0, triggerSubentries.size() );
        }
    }


    public void testSubentryDelete() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        super.sysRoot.destroySubcontext( "cn=testsubentry" );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        if ( triggerSubentries != null )
        {
            assertEquals( "ou=configuration,ou=system should not be marked", 0, triggerSubentries.size() );
        }

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        if ( triggerSubentries != null )
        {
            assertEquals( "ou=interceptors,ou=configuration,ou=system should not be marked", 0, triggerSubentries.size() );
        }

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );
    }


    public void testSubentryModifyRdn() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        super.sysRoot.rename( "cn=testsubentry", "cn=newname" );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );
    }


    public void testEntryModifyRdn() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        super.sysRoot.createSubcontext( "cn=unmarked,ou=configuration", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        super.sysRoot.destroySubcontext( "cn=unmarked,ou=configuration" );
        super.sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked,ou=configuration" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        super.sysRoot.rename( "cn=unmarked,ou=configuration", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=configuration,ou=system" ) );
        marked = results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );
    }


    public void testEntryMoveWithRdnChange() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        super.sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        super.sysRoot.destroySubcontext( "cn=unmarked" );
        super.sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        super.sysRoot.rename( "cn=unmarked", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=system" ) );
        marked = results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );
    }


    public void testEntryMove() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        super.sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        super.sysRoot.destroySubcontext( "cn=unmarked" );
        super.sysRoot.rename( "cn=marked,ou=configuration", "cn=marked,ou=interceptors,ou=configuration" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        marked = results.get( "cn=marked,ou=interceptors,ou=configuration,ou=system" );
        assertNotNull( marked );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=interceptors,ou=configuration should be marked", triggerSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );
    }

}
