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


import java.io.*;
import java.util.List;
import java.util.ArrayList;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.VelocityContext;

import org.apache.ldap.server.schema.bootstrap.BootstrapSchema;
import org.apache.ldap.server.schema.bootstrap.ProducerTypeEnum;


/**
 * Generates Eve schema classses from OpenLDAP schema files.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DirectorySchemaTool
{
    private static String basedir = System.getProperty( "basedir", "." );

    /** property for dir where OpenLDAP schema files and deps file are stored */
    public static final String SCHEMA_SRC_DIR_PROP = "maven.ldap.server.schema.src.dir";

    /** property for dir where the generated class files are created */
    public static final String SCHEMA_TARGET_DIR_PROP = "maven.ldap.server.schema.target.dir";

    /** default dir where OpenLDAP schema files and deps file are kept */
    public static final String SCHEMA_SRC_DIR_DEFAULT =
            basedir + File.separator + "src" + File.separator + "main" 
		+ File.separator + "schema";

    /** default dir where java src files are kept */
    public static final String JAVA_SRC_DIR_DEFAULT =
            basedir + File.separator + "src" + File.separator + "main" 
		+ File.separator + "java";

   /** property for the name of the schema dependency file */
    public static final String SCHEMA_DEP_FILE_DEFAULT = "schema.deps";

    /** default dir where the generated class files are created */
    public static final String SCHEMA_TARGET_DIR_DEFAULT =
            basedir + File.separator + "target" + File.separator + "schema";


    /** the source directory where the schema OpenLDAP source files are kept */
    private String schemaSrcDir = SCHEMA_SRC_DIR_DEFAULT;

    /** the directory where we generate schema class files */
    private String schemaTargetDir = SCHEMA_TARGET_DIR_DEFAULT;

    private String javaSrcDir = JAVA_SRC_DIR_DEFAULT;

    private BootstrapSchema schema;

    private OpenLdapSchemaParser parser;



    public DirectorySchemaTool() throws Exception
    {
        parser = new OpenLdapSchemaParser();

        Velocity.init();
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


    public String getJavaSrcDir()
    {
        return javaSrcDir;
    }


    public void setJavaSrcDir( String javaSrcDir )
    { 
        this.javaSrcDir = javaSrcDir;
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

        String filePath = schemaSrcDir + File.separator + schema.getSchemaName() + ".schema";

        InputStream in = new FileInputStream( filePath );

        parser.parse( in );

        generateSchema();

        generateAttributeTypes();

        generateObjectClasses();

        generateRest();
    }


    protected void generateSchema() throws Exception
    {
        StringBuffer schemaCapped = new StringBuffer();

        schemaCapped.append( Character.toUpperCase( schema.getSchemaName().charAt( 0 ) ) );

        schemaCapped.append( schema.getSchemaName().substring( 1, schema.getSchemaName().length() ) );

        VelocityContext context = new VelocityContext();

        context.put( "package", schema.getPackageName() );

        context.put( "classname", schemaCapped.toString() + "Schema" );

        context.put( "schema", schema.getSchemaName() );

        context.put( "owner", schema.getOwner() ) ;

        context.put( "deps", schema.getDependencies()  ) ;

        Reader fileIn = getResourceReader( "Schema.template" );

        Writer writer = getResourceWriter( schema.getUnqualifiedClassName() );

        Velocity.evaluate( context, writer, "LOG", fileIn );

        writer.flush();

        writer.close();
    }


    protected void generateRest() throws Exception
    {
        List types = new ArrayList();

        types.addAll( ProducerTypeEnum.list() );

        types.remove( ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER );

        types.remove( ProducerTypeEnum.OBJECT_CLASS_PRODUCER );

        ProducerTypeEnum type = null;

        for ( int ii = 0; ii < types.size(); ii++ )
        {
            type = ( ProducerTypeEnum ) types.get( ii );

            if ( exists( type ) )
            {
                continue;
            }


            VelocityContext context = new VelocityContext();

            context.put( "package", schema.getPackageName() );

            context.put( "classname", schema.getUnqualifiedClassName( type ) );

            context.put( "schema", schema.getSchemaName() );

            context.put( "owner", schema.getOwner() ) ;

            context.put( "type", type.getName().substring( 0, type.getName().length() - 8 ) ) ;

            String typeName = null;

            switch( type.getValue() )
            {
                case( ProducerTypeEnum.COMPARATOR_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.COMPARATOR_PRODUCER";

                    break;

                case( ProducerTypeEnum.DIT_CONTENT_RULE_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.DIT_CONTENT_RULE_PRODUCER";

                    break;

                case( ProducerTypeEnum.DIT_STRUCTURE_RULE_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.DIT_STRUCTURE_RULE_PRODUCER";

                    break;

                case( ProducerTypeEnum.MATCHING_RULE_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.MATCHING_RULE_PRODUCER";

                    break;

                case( ProducerTypeEnum.MATCHING_RULE_USE_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.MATCHING_RULE_USE_PRODUCER";

                    break;

                case( ProducerTypeEnum.NAME_FORM_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.NAME_FORM_PRODUCER";

                    break;

                case( ProducerTypeEnum.NORMALIZER_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.NORMALIZER_PRODUCER";

                    break;

                case( ProducerTypeEnum.SYNTAX_CHECKER_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.SYNTAX_CHECKER_PRODUCER";

                    break;

                case( ProducerTypeEnum.SYNTAX_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.SYNTAX_PRODUCER";

                    break;

                case( ProducerTypeEnum.STATE_FACTORY_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.STATE_FACTORY_PRODUCER";

                    break;

                case( ProducerTypeEnum.OBJECT_FACTORY_PRODUCER_VAL ):

                    typeName = "ProducerTypeEnum.OBJECT_FACTORY_PRODUCER";

                    break;

                default:

                    throw new IllegalStateException( "Unexpected producer: " + type.getName() );

            }

            context.put( "typeName", typeName ) ;

            runVelocity( context, "typeless.template", type );
        }
    }


    protected void generateAttributeTypes() throws Exception
    {
        final ProducerTypeEnum type = ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER;

        // check to see if the producer exists for this type
        if ( exists( type ) )
        {
            return;
        }

        int size = parser.getAttributeTypes().size();

        AttributeTypeLiteral[] attributeTypes = new AttributeTypeLiteral[size];

        attributeTypes = ( AttributeTypeLiteral[] ) parser.getAttributeTypes().values().toArray( attributeTypes );

        VelocityContext context = new VelocityContext();

        context.put( "package", schema.getPackageName() );

        context.put( "classname", schema.getUnqualifiedClassName( type ) );

        context.put( "schema", schema.getSchemaName() );

        context.put( "owner", schema.getOwner() ) ;

        context.put( "schemaDepCount", new Integer( schema.getDependencies().length ) );

        context.put( "schemaDeps", new String[] { "dep1", "dep2" }  ) ;

        context.put( "attrTypes", attributeTypes );

        runVelocity( context, "AttributeTypes.template", type );
    }


    protected void generateObjectClasses() throws Exception
    {
        final ProducerTypeEnum type = ProducerTypeEnum.OBJECT_CLASS_PRODUCER;

        // check to see if the producer exists for this type
        if ( exists( type ) )
        {
            return;
        }

        int size = parser.getObjectClassTypes().size();

        ObjectClassLiteral[] objectClasses = new ObjectClassLiteral[size];

        objectClasses = ( ObjectClassLiteral[] ) parser.getObjectClassTypes().values().toArray( objectClasses );

        VelocityContext context = new VelocityContext();

        context.put( "package", schema.getPackageName() );

        context.put( "classname", schema.getUnqualifiedClassName( type ) );

        context.put( "schema", schema.getSchemaName() );

        context.put( "owner", schema.getOwner() ) ;

        context.put( "schemaDepCount", new Integer( schema.getDependencies().length ) );

        context.put( "schemaDeps", new String[] { "dep1", "dep2" }  ) ;

        context.put( "objectClasses", objectClasses );

        runVelocity( context, "ObjectClasses.template", type );
    }



    protected void runVelocity( VelocityContext context, String template, ProducerTypeEnum type )
            throws Exception
    {
        Reader fileIn = getResourceReader( template );

        Writer writer = getResourceWriter( schema.getUnqualifiedClassName( type ) );

        Velocity.evaluate( context, writer, "LOG", fileIn );

        writer.flush();

        writer.close();
    }


    protected Reader getResourceReader( String res ) throws IOException
    {
        return new InputStreamReader( getClass().getResourceAsStream( res ) );
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
        String defaultClass = schema.getFullDefaultBaseClassName( type );

        // check to see if any of the classes are available in the java 
        // source directory, if so we return true
        File defaultFile = new File( getJavaSrcDir() + File.separator + getFilePath( defaultClass ) );
          
        return defaultFile.exists();
    }


    private String getFilePath( String fqcn )
    {
        String path = fqcn.replace( '.', File.separatorChar );

        path += ".java";

        return path;
    }
}
