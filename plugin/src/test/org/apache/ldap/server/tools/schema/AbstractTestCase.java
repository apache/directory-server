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


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.ldap.server.schema.bootstrap.BootstrapSchema;
import org.apache.ldap.server.schema.bootstrap.ProducerTypeEnum;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;


/**
 * An abstract test case that incorporates both the parser and code generators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AbstractTestCase extends TestCase
{
    private OpenLdapSchemaParser parser;
    private String basedir;

    protected void setUp() throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir", "." );

        parser = new OpenLdapSchemaParser();
        parser.setParserMonitor( new ConsoleParserMonitor() );
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        parser = null;
    }

    protected ObjectClassLiteral[] getObjectClasses( String schemaFile )
        throws Exception
    {
        InputStream in = getClass().getResourceAsStream( schemaFile );
        parser.parse( in );
        int size = parser.getObjectClassTypes().size();
        ObjectClassLiteral[] objectClasses = new ObjectClassLiteral[size];
        objectClasses = ( ObjectClassLiteral[] )
            parser.getObjectClassTypes().values().toArray( objectClasses );
        return objectClasses;
    }

    protected AttributeTypeLiteral[] getSchemaAttributes( String schemaFile )
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

    protected void generateAttributeTypeProducer( BootstrapSchema schema )
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
        context.put( "attrTypes", attributeTypes );

        FileReader template = getResourceReader( "AttributeTypes.template" );
        FileWriter writer = getResourceWriter( basedir + "/target/schema",
            schema.getPackageName(),
            schema.getUnqualifiedClassName( ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER ) );
        Velocity.init();
        Velocity.evaluate( context, writer, "LOG", template );
        writer.flush();
        writer.close();
    }

    protected void generateObjectClassProducer( BootstrapSchema schema )
        throws Exception
    {
        ObjectClassLiteral[] objectClasses =
            getObjectClasses( schema.getSchemaName() + ".schema" );

        VelocityContext context = new VelocityContext();
        context.put( "package", schema.getPackageName() );
        context.put( "classname",
            schema.getUnqualifiedClassName( ProducerTypeEnum.OBJECT_CLASS_PRODUCER ) );
        context.put( "schema", schema.getSchemaName() );
        context.put( "owner", schema.getOwner() ) ;
        context.put( "schemaDepCount", new Integer( schema.getDependencies().length ) );
        context.put( "schemaDeps", new String[] { "dep1", "dep2" }  ) ;
        context.put( "objectClasses", objectClasses );

        FileReader template = getResourceReader( "ObjectClasses.template" );
        FileWriter writer = getResourceWriter( basedir + "/target/schema",
            schema.getPackageName(),
            schema.getUnqualifiedClassName( ProducerTypeEnum.OBJECT_CLASS_PRODUCER ) );
        Velocity.init();
        Velocity.evaluate( context, writer, "LOG", template );
        writer.flush();
        writer.close();
    }

    protected FileReader getResourceReader( String res ) throws Exception
    {
        String path = getClass().getResource( res ).getFile() ;
        return new FileReader( path );
    }

    protected boolean mkdirs( String base, String path )
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

    protected FileWriter getResourceWriter( String srcBase, String pkg,
                                            String classname ) throws Exception
    {
        mkdirs( srcBase, pkg.replace( '.', File.separatorChar ) );
        File base = new File( srcBase );
        String relativePath = pkg.replace( '.', File.separatorChar );
        File dir = new File( base, relativePath );
        return new FileWriter( new File( dir, classname + ".java" ) );
    }
}
