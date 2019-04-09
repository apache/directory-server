/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.osgi.integ;


import static org.junit.Assert.assertSame;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.apache.directory.server.core.shared.NullStringSerializer;
import org.apache.directory.server.core.shared.partition.DefaultPartitionNexus;


public class ServerCoreSharedOsgiTest extends ServerOsgiTestBase
{

    @Override
    protected String getBundleName()
    {
        return "org.apache.directory.server.core.shared";
    }


    @Override
    protected void useBundleClasses() throws Exception
    {
        DefaultDnFactory dnFactory = new DefaultDnFactory( null, 100 );
        Dn dn1 = dnFactory.create( "cn=foo" );
        Dn dn2 = dnFactory.create( "cn=foo" );
        assertSame( dn1, dn2 );

        NullStringSerializer.INSTANCE.serialize( null );
        NullStringSerializer.INSTANCE.deserialize( null );
        new DefaultPartitionNexus( new DefaultEntry() );
    }

}
