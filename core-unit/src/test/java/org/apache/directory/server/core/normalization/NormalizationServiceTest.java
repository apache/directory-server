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
package org.apache.directory.server.core.normalization;


import org.apache.directory.server.core.unit.AbstractAdminTestCase;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;


/**
 * Test cases for the normalization service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public final class NormalizationServiceTest extends AbstractAdminTestCase
{
    public NormalizationServiceTest()
    {
        super.setLoadClass( getClass() );
    }
    
    
    public void testDireve308Example() throws NamingException
    {
        Attributes attrs = sysRoot.getAttributes( "ou=corporate category\\, operations,ou=direct report view" );
        assertNotNull( attrs );
        Attribute ou = attrs.get( "ou" );
        assertEquals( "corporate category\\, operations", ou.get() );
        Attribute oc = attrs.get( "objectClass" );
        assertTrue( oc.contains( "top" ) );
        assertTrue( oc.contains( "organizationalUnit" ) );
    }
}
