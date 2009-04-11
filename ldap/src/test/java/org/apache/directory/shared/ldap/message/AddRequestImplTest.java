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
package org.apache.directory.shared.ldap.message;


import java.util.HashMap;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import javax.naming.ldap.Control;

import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.message.AbandonListener;
import org.apache.directory.shared.ldap.message.InternalAddRequest;
import org.apache.directory.shared.ldap.message.AddRequestImpl;
import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.InternalResultResponse;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * TestCase for the AddRequestImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public class AddRequestImplTest
{
    private static final Map<String, Control> EMPTY_CONTROL_MAP = new HashMap<String, Control>();
    
    /**
     * Creates and populates a AttributeImpl with a specific id.
     * 
     * @param id
     *            the id for the attribute
     * @return the AttributeImpl assembled for testing
     */
    private EntryAttribute getAttribute( String id )
    {
        EntryAttribute attr = new DefaultClientAttribute( id );
        attr.add( "value0" );
        attr.add( "value1" );
        attr.add( "value2" );
        return attr;
    }


    /**
     * Creates and populates a LockableAttributes object
     * 
     * @return
     */
    private Entry getEntry()
    {
        Entry entry = new DefaultClientEntry();
        
        try
        {
            entry.put( getAttribute( "attr0" ) );
            entry.put( getAttribute( "attr1" ) );
            entry.put( getAttribute( "attr2" ) );
        }
        catch ( NamingException ne )
        {
            // Do nothing
        }
        
        return entry;
    }


    /**
     * Tests the same object referrence for equality.
     */
    @Test
    public void testEqualsSameObj()
    {
        AddRequestImpl req = new AddRequestImpl( 5 );
        assertTrue( req.equals( req ) );
    }


    /**
     * Tests for equality using exact copies.
     */
    @Test
    public void testEqualsExactCopy() throws InvalidNameException, NamingException
    {
        AddRequestImpl req0 = new AddRequestImpl( 5 );
        req0.setEntryDn( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req0.setEntry( getEntry() );

        AddRequestImpl req1 = new AddRequestImpl( 5 );
        req1.setEntryDn( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req1.setEntry( getEntry() );

        assertTrue( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the IDs are different.
     */
    @Test
    public void testNotEqualDiffId() throws InvalidNameException, NamingException
    {
        AddRequestImpl req0 = new AddRequestImpl( 7 );
        req0.setEntryDn( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req0.setEntry( getEntry() );

        AddRequestImpl req1 = new AddRequestImpl( 5 );
        req1.setEntryDn( new LdapDN( "cn=admin,dc=example,dc=com" ) );
        req1.setEntry( getEntry() );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the DN names are different.
     */
    @Test
    public void testNotEqualDiffName() throws InvalidNameException, NamingException
    {
        AddRequestImpl req0 = new AddRequestImpl( 5 );
        req0.setEntry( getEntry() );
        req0.setEntryDn( new LdapDN( "cn=admin,dc=example,dc=com" ) );

        AddRequestImpl req1 = new AddRequestImpl( 5 );
        req1.setEntry( getEntry() );
        req1.setEntryDn( new LdapDN( "cn=admin,dc=apache,dc=org" ) );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the DN names are different.
     */
    @Test
    public void testNotEqualDiffAttributes() throws InvalidNameException, NamingException
    {
        AddRequestImpl req0 = new AddRequestImpl( 5 );
        req0.setEntryDn( new LdapDN( "cn=admin,dc=apache,dc=org" ) );
        req0.setEntry( getEntry() );

        AddRequestImpl req1 = new AddRequestImpl( 5 );
        req1.setEntryDn( new LdapDN( "cn=admin,dc=apache,dc=org" ) );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );

        req1.setEntry( getEntry() );

        assertTrue( req0.equals( req1 ) );
        assertTrue( req1.equals( req0 ) );

        req1.getEntry().put( "asdf", "asdf" );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Tests for equality even when another BindRequest implementation is used.
     */
    @Test
    public void testEqualsDiffImpl()
    {
        InternalAddRequest req0 = new InternalAddRequest()
        {
            public Entry getEntry()
            {
                return AddRequestImplTest.this.getEntry();
            }


            public void setEntry( Entry entry )
            {
            }


            public LdapDN getEntryDn()
            {
                return null;
            }


            public void setEntryDn( LdapDN entryDn )
            {
            }


            public MessageTypeEnum getResponseType()
            {
                return MessageTypeEnum.ADD_RESPONSE;
            }


            public boolean hasResponse()
            {
                return true;
            }


            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.ADD_REQUEST;
            }


            public Map<String, Control> getControls()
            {
                return EMPTY_CONTROL_MAP;
            }


            public void add( javax.naming.ldap.Control control ) throws MessageException
            {
            }


            public void remove( javax.naming.ldap.Control control ) throws MessageException
            {
            }


            public int getMessageId()
            {
                return 5;
            }


            public Object get( Object key )
            {
                return null;
            }


            public Object put( Object key, Object value )
            {
                return null;
            }


            public void abandon()
            {
            }


            public boolean isAbandoned()
            {
                return false;
            }


            public void addAbandonListener( AbandonListener listener )
            {
            }


            public InternalResultResponse getResultResponse()
            {
                return null;
            }


            public void addAll( javax.naming.ldap.Control[] controls ) throws MessageException
            {
            }


            public boolean hasControl( String oid )
            {
                return false;
            }
        };

        AddRequestImpl req1 = new AddRequestImpl( 5 );
        req1.setEntry( getEntry() );
        assertTrue( req1.equals( req0 ) );
    }
}
