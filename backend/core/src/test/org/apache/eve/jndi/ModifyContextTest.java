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


import javax.naming.directory.DirContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.Attribute;
import javax.naming.NamingException;


/**
 * Tests the methods on JNDI contexts that are analogous to entry modify
 * operations in LDAP.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ModifyContextTest extends AbstractJndiTest
{
    protected void setUp() throws Exception
    {
        super.setUp();

        CreateContextTest createContextTest = new CreateContextTest();
        createContextTest.setUp();
        createContextTest.testCreateContexts();
    }


    public void testModifyOperation() throws NamingException
    {
        Attributes attributes = new BasicAttributes();
        attributes.put( "ou", "testCases" );
        sysRoot.modifyAttributes( "ou=testing00", DirContext.ADD_ATTRIBUTE, attributes );
        attributes = null;

        DirContext ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
        attributes = ctx.getAttributes( "" );
        assertTrue( attributes.get( "ou" ).contains( "testCases" ) );

        Attribute attribute;

        attribute = attributes.get( "creatorsName" );
        assertNotNull( attribute );

        attribute = attributes.get( "createTimestamp" );
        assertNotNull( attribute );

        attribute = attributes.get( "modifiersName" );
        assertNotNull( attribute );

        attributes.get( "modifyTimestamp" );
        assertNotNull( attribute );
    }
}
