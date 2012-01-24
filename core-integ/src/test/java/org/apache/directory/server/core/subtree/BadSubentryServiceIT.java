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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Testcases for the SubentryInterceptor. Investigation on some serious problems.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "BadSubentryServiceIT-class")
public class BadSubentryServiceIT extends AbstractLdapTestUnit
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
        subentry.put( "subtreeSpecification", "{ }" );
        subentry.put( "c-o", "Test Org" );
        subentry.put( "cn", cn );
        return subentry;
    }


    public Attributes getAccessControlTestSubentry( String cn )
    {
        Attributes subentry = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( SchemaConstants.SUBENTRY_OC );
        objectClass.add( "accessControlSubentry" );
        subentry.put( objectClass );
        subentry.put( "subtreeSpecification", "{ }" );
        subentry.put( "prescriptiveACI",
            "{ " +
                "identificationTag \"alllUsersFullAccessACI\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: " +
                "{ " +
                "userClasses " +
                "{ " +
                "allUsers " +
                "}, " +
                "userPermissions " +
                "{ " +
                "{ " +
                "protectedItems " +
                "{ " +
                "entry, allUserAttributeTypesAndValues " +
                "}, " +
                "grantsAndDenials " +
                "{ " +
                "grantAdd, grantDiscloseOnError, grantRead, " +
                "grantRemove, grantBrowse, grantExport, grantImport, " +
                "grantModify, grantRename, grantReturnDN, " +
                "grantCompare, grantFilterMatch, grantInvoke " +
                "} " +
                "} " +
                "} " +
                "} " +
                "} "
            );
        subentry.put( "cn", cn );
        return subentry;
    }


    public void addAdministrativeRoles() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        Attribute attribute = new BasicAttribute( "administrativeRole" );
        attribute.add( "collectiveAttributeSpecificArea" );
        attribute.add( "accessControlSpecificArea" );
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
    public void testTrackingOfSubentryOperationals() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        addAdministrativeRoles();
        sysRoot.createSubcontext( "cn=collectiveAttributeTestSubentry",
            getCollectiveAttributeTestSubentry( "collectiveAttributeTestSubentry" ) );
        sysRoot.createSubcontext( "cn=accessControlTestSubentry",
            getAccessControlTestSubentry( "accessControlTestSubentry" ) );
        sysRoot.createSubcontext( "cn=testEntry", getTestEntry( "testEntry" ) );

        Map<String, Attributes> results = getAllEntries();
        Attributes testEntry = results.get( "cn=testEntry,ou=system" );

        //----------------------------------------------------------------------

        Attribute collectiveAttributeSubentries = testEntry.get( "collectiveAttributeSubentries" );

        assertTrue( collectiveAttributeSubentries.contains( "2.5.4.3=collectiveattributetestsubentry,2.5.4.11=system" ) );

        assertFalse( "'collectiveAttributeSubentries' operational attribute SHOULD NOT " +
            "contain references to non-'collectiveAttributeSubentry's like 'accessControlSubentry's",
            collectiveAttributeSubentries.contains( "2.5.4.3=accesscontroltestsubentry,2.5.4.11=system" ) );

        assertEquals( 1, collectiveAttributeSubentries.size() );

        //----------------------------------------------------------------------

        Attribute accessControlSubentries = testEntry.get( "accessControlSubentries" );

        assertTrue( accessControlSubentries.contains( "2.5.4.3=accesscontroltestsubentry,2.5.4.11=system" ) );

        assertFalse( "'accessControlSubentries' operational attribute SHOULD NOT " +
            "contain references to non-'accessControlSubentry's like 'collectiveAttributeSubentry's",
            accessControlSubentries.contains( "2.5.4.3=collectiveattributetestsubentry,2.5.4.11=system" ) );

        assertEquals( 1, accessControlSubentries.size() );

    }
}
