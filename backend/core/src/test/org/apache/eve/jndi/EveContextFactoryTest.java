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
package org.apache.eve.jndi;


import java.util.HashMap;
import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

import org.apache.ldap.common.message.DerefAliasesEnum;


/**
 * Tests to see if we can fire up the Eve directory server via JNDI.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EveContextFactoryTest extends AbstractJndiTest
{
    /**
     * Makes sure the system context has the right attributes and values.
     *
     * @throws NamingException if there are failures
     */
    public void testSystemContext() throws NamingException
    {
        assertNotNull( sysRoot );

        Attributes attributes = sysRoot.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "system", attributes.get( "ou" ).get() );
        Attribute attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );
    }


    public void testSetupTeardown() throws NamingException
    {
        assertNotNull( sysRoot );

        Attributes attributes = sysRoot.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "system", attributes.get( "ou" ).get() );
        Attribute attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );
    }
}
