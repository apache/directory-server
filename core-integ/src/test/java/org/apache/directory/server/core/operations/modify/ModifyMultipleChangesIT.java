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
package org.apache.directory.server.core.operations.modify;


import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test case with multiple modifications on a person entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "ModifyMultipleChangesIT")
@ApplyLdifs(
    {
        // Entry # 1
        "dn: cn=Tori Amos,ou=system",
        "objectClass: inetOrgPerson",
        "objectClass: organizationalPerson",
        "objectClass: person",
        "objectClass: top",
        "description: an American singer-songwriter",
        "cn: Tori Amos",
        "sn: Amos",
        // Entry # 2
        "dn: cn=Debbie Harry,ou=system",
        "objectClass: inetOrgPerson",
        "objectClass: organizationalPerson",
        "objectClass: person",
        "objectClass: top",
        "cn: Debbie Harry",
        "sn: Harry"
})
public class ModifyMultipleChangesIT extends AbstractLdapTestUnit
{
private static final String RDN_TORI_AMOS = "cn=Tori Amos";


/**
 * @param sysRoot the system root to add entries to
 * @throws NamingException on errors
 */
protected void createData( LdapContext sysRoot ) throws Exception
{
    // -------------------------------------------------------------------
    // Enable the nis schema
    // -------------------------------------------------------------------
    // check if nis is disabled
    LdapContext schemaRoot = getSchemaContext( getService() );
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
        ModificationItem[] mods = new ModificationItem[]
            {
                new ModificationItem( DirContext.REMOVE_ATTRIBUTE, disabled ) };
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

    // Create a test account
    Attributes test = new BasicAttributes( true );
    Attribute oc = new BasicAttribute( "ObjectClass" );
    oc.add( "top" );
    oc.add( "account" );
    oc.add( "posixAccount" );
    test.put( oc );

    test.put( "cn", "test" );
    test.put( "uid", "1" );
    test.put( "uidNumber", "1" );
    test.put( "gidNumber", "1" );
    test.put( "homeDirectory", "/" );
    test.put( "description", "A test account" );

    getSystemContext( getService() ).createSubcontext( "cn=test", test );
}


/**
 * Create a NIS group
 */
private DirContext addNisPosixGroup( String name, int gid ) throws Exception
{
    Attributes attrs = new BasicAttributes( "objectClass", "top", true );
    attrs.get( "objectClass" ).add( "posixGroup" );
    attrs.put( "cn", name );
    attrs.put( "gidNumber", String.valueOf( gid ) );
    return getSystemContext( getService() ).createSubcontext( "cn=" + name + ",ou=groups", attrs );
}


/**
 * Create a person entry and perform a modify op, in which
 * we modify an attribute two times.
 */
@Test
public void testModifyMultipleChangeDeleteAddSnInMust() throws Exception
{
    LdapContext sysRoot = getSystemContext( getService() );
    createData( sysRoot );

    // Try to delete and add the SN which is in MUST
    ModificationItem[] mods = new ModificationItem[2];

    Attribute snOld = new BasicAttribute( "sn", "Amos" );
    mods[0] = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, snOld );
    Attribute snNew = new BasicAttribute( "sn", "TAmos" );
    mods[1] = new ModificationItem( DirContext.ADD_ATTRIBUTE, snNew );

    sysRoot.modifyAttributes( RDN_TORI_AMOS, mods );

    // Verify that the attribute value has been added
    Attributes attrs = sysRoot.getAttributes( RDN_TORI_AMOS );
    Attribute attr = attrs.get( "sn" );
    assertNotNull( attr );
    assertTrue( attr.contains( snNew.get() ) );
    assertEquals( 1, attr.size() );
}
}
