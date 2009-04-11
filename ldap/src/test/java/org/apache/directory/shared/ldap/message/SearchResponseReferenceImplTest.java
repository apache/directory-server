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

import javax.naming.ldap.Control;

import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.InternalReferral;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.InternalSearchResponseReference;
import org.apache.directory.shared.ldap.message.SearchResponseReferenceImpl;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * TestCase for the SearchResponseReferenceImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchResponseReferenceImplTest
{
    private static final Map<String, Control> EMPTY_CONTROL_MAP = new HashMap<String, Control>();

    /**
     * Creates a baseline referral to test with and adds it to the supplied
     * response object.
     * 
     * @param resp
     *            the parent lockable
     * @return the newly created referral for testing
     */
    private InternalReferral getReferral( InternalSearchResponseReference resp )
    {
        ReferralImpl ref = new ReferralImpl();
        resp.setReferral( ref );
        ref.addLdapUrl( "http://apache.org???" );
        ref.addLdapUrl( "http://mit.edu???" );
        ref.addLdapUrl( "http://abc.com???" );
        return ref;
    }


    /**
     * Tests for equality when the same object referrence is used.
     */
    @Test
    public void testEqualsSameObject()
    {
        SearchResponseReferenceImpl resp = new SearchResponseReferenceImpl( 5 );
        getReferral( resp );
        assertTrue( "the same object should be equal", resp.equals( resp ) );
    }


    /**
     * Tests for equality when an exact copy is compared.
     */
    @Test
    public void testEqualsExactCopy()
    {
        SearchResponseReferenceImpl resp0 = new SearchResponseReferenceImpl( 5 );
        getReferral( resp0 );
        SearchResponseReferenceImpl resp1 = new SearchResponseReferenceImpl( 5 );
        getReferral( resp1 );

        assertTrue( "exact copies should be equal", resp0.equals( resp1 ) );
        assertTrue( "exact copies should be equal", resp1.equals( resp0 ) );
    }


    /**
     * Tests for equality when a different implementation is used.
     */
    @Test
    public void testEqualsDiffImpl()
    {
        InternalSearchResponseReference resp0 = new InternalSearchResponseReference()
        {
            public InternalReferral getReferral()
            {
                return SearchResponseReferenceImplTest.this.getReferral( this );
            }


            public void setReferral( InternalReferral a_referral )
            {
            }


            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.SEARCH_RES_REF;
            }


            public Map<String, Control> getControls()
            {
                return EMPTY_CONTROL_MAP;
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


            public void addAll( Control[] controls ) throws MessageException
            {
            }


            public boolean hasControl( String oid )
            {
                return false;
            }
        };

        SearchResponseReferenceImpl resp1 = new SearchResponseReferenceImpl( 5 );
        getReferral( resp1 );

        assertFalse( "using Object.equal() should NOT be equal", resp0.equals( resp1 ) );
        assertTrue( "same but different implementations should be equal", resp1.equals( resp0 ) );
    }


    /**
     * Tests for inequality when the urls are not the same.
     */
    @Test
    public void testNotEqualDiffUrls()
    {
        SearchResponseReferenceImpl resp0 = new SearchResponseReferenceImpl( 5 );
        getReferral( resp0 );
        SearchResponseReferenceImpl resp1 = new SearchResponseReferenceImpl( 5 );
        getReferral( resp1 );
        resp1.getReferral().addLdapUrl( "ldap://asdf.com???" );

        assertFalse( "different urls should not be equal", resp1.equals( resp0 ) );
        assertFalse( "different urls should not be equal", resp0.equals( resp1 ) );
    }

}
