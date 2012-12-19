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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Testcases for the SubentryInterceptor. Investigation on handling Subtree Refinement
 * Selection Membership upon objectClass attribute value changes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(factory = DefaultDirectoryServiceFactory.class, name = "SubentryServiceObjectClassChangeHandlingIT-class")
public class SubentryServiceObjectClassChangeHandlingIT extends AbstractLdapTestUnit
{

    public Attributes getTestEntry( String cn )
    {
        Attributes entry = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "person" );
        entry.put( objectClass );
        entry.put( "cn", cn );
        entry.put( "sn", cn );
        return entry;
    }


    public Attributes getCollectiveAttributeTestSubentry( String cn )
    {
        Attributes subentry = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( SchemaConstants.SUBENTRY_OC );
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        subentry.put( "subtreeSpecification", "{ specificationFilter item:organizationalPerson }" );
        subentry.put( "c-o", "Test Org" );
        subentry.put( "cn", cn );
        return subentry;
    }


    public void addAdministrativeRoles() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        Attribute attribute = new BasicAttribute( "administrativeRole" );
        attribute.add( "collectiveAttributeSpecificArea" );
        ModificationItem item = new ModificationItem( DirContext.ADD_ATTRIBUTE, attribute );
        sysRoot.modifyAttributes( "", new ModificationItem[]
            { item } );
    }


    public Map<String, Attributes> getAllEntries() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
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
    public void testTrackingOfOCChangesInSubentryServiceModifyRoutine() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        addAdministrativeRoles();
        sysRoot.createSubcontext( "cn=collectiveAttributeTestSubentry",
            getCollectiveAttributeTestSubentry( "collectiveAttributeTestSubentry" ) );
        Attributes entry = getTestEntry( "testEntry" );
        sysRoot.createSubcontext( "cn=testEntry", entry );

        //----------------------------------------------------------------------

        Map<String, Attributes> results = getAllEntries();
        Attributes testEntry = results.get( "cn=testEntry,ou=system" );

        Attribute collectiveAttributeSubentries = testEntry.get( "collectiveAttributeSubentries" );

        assertNull( collectiveAttributeSubentries );

        //----------------------------------------------------------------------
        ModificationItem[] modItems = new ModificationItem[2];
        Attribute objectClass = new BasicAttribute( "ObjectClass", "organizationalPerson" );
        modItems[0] = new ModificationItem( DirContext.ADD_ATTRIBUTE, objectClass );
        Attribute ou = new BasicAttribute( "ou", "Test Organizational Unit" );
        modItems[1] = new ModificationItem( DirContext.ADD_ATTRIBUTE, ou );

        sysRoot.modifyAttributes( "cn=testEntry", modItems );

        results = getAllEntries();
        testEntry = ( Attributes ) results.get( "cn=testEntry,ou=system" );

        collectiveAttributeSubentries = testEntry.get( "collectiveAttributeSubentries" );
        assertNotNull( collectiveAttributeSubentries );
    }

}
