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


import java.io.*;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

import org.apache.eve.schema.bootstrap.BootstrapSchema;
import org.apache.eve.schema.bootstrap.ProducerTypeEnum;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;


/**
 * Generates Eve schema classses from OpenLDAP schema files.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EveSchemaTool
{

    /** property for dir where OpenLDAP schema files and deps file are stored */
    public static final String SCHEMA_SRC_DIR_PROP =
            "maven.eve.schema.src.dir";
    /** property for dir where the generated class files are created */
    public static final String SCHEMA_TARGET_DIR_PROP =
            "maven.eve.schema.target.dir";

    /** default dir where OpenLDAP schema files and deps file are kept */
    public static final String SCHEMA_SRC_DIR_DEFAULT =
            "src" + File.separator + "schema";
    /** property for the name of the schema dependency file */
    public static final String SCHEMA_DEP_FILE_DEFAULT = "schema.deps";
    /** default dir where the generated class files are created */
    public static final String SCHEMA_TARGET_DIR_DEFAULT =
            "target" + File.separator + "schema";


    /** the source directory where the schema OpenLDAP source files are kept */
    private String schemaSrcDir = SCHEMA_SRC_DIR_DEFAULT;
    /** the directory where we generate schema class files */
    private String schemaTargetDir = SCHEMA_TARGET_DIR_DEFAULT;


    private BootstrapSchema schema;
    private OpenLdapSchemaParser parser;



    public EveSchemaTool() throws IOException
    {
        parser = new OpenLdapSchemaParser();
    }


    public String getSchemaSrcDir()
    {
        return schemaSrcDir;
    }


    public void setSchemaSrcDir( String schemaSrcDir )
    {
        this.schemaSrcDir = schemaSrcDir;
    }


    public String getSchemaTargetDir()
    {
        return schemaTargetDir;
    }


    public void setSchemaTargetDir( String schemaTargetDir )
    {
        this.schemaTargetDir = schemaTargetDir;
    }


    public BootstrapSchema getSchema()
    {
        return schema;
    }


    public void setSchema( BootstrapSchema schema )
    {
        this.schema = schema;
    }


    public void generate() throws Exception
    {
        if ( schema == null )
        {
            throw new NullPointerException( "the schema property must be set" );
        }


        InputStream in = new FileInputStream( schemaSrcDir + File.separator
                + schema.getSchemaName() + ".schema" );
        parser.parse( in );

        generateAttributeTypes();
        generateObjectClasses();
    }


    protected void generateAttributeTypes() throws Exception
    {
        // check to see if the producer exists for this type
        if ( exists( ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER ) )
        {
            return;
        }

        int size = parser.getAttributeTypes().size();
        AttributeTypeLiteral[] attributeTypes = new AttributeTypeLiteral[size];
        attributeTypes = ( AttributeTypeLiteral[] )
            parser.getAttributeTypes().values().toArray( attributeTypes );

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

        FileWriter writer = getResourceWriter( schema.getUnqualifiedClassName(
                ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER ) );
        Velocity.init();
        Velocity.evaluate( context, writer, "LOG", template );
        writer.flush();
        writer.close();
    }


    protected void generateObjectClasses() throws Exception
    {
        // check to see if the producer exists for this type
        if ( exists( ProducerTypeEnum.OBJECT_CLASS_PRODUCER ) )
        {
            return;
        }

        int size = parser.getObjectClassTypes().size();
        ObjectClassLiteral[] objectClasses = new ObjectClassLiteral[size];
        objectClasses = ( ObjectClassLiteral[] )
            parser.getObjectClassTypes().values().toArray( objectClasses );

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
        FileWriter writer = getResourceWriter( schema.getUnqualifiedClassName(
                ProducerTypeEnum.OBJECT_CLASS_PRODUCER ) );
        Velocity.init();
        Velocity.evaluate( context, writer, "LOG", template );
        writer.flush();
        writer.close();
    }


    protected FileReader getResourceReader( String res ) throws IOException
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


    protected FileWriter getResourceWriter( String classname ) throws IOException
    {
        String pkg = schema.getPackageName();
        mkdirs( schemaTargetDir, pkg.replace( '.', File.separatorChar ) );
        File base = new File( schemaTargetDir );
        String relativePath = pkg.replace( '.', File.separatorChar );
        File dir = new File( base, relativePath );
        return new FileWriter( new File( dir, classname + ".java" ) );
    }


    protected boolean exists( ProducerTypeEnum type )
    {
        boolean exists = true;

        String defaultClass = schema.getFullDefaultBaseClassName(
                ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER );
        String targetClass = schema.getFullDefaultBaseClassName(
                ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER );

        try
        {
            exists = Class.forName( defaultClass ) != null;
        }
        catch ( ClassNotFoundException e )
        {
            exists = false;
        }

        try
        {
            exists = Class.forName( targetClass ) != null;
        }
        catch ( ClassNotFoundException e )
        {
            exists = false;
        }

        return exists;
    }
}
