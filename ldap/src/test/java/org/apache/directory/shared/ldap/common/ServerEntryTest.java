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

import java.util.Iterator;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
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
        
        assertNull( entry.getDN() );
        assertEquals( 0, entry.size() );
    }

    
    /**
     * Test an entry with a valid DN and no attributes
     */
    @Test public void testServerEntryDnNoAttribute() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        assertEquals( dn, entry.getDN() );
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
        entry.setDN( newDn );
        
        assertEquals( newDn, entry.getDN() );
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
        
        assertEquals( dn, entry.getDN() );
        assertEquals( 3, entry.size() );
        
        ServerAttribute attr = entry.get( oid2 );
        assertNotNull( attr );
        assertEquals( oid2, attr.getOid() );
        
        Iterator<OID> oids = entry.getOIDs();
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
    @Test public void testServerEntrySize() 
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
     * Test the getDN/setDN method
     */
    @Test public void testGetSetDN() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "dc=example, dc=org" );
        
        ServerEntry entry = new ServerEntryImpl( dn );
        
        assertEquals( dn, entry.getDN() );

        ServerEntry entry2 = new ServerEntryImpl();
        
        assertNull( entry2.getDN() );
        
        entry2.setDN( dn );
        
        assertEquals( dn, entry2.getDN() );
    }
}
