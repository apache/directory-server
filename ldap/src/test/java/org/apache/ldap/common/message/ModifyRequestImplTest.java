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
package org.apache.ldap.common.message;


import junit.framework.TestCase;

import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;


/**
 * Test case for the ModifyRequestImpl class.
 *
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory
 *         Project</a>
 * @version $Rev$
 */
public class ModifyRequestImplTest extends TestCase
{
    /**
     * Builds a ModifyRequest for testing purposes.
     *
     * @return the ModifyRequest to use for tests
     */
    public ModifyRequestImpl getRequest()
    {
        // Construct the Modify request to test
        ModifyRequestImpl req = new ModifyRequestImpl( 45 );
        req.setName( "cn=admin,dc=apache,dc=org" );

        LockableAttributeImpl attr = new LockableAttributeImpl( "attr0" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        ModificationItem item =
                new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        req.addModification( item );

        attr = new LockableAttributeImpl( "attr1" );
        attr.add( "val3" );
        item = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, attr );
        req.addModification( item );

        attr = new LockableAttributeImpl( "attr2" );
        attr.add( "val4" );
        attr.add( "val5" );
        item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        req.addModification( item );

        return req;
    }


    /**
     * Tests the same object referrence for equality.
     */
    public void testEqualsSameObj()
    {
        ModifyRequestImpl req = getRequest();
        assertTrue( req.equals( req ) );
    }


    /**
     * Tests for equality using exact copies.
     */
    public void testEqualsExactCopy()
    {
        ModifyRequestImpl req0 = getRequest();
        ModifyRequestImpl req1 = getRequest();
        assertTrue( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the IDs are different.
     */
    public void testNotEqualDiffId()
    {
        ModifyRequestImpl req0 = new ModifyRequestImpl( 7 );
        ModifyRequestImpl req1 = new ModifyRequestImpl( 5 );
        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the DN names are different.
     */
    public void testNotEqualDiffName()
    {
        ModifyRequestImpl req0 = getRequest();
        req0.setName( "cn=admin,dc=example,dc=com" );
        ModifyRequestImpl req1 = getRequest();
        req1.setName( "cn=admin,dc=apache,dc=org" );

        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the mods ops are different.
     */
    public void testNotEqualDiffModOps()
    {
        ModifyRequestImpl req0 = getRequest();
        LockableAttributeImpl attr = new LockableAttributeImpl( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        ModificationItem item =
                new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        ModifyRequestImpl req1 = getRequest();
        attr = new LockableAttributeImpl( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        item = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, attr );
        req0.addModification( item );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Test for inequality when only the number of mods are different.
     */
    public void testNotEqualDiffModCount()
    {
        ModifyRequestImpl req0 = getRequest();
        LockableAttributeImpl attr = new LockableAttributeImpl( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        ModificationItem item =
                new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        ModifyRequestImpl req1 = getRequest();

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Test for inequality when only the mods attribute Id's are different.
     */
    public void testNotEqualDiffModIds()
    {
        ModifyRequestImpl req0 = getRequest();
        LockableAttributeImpl attr = new LockableAttributeImpl( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        ModificationItem item =
                new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        ModifyRequestImpl req1 = getRequest();
        attr = new LockableAttributeImpl( "attr4" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        item = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Test for inequality when only the mods attribute values are different.
     */
    public void testNotEqualDiffModValues()
    {
        ModifyRequestImpl req0 = getRequest();
        LockableAttributeImpl attr = new LockableAttributeImpl( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        ModificationItem item =
                new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        ModifyRequestImpl req1 = getRequest();
        attr = new LockableAttributeImpl( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        attr.add( "val3" );
        item = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Tests for equality even when another BindRequest implementation is used.
     */
    public void testEqualsDiffImpl()
    {
        ModifyRequest req0 = new ModifyRequest()
        {
            public Collection getModificationItems()
            {
                ArrayList list = new ArrayList();
                LockableAttributeImpl attr = new LockableAttributeImpl( "attr0" );
                attr.add( "val0" );
                attr.add( "val1" );
                attr.add( "val2" );
                ModificationItem item =
                        new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );
                list.add( item );

                attr = new LockableAttributeImpl( "attr1" );
                attr.add( "val3" );
                item = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, attr );
                list.add( item );

                attr = new LockableAttributeImpl( "attr2" );
                attr.add( "val4" );
                attr.add( "val5" );
                item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
                list.add( item );

                return list;
            }

            public void addModification( ModificationItem a_mod )
            {
            }

            public void removeModification( ModificationItem a_mod )
            {
            }

            public String getName()
            {
                return "cn=admin,dc=apache,dc=org";
            }

            public void setName( String a_name )
            {
            }

            public MessageTypeEnum getResponseType()
            {
                return MessageTypeEnum.MODIFYRESPONSE;
            }

            public boolean hasResponse()
            {
                return true;
            }

            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.MODIFYREQUEST;
            }

            public Map getControls()
            {
                return Collections.EMPTY_MAP;
            }

            public void add( Control a_control ) throws MessageException
            {
            }

            public void remove( Control a_control ) throws MessageException
            {
            }

            public int getMessageId()
            {
                return 45;
            }

            public Object get( Object a_key )
            {
                return null;
            }

            public Object put( Object a_key, Object a_value )
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

            public void addAbandonListener(AbandonListener listener)
            {
            }

            public ResultResponse getResultResponse()
            {
                return null;
            }
        };

        ModifyRequestImpl req1 = getRequest();
        assertTrue( req1.equals( req0 ) );
    }
}
