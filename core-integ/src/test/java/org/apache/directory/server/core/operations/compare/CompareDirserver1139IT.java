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
package org.apache.directory.server.core.operations.compare;

import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.apache.directory.shared.util.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests for DIRSERVERR-1139
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "CompareDirserver1139IT")
public class CompareDirserver1139IT extends AbstractLdapTestUnit
{
    
    /**
     * Activate the NIS and KRB5KDC schemas
     * @throws Exception
     */
    @Before
    public void init() throws Exception
    {
        // -------------------------------------------------------------------
        // Enable the nis schema
        // -------------------------------------------------------------------
        // check if nis is disabled
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes nisAttrs = schemaRoot.getAttributes( "cn=nis" );
        boolean isNisDisabled = false;
        
        if ( nisAttrs.get( "m-disabled" ) != null )
        {
            isNisDisabled = ( ( String ) nisAttrs.get( "m-disabled" ).get() ).equalsIgnoreCase( "TRUE" );
        }

        // if nis is disabled then enable it
        if ( isNisDisabled )
        {
            Attribute disabled = new BasicAttribute( "m-disabled" );
            ModificationItem[] mods = new ModificationItem[] {
                new ModificationItem( DirContext.REMOVE_ATTRIBUTE, disabled ) };
            schemaRoot.modifyAttributes( "cn=nis", mods );
        }

        // -------------------------------------------------------------------
        // Enable the krb5kdc schema
        // -------------------------------------------------------------------
        // Check if krb5kdc is loaded
        if ( !service.getSchemaManager().isSchemaLoaded( "krb5kdc" ) )
        {
            service.getSchemaManager().load( "krb5kdc" );
        }

        // check if krb5kdc is disabled
        Attributes krb5kdcAttrs = schemaRoot.getAttributes( "cn=krb5kdc" );
        boolean isKrb5kdcDisabled = false;
        
        if ( krb5kdcAttrs.get( "m-disabled" ) != null )
        {
            isKrb5kdcDisabled = ( ( String ) krb5kdcAttrs.get( "m-disabled" ).get() ).equalsIgnoreCase( "TRUE" );
        }

        // if krb5kdc is disabled then enable it
        if ( isKrb5kdcDisabled )
        {
            Attribute disabled = new BasicAttribute( "m-disabled" );
            ModificationItem[] mods = new ModificationItem[] {
                new ModificationItem( DirContext.REMOVE_ATTRIBUTE, disabled ) };
            schemaRoot.modifyAttributes( "cn=krb5kdc", mods );
        }
    }
    
    
    /**
     * Inject entries into the server
     */
    private void injectEntries( LdapContext sysRoot ) throws Exception
    {
        // Add the group
        Attributes attrs = LdifUtils.createAttributes( 
            "ObjectClass: top",
            "ObjectClass: groupOfNames",
            "cn: group",
            "member: cn=user,ou=users,ou=system" );
        
        sysRoot.createSubcontext( "cn=group,ou=groups", attrs );
        
        // Add the user
        attrs = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass: organizationalPerson",
            "objectClass: person",
            "objectClass: krb5Principal",
            "objectClass: posixAccount",
            "objectClass: shadowAccount",
            "objectClass: krb5KDCEntry",
            "objectClass: inetOrgPerson",
            "cn: user",
            "gidnumber: 100",
            "givenname: user",
            "homedirectory: /home/users/user",
            "krb5KeyVersionNumber: 1",
            "krb5PrincipalName: user@APACHE.ORG",
            "loginshell: /bin/bash",
            "mail: user@apache.org",
            "sn: User",
            "uid: user",
            "uidnumber: 1001" );
        
        sysRoot.createSubcontext( "cn=user,ou=users", attrs );
    }
    
    
    /**
     * Compare a member attribute. This test is used to check DIRSERVER-1139
     */
    @Test
    public void testCompare() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        
        injectEntries( sysRoot);

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes(  new String[0] );

        NamingEnumeration<SearchResult> list = 
            sysRoot.search( "cn=group,ou=groups", "(member=cn=user,ou=users,ou=system)", controls );
        
        int count = 0;
        
        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            assertNotNull( result );
            assertTrue( Strings.isEmpty(result.getName()) );
            assertNotNull( result.getAttributes() );
            assertEquals( 0, result.getAttributes().size() );
            count++;
        }
        
        assertEquals( 1, count );
    }

}
