/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.ldap.common;

import java.util.Arrays;
import java.util.Iterator;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotSame;

/**
 * A class to test ServerEntry
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerEntryTest
{
    /**
     * Test a null entry
     */
    @Test public void testCreateNullServerEntry()
    {
        ServerEntry entry = new ServerEntryImpl();
        
        assertNull( entry.getDn() );
        assertEquals( 0, entry.size() );
    }

    
    /**
     * Test an entry with a valid DN and no attributes
     */
    @Test public void testServerEntryDnNoAttribute() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        assertEquals( dn, entry.getDn() );
        assertEquals( 0, entry.size() );
    }


    /**
     * Change an entry's DN
     */
    @Test public void testServerEntryChangeDn() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        LdapDN newDn = new LdapDN( "dc=real, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        entry.setDn( newDn );
        
        assertEquals( newDn, entry.getDn() );
        assertEquals( 0, entry.size() );
    }
    
    
    /**
     * Test an entry with a valid DN and attributes
     */
    @Test public void testServerEntryDnAttributes() 
        throws InvalidNameException, DecoderException, NamingException
    {
        byte[] b1 = StringTools.getBytesUtf8( "test1" );
        byte[] b2 = StringTools.getBytesUtf8( "test2" );

        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        OID oid1 = new OID( "1.2.3" ); 
        OID oid2 = new OID( "1.5.6" ); 
        OID oid3 = new OID( "1.8.9" );
        
        ServerAttribute attr1 = new ServerAttributeImpl( oid1, "1.2.3" );
        attr1.add( "test1" );
        
        ServerAttribute attr2 = new ServerAttributeImpl( oid2, "4.5.6" );
        attr2.add( "test2" );
        
        ServerAttribute attr3 = new ServerAttributeImpl( oid3, b1 );
        attr3.add( b2 );
        
        entry.put(  attr1 );
        entry.put(  attr2 );
        entry.put(  attr3 );
        
        assertEquals( dn, entry.getDn() );
        assertEquals( 3, entry.size() );
        
        ServerAttribute attr = entry.get( oid2 );
        assertNotNull( attr );
        assertEquals( oid2, attr.getOid() );
        
        Iterator<OID> oids = entry.getOids();
        OID[] expected = new OID[]{oid1, oid2, oid3};
        int i = 0;
        
        while ( oids.hasNext() )
        {
            OID oid = oids.next();
            
            assertEquals( expected[i], oid );
        }
    }

    
    /**
     * Test the size method
     */
    @Test public void testSize() 
        throws InvalidNameException, DecoderException, NamingException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        assertEquals( 0, entry.size() );
        
        OID oid1 = new OID( "1.2.3" ); 
        
        ServerAttribute attr1 = new ServerAttributeImpl( oid1, "1.2.3" );

        entry.put( attr1 );
        
        assertEquals( 1, entry.size() );
        assertEquals( 1, entry.get( oid1 ).size() );
        
        // The attribute is not cloned inside the entry
        attr1.add( "test1" );
        
        assertEquals( 1, entry.size() );
        assertEquals( 2, entry.get( oid1 ).size() );
        
        // add a second attribute
        OID oid2 = new OID( "1.2.4" ); 
        
        ServerAttribute attr2 = new ServerAttributeImpl( oid2, "1.2.4" );

        entry.put( attr2 );
        assertEquals( 2, entry.size() );
        
        // Now remove the first attribute
        entry.remove( attr1 );
        assertEquals( 1, entry.size() );
        
        // And remove the second attribute
        entry.remove( oid2 );
        assertEquals( 0, entry.size() );
        
        // Add again both attributes and clear the entry
        entry.put( attr1 );
        entry.put( attr2 );
        
        assertEquals( 2, entry.size() );
        
        entry.clear();

        assertEquals( 0, entry.size() );
    }
    
    
    /**
     * Test the getDn/setDn method
     */
    @Test public void testGetsetDn() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        assertEquals( dn, entry.getDn() );

        ServerEntry entry2 = new ServerEntryImpl();
        
        assertNull( entry2.getDn() );
        
        entry2.setDn( dn );
        
        assertEquals( dn, entry2.getDn() );
    }
    

    /**
     * Test the getOIDs method
     */
    @Test public void testGetOIDs() throws InvalidNameException, DecoderException, NamingException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        OID oid1 = new OID( "1.2.3" ); 
        OID oid2 = new OID( "1.2.4" ); 
        OID oid3 = new OID( "1.2.5" );
        
        OID[] expectedOIDs = new OID[]{ oid1, oid2, oid3 };
        
        ServerAttribute attr1 = new ServerAttributeImpl( oid1, oid1.toString() );
        ServerAttribute attr2 = new ServerAttributeImpl( oid2, oid2.toString() );

        entry.put( attr1 );
        entry.put( attr2 );
        entry.put( oid3, oid3.toString() );
        
        Iterator<OID> iterOids = entry.getOids();
        int i = 0;
        
        while ( iterOids.hasNext() )
        {
            OID oid = iterOids.next();
            assertEquals( expectedOIDs[i], oid );
            i++;
        }
    }

    
    /**
     * Test the clear method
     */
    @Test public void testClear() throws InvalidNameException, DecoderException, NamingException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        OID oid1 = new OID( "1.2.3" ); 
        OID oid2 = new OID( "1.2.4" ); 
        OID oid3 = new OID( "1.2.5" );
        
        entry.put( oid1, oid1.toString() );
        entry.put( oid2, oid2.toString() );
        entry.put( oid3, oid3.toString() );
     
        assertEquals( 3, entry.size() );

        // Now, clear the entry
        entry.clear();
        
        assertEquals( 0, entry.size() );
        assertNull( entry.remove( oid2 ) );
    }

    
    /**
     * Test the clone method
     */
    @Test public void testClone() throws InvalidNameException, DecoderException, NamingException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        OID oid1 = new OID( "1.2.3" ); 
        OID oid2 = new OID( "1.2.4" ); 
        OID oid3 = new OID( "1.2.5" );
        
        // Create three attributes : two StringValues and one BinaryValue
        ServerAttribute attr1 = new ServerAttributeImpl( oid1, oid1.toString() );
        ServerAttribute attr2 = new ServerAttributeImpl( oid2, StringTools.getBytesUtf8( "test" ) );
        ServerAttribute attr3 = new ServerAttributeImpl( oid3, oid2.toString() );
        
        entry.put( attr1 );
        entry.put( attr2 );
        entry.put( attr3 );
     
        assertEquals( 3, entry.size() );

        // Now, clone the entry
        ServerEntry clone = (ServerEntry)entry.clone();
        
        assertEquals( 3, clone.size() );
        assertEquals( attr1, clone.get( oid1 ) );
        assertEquals( attr2, clone.get( oid2 ) );
        assertEquals( attr3, clone.get( oid3 ) );
        
        // Modify the initial attribute
        entry.remove( oid1 );
        assertNull( entry.get( oid1 ) );
        assertEquals( attr1, clone.get( oid1 ) );
        
        // modify the clone
        clone.remove( oid3 );
        assertNull( clone.get( oid3 ) );
        assertEquals( attr3, entry.get( oid3 ) );
        
        // Modify an attribute : the clone should not change
        ServerAttribute attr = entry.get( oid2 );
        ServerAttribute clonedAttr = (ServerAttribute)attr.clone();
        attr.clear();
        
        assertEquals( clonedAttr, clone.get( oid2 ) );
    }    


    /**
     * Test the get method
     */
    @Test public void testGet() throws InvalidNameException, DecoderException, NamingException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        OID oid1 = new OID( "1.2.3" ); 
        OID oid2 = new OID( "1.2.4" ); 
        OID oid3 = new OID( "1.2.5" );
        
        ServerAttribute attr1 = new ServerAttributeImpl( oid1, oid1.toString() );
        ServerAttribute attr2 = new ServerAttributeImpl( oid2, oid2.toString() );
        ServerAttribute attr3 = new ServerAttributeImpl( oid3, oid3.toString() );
        
        entry.put( attr1 );
        entry.put( attr2 );
        entry.put( attr3 );
        
        assertEquals( 3, entry.size() );
        
        assertEquals( attr2, entry.get( oid2 ) );
        
        entry.remove( oid2 );
        assertNull( entry.get(  oid2 ) );
    }

    
    /**
     * Test the getAll method
     */
    @Test public void testGetAll() throws InvalidNameException, DecoderException, NamingException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        OID oid1 = new OID( "1.2.3" ); 
        OID oid2 = new OID( "1.2.4" ); 
        OID oid3 = new OID( "1.2.5" );
        
        ServerAttribute attr1 = new ServerAttributeImpl( oid1, oid1.toString() );
        ServerAttribute attr2 = new ServerAttributeImpl( oid2, oid2.toString() );
        ServerAttribute attr3 = new ServerAttributeImpl( oid3, oid3.toString() );
        
        entry.put( attr1 );
        entry.put( oid2, oid2.toString() );
        entry.put( oid3, "1.2.5" );
        
        ServerAttribute[] expectedAttrs = new ServerAttribute[]{ attr1, attr2, attr3 };
        
        assertEquals( 3, entry.size() );
        Iterator<ServerAttribute> iter = entry.getAll();
        int i = 0;
        
        assertTrue( iter.hasNext() );
        
        while ( iter.hasNext() )
        {
            ServerAttribute attr = iter.next();
            assertEquals( expectedAttrs[i], attr );
            i++;
        }
        
        assertEquals( 3, i );
    }

    /**
     * Test the put methods
     */
    @Test public void testPut() throws InvalidNameException, DecoderException, NamingException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        OID oid1 = new OID( "1.2.3" ); 
        OID oid2 = new OID( "1.2.4" ); 
        OID oid3 = new OID( "1.2.5" );
        OID oid4 = new OID( "1.2.6" );
        
        ServerAttribute attr1 = new ServerAttributeImpl( oid1, oid1.toString() );
        ServerAttribute attr2 = new ServerAttributeImpl( oid2, oid2.toString() );
        ServerAttribute attr3 = new ServerAttributeImpl( oid3, oid3.toString() );
        ServerAttribute attr4 = new ServerAttributeImpl( oid4, oid4.getOID() );
        
        // Check the put( ServerAttribute )
        entry.put( attr1 );
        assertEquals( attr1, entry.get( oid1 ) );
        assertEquals( 1, entry.size() );
        assertEquals( "1.2.3", entry.get( oid1 ).get().getValue() );
        
        // Check the put( OID, Value )
        entry.put( oid2, new StringValue( oid2.toString() ) );
        assertEquals( attr2, entry.get( oid2 ) );
        assertEquals( 2, entry.size() );
        assertEquals( "1.2.4", entry.get( oid2 ).get().getValue() );
        
        // Check the put( OID, String )
        entry.put( oid3, oid3.toString() );
        assertEquals( attr3, entry.get( oid3 ) );
        assertEquals( 3, entry.size() );
        assertEquals( "1.2.5", entry.get( oid3 ).get().getValue() );
        
        // Check the put( OID, byte[] )
        entry.put( oid4, oid4.getOID() );
        assertEquals( attr4, entry.get( oid4 ) );
        assertEquals( 4, entry.size() );
        assertTrue( Arrays.equals( oid4.getOID(), (byte[])entry.get( oid4 ).get().getValue() ) );
    }


    /**
     * Test the remove methods
     */
    @Test public void testRemove() throws InvalidNameException, DecoderException, NamingException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        OID oid1 = new OID( "1.2.3" ); 
        OID oid2 = new OID( "1.2.4" ); 
        OID oid3 = new OID( "1.2.5" );
        OID oid4 = new OID( "1.2.6" );
        
        ServerAttribute attr1 = new ServerAttributeImpl( oid1, oid1.toString() );
        ServerAttribute attr2 = new ServerAttributeImpl( oid2, (String)null );
        ServerAttribute attr3 = new ServerAttributeImpl( oid3, oid3.toString() );
        ServerAttribute attr4 = new ServerAttributeImpl( oid4, oid4.getOID() );
        
        entry.put( attr1 );
        entry.put( attr2 );
        entry.put( attr3 );
        entry.put( attr4 );
        
        assertEquals( 4, entry.size() );
        
        // Test remove(OID)
        ServerAttribute deleted = entry.remove( oid1 );
        
        assertEquals( 3, entry.size() );
        assertEquals( attr1, deleted );
        
        // test remove( ServerAttribute )
        deleted = entry.remove( attr2 );
        
        assertEquals( 2, entry.size() );
        assertEquals( attr2, deleted );
        
        deleted = entry.remove( attr3 );
        
        assertEquals( 1, entry.size() );
        assertEquals( attr3, deleted );

        deleted = entry.remove( attr4 );
        
        assertEquals( 0, entry.size() );
        assertEquals( attr4, deleted );
    }
    

    /**
     * Test the copy methods
     */
    @Test public void testCopy() throws InvalidNameException, DecoderException, NamingException
    {
        LdapDN dn1 = new LdapDN( "dc=example1, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn1 );
        
        OID oid1 = new OID( "1.2.3" ); 
        OID oid2 = new OID( "1.2.4" ); 
        OID oid3 = new OID( "1.2.5" );
        
        ServerAttribute attr1 = new ServerAttributeImpl( oid1, oid1.toString() );
        ServerAttribute attr2 = new ServerAttributeImpl( oid2, (String)null );
        ServerAttribute attr3 = new ServerAttributeImpl( oid3, oid3.getOID() );
        
        assertEquals( 0, entry.size() );
        
        // First, copy a StringValue
        ServerAttribute copy = entry.copy( attr1 );
        assertEquals( 1, entry.size() );
        assertNull( copy );
        
        // Then check that even if we modify the original attribute, the
        // copied value is not changed
        attr1.clear();
        copy = entry.get( oid1 );
        assertNotSame( attr1, copy );
        assertEquals( "1.2.3", copy.get().getValue() );
        
        // Do the same thing with a BinaryValue
        copy = entry.copy( attr3 );
        assertEquals( 2, entry.size() );
        assertNull( copy );
        
        // Then check that even if we modify the original attribute, the
        // copied value is not changed
        ((byte[])attr3.get().getValue())[0] = '0';
        copy = entry.get( oid3 );
        assertNotSame( attr3, copy );
        assertTrue( Arrays.equals( oid3.getOID(), (byte[])copy.get().getValue() ) );
        
        // Do the same thing with a null value
        copy = entry.copy( attr2 );
        assertEquals( 3, entry.size() );
        assertNull( copy );
        
        // Then check that even if we modify the original attribute, the
        // copied value is not changed
        attr2.clear();
        copy = entry.get( oid2 );
        assertNotSame( attr2, copy );
        assertNull( copy.get().getValue() );
    }    


    

    /**
     * Test the equals methods
     */
    @Test public void testEquals() throws InvalidNameException, DecoderException, NamingException
    {
        OID oid1 = new OID( "1.2.3" ); 
        OID oid2 = new OID( "1.2.4" ); 
        
        ServerAttribute attr1 = new ServerAttributeImpl( oid1, oid1.toString() );
        ServerAttribute attr2 = new ServerAttributeImpl( oid2, (String)null );
        
        LdapDN dn1 = new LdapDN( "dc=example1, dc=org" );
        ServerEntry entry1 = new ServerEntryImpl( dn1 );
        
        entry1.put( attr1 );
        entry1.put( attr2 );
        
        LdapDN dn2 = new LdapDN( "dc=example2, dc=org" );
        ServerEntry entry2 = new ServerEntryImpl( dn2 );
        
        entry2.put( attr1 );
        entry2.put( attr2 );
        
        assertEquals( entry1, entry1 );
        assertNotSame( entry1, entry2 );
        
        entry2.setDn( dn1 );
        
        assertEquals( entry1, entry2 );
        
        entry2.clear();
        
        assertNotSame( entry1, entry2 );
        
        ServerEntry clone = entry1.clone();
        
        assertEquals( entry1, clone );
        
    }        
        
}
