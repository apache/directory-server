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
package org.apache.ldap.server.tools.schema;


import junit.framework.TestCase;

import org.apache.ldap.server.schema.bootstrap.AbstractBootstrapSchema;


/**
 * Tests the DirectorySchemaTool.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DirectorySchemaToolTest extends TestCase
{
    public void testEveSchemaTool() throws Exception
    {
        DirectorySchemaTool tool = new DirectorySchemaTool();
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
                "cn=admin,ou=system", "core", null, new String[] { "system", "dep2" }
        ){};
        tool.setSchema( schema );
        tool.generate();
    }
}
