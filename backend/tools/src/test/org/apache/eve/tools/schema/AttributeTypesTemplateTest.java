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
package org.apache.eve.tools.schema;

import junit.framework.TestCase;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.ldap.common.schema.BaseAttributeType;

import java.io.StringWriter;
import java.io.FileReader;
import java.io.FileWriter;


/**
 * Document me.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeTypesTemplateTest extends TestCase
{
    public void testGeneration() throws Exception
    {
        TestAttributeType[] attributeTypes = new TestAttributeType[2];
        attributeTypes[0] = new TestAttributeType( "1.1.1.1" );
        attributeTypes[1] = new TestAttributeType( "1.1.1.2" );

        VelocityContext context = new VelocityContext();
        context.put( "package", "org.apache.eve.schema.config" );
        context.put( "classname", "CoreAttributeTypes" );
        context.put( "schema", "core" );
        context.put( "owner", "uid=admin,ou=system" ) ;
        context.put( "schemaDepCount", new Integer( 2 ) );
        context.put( "schemaDeps", new String[] { "dep1", "dep2" }  ) ;
        context.put( "attrTypeCount", new Integer( attributeTypes.length ) );
        context.put( "attrTypes", attributeTypes );

        FileReader template = new FileReader(
            "c:\\projects\\home\\akarasulu\\projects\\directory\\eve\\trunk\\backend\\tools\\src\\template\\AttributeTypes.template" );
        FileWriter writer = new FileWriter(
            "c:\\projects\\home\\akarasulu\\projects\\directory\\eve\\trunk\\backend\\tools\\src\\java\\org\\apache\\eve\\schema\\config\\CoreAttributeTypes.java" );
        Velocity.init();
        Velocity.evaluate( context, writer, "LOG", template );
        writer.flush();
        writer.close();
    }


    class TestAttributeType extends BaseAttributeType
    {
        protected TestAttributeType( String oid )
        {
            super( oid );
        }
    }
}
