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
package org.apache.eve.jndi.ibs;


import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

import org.apache.ldap.common.message.DerefAliasesEnum;
import org.apache.eve.jndi.AbstractJndiTest;


/**
 * Tests the methods on JNDI contexts that are analogous to entry modify
 * operations in LDAP.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OperationalAttributesTest extends AbstractJndiTest
{
    private static final String CREATORS_NAME = "creatorsName";
    private static final String CREATE_TIMESTAMP = "createTimestamp";


    public void testModifyOperationalOpAttrs() throws NamingException
    {
        /*
         * create ou=testing00,ou=system
         */
        Attributes attributes = new BasicAttributes();
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
        assertNull( attributes.get( CREATE_TIMESTAMP ) );
        assertNull( attributes.get( CREATORS_NAME ) );

        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes( new String[]
            { "ou", "createTimestamp", "creatorsName" } );

        sysRoot.addToEnvironment( DerefAliasesEnum.JNDI_PROP,
                DerefAliasesEnum.NEVERDEREFALIASES_NAME );
        NamingEnumeration list;
        list = ( NamingEnumeration ) sysRoot.search( "", "(ou=testing00)", ctls );
        SearchResult result = ( SearchResult ) list.next();
        list.close();

        System.out.println( result );

        assertNotNull( result.getAttributes().get( "ou" ) );
        assertNotNull( result.getAttributes().get( CREATORS_NAME ) );
        assertNotNull( result.getAttributes().get( CREATE_TIMESTAMP ) );
    }
}
