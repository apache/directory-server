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
package org.apache.directory.shared.ldap.schema.loader.ldif;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.junit.Test;


/**
 * Tests the LdifSchemaLoader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class JarLdifSchemaLoaderTest
{
    @Test
    public void testJarLdifSchemaLoader() throws Exception
    {
        JarLdifSchemaLoader loader = new JarLdifSchemaLoader();
        SchemaManager sm = new DefaultSchemaManager( loader );

        sm.loadWithDeps( "system" );
        
        assertTrue( sm.getRegistries().getAttributeTypeRegistry().contains( "cn" ) );
        assertFalse( sm.getRegistries().getAttributeTypeRegistry().contains( "m-aux" ) );
        
        sm.loadWithDeps( "apachemeta" );

        assertTrue( sm.getRegistries().getAttributeTypeRegistry().contains( "m-aux" ) );
    }
}
