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

import java.io.StringWriter;


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
        VelocityContext context = new VelocityContext();
        context.put( "args", new String[] { "uno", "dos", "tres" } );
        String template = "args = #foreach($arg in $args)$arg #end";
        StringWriter writer = new StringWriter();
        Velocity.init();
        Velocity.evaluate( context, writer, "LOG", template );
        String generated = writer.getBuffer().toString();
        assertEquals( "args = uno dos tres", generated.trim() );
    }
}
