/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.changelog;


import junit.framework.TestCase;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.shared.ldap.ldif.ChangeType;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.AddRequestImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;


/**
 * Tests the MemoryChangeLogStore.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MemoryChangeLogStoreTest extends TestCase
{
    MemoryChangeLogStore store;


    public void setUp() throws Exception
    {
        super.setUp();
        store = new MemoryChangeLogStore();
    }


    public void tearDown() throws Exception
    {
        super.tearDown();
        store = null;
    }


    public void testLogCheckRevision() throws NamingException
    {
        assertEquals( "first revision is always 0", 0, store.getCurrentRevision() );

        Entry forward = new Entry();
        forward.setDn( "ou=system" );
        forward.setChangeType( ChangeType.Add );
        forward.putAttribute( "objectClass", "organizationalUnit" );
        forward.putAttribute( "ou", "system" );

        AddRequest addRequest = new AddRequestImpl( 0 );
        addRequest.setAttributes( forward.getAttributes() );
        addRequest.setEntry( new LdapDN( forward.getDn() ) );
        Entry reverse = LdifUtils.reverseAdd( addRequest );
        assertEquals( 1, store.log( new LdapPrincipal(), forward, reverse ) );
        assertEquals( 1, store.getCurrentRevision() );
    }
}