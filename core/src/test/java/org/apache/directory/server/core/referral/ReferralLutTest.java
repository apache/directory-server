/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.server.core.referral;


import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.server.core.referral.ReferralLut;
import org.apache.directory.shared.ldap.name.LdapName;

import junit.framework.TestCase;


/**
 * Unit tests for ReferralLut.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReferralLutTest extends TestCase
{
    public void testNullLimits() 
    {
        ReferralLut lut = new ReferralLut();
        try { lut.isReferral( ( String ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.isReferral( ( Name ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.getFarthestReferralAncestor( ( Name ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.getNearestReferralAncestor( ( Name ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.referralAdded( ( Name ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.referralAdded( ( String ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.referralDeleted( ( Name ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.referralDeleted( ( String ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.referralChanged( ( Name ) null, ( Name ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.referralChanged( ( String ) null, ( String ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.referralChanged( ( Name ) null, ( String ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
        try { lut.referralChanged( ( String ) null, ( Name ) null ); fail( "can't get here" ); } catch( IllegalArgumentException e ) {}
    }
    
    
    public void testUpdateOperations() throws NamingException
    {
        String dn = "ou=users,ou=system";
        Name name = new LdapName( dn );
        ReferralLut lut = new ReferralLut();

        // some add delete tests
        assertFalse( lut.isReferral( dn ) );
        lut.referralAdded( dn );
        assertTrue( lut.isReferral( dn ) );
        lut.referralDeleted( dn );
        assertFalse( lut.isReferral( dn ) );

        assertFalse( lut.isReferral( name ) );
        lut.referralAdded( name );
        assertTrue( lut.isReferral( name ) );
        lut.referralDeleted( name );
        assertFalse( lut.isReferral( name ) );

        assertFalse( lut.isReferral( name ) );
        lut.referralAdded( dn );
        assertTrue( lut.isReferral( name ) );
        lut.referralDeleted( name );
        assertFalse( lut.isReferral( name ) );

        assertFalse( lut.isReferral( dn ) );
        lut.referralAdded( name );
        assertTrue( lut.isReferral( dn ) );
        lut.referralDeleted( dn );
        assertFalse( lut.isReferral( dn ) );

        assertFalse( lut.isReferral( name ) );
        lut.referralAdded( dn );
        assertTrue( lut.isReferral( name ) );
        lut.referralDeleted( dn );
        assertFalse( lut.isReferral( name ) );

        assertFalse( lut.isReferral( dn ) );
        lut.referralAdded( name );
        assertTrue( lut.isReferral( dn ) );
        lut.referralDeleted( name );
        assertFalse( lut.isReferral( dn ) );
        
        // change (rename and move) tests
        String newDn = "ou=people,ou=system";
        Name newName = new LdapName( newDn );

        assertFalse( lut.isReferral( dn ) );
        lut.referralAdded( dn );
        assertTrue( lut.isReferral( dn ) );
        lut.referralChanged( dn, newDn );
        assertFalse( lut.isReferral( dn ) );
        assertTrue( lut.isReferral( newDn ) );
        lut.referralDeleted( dn );

        assertFalse( lut.isReferral( name ) );
        lut.referralAdded( name );
        assertTrue( lut.isReferral( name ) );
        lut.referralChanged( name, newName );
        assertFalse( lut.isReferral( name ) );
        assertTrue( lut.isReferral( newName ) );
        lut.referralDeleted( name );

        assertFalse( lut.isReferral( dn ) );
        lut.referralAdded( dn );
        assertTrue( lut.isReferral( dn ) );
        lut.referralChanged( dn, newName );
        assertFalse( lut.isReferral( dn ) );
        assertTrue( lut.isReferral( newDn ) );
        lut.referralDeleted( dn );

        assertFalse( lut.isReferral( dn ) );
        lut.referralAdded( dn );
        assertTrue( lut.isReferral( dn ) );
        lut.referralChanged( name, newDn );
        assertFalse( lut.isReferral( dn ) );
        assertTrue( lut.isReferral( newDn ) );
        lut.referralDeleted( dn );
    }
    
    
    public void testReferralAncestors() throws NamingException
    {
        Name ancestor = new LdapName( "ou=users,ou=system" );
        Name farthest = new LdapName( "ou=system" );
        Name nearest = new LdapName( "ou=apache,ou=users,ou=system" );
        Name testDn = new LdapName( "cn=Alex Karasulu,ou=apache,ou=users,ou=system" );
        ReferralLut lut = new ReferralLut();
        assertNull( lut.getNearestReferralAncestor( testDn ) );
        assertNull( lut.getFarthestReferralAncestor( testDn ) );
        lut.referralAdded( testDn );
        assertNull( lut.getNearestReferralAncestor( testDn ) );
        assertNull( lut.getFarthestReferralAncestor( testDn ) );
        lut.referralDeleted( testDn );
        lut.referralAdded( ancestor );
        assertEquals( ancestor, lut.getNearestReferralAncestor( testDn ) );
        assertEquals( ancestor, lut.getFarthestReferralAncestor( testDn ) );
        lut.referralAdded( testDn );
        assertEquals( ancestor, lut.getNearestReferralAncestor( testDn ) );
        assertEquals( ancestor, lut.getFarthestReferralAncestor( testDn ) );
        lut.referralAdded( nearest );
        assertEquals( nearest, lut.getNearestReferralAncestor( testDn ) );
        assertEquals( ancestor, lut.getFarthestReferralAncestor( testDn ) );
        lut.referralAdded( farthest );
        assertEquals( nearest, lut.getNearestReferralAncestor( testDn ) );
        assertEquals( farthest, lut.getFarthestReferralAncestor( testDn ) );
    }
}
