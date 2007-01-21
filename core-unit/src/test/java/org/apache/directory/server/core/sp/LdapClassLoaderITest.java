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


import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.util.Base64;


/**
 * Test case for LdapClassLoader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public class LdapClassLoaderITest extends AbstractAdminTestCase
{
    private static final String HELLOWORLD_CLASS_BASE64 = "yv66vgAAADEAHQoABgAPCQAQABEIABIKABMAFAcAFQcAFgEABjxpbml0PgEAAygpV"
        + "gEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBAARtYWluAQAWKFtMamF2YS9sYW5nL1N0cmluZzsp"
        + "VgEAClNvdXJjZUZpbGUBAA9IZWxsb1dvcmxkLmphdmEMAAcACAcAFwwAGAAZAQAMSGVsbG8gV29"
        + "ybGQhBwAaDAAbABwBAApIZWxsb1dvcmxkAQAQamF2YS9sYW5nL09iamVjdAEAEGphdmEvbGFuZy"
        + "9TeXN0ZW0BAANvdXQBABVMamF2YS9pby9QcmludFN0cmVhbTsBABNqYXZhL2lvL1ByaW50U3RyZ"
        + "WFtAQAHcHJpbnRsbgEAFShMamF2YS9sYW5nL1N0cmluZzspVgAhAAUABgAAAAAAAgABAAcACAAB"
        + "AAkAAAAdAAEAAQAAAAUqtwABsQAAAAEACgAAAAYAAQAAAAEACQALAAwAAQAJAAAAJQACAAEAAAA"
        + "JsgACEgO2AASxAAAAAQAKAAAACgACAAAABQAIAAYAAQANAAAAAgAO";

    private static final byte[] HELLOWORLD_CLASS_BYTES = Base64.decode( HELLOWORLD_CLASS_BASE64.toCharArray() );


    protected void setUp() throws Exception
    {
        // bind to RootDSE
        overrideEnvironment( Context.PROVIDER_URL, "" );
        super.setUp();
    }


    public void testLdapClassLoaderWithClassLoadedAnywhere() throws Exception
    {
        // get default naming context to work on
        ServerLdapContext defaultContext = ( ServerLdapContext ) sysRoot.lookup( "ou=system" );

        // set up
        Attributes attributes = new AttributesImpl( "objectClass", "top", true );
        attributes.get( "objectClass" ).add( "javaClass" );
        attributes.put( "fullyQualifiedJavaClassName", "HelloWorld" );
        attributes.put( "javaClassByteCode", HELLOWORLD_CLASS_BYTES );
        defaultContext.createSubcontext( "fullyQualifiedJavaClassName=HelloWorld", attributes );

        // assert set up successfull
        assertNotNull( defaultContext.lookup( "fullyQualifiedJavaClassName=HelloWorld" ) );

        // load the class
        LdapClassLoader loader = new LdapClassLoader( ( ServerLdapContext ) ( sysRoot.lookup( "" ) ) );
        Class clazz = loader.loadClass( "HelloWorld" );

        // assert class loaded successfully
        assertEquals( clazz.getName(), "HelloWorld" );
    }


    public void testLdapClassLoaderWithClassLoadedAtDefaultSearchSubtree() throws Exception
    {

        // get default naming context to work on
        ServerLdapContext defaultContext = ( ServerLdapContext ) sysRoot.lookup( "ou=system" );

        // create an extensible object for holding custom config data
        Attributes classLoaderDefaultSearchContextConfig = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "extensibleObject" );

        // create custom config entry
        classLoaderDefaultSearchContextConfig.put( objectClass );
        classLoaderDefaultSearchContextConfig.put( new AttributeImpl( "cn", "classLoaderDefaultSearchContext" ) );

        // add a default search context to the configuration
        classLoaderDefaultSearchContextConfig.put( new AttributeImpl( "classLoaderDefaultSearchContext", "ou=system" ) );

        // add the configuration entry to the DIT
        ServerLdapContext configContext = ( ServerLdapContext ) defaultContext.lookup( "ou=configuration" );
        configContext.createSubcontext( "cn=classLoaderDefaultSearchContext", classLoaderDefaultSearchContextConfig );

        // create a class holder entry and add it to the DIT
        Attributes attributes = new AttributesImpl( "objectClass", "top", true );
        attributes.get( "objectClass" ).add( "javaClass" );
        attributes.put( "fullyQualifiedJavaClassName", "HelloWorld" );
        attributes.put( "javaClassByteCode", HELLOWORLD_CLASS_BYTES );
        defaultContext.createSubcontext( "fullyQualifiedJavaClassName=HelloWorld", attributes );

        // assert set up successfull
        assertNotNull( defaultContext.lookup( "fullyQualifiedJavaClassName=HelloWorld" ) );

        // load the class
        LdapClassLoader loader = new LdapClassLoader( ( ServerLdapContext ) ( sysRoot.lookup( "" ) ) );
        Class clazz = loader.loadClass( "HelloWorld" );

        // assert class loaded successfully
        assertEquals( clazz.getName(), "HelloWorld" );
    }
}
