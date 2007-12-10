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

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.SubentriesControl;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import java.util.HashMap;
import java.util.Map;


/**
 * Testcases for the SubentryInterceptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
@Ignore
public class SubentryServiceIT
{
    public static DirectoryService service;


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
        objectClass.add( SchemaConstants.SUBENTRY_OC );
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
        objectClass.add( SchemaConstants.SUBENTRY_OC );
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
        LdapContext sysRoot = getSystemContext( service );
        Attribute attribute = new AttributeImpl( "administrativeRole" );
        attribute.add( role );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attribute );
        sysRoot.modifyAttributes( "", new ModificationItemImpl[]
            { item } );
    }


    public Map<String, Attributes> getAllEntries() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        Map<String, Attributes> resultMap = new HashMap<String, Attributes>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+", "*" } );
        NamingEnumeration results = sysRoot.search( "", "(objectClass=*)", controls );
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            resultMap.put( result.getName(), result.getAttributes() );
        }
        return resultMap;
    }


    @Test
    public void testEntryAdd() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = marked.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );
    }


    @Test
    public void testSubentryAdd() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        try
        {
            sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
            fail( "should never get here: cannot create subentry under regular entries" );
        }
        catch ( LdapNoSuchAttributeException e )
        {
        }

        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

    }


    @Test
    public void testSubentryModify() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        // --------------------------------------------------------------------
        // Now modify the subentry by introducing an exclusion
        // --------------------------------------------------------------------

        Attribute subtreeSpecification = new AttributeImpl( "subtreeSpecification" );
        subtreeSpecification.add( "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=services\" } }" );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, subtreeSpecification );
        sysRoot.modifyAttributes( "cn=testsubentry", new ModificationItemImpl[]
            { item } );
        results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        configuration = results.get( "ou=configuration,ou=system" );
        collectiveAttributeSubentries = configuration.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=services,ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }
    }


    @Test
    public void testSubentryModify2() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        // --------------------------------------------------------------------
        // Now modify the subentry by introducing an exclusion
        // --------------------------------------------------------------------

        Attributes changes = new AttributesImpl();
        changes.put( "subtreeSpecification",
            "{ base \"ou=configuration\", specificExclusions { chopBefore:\"ou=services\" } }" );
        sysRoot.modifyAttributes( "cn=testsubentry", DirContext.REPLACE_ATTRIBUTE, changes );
        results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        configuration = results.get( "ou=configuration,ou=system" );
        collectiveAttributeSubentries = configuration.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=services,ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }
    }


    @Test
    public void testSubentryDelete() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        sysRoot.destroySubcontext( "cn=testsubentry" );

        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=interceptors,ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries
                .size() );
        }

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=partitions,ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        if ( collectiveAttributeSubentries != null )
        {
            assertEquals( "ou=services,ou=configuration,ou=system should not be marked", 0, collectiveAttributeSubentries.size() );
        }

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

    }


    @Test
    public void testSubentryModifyRdn() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        sysRoot.rename( "cn=testsubentry", "cn=newname" );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=newname,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

    }


    @Test
    public void testEntryModifyRdn() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        sysRoot.createSubcontext( "cn=unmarked,ou=configuration", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        collectiveAttributeSubentries = marked.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        sysRoot.destroySubcontext( "cn=unmarked,ou=configuration" );
        sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked,ou=configuration" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=configuration,ou=system" );
        assertNull( "cn=unmarked,ou=configuration,ou=system should not be marked", unmarked
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        sysRoot.rename( "cn=unmarked,ou=configuration", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=configuration,ou=system" ) );
        marked = results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        collectiveAttributeSubentries = marked.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );
    }


    @Test
    public void testEntryMoveWithRdnChange() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        collectiveAttributeSubentries = marked.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        sysRoot.destroySubcontext( "cn=unmarked" );
        sysRoot.rename( "cn=marked,ou=configuration", "cn=unmarked" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        // --------------------------------------------------------------------
        // Now rename unmarked to marked and see that subentry op attr is there
        // --------------------------------------------------------------------

        sysRoot.rename( "cn=unmarked", "cn=marked,ou=configuration" );
        results = getAllEntries();
        assertNull( results.get( "cn=unmarked,ou=system" ) );
        marked = results.get( "cn=marked,ou=configuration,ou=system" );
        assertNotNull( marked );
        collectiveAttributeSubentries = marked.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );
    }


    @Test
    public void testEntryMove() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map<String, Attributes> results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = results.get( "ou=configuration,ou=system" );
        Attribute collectiveAttributeSubentries = configuration.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes interceptors = results.get( "ou=interceptors,ou=configuration,ou=system" );
        collectiveAttributeSubentries = interceptors.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes partitions = results.get( "ou=partitions,ou=configuration,ou=system" );
        collectiveAttributeSubentries = partitions.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes services = results.get( "ou=services,ou=configuration,ou=system" );
        collectiveAttributeSubentries = services.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        Attributes marked = results.get( "cn=marked,ou=configuration,ou=system" );
        collectiveAttributeSubentries = marked.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=configuration,ou=system should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = results.get( "ou=system" );
        assertNull( "ou=system should not be marked", system.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes users = results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked", users.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes groups = results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked", groups.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes admin = results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked", admin.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes sysPrefRoot = results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked", sysPrefRoot
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        Attributes unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked
            .get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) );

        // --------------------------------------------------------------------
        // Now destry one of the marked/unmarked and rename to deleted entry
        // --------------------------------------------------------------------

        sysRoot.destroySubcontext( "cn=unmarked" );
        sysRoot.rename( "cn=marked,ou=configuration", "cn=marked,ou=services,ou=configuration" );
        results = getAllEntries();

        unmarked = results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked", unmarked );
        assertNull( results.get( "cn=marked,ou=configuration,ou=system" ) );

        marked = results.get( "cn=marked,ou=services,ou=configuration,ou=system" );
        assertNotNull( marked );
        collectiveAttributeSubentries = marked.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        assertNotNull( "cn=marked,ou=services,ou=configuration should be marked", collectiveAttributeSubentries );
        assertEquals( "2.5.4.3=testsubentry,2.5.4.11=system", collectiveAttributeSubentries.get() );
        assertEquals( 1, collectiveAttributeSubentries.size() );
    }


    @Test
    public void testSubentriesControl() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        // perform the search without the control
        Map<String, SearchResult> entries = new HashMap<String, SearchResult>();
        NamingEnumeration list = sysRoot.search( "", "(objectClass=*)", searchControls );
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
        sysRoot.setRequestControls( new Control[] { ctl } );
        list = sysRoot.search( "", "(objectClass=*)", searchControls );
        SearchResult result = ( SearchResult ) list.next();
        assertFalse( list.hasMore() );
        assertEquals( "cn=testsubentry,ou=system", result.getName() );
    }
    

    @Test
    public void testBaseScopeSearchSubentryVisibilityWithoutTheControl() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        addAdministrativeRole( "collectiveArributeSpecificArea" );
        sysRoot.createSubcontext( "cn=testsubentry", getTestSubentryWithExclusion() );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.OBJECT_SCOPE );

        Map<String, SearchResult> entries = new HashMap<String, SearchResult>();
        NamingEnumeration list = sysRoot.search( "cn=testsubentry", "(objectClass=subentry)", searchControls );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            entries.put( result.getName(), result );
        }
        assertEquals( 1, entries.size() );
        assertNotNull( entries.get( "cn=testsubentry,ou=system" ) );
    }
}
