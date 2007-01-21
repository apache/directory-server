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
package org.apache.directory.server.core;


import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.partition.impl.btree.MutableIndexConfiguration;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;


/**
 * Tests various search scenarios.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchOpsITest extends AbstractAdminTestCase
{
    private DirContext addNisPosixGroup( String name, int gid ) throws NamingException
    {
        Attributes attrs = new AttributesImpl( "objectClass", "top", true );
        attrs.get( "objectClass" ).add( "posixGroup" );
        attrs.put( "cn", name );
        attrs.put( "gidNumber", String.valueOf( gid ) );
        return sysRoot.createSubcontext( "cn="+name+",ou=groups", attrs );
    }
    
    
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception
    {
        // -------------------------------------------------------------------
        // Alter the partition configuration to index gidNumber
        // -------------------------------------------------------------------

        if ( getName().indexOf( "WithIndices" ) != -1 )
        {
            MutablePartitionConfiguration sysConf = new MutablePartitionConfiguration();
            sysConf.setName( "system" );
            Attributes attrs = new AttributesImpl( "objectClass", "top", true );
            attrs.get( "objectClass" ).add( "organizationalUnit" );
            attrs.put( "ou", "system" );
            sysConf.setContextEntry( attrs );
            sysConf.setSuffix( "ou=system" );
            Set indices = new HashSet();
            indices.addAll( sysConf.getIndexedAttributes() );
            MutableIndexConfiguration idxConfig = new MutableIndexConfiguration();
            idxConfig.setAttributeId( "gidNumber" );
            indices.add( idxConfig );
            sysConf.setIndexedAttributes( indices );
            configuration.setSystemPartitionConfiguration( sysConf );
        }
        
        super.setUp();

        // -------------------------------------------------------------------
        // Enable the nis schema
        // -------------------------------------------------------------------

        // check if nis is disabled
        Attributes nisAttrs = schemaRoot.getAttributes( "cn=nis" );
        boolean isNisDisabled = false;
        if ( nisAttrs.get( "m-disabled" ) != null )
        {
            isNisDisabled = ( ( String ) nisAttrs.get( "m-disabled" ).get() ).equalsIgnoreCase( "TRUE" );
        }
        
        // if nis is disabled then enable it
        if ( isNisDisabled )
        {
            Attribute disabled = new AttributeImpl( "m-disabled" );
            ModificationItemImpl[] mods = new ModificationItemImpl[] { 
                new ModificationItemImpl( DirContext.REMOVE_ATTRIBUTE, disabled ) };
            schemaRoot.modifyAttributes( "cn=nis", mods );
        }
        
        // -------------------------------------------------------------------
        // Add a bunch of nis groups
        // -------------------------------------------------------------------
        
        addNisPosixGroup( "testGroup0", 0 );
        addNisPosixGroup( "testGroup1", 1 );
        addNisPosixGroup( "testGroup2", 2 );
        addNisPosixGroup( "testGroup4", 4 );
        addNisPosixGroup( "testGroup5", 5 );
    }
    
    
    /**
     *  Convenience method that performs a one level search using the 
     *  specified filter returning their DNs as Strings in a set.
     */
    public Set searchGroups( String filter ) throws NamingException
    {
        Set<String> results = new HashSet<String>();
        NamingEnumeration list = sysRoot.search( "ou=groups", filter, new SearchControls() );
        while( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            results.add( result.getName() );
        }
        return results;
    }
    
    
    public void testSetup() throws Exception
    {
        Set results = searchGroups( "(objectClass=posixGroup)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }

    
    public void testLessThanSearch() throws Exception
    {
        Set results = searchGroups( "(gidNumber<=5)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=4)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=3)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=0)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=-1)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }

    
    public void testGreaterThanSearch() throws Exception
    {
        Set results = searchGroups( "(gidNumber>=0)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=1)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=3)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=6)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }

    
    public void testLessThanSearchWithIndices() throws Exception
    {
        Set results = searchGroups( "(gidNumber<=5)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=4)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=3)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=0)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=-1)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }

    
    public void testGreaterThanSearchWithIndices() throws Exception
    {
        Set results = searchGroups( "(gidNumber>=0)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=1)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=3)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=6)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }
}
