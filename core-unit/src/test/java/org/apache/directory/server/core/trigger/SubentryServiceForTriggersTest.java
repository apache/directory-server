/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.server.core.trigger;


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.subtree.SubentryService;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;


/**
 * Testcases for the SubentryService for Triggers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class SubentryServiceForTriggersTest extends AbstractAdminTestCase
{
    public Attributes getTestEntry( String cn )
    {
        Attributes subentry = new LockableAttributesImpl();
        Attribute objectClass = new LockableAttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "person" );
        subentry.put( objectClass );
        subentry.put( "cn", cn );
        subentry.put( "sn", "testentry" );
        return subentry;
    }


    public Attributes getTestSubentry()
    {
        Attributes subentry = new LockableAttributesImpl();
        Attribute objectClass = new LockableAttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "triggerSubentry" );
        subentry.put( objectClass );
        subentry.put( "subtreeSpecification", "{ base \"ou=configuration\" }" );
        subentry.put( "prescriptiveTrigger", "BEFORE bind CALL \"AuthUtilities.beforeBind\"($name)" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }
    
    public Attributes getTestSubentryWithExclusion()
    {
        Attributes subentry = new LockableAttributesImpl();
        Attribute objectClass = new LockableAttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "triggerSubentry" );
        subentry.put( objectClass );
        String spec = "{ base \"ou=configuration\", specificExclusions { chopBefore:\"cn=unmarked\" } }";
        subentry.put( "subtreeSpecification", spec );
        subentry.put( "prescriptiveTrigger", "BEFORE bind CALL \"AuthUtilities.beforeBind\"($name)" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }


    public void addTheAdministrativeRole() throws NamingException
    {
        Attribute attribute = new LockableAttributeImpl( "administrativeRole" );
        attribute.add( "autonomousArea" );
        attribute.add( "triggerSpecificArea" );
        ModificationItem item = new ModificationItem( DirContext.ADD_ATTRIBUTE, attribute );
        super.sysRoot.modifyAttributes( "", new ModificationItem[] { item } );
    }


    public Map getAllEntries() throws NamingException
    {
        Map resultMap = new HashMap();
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
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        Attribute triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
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
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );
    }


    public void testSubentryModify() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now modify the subentry by introducing an exclusion
        // --------------------------------------------------------------------

        Attribute subtreeSpecification = new LockableAttributeImpl( "subtreeSpecification" );
        subtreeSpecification.add( "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=interceptors\" } }" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, subtreeSpecification );
        super.sysRoot.modifyAttributes( "cn=testsubentry", new ModificationItem[] { item } );
        results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );

        interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
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
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        if ( triggerSubentries != null )
        {
            assertEquals( "ou=configuration,ou=system should not be marked", 0, triggerSubentries.size() );
        }

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        if ( triggerSubentries != null )
        {
            assertEquals( "ou=interceptors,ou=configuration,ou=system should not be marked", 0, triggerSubentries.size() );
        }

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );
    }


    public void testSubentryModifyRdn() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        super.sysRoot.rename( "cn=testsubentry", "cn=newname" );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=newname,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=newname,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );
    }


    public void testEntryModifyRdn() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        super.sysRoot.createSubcontext( "cn=unmarked,ou=configuration", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes unmarked = ( Attributes ) results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        super.sysRoot.destroySubcontext( "cn=unmarked,ou=configuration" );
        super.sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked,ou=configuration" );
        results = getAllEntries();

        unmarked = ( Attributes ) results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        super.sysRoot.rename( "cn=unmarked,ou=configuration", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=configuration,ou=system" ) );
        marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );
    }


    public void testEntryMoveWithRdnChange() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        super.sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        super.sysRoot.destroySubcontext( "cn=unmarked" );
        super.sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked" );
        results = getAllEntries();

        unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        super.sysRoot.rename( "cn=unmarked", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=system" ) );
        marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );
    }


    public void testEntryMove() throws NamingException
    {
        addTheAdministrativeRole();
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        super.sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute triggerSubentries = configuration.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        triggerSubentries = interceptors.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        Attributes marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.TRIGGER_SUBENTRIES ) );

        Attributes unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.TRIGGER_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        super.sysRoot.destroySubcontext( "cn=unmarked" );
        super.sysRoot.rename( "cn=marked,ou=configuration", "cn=marked,ou=interceptors,ou=configuration" );
        results = getAllEntries();

        unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        marked = ( Attributes ) results.get( "cn=marked,ou=interceptors,ou=configuration,ou=system" );
        assertNotNull( marked );
        triggerSubentries = marked.get( SubentryService.TRIGGER_SUBENTRIES );
        assertNotNull( "cn=marked,ou=interceptors,ou=configuration should be marked", triggerSubentries );
        assertEquals( "cn=testsubentry,ou=system", triggerSubentries.get() );
        assertEquals( 1, triggerSubentries.size() );
    }

}
