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


import org.apache.directory.server.core.subtree.SubentryService;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.SubentriesControl;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;

import java.util.Map;
import java.util.HashMap;


/**
 * Testcases for the SubentryService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubentryServiceITest extends AbstractAdminTestCase
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
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        subentry.put( "subtreeSpecification", "{ base \"ou=configuration\" }" );
        subentry.put( "c-o", "Test Org" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }


    public Attributes getTestSubentryWithExclusion()
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        String spec = "{ base \"ou=configuration\", specificExclusions { chopBefore:\"cn=unmarked\" } }";
        subentry.put( "subtreeSpecification", spec );
        subentry.put( "c-o", "Test Org" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }


    public void addAdministrativeRole( String role ) throws NamingException
    {
        Attribute attribute = new AttributeImpl( "administrativeRole" );
        attribute.add( role );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attribute );
        super.sysRoot.modifyAttributes( "", new ModificationItemImpl[]
            { item } );
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
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        super.sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = marked.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );
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

        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes groups = ( Attributes ) results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

    }


    public void testSubentryModify() throws NamingException
    {
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes groups = ( Attributes ) results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now modify the subentry by introducing an exclusion
        // --------------------------------------------------------------------

        Attribute subtreeSpecification = new AttributeImpl( "subtreeSpecification" );
        subtreeSpecification.add( "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=services\" } }" );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, subtreeSpecification );
        super.sysRoot.modifyAttributes( "cn=testsubentry", new ModificationItemImpl[]
            { item } );
        results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        collectiveAttributeSubentries = configuration.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        groups = ( Attributes ) results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=services,ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }
    }


    public void testSubentryModify2() throws NamingException
    {
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes groups = ( Attributes ) results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now modify the subentry by introducing an exclusion
        // --------------------------------------------------------------------

        Attributes changes = new AttributesImpl();
        changes.put( "subtreeSpecification",
            "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=services\" } }" );
        super.sysRoot.modifyAttributes( "cn=testsubentry", DirContext.REPLACE_ATTRIBUTE, changes );
        results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        collectiveAttributeSubentries = configuration.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        groups = ( Attributes ) results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=services,ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }
    }


    public void testSubentryDelete() throws NamingException
    {
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        super.sysRoot.destroySubcontext( "cn=testsubentry" );

        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=interceptors,ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries
                .size() );
        }

        Attributes partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=partitions,ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }

        Attributes services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=services,ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

    }


    public void testSubentryModifyRdn() throws NamingException
    {
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        super.sysRoot.rename( "cn=testsubentry", "cn=newname" );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes groups = ( Attributes ) results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

    }


    public void testEntryModifyRdn() throws NamingException
    {
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        super.sysRoot.createSubcontext( "cn=unmarked,ou=configuration", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        collectiveAttributeSubentries = marked.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes groups = ( Attributes ) results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes unmarked = ( Attributes ) results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        super.sysRoot.destroySubcontext( "cn=unmarked,ou=configuration" );
        super.sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked,ou=configuration" );
        results = getAllEntries();

        unmarked = ( Attributes ) results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        super.sysRoot.rename( "cn=unmarked,ou=configuration", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=configuration,ou=system" ) );
        marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        collectiveAttributeSubentries = marked.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );
    }


    public void testEntryMoveWithRdnChange() throws NamingException
    {
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        super.sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        collectiveAttributeSubentries = marked.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes groups = ( Attributes ) results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        super.sysRoot.destroySubcontext( "cn=unmarked" );
        super.sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked" );
        results = getAllEntries();

        unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        super.sysRoot.rename( "cn=unmarked", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=system" ) );
        marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        collectiveAttributeSubentries = marked.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );
    }


    public void testEntryMove() throws NamingException
    {
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        super.sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        collectiveAttributeSubentries = marked.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes groups = ( Attributes ) results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        Attributes unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        super.sysRoot.destroySubcontext( "cn=unmarked" );
        super.sysRoot.rename( "cn=marked,ou=configuration", "cn=marked,ou=services,ou=configuration" );
        results = getAllEntries();

        unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        marked = ( Attributes ) results.get( "cn=marked,ou=services,ou=configuration,ou=system" );
        assertNotNull( marked );
        collectiveAttributeSubentries = marked.get( SubentryService.COLLECTIVE_ATTRIBUTE_SUBENTRIES );
        assertNotNull( "cn=marked,ou=services,ou=configuration should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );
    }


    public void testSubentriesControl() throws Exception
    {
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        // perform the search without the control
        Map entries = new HashMap();
        NamingEnumeration list = super.sysRoot.search( "", "(objectClass=*)", searchControls );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            entries.put( result.getName(), result );
        }
        assertTrue( entries.size() > 1 );
        assertNull( entries.get( "cn=testsubentry,ou=system" ) );

        // now add the control with visibility set to true where all entries 
        // except subentries disappear
        SubentriesControl ctl = new SubentriesControl();
        ctl.setVisibility( true );
        super.sysRoot.setRequestControls( new Control[]
            { ctl } );
        list = super.sysRoot.search( "", "(objectClass=*)", searchControls );
        SearchResult result = ( SearchResult ) list.next();
        assertFalse( list.hasMore() );
        assertEquals( "cn=testsubentry,ou=system", result.getName() );
    }
    
    public void testBaseScopeSearchSubentryVisibilityWithoutTheControl() throws Exception
    {
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.OBJECT_SCOPE );

        Map entries = new HashMap();
        NamingEnumeration list = super.sysRoot.search( "cn=testsubentry", "(objectClass=subentry)", searchControls );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            entries.put( result.getName(), result );
        }
        assertEquals( 1, entries.size() );
        assertNotNull( entries.get( "cn=testsubentry,ou=system" ) );
    }
}
