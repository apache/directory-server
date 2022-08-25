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

package org.apache.directory.server.core.sp;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Base64;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.sp.LdapClassLoader;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test case for LdapClassLoader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(factory = DefaultDirectoryServiceFactory.class, name = "LdapClassLoaderIT-class")
public class LdapClassLoaderIT extends AbstractLdapTestUnit
{
    private static final String HELLOWORLD_CLASS_BASE64 = "yv66vgAAADEAHQoABgAPCQAQABEIABIKABMAFAcAFQcAFgEABjxpbml0PgEAAygpV"
        + "gEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBAARtYWluAQAWKFtMamF2YS9sYW5nL1N0cmluZzsp"
        + "VgEAClNvdXJjZUZpbGUBAA9IZWxsb1dvcmxkLmphdmEMAAcACAcAFwwAGAAZAQAMSGVsbG8gV29"
        + "ybGQhBwAaDAAbABwBAApIZWxsb1dvcmxkAQAQamF2YS9sYW5nL09iamVjdAEAEGphdmEvbGFuZy"
        + "9TeXN0ZW0BAANvdXQBABVMamF2YS9pby9QcmludFN0cmVhbTsBABNqYXZhL2lvL1ByaW50U3RyZ"
        + "WFtAQAHcHJpbnRsbgEAFShMamF2YS9sYW5nL1N0cmluZzspVgAhAAUABgAAAAAAAgABAAcACAAB"
        + "AAkAAAAdAAEAAQAAAAUqtwABsQAAAAEACgAAAAYAAQAAAAEACQALAAwAAQAJAAAAJQACAAEAAAA"
        + "JsgACEgO2AASxAAAAAQAKAAAACgACAAAABQAIAAYAAQANAAAAAgAO";

    private static final byte[] HELLOWORLD_CLASS_BYTES = Base64.getDecoder().decode( HELLOWORLD_CLASS_BASE64 );


    @Test
    public void testLdapClassLoaderWithClassLoadedAnywhere() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( classDirectoryService ) )
        {
            // add the configuration entry to the DIT
            conn.add( 
                new DefaultEntry(
                    "fullyQualifiedJavaClassName=HelloWorld,ou=system",
                    "objectClass", "top",
                    "objectClass", "javaClass",
                    "fullyQualifiedJavaClassName", "HelloWorld",
                    "javaClassByteCode", HELLOWORLD_CLASS_BYTES ) );
    
            // assert set up successfull
            assertNotNull( conn.lookup( "fullyQualifiedJavaClassName=HelloWorld,ou=system" ) );
    
            // load the class
            LdapClassLoader loader = new LdapClassLoader( getService() );
            Class<?> clazz = loader.loadClass( "HelloWorld" );
    
            // assert class loaded successfully
            assertEquals( "HelloWorld", clazz.getName() );
        }
    }


    @Test
    public void testLdapClassLoaderWithClassLoadedAtDefaultSearchSubtree() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( classDirectoryService ) )
        {
            // add the configuration entry to the DIT
            conn.add( 
                new DefaultEntry(
                    "cn=classLoaderDefaultSearchContext,ou=system",
                    "objectClass", "top",
                    "objectClass", "javaContainer",
                    "objectClass", "extensibleObject",
                    "cn", "classLoaderDefaultSearchContext",
                    "classLoaderDefaultSearchContext", "ou=system" ) );
    
            // create a class holder entry and add it to the DIT
            conn.add( 
                new DefaultEntry(
                    "fullyQualifiedJavaClassName=HelloWorld,cn=classLoaderDefaultSearchContext,ou=system",
                    "objectClass", "top",
                    "objectClass", "javaClass",
                    "fullyQualifiedJavaClassName", "HelloWorld",
                    "javaClassByteCode", HELLOWORLD_CLASS_BYTES ) );


            // assert set up successfull
            assertNotNull( conn.lookup( "fullyQualifiedJavaClassName=HelloWorld,cn=classLoaderDefaultSearchContext,ou=system" ) );
    
            // load the class
            LdapClassLoader loader = new LdapClassLoader( getService() );
            Class<?> clazz = loader.loadClass( "HelloWorld" );
    
            // assert class loaded successfully
            assertEquals( "HelloWorld", clazz.getName() );
        }
    }
}
