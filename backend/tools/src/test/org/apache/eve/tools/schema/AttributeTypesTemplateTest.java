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


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;

import junit.framework.TestCase;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.eve.schema.bootstrap.BootstrapSchema;
import org.apache.eve.schema.bootstrap.ProducerTypeEnum;
import org.apache.eve.schema.bootstrap.AbstractBootstrapSchema;


/**
 * A test which tries to generate AttributeType producers for all schemas.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeTypesTemplateTest extends TestCase
{
    private OpenLdapSchemaParser parser;


    protected void setUp() throws Exception
    {
        super.setUp();

        parser = new OpenLdapSchemaParser();
        parser.setParserMonitor( new ConsoleParserMonitor() );
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();
        parser = null;
    }


    public AttributeTypeLiteral[] getSchemaAttributes( String schemaFile )
        throws Exception
    {
        InputStream in = getClass().getResourceAsStream( schemaFile );
        parser.parse( in );
        int size = parser.getAttributeTypes().size();
        AttributeTypeLiteral[] attributeTypes = new AttributeTypeLiteral[size];
        attributeTypes = ( AttributeTypeLiteral[] )
            parser.getAttributeTypes().values().toArray( attributeTypes );
        return attributeTypes;
    }


    public void generateAttributeTypeProducer( BootstrapSchema schema )
        throws Exception
    {
        AttributeTypeLiteral[] attributeTypes =
            getSchemaAttributes( schema.getSchemaName() + ".schema" );

        VelocityContext context = new VelocityContext();
        context.put( "package", schema.getPackageName() );
        context.put( "classname",
            schema.getUnqualifiedClassName( ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER ) );
        context.put( "schema", schema.getSchemaName() );
        context.put( "owner", schema.getOwner() ) ;
        context.put( "schemaDepCount", new Integer( schema.getDependencies().length ) );
        context.put( "schemaDeps", new String[] { "dep1", "dep2" }  ) ;
        context.put( "attrTypeCount", new Integer( attributeTypes.length ) );
        context.put( "attrTypes", attributeTypes );

        FileReader template = getResourceReader( "AttributeTypes.template" );
        FileWriter writer = getResourceWriter( "target/schema",
            schema.getPackageName(),
            schema.getUnqualifiedClassName( ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER ) );
        Velocity.init();
        Velocity.evaluate( context, writer, "LOG", template );
        writer.flush();
        writer.close();
    }


    private FileReader getResourceReader( String res ) throws Exception
    {
        String path = getClass().getResource( res ).getFile() ;
        return new FileReader( path );
    }


    private boolean mkdirs( String base, String path )
    {
        String[] comps = path.split( "/" );
        File file = new File( base );

        if ( ! file.exists() )
        {
            file.mkdirs();
        }

        for ( int ii = 0; ii < comps.length; ii++ )
        {
            file = new File( file, comps[ii] );
            if ( ! file.exists() )
            {
                file.mkdirs();
            }
        }

        return file.exists();
    }


    private FileWriter getResourceWriter( String srcBase, String pkg,
                                          String classname ) throws Exception
    {
        mkdirs( srcBase, pkg.replace( '.', File.separatorChar ) );
        File base = new File( srcBase );
        String relativePath = pkg.replace( '.', File.separatorChar );
        File dir = new File( base, relativePath );
        return new FileWriter( new File( dir, classname + ".java" ) );
    }


    public void testCoreAttributeTypeGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "core", "org.apache.eve.schema.bootstrap",
            new String[] { "dep1", "dep2" }) {};
        generateAttributeTypeProducer( schema );
    }


    public void testJavaAttributeTypeGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "java", "org.apache.eve.schema.bootstrap",
            new String[] { "dep1", "dep2" }) {};
        generateAttributeTypeProducer( schema );
    }


    public void testCorbaAttributeTypeGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "corba", "org.apache.eve.schema.bootstrap",
            new String[] { "dep1", "dep2" }) {};
        generateAttributeTypeProducer( schema );
    }


    public void testCosineAttributeTypeGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "cosine", "org.apache.eve.schema.bootstrap",
            new String[] { "dep1", "dep2" }) {};
        generateAttributeTypeProducer( schema );
    }


    public void testInetorgpersonAttributeTypeGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "inetorgperson", "org.apache.eve.schema.bootstrap",
            new String[] { "dep1", "dep2" }) {};
        generateAttributeTypeProducer( schema );
    }


    public void testMiscAttributeTypeGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "misc", "org.apache.eve.schema.bootstrap",
            new String[] { "dep1", "dep2" }) {};
        generateAttributeTypeProducer( schema );
    }


    public void testNisAttributeTypeGeneration() throws Exception
    {
        AbstractBootstrapSchema schema = new AbstractBootstrapSchema(
            "uid=admin,ou=system", "nis", "org.apache.eve.schema.bootstrap",
            new String[] { "dep1", "dep2" }) {};
        generateAttributeTypeProducer( schema );
    }
}
