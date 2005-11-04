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


import org.apache.ldap.server.schema.bootstrap.AbstractBootstrapSchema;


/**
 * A test which tries to generate ObjectClass producers for all schemas.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ObjectClassesTemplateTest extends AbstractTestCase
{


    public void testCoreObjectClassGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "core", "dummy.test",
            new String[] { "dep1", "dep2" }) {};
        generateObjectClassProducer( schema );
    }


    public void testJavaObjectClassGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "java", "dummy.test",
            new String[] { "dep1", "dep2" }) {};
        generateObjectClassProducer( schema );
    }


    public void testCorbaObjectClassGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "corba", "dummy.test",
            new String[] { "dep1", "dep2" }) {};
        generateObjectClassProducer( schema );
    }


    public void testCosineObjectClassGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "cosine", "dummy.test",
            new String[] { "dep1", "dep2" }) {};
        generateObjectClassProducer( schema );
    }


    public void testInetorgpersonObjectClassGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "inetorgperson", "dummy.test",
            new String[] { "dep1", "dep2" }) {};
        generateObjectClassProducer( schema );
    }


    public void testMiscObjectClassGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "misc", "dummy.test",
            new String[] { "dep1", "dep2" }) {};
        generateObjectClassProducer( schema );
    }


    public void testNisObjectClassGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "nis", "dummy.test",
            new String[] { "dep1", "dep2" }) {};
        generateObjectClassProducer( schema );
    }
}
