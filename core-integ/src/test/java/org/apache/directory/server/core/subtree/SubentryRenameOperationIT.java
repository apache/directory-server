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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.directory.server.core.administrative.AdministrativePoint;
import org.apache.directory.server.core.administrative.Subentry;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.AddResponse;
import org.apache.directory.shared.ldap.message.ModifyDnResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the AdministrativePoint interceptor Add operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "AdministrativePointServiceIT")
@ApplyLdifs(
    {
        // An entry used to create a User session
        "dn: cn=testUser,ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: testUser",
        "sn: test User",
        "userpassword: test"
    })
public class SubentryRenameOperationIT extends AbstractSubentryUnitTest
{
    // ===================================================================
    // Test the Rename operation for APs
    // -------------------------------------------------------------------
    // Failure expected
    // -------------------------------------------------------------------
    
    
    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
    
    
    // ===================================================================
    // Test the Add operation for Subentries
    // -------------------------------------------------------------------
    // Failure expected
    // -------------------------------------------------------------------
    /**
     * Test the renaming of a subentry using another AT than CN 
     */
    @Test
    public void testRenameSubentryUsingOC() throws Exception
    {
        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" ); 
        
        // Add a subentry
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}" );

        // Now, try to rename the subentry to "objectClass=subentry, ..."
        ModifyDnResponse response = adminConnection.rename( "cn=test,ou=SAP,ou=system", "objectClass=subentry" );
        
        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the renaming of a subentry to a name already in use 
     */
    @Test
    public void testRenameSubentryAlreadyExist() throws Exception
    {
        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" ); 
        
        // Add a subentry
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}" );

        // Add a second subentry
        createCaSubentry( "cn=test2,ou=SAP,ou=system", "{}" );

        // Now, try to rename the subentry to "sn=test, ..."
        ModifyDnResponse response = adminConnection.rename( "cn=test,ou=SAP,ou=system", "cn=test2" );
        
        assertEquals( ResultCodeEnum.ENTRY_ALREADY_EXISTS, response.getLdapResult().getResultCode() );
    }
    
    
    /**
     * Test the renaming of a subentry using an empty CN 
     */
    @Test
    public void testRenameSubentryEmptyCN() throws Exception
    {
        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" ); 
        
        // Add a subentry
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}" );

        // Now, try to rename the subentry to "cn=,..."
        ModifyDnResponse response = adminConnection.rename( "cn=test,ou=SAP,ou=system", "cn=" );
        
        assertEquals( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, response.getLdapResult().getResultCode() );
    }
    
    
    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
    /**
     * Test the rename of a subentry under an AAP, with 2 roles
     */
    @Test
    public void testRenameSubentryUnderAAP() throws Exception
    {
        DN aapDn = service.getDNFactory().create( "ou=AAP,ou=system" );
        DN oldSubentryDn = service.getDNFactory().create( "cn=test,ou=AAP,ou=system" );
        DN newSubentryDn = service.getDNFactory().create( "cn=test1,ou=AAP,ou=system" );
        
        // First add an AAP
        createAAP( "ou=AAP,ou=system" );
        
        // Add a subentry now
        Entry subentry = LdifUtils.createEntry( 
            "cn=test,ou=AAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: subentry", 
            "ObjectClass: collectiveAttributeSubentry",
            "ObjectClass: accessControlSubentry",
            "cn: test",
            "subtreeSpecification: {}", 
            "c-o: Test Org",
            "prescriptiveACI: { " 
            + "  identificationTag \"addAci\", "
            + "  precedence 14, " 
            + "  authenticationLevel none, " 
            + "  itemOrUserFirst userFirst: " 
            + "  { "
            + "    userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }," 
            + "    userPermissions "
            + "    { " 
            + "      { " 
            + "        protectedItems { entry, allUserAttributeTypesAndValues }, "
            + "        grantsAndDenials { grantCompare, grantRead, grantBrowse } " 
            + "      } " 
            + "    } " 
            + "  } "
            + "}" );

        AddResponse addResponse = adminConnection.add( subentry );
        assertEquals( ResultCodeEnum.SUCCESS, addResponse.getLdapResult().getResultCode() );
        
        long acSeqNumber = getAcSeqNumber( "ou=AAP,ou=system" );
        long caSeqNumber = getCaSeqNumber( "ou=AAP,ou=system" );

        // Check the rename
        ModifyDnResponse renameResponse = adminConnection.rename( "cn=test,ou=AAP,ou=system", "cn=test1" );
        assertEquals( ResultCodeEnum.SUCCESS, renameResponse.getLdapResult().getResultCode() );
        
        // The SeqNumber should not have changed
        assertEquals(acSeqNumber, getAcSeqNumber( "ou=AAP,ou=system" ) );
        assertEquals(caSeqNumber, getCaSeqNumber( "ou=AAP,ou=system" ) );
        
        // The APCache should point to the new subentries
        // First, AC
        AdministrativePoint acAP = service.getAccessControlAPCache().getElement( aapDn );
        
        assertNotNull( acAP );
        Set<Subentry> subentries = acAP.getSubentries();
        
        assertNotNull( subentries );
        
        for ( Subentry sub : subentries )
        {
            assertEquals( "test1", sub.getCn().getString() );
        }
        
        // Then CA
        AdministrativePoint caAP = service.getAccessControlAPCache().getElement( aapDn );
        
        assertNotNull( caAP );
        subentries = caAP.getSubentries();
        
        assertNotNull( subentries );
        
        for ( Subentry sub : subentries )
        {
            assertEquals( "test1", sub.getCn().getString() );
        }
        
        // Now check the UUID cache
        Subentry[] subArray = service.getSubentryCache().getSubentries( oldSubentryDn );
        
        assertNull( subArray );
        
        subArray = service.getSubentryCache().getSubentries( newSubentryDn );
        
        assertNotNull( subArray );

        for ( Subentry sub : subentries )
        {
            if ( sub != null )
            {
                assertEquals( "test1", sub.getCn().getString() );
            }
        }
    }
    
    
    /**
     * Test the rename of a subentry under a SAP
     */
    @Test
    public void testRenameSubentryUnderSAP() throws Exception
    {
        DN sapDn = service.getDNFactory().create( "ou=SAP,ou=system" );
        DN oldSubentryDn = service.getDNFactory().create( "cn=test,ou=SAP,ou=system" );
        DN newSubentryDn = service.getDNFactory().create( "cn=test1,ou=SAP,ou=system" );

        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" ); 
        
        // Add a subentry now
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}" );
        
        long caSeqNumber = getCaSeqNumber( "ou=SAP,ou=system" );

        // Check the rename
        ModifyDnResponse renameResponse = adminConnection.rename( "cn=test,ou=SAP,ou=system", "cn=test1" );
        assertEquals( ResultCodeEnum.SUCCESS, renameResponse.getLdapResult().getResultCode() );
        
        // The SeqNumber should not have changed
        assertEquals(caSeqNumber, getCaSeqNumber( "ou=SAP,ou=system" ) );
        
        // The CA APCache should point to the new subentries
        // Then CA
        AdministrativePoint caAP = service.getCollectiveAttributeAPCache().getElement( sapDn );
        
        assertNotNull( caAP );
        Set<Subentry> subentries = caAP.getSubentries();
        
        assertNotNull( subentries );
        
        for ( Subentry sub : subentries )
        {
            assertEquals( "test1", sub.getCn().getString() );
        }
        
        // Now check the UUID cache
        Subentry[] subArray = service.getSubentryCache().getSubentries( oldSubentryDn );
        
        assertNull( subArray );
        
        subArray = service.getSubentryCache().getSubentries( newSubentryDn );
        
        assertNotNull( subArray );

        for ( Subentry sub : subentries )
        {
            if ( sub != null )
            {
                assertEquals( "test1", sub.getCn().getString() );
            }
        }
    }
    
    
    /**
     * Test the rename of a subentry under a IAP with no reference to a local name
     */
    @Test
    public void testRenameSubentryUnderIAPNoLocalName() throws Exception
    {
        DN iapDn = service.getDNFactory().create( "ou=IAP,ou=SAP,ou=system" );
        DN oldSubentryDn = service.getDNFactory().create( "cn=test,ou=IAP,ou=SAP,ou=system" );
        DN newSubentryDn = service.getDNFactory().create( "cn=test1,ou=IAP,ou=SAP,ou=system" );

        // First add an SAP
        createCaSAP( "ou=SAP,ou=system" ); 
        
        // Add a subentry now, with no localname
        createCaSubentry( "cn=test,ou=SAP,ou=system", "{}" );
        
        long sapCaSeqNumber = getCaSeqNumber( "ou=SAP,ou=system" );
        String sapCaSeUuid = getEntryUuid( "cn=test,ou=SAP,ou=system" );

        // Add an IAP
        createCaIAP( "ou=IAP,ou=SAP,ou=system" ); 

        // Add the associated subentry
        createCaSubentry( "cn=test,ou=IAP,ou=SAP,ou=system", "{}" );
        
        long iapCaSeqNumber = getCaSeqNumber( "ou=IAP,ou=SAP,ou=system" );
        String iapCaSeUuid = getEntryUuid( "cn=test,ou=IAP,ou=SAP,ou=system" );
        
        // Add an entry under the SAP
        Entry e1 = LdifUtils.createEntry( 
            "ou=e1,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: e1" );
        
        createEntryAdmin( e1 );
        
        // Add an entry under the IAP
        Entry e2 = LdifUtils.createEntry( 
            "ou=e2,ou=IAP,ou=SAP,ou=system", 
            "ObjectClass: top",
            "ObjectClass: organizationalUnit", 
            "ou: e2" );
        
        createEntryAdmin( e2 );

        // Check that the first entry refers the first subentry
        assertEquals( sapCaSeUuid, getCaUuidRef( "ou=e1,ou=SAP,ou=system" ) );
        
        // Check that the second entry refers both subentries
        Entry result = adminConnection.lookup( "ou=e2,ou=IAP,ou=SAP,ou=system", "CollectiveAttributeSubentriesUuid" );
        assertNotNull( result );
        EntryAttribute attribute = result.get( "CollectiveAttributeSubentriesUuid" );
        
        assertEquals( 2, attribute.size() );
        assertTrue( attribute.contains( sapCaSeUuid, iapCaSeUuid ) );
        
        // Rename the IAP subentry
        ModifyDnResponse renameResponse = adminConnection.rename( "cn=test,ou=IAP,ou=SAP,ou=system", "cn=test1" );
        assertEquals( ResultCodeEnum.SUCCESS, renameResponse.getLdapResult().getResultCode() );
        
        // The SeqNumber should not have changed
        assertEquals( sapCaSeqNumber, getCaSeqNumber( "ou=SAP,ou=system" ) );
        assertEquals( iapCaSeqNumber, getCaSeqNumber( "ou=IAP,ou=SAP,ou=system" ) );
        
        // The CA APCache should point to the new subentries
        AdministrativePoint caAP = service.getCollectiveAttributeAPCache().getElement( iapDn );
        
        assertNotNull( caAP );
        Set<Subentry> subentries = caAP.getSubentries();
        
        assertNotNull( subentries );
        
        for ( Subentry sub : subentries )
        {
            assertEquals( "test1", sub.getCn().getString() );
        }
        
        // Now check the UUID cache
        Subentry[] subArray = service.getSubentryCache().getSubentries( oldSubentryDn );
        
        assertNull( subArray );
        
        subArray = service.getSubentryCache().getSubentries( newSubentryDn );
        
        assertNotNull( subArray );

        for ( Subentry sub : subentries )
        {
            if ( sub != null )
            {
                assertEquals( "test1", sub.getCn().getString() );
            }
        }
    }
    
    
    // ===================================================================
    // Test the Rename operation for Entries
    // -------------------------------------------------------------------
    // -------------------------------------------------------------------
    // Success expected
    // -------------------------------------------------------------------
}
