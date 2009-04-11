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


import javax.naming.InvalidNameException;
import javax.naming.ldap.Control;

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.message.AbandonListener;
import org.apache.directory.shared.ldap.message.MessageException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.InternalModifyRequest;
import org.apache.directory.shared.ldap.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.message.InternalResultResponse;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Test case for the ModifyRequestImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public class ModifyRequestImplTest
{
    private static final Map<String, Control> EMPTY_CONTROL_MAP = new HashMap<String, Control>();

    /**
     * Builds a ModifyRequest for testing purposes.
     * 
     * @return the ModifyRequest to use for tests
     */
    private ModifyRequestImpl getRequest()
    {
        // Construct the Modify request to test
        ModifyRequestImpl req = new ModifyRequestImpl( 45 );
        
        try 
        {
            req.setName( new LdapDN( "cn=admin,dc=apache,dc=org" ) );
        }
        catch ( InvalidNameException ne )
        {
            // do nothing
        }

        EntryAttribute attr = new DefaultClientAttribute( "attr0" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        Modification item = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
        req.addModification( item );

        attr = new DefaultClientAttribute( "attr1" );
        attr.add( "val3" );
        item = new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, attr );
        req.addModification( item );

        attr = new DefaultClientAttribute( "attr2" );
        attr.add( "val4" );
        attr.add( "val5" );
        item = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE, attr );
        req.addModification( item );

        return req;
    }


    /**
     * Tests the same object referrence for equality.
     */
    @Test
    public void testEqualsSameObj()
    {
        ModifyRequestImpl req = getRequest();
        assertTrue( req.equals( req ) );
    }


    /**
     * Tests for equality using exact copies.
     */
    @Test
    public void testEqualsExactCopy()
    {
        ModifyRequestImpl req0 = getRequest();
        ModifyRequestImpl req1 = getRequest();
        assertTrue( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the IDs are different.
     */
    @Test
    public void testNotEqualDiffId()
    {
        ModifyRequestImpl req0 = new ModifyRequestImpl( 7 );
        ModifyRequestImpl req1 = new ModifyRequestImpl( 5 );
        assertFalse( req0.equals( req1 ) );
    }


    /**
     * Test for inequality when only the DN names are different.
     */
    @Test
    public void testNotEqualDiffName()
    {
        try
        {
            ModifyRequestImpl req0 = getRequest();
            req0.setName( new LdapDN( "cn=admin,dc=example,dc=com" ) );
            ModifyRequestImpl req1 = getRequest();
            req1.setName( new LdapDN( "cn=admin,dc=apache,dc=org" ) );

            assertFalse( req0.equals( req1 ) );
        }
        catch ( InvalidNameException ine )
        {
            // do nothing
        }
    }


    /**
     * Test for inequality when only the mods ops are different.
     */
    @Test
    public void testNotEqualDiffModOps()
    {
        ModifyRequestImpl req0 = getRequest();
        EntryAttribute attr = new DefaultClientAttribute( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        Modification item = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        ModifyRequestImpl req1 = getRequest();
        attr = new DefaultClientAttribute( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        item = new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, attr );
        req0.addModification( item );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Test for inequality when only the number of mods are different.
     */
    @Test
    public void testNotEqualDiffModCount()
    {
        ModifyRequestImpl req0 = getRequest();
        EntryAttribute attr = new DefaultClientAttribute( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        Modification item = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        ModifyRequestImpl req1 = getRequest();

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Test for inequality when only the mods attribute Id's are different.
     */
    @Test
    public void testNotEqualDiffModIds()
    {
        ModifyRequestImpl req0 = getRequest();
        EntryAttribute attr = new DefaultClientAttribute( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        Modification item = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        ModifyRequestImpl req1 = getRequest();
        attr = new DefaultClientAttribute( "attr4" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        item = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Test for inequality when only the mods attribute values are different.
     */
    @Test
    public void testNotEqualDiffModValues()
    {
        ModifyRequestImpl req0 = getRequest();
        EntryAttribute attr = new DefaultClientAttribute( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        Modification item = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        ModifyRequestImpl req1 = getRequest();
        attr = new DefaultClientAttribute( "attr3" );
        attr.add( "val0" );
        attr.add( "val1" );
        attr.add( "val2" );
        attr.add( "val3" );
        item = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
        req0.addModification( item );

        assertFalse( req0.equals( req1 ) );
        assertFalse( req1.equals( req0 ) );
    }


    /**
     * Tests for equality even when another BindRequest implementation is used.
     */
    @Test
    public void testEqualsDiffImpl()
    {
        InternalModifyRequest req0 = new InternalModifyRequest()
        {
            public Collection<Modification> getModificationItems()
            {
                List<Modification> list = new ArrayList<Modification>();
                EntryAttribute attr = new DefaultClientAttribute( "attr0" );
                attr.add( "val0" );
                attr.add( "val1" );
                attr.add( "val2" );
                Modification item = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
                list.add( item );

                attr = new DefaultClientAttribute( "attr1" );
                attr.add( "val3" );
                item = new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, attr );
                list.add( item );

                attr = new DefaultClientAttribute( "attr2" );
                attr.add( "val4" );
                attr.add( "val5" );
                item = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE, attr );
                list.add( item );

                return list;
            }


            public void addModification( Modification mod )
            {
            }


            public void removeModification( Modification mod )
            {
            }


            public LdapDN getName()
            {
                try
                {
                    return new LdapDN( "cn=admin,dc=apache,dc=org" );
                }
                catch ( Exception e )
                {
                    //do nothing
                    return null;
                }
            }


            public void setName( LdapDN name )
            {
            }


            public MessageTypeEnum getResponseType()
            {
                return MessageTypeEnum.MODIFY_RESPONSE;
            }


            public boolean hasResponse()
            {
                return true;
            }


            public MessageTypeEnum getType()
            {
                return MessageTypeEnum.MODIFY_REQUEST;
            }


            public Map<String, Control> getControls()
            {
                return EMPTY_CONTROL_MAP;
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


            public void addAbandonListener( AbandonListener listener )
            {
            }


            public InternalResultResponse getResultResponse()
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

        ModifyRequestImpl req1 = getRequest();
        assertTrue( req1.equals( req0 ) );
    }
}
