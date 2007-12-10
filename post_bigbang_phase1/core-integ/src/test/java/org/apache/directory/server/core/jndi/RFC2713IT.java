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
package org.apache.directory.server.core.jndi;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;
import java.util.ArrayList;


/**
 * Tests to validate whatever functionality we have for complying with
 * <a href="http://www.faqs.org/rfcs/rfc2713.html">RFC 2713</a>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class RFC2713IT
{
    public static DirectoryService service;
    

    @Test
    @SuppressWarnings("unchecked")
    public void testSerializatin() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );

        ArrayList<String> colors = new ArrayList<String>();
        colors.add( "red" );
        colors.add( "white" );
        colors.add( "blue" );
        sysRoot.bind( "cn=colors", colors );

        Object obj = sysRoot.lookup( "cn=colors" );
        assertTrue( obj instanceof ArrayList );
        colors = ( ArrayList<String> ) obj;
        assertEquals( 3, colors.size() );
        assertTrue( colors.contains( "red" ) );
        assertTrue( colors.contains( "white" ) );
        assertTrue( colors.contains( "blue" ) );

        Attributes attrs = sysRoot.getAttributes( "cn=colors" );
        Attribute attr = attrs.get( "objectClass" );
        assertNotNull( attr );
        assertEquals( 4, attr.size() );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "javaObject" ) );
        assertTrue( attr.contains( "javaContainer" ) );
        assertTrue( attr.contains( "javaSerializedObject" ) );
        attr = attrs.get( "javaClassName" );
        assertNotNull( attr );
        assertEquals( 1, attr.size() );
        assertTrue( attr.contains( "java.util.ArrayList" ) );

        attr = attrs.get( "javaSerializedData" );
        assertNotNull( attr );
        assertEquals( 1, attr.size() );
    }
}
