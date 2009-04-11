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

import org.apache.directory.shared.ldap.message.InternalAbstractResultResponse;
import org.apache.directory.shared.ldap.message.InternalControl;
import org.apache.directory.shared.ldap.message.InternalLdapResult;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.InternalReferral;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * TestCase for the methods of the AbstractResultResponse class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public class AbstractResultResponseTest
{
    /**
     * Tests to see the same object returns true.
     */
    @Test
    public void testEqualsSameObj()
    {
        InternalAbstractResultResponse msg;
        msg = new InternalAbstractResultResponse( 5, MessageTypeEnum.BIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        assertTrue( msg.equals( msg ) );
    }


    /**
     * Tests to see the same exact copy returns true.
     */
    @Test
    public void testEqualsExactCopy() throws InvalidNameException
    {
        InternalAbstractResultResponse msg0 = new InternalAbstractResultResponse( 5, MessageTypeEnum.BIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        InternalAbstractResultResponse msg1 = new InternalAbstractResultResponse( 5, MessageTypeEnum.BIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        InternalLdapResult r0 = msg0.getLdapResult();
        InternalLdapResult r1 = msg1.getLdapResult();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );
        r1.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );

        r0.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );

        InternalReferral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        InternalReferral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertTrue( msg0.equals( msg1 ) );
        assertTrue( msg1.equals( msg0 ) );
    }


    /**
     * Tests to see the same exact copy returns true.
     */
    @Test
    public void testNotEqualsDiffResult() throws InvalidNameException
    {
        InternalAbstractResultResponse msg0 = new InternalAbstractResultResponse( 5, MessageTypeEnum.BIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        InternalAbstractResultResponse msg1 = new InternalAbstractResultResponse( 5, MessageTypeEnum.BIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        InternalLdapResult r0 = msg0.getLdapResult();
        InternalLdapResult r1 = msg1.getLdapResult();

        r0.setErrorMessage( "blah blah blah" );
        r1.setErrorMessage( "blah blah blah" );

        r0.setMatchedDn( new LdapDN( "dc=example,dc=com" ) );
        r1.setMatchedDn( new LdapDN( "dc=apache,dc=org" ) );

        r0.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );
        r1.setResultCode( ResultCodeEnum.TIME_LIMIT_EXCEEDED );

        InternalReferral refs0 = new ReferralImpl();
        refs0.addLdapUrl( "ldap://someserver.com" );
        refs0.addLdapUrl( "ldap://anotherserver.org" );

        InternalReferral refs1 = new ReferralImpl();
        refs1.addLdapUrl( "ldap://someserver.com" );
        refs1.addLdapUrl( "ldap://anotherserver.org" );

        assertFalse( msg0.equals( msg1 ) );
        assertFalse( msg1.equals( msg0 ) );
    }


    /**
     * Tests to make sure changes in the id result in inequality.
     */
    @Test
    public void testNotEqualsDiffId()
    {
        InternalAbstractResultResponse msg0;
        InternalAbstractResultResponse msg1;
        msg0 = new InternalAbstractResultResponse( 5, MessageTypeEnum.BIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        msg1 = new InternalAbstractResultResponse( 6, MessageTypeEnum.BIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        assertFalse( msg0.equals( msg1 ) );
        assertFalse( msg1.equals( msg0 ) );
    }


    /**
     * Tests to make sure changes in the type result in inequality.
     */
    @Test
    public void testNotEqualsDiffType()
    {
        InternalAbstractResultResponse msg0;
        InternalAbstractResultResponse msg1;
        msg0 = new InternalAbstractResultResponse( 5, MessageTypeEnum.BIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        msg1 = new InternalAbstractResultResponse( 5, MessageTypeEnum.UNBIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        assertFalse( msg0.equals( msg1 ) );
        assertFalse( msg1.equals( msg0 ) );
    }


    /**
     * Tests to make sure changes in the controls result in inequality.
     */
    @Test
    public void testNotEqualsDiffControls()
    {
        InternalAbstractResultResponse msg0;
        InternalAbstractResultResponse msg1;
        msg0 = new InternalAbstractResultResponse( 5, MessageTypeEnum.BIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        msg0.add( new InternalControl()
        {
            private static final long serialVersionUID = 1L;


            public void setID( String a_oid )
            {
            }

            
            public boolean isCritical()
            {
                return false;
            }


            public void setCritical( boolean a_isCritical )
            {
            }


            public byte[] getEncodedValue()
            {
                return null;
            }


            public String getID()
            {
                return null;
            }
        } );
        msg1 = new InternalAbstractResultResponse( 5, MessageTypeEnum.BIND_REQUEST )
        {
            private static final long serialVersionUID = 1L;
        };
        assertFalse( msg0.equals( msg1 ) );
        assertFalse( msg1.equals( msg0 ) );
    }
}
