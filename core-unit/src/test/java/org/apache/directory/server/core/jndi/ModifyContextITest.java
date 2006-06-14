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
package org.apache.directory.server.core.jndi;


import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;


/**
 * Tests the methods on JNDI contexts that are analogous to entry modify
 * operations in LDAP.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ModifyContextITest extends AbstractAdminTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();

        try
        {
            /*
             * create ou=testing00,ou=system
             */
            Attributes attributes = new BasicAttributes( true );
            Attribute attribute = new BasicAttribute( "objectClass" );
            attribute.add( "top" );
            attribute.add( "organizationalUnit" );
            attributes.put( attribute );
            attributes.put( "ou", "testing00" );
            DirContext ctx = sysRoot.createSubcontext( "ou=testing00", attributes );
            assertNotNull( ctx );

            ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
            assertNotNull( ctx );

            attributes = ctx.getAttributes( "" );
            assertNotNull( attributes );
            assertEquals( "testing00", attributes.get( "ou" ).get() );
            attribute = attributes.get( "objectClass" );
            assertNotNull( attribute );
            assertTrue( attribute.contains( "top" ) );
            assertTrue( attribute.contains( "organizationalUnit" ) );

            /*
             * create ou=testing01,ou=system
             */
            attributes = new BasicAttributes( true );
            attribute = new BasicAttribute( "objectClass" );
            attribute.add( "top" );
            attribute.add( "organizationalUnit" );
            attributes.put( attribute );
            attributes.put( "ou", "testing01" );
            ctx = sysRoot.createSubcontext( "ou=testing01", attributes );
            assertNotNull( ctx );

            ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );
            assertNotNull( ctx );

            attributes = ctx.getAttributes( "" );
            assertNotNull( attributes );
            assertEquals( "testing01", attributes.get( "ou" ).get() );
            attribute = attributes.get( "objectClass" );
            assertNotNull( attribute );
            assertTrue( attribute.contains( "top" ) );
            assertTrue( attribute.contains( "organizationalUnit" ) );

            /*
             * create ou=testing02,ou=system
             */
            attributes = new BasicAttributes( true );
            attribute = new BasicAttribute( "objectClass" );
            attribute.add( "top" );
            attribute.add( "organizationalUnit" );
            attributes.put( attribute );
            attributes.put( "ou", "testing02" );
            ctx = sysRoot.createSubcontext( "ou=testing02", attributes );
            assertNotNull( ctx );

            ctx = ( DirContext ) sysRoot.lookup( "ou=testing02" );
            assertNotNull( ctx );

            attributes = ctx.getAttributes( "" );
            assertNotNull( attributes );
            assertEquals( "testing02", attributes.get( "ou" ).get() );
            attribute = attributes.get( "objectClass" );
            assertNotNull( attribute );
            assertTrue( attribute.contains( "top" ) );
            assertTrue( attribute.contains( "organizationalUnit" ) );

            /*
             * create ou=subtest,ou=testing01,ou=system
             */
            ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );

            attributes = new BasicAttributes( true );
            attribute = new BasicAttribute( "objectClass" );
            attribute.add( "top" );
            attribute.add( "organizationalUnit" );
            attributes.put( attribute );
            attributes.put( "ou", "subtest" );
            ctx = ctx.createSubcontext( "ou=subtest", attributes );
            assertNotNull( ctx );

            ctx = ( DirContext ) sysRoot.lookup( "ou=subtest,ou=testing01" );
            assertNotNull( ctx );

            attributes = ctx.getAttributes( "" );
            assertNotNull( attributes );
            assertEquals( "subtest", attributes.get( "ou" ).get() );
            attribute = attributes.get( "objectClass" );
            assertNotNull( attribute );
            assertTrue( attribute.contains( "top" ) );
            assertTrue( attribute.contains( "organizationalUnit" ) );
        }
        catch ( NamingException e )
        {
        }
    }


    public void testModifyOperation() throws NamingException
    {
        Attributes attributes = new BasicAttributes( true );
        attributes.put( "ou", "testCases" );
        sysRoot.modifyAttributes( "ou=testing00", DirContext.ADD_ATTRIBUTE, attributes );
        attributes = null;

        DirContext ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
        attributes = ctx.getAttributes( "" );
        assertTrue( attributes.get( "ou" ).contains( "testCases" ) );

        Attribute attribute = attributes.get( "creatorsName" );
        assertNull( attribute );

        attribute = attributes.get( "createTimestamp" );
        assertNull( attribute );

        attribute = attributes.get( "modifiersName" );
        assertNull( attribute );

        attributes.get( "modifyTimestamp" );
        assertNull( attribute );
    }
}
