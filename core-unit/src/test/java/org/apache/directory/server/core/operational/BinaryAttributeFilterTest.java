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
package org.apache.directory.server.core.operational;


import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Tests to see that the binary property filtering in the schema service's
 * filter class {@link org.apache.directory.server.core.schema.SchemaService} is working
 * properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BinaryAttributeFilterTest extends AbstractAdminTestCase
{
    private static final String BINARY_KEY = "java.naming.ldap.attributes.binary";


    public void testBinaryExtension() throws NamingException
    {
        Attributes attributes = new BasicAttributes( true );
        attributes.put( "objectClass", "top" );
        attributes.put( "objectClass", "organizationalUnit" );
        attributes.put( "objectClass", "extensibleObject" );
        attributes.put( "ou", "testing" );
        sysRoot.createSubcontext( "ou=test", attributes );

        // test without turning on the property
        DirContext ctx = ( DirContext ) sysRoot.lookup( "ou=test" ) ;
        Attribute ou = ctx.getAttributes( "" ).get( "ou" );
        Object value = ou.get();
        assertTrue( value instanceof String );

        // test with the property now making ou into a binary value
        sysRoot.addToEnvironment( BINARY_KEY, "ou" );
        ctx = ( DirContext ) sysRoot.lookup( "ou=test" ) ;
        ou = ctx.getAttributes( "" ).get( "ou" );
        value = ou.get();
        assertEquals( "test", value );

        // try krb5Key which should be binary automatically - use ou as control
        byte[] keyValue = new byte[] { 0x45, 0x23, 0x7d, 0x7f };
        attributes.put( "jpegPhoto", keyValue );
        sysRoot.createSubcontext( "ou=anothertest", attributes );
        ctx = ( DirContext ) sysRoot.lookup( "ou=anothertest" ) ;
        ou = ctx.getAttributes( "" ).get( "ou" );
        value = ou.get();
        assertEquals( "anothertest", value );
        Attribute jpegPhoto = ctx.getAttributes( "" ).get( "jpegPhoto" );
        value = jpegPhoto.get();
        assertTrue( value instanceof byte[] );
        assertEquals( "0x45 0x23 0x7D 0x7F ", StringTools.dumpBytes( (byte[])value ) );

        // try jpegPhoto which should be binary automatically but use String to
        // create so we should still get back a byte[] - use ou as control
        attributes.remove( "jpegPhoto" );
        attributes.put( "jpegPhoto", "testing a string" );
        sysRoot.createSubcontext( "ou=yetanothertest", attributes );
        ctx = ( DirContext ) sysRoot.lookup( "ou=yetanothertest" ) ;
        ou = ctx.getAttributes( "" ).get( "ou" );
        value = ou.get();
        assertEquals( "yetanothertest", value );
        jpegPhoto = ctx.getAttributes( "" ).get( "jpegPhoto" );
        value = jpegPhoto.get();
        assertTrue( value instanceof byte[] );
    }
}
