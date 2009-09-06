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

package org.apache.directory.server.core.builder;

import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.schema.loader.ldif.JarLdifSchemaLoader;

/**
 * TODO JarLdifBootstrapRegistryLoader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JarLdifBootstrapRegistryLoader implements BootstrapRegistryLoader
{
    private final SchemaLoader schemaLoader;

    
    public JarLdifBootstrapRegistryLoader() throws Exception
    {
        schemaLoader = new JarLdifSchemaLoader();
    }
    

    /* (non-Javadoc)
     * @see org.apache.directory.server.core.builder.BootstrapRegistryLoader#loadBootstrapSchema(org.apache.directory.shared.ldap.schema.registries.Registries)
     */
    public void loadBootstrapSchema( Registries registries ) throws Exception
    {
        schemaLoader.loadAllEnabled( registries );
    }
}
