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
package org.apache.directory.shared.ldap.message;


import junit.framework.TestCase;

import java.util.Collections;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;

import org.apache.directory.shared.ldap.message.Control;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.SearchResponseEntry;
import org.apache.directory.shared.ldap.message.SearchResponseEntryImpl;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Test cases for the methods of the SearchResponseEntryImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev$
 */
public class SearchResponseEntryImplTest extends TestCase
{
    /**
     * Creates and populates a LockableAttributeImpl with a specific id.
     * 
     * @param id
     *            the id for the attribute
     * @return the LockableAttributeImpl assembled for testing
     */
    private LockableAttributeImpl getAttribute( String id )
    {
        LockableAttributeImpl attr = new LockableAttributeImpl( id );
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
    LockableAttributesImpl getAttributes()
    {
        LockableAttributesImpl attrs = new LockableAttributesImpl();
        attrs.put( getAttribute( "attr0" ) );
        attrs.put( getAttribute( "attr1" ) );
        attrs.put( getAttribute( "attr2" ) );
        return attrs;
    }


    /**
     * Tests for equality when the same object referrence is used.
     */
    public void testEqualsSameObject()
    {
        SearchResponseEntryImpl resp = new SearchResponseEntryImpl( 5 );
        assertTrue( "the same object should be equal", resp.equals( resp ) );
    }


    /**
     * Tests for equality when an exact copy is compared.
     */
    public void testEqualsExactCopy() throws InvalidNameException
    {
        SearchResponseEntryImpl resp0 = new SearchResponseEntryImpl( 5 );
        resp0.setAttributes( getAttributes() );
        resp0.setObjectName( new LdapDN( "dc=example,dc=com" ) );

        SearchResponseEntryImpl resp1 = new SearchResponseEntryImpl( 5 );
        resp1.setAttributes( getAttributes() );
        resp1.setObjectName( new LdapDN( "dc=example,dc=com" ) );

        assertTrue( "exact copies should be equal", resp0.equals( resp1 ) );
        assertTrue( "exact copies should be equal", resp1.equals( resp0 ) );
    }


    /**
     * Tests for equality when a different implementation is used.
     */
    public void testEqualsDiffImpl()
    {
        SearchResponseEntry resp0 = new SearchResponseEntry()
        {
            public LdapDN getObjectName()
            {
                try
                {
                    return new LdapDN( "dc=example,dc=com" );
                }
                catch ( InvalidNameException ine )
                {
                    // Do nothing
                    return null;
                }
            }


            public void setObjectName( LdapDN dn )
            {
            }


            public Attributes getAttributes()
            {
                return SearchResponseEntryImplTest.this.getAttributes();
            }


            public void setAttributes( Attributes attributes )
            {
            }


            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.SEARCHRESENTRY;
            }


            public Map getControls()
            {
                return Collections.EMPTY_MAP;
            }


            public void add( Control control ) throws MessageException
            {
            }


            public void remove( Control control ) throws MessageException
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
        };

        SearchResponseEntryImpl resp1 = new SearchResponseEntryImpl( 5 );
        resp1.setAttributes( getAttributes() );
        
        try
        {
            resp1.setObjectName( new LdapDN( "dc=example,dc=com" ) );
        }
        catch ( Exception e )
        {
            // Do nothing
        }

        assertFalse( "using Object.equal() should NOT be equal", resp0.equals( resp1 ) );
        assertTrue( "same but different implementations should be equal", resp1.equals( resp0 ) );
    }


    /**
     * Tests for inequality when the objectName dn is not the same.
     */
    public void testNotEqualDiffObjectName() throws InvalidNameException
    {
        SearchResponseEntryImpl resp0 = new SearchResponseEntryImpl( 5 );
        resp0.setAttributes( getAttributes() );
        resp0.setObjectName( new LdapDN( "dc=apache,dc=org" ) );

        SearchResponseEntryImpl resp1 = new SearchResponseEntryImpl( 5 );
        resp1.setAttributes( getAttributes() );
        resp1.setObjectName( new LdapDN( "dc=example,dc=com" ) );

        assertFalse( "different object names should not be equal", resp1.equals( resp0 ) );
        assertFalse( "different object names should not be equal", resp0.equals( resp1 ) );
    }


    /**
     * Tests for inequality when the attributes are not the same.
     */
    public void testNotEqualDiffAttributes() throws InvalidNameException
    {
        SearchResponseEntryImpl resp0 = new SearchResponseEntryImpl( 5 );
        resp0.setAttributes( getAttributes() );
        resp0.getAttributes().put( "abc", "123" );
        resp0.setObjectName( new LdapDN( "dc=apache,dc=org" ) );

        SearchResponseEntryImpl resp1 = new SearchResponseEntryImpl( 5 );
        resp1.setAttributes( getAttributes() );
        resp1.setObjectName( new LdapDN( "dc=apache,dc=org" ) );

        assertFalse( "different attributes should not be equal", resp1.equals( resp0 ) );
        assertFalse( "different attributes should not be equal", resp0.equals( resp1 ) );
    }
}
