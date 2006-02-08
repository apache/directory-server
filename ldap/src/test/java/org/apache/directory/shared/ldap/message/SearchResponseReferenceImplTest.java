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

import org.apache.directory.shared.ldap.message.Control;
import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.Referral;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.SearchResponseReference;
import org.apache.directory.shared.ldap.message.SearchResponseReferenceImpl;


/**
 * TestCase for the SearchResponseReferenceImpl class.
 *
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory
 *         Project</a>
 * @version $Rev$
 */
public class SearchResponseReferenceImplTest extends TestCase
{
    /**
     * Creates a baseline referral to test with and adds it to the supplied
     * response object.
     *
     * @param resp the parent lockable
     * @return the newly created referral for testing
     */
    public Referral getReferral( SearchResponseReference resp )
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
    public void testEqualsSameObject()
    {
        SearchResponseReferenceImpl resp = new SearchResponseReferenceImpl( 5 );
        getReferral( resp );
        assertTrue( "the same object should be equal", resp.equals( resp ) );
    }


    /**
     * Tests for equality when an exact copy is compared.
     */
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
    public void testEqualsDiffImpl()
    {
        SearchResponseReference resp0 = new SearchResponseReference()
        {
            public Referral getReferral()
            {
                return SearchResponseReferenceImplTest.this.getReferral( this );
            }

            public void setReferral( Referral a_referral )
            {
            }

            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.SEARCHRESREF;
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

        SearchResponseReferenceImpl resp1 =
                new SearchResponseReferenceImpl( 5 );
        getReferral( resp1 );

        assertFalse( "using Object.equal() should NOT be equal",
                resp0.equals( resp1 ) );
        assertTrue( "same but different implementations should be equal",
                resp1.equals( resp0 ) );
    }


    /**
     * Tests for inequality when the urls are not the same.
     */
    public void testNotEqualDiffUrls()
    {
        SearchResponseReferenceImpl resp0 =
                new SearchResponseReferenceImpl( 5 );
        getReferral( resp0 );
        SearchResponseReferenceImpl resp1 =
                new SearchResponseReferenceImpl( 5 );
        getReferral( resp1 );
        resp1.getReferral().addLdapUrl( "ldap://asdf.com???" );

        assertFalse( "different urls should not be equal",
                resp1.equals( resp0 ) );
        assertFalse( "different urls should not be equal",
                resp0.equals( resp1 ) );
    }

}
