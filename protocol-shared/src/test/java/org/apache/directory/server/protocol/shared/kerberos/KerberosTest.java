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

package org.apache.directory.server.protocol.shared.kerberos;


import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.protocol.shared.AbstractBackingStoreTest;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KerberosTest extends AbstractBackingStoreTest
{
    /**
     * Setup the backing store with test partitions.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        loadPartition( "dc=apache,dc=org", "kerberos-apache.ldif" );
        loadPartition( "dc=example,dc=com", "kerberos-example.ldif" );
    }


    /**
     * Makes sure the context has the right attributes and values.
     *
     * @throws NamingException if there are failures
     */
    public void testContext() throws Exception
    {
        env.put( Context.PROVIDER_URL, "dc=example,dc=com" );
        DirContext ctx = ( DirContext ) factory.getInitialContext( env );

        Attributes matchAttrs = new BasicAttributes( true );
        matchAttrs.put( new BasicAttribute( "krb5PrincipalName", "kadmin/changepw@EXAMPLE.COM" ) );

        Attributes attributes = ctx.getAttributes( "ou=users" );
        System.out.println( attributes );
        assertNotNull( attributes );
        assertTrue( "users".equalsIgnoreCase( ( String ) attributes.get( "ou" ).get() ) );

        Attribute attribute = attributes.get( "objectClass" );

        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalunit" ) );
    }
}
