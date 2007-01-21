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


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;


/**
 * Testcases for the SubentryService. Investigation on some serious problems.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BadSubentryServiceITest extends AbstractAdminTestCase
{
    public Attributes getTestEntry( String cn )
    {
        Attributes entry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "person" );
        entry.put( objectClass );
        entry.put( "cn", cn );
        entry.put( "sn", cn );
        return entry;
    }


    public Attributes getCollectiveAttributeTestSubentry( String cn )
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        subentry.put( "subtreeSpecification", "{ }" );
        subentry.put( "c-o", "Test Org" );
        subentry.put( "cn", cn );
        return subentry;
    }
    
    
    public Attributes getAccessControlTestSubentry( String cn )
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
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


    public void addAdministrativeRoles() throws NamingException
    {
        Attribute attribute = new AttributeImpl( "administrativeRole" );
        attribute.add( "autonomousArea" );
        attribute.add( "collectiveAttributeSpecificArea" );
        attribute.add( "accessControlSpecificArea" );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attribute );
        super.sysRoot.modifyAttributes( "", new ModificationItemImpl[] { item } );
    }


    public Map<String, Attributes> getAllEntries() throws NamingException
    {
        Map<String, Attributes> resultMap = new HashMap<String, Attributes>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[] { "+", "*" } );
        NamingEnumeration results = super.sysRoot.search( "", "(objectClass=*)", controls );
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            resultMap.put( result.getName(), result.getAttributes() );
        }
        return resultMap;
    }
    
    /*
     * FIXME: The test fails badly.
     */
    
    public void testTrackingOfSubentryOperationals() throws NamingException
    {
        
        addAdministrativeRoles();        
        super.sysRoot.createSubcontext( "cn=collectiveAttributeTestSubentry", 
            getCollectiveAttributeTestSubentry( "collectiveAttributeTestSubentry" ) );
        super.sysRoot.createSubcontext( "cn=accessControlTestSubentry", 
            getAccessControlTestSubentry( "accessControlTestSubentry" ) );
        super.sysRoot.createSubcontext( "cn=testEntry", getTestEntry( "testEntry" ) );
        
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
