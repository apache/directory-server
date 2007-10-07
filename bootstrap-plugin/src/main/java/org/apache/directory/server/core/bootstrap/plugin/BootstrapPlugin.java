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
package org.apache.directory.server.core.bootstrap.plugin;


import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.IndexNotFoundException;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStore;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.bootstrap.*;
import org.apache.directory.server.schema.registries.*;
import org.apache.directory.server.utils.AttributesFactory;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.*;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;


/**
 * A plugin used to pre-load meta schema entries into the schema partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 * @goal load
 * @description creates and pre-loads ApacheDS schema partition
 * @phase compile
 * @requiresDependencyResolution compile
 */
public class BootstrapPlugin extends AbstractMojo
{
    private static final String ADMIN_NORM_NAME = "0.9.2342.19200300.100.1.1=admin,2.5.4.11=system";

    /**
     * The classpath elements of the project being tested.
     *
     * @parameter expression="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    private List<String> classpathElements;

    /**
     * The package to put the db file entry listing info as well as the partition.
     *
     * @parameter expression="org.apache.directory.server.schema.bootstrap.partition"
     */
    private String outputPackage;

    /**
     * The file name to use for the package listing.
     *
     * @parameter expression="DBFILES"
     */
    private String listingFileName;

    /**
     * The target directory into which the plugin generates schema partion files
     * within the specified outputPackage.
     *
     * @parameter expression="target/classes"
     */
    private File outputDirectory;

    /**
     * The name of the set of bootstrap schemas to load into the registries
     * and ultimately into the schema partition being built.
     *
     * @parameter
     */
    private String[] bootstrapSchemaClasses;

    /**
     * The set of disabled schema names.
     *
     * @parameter
     */
    private String[] disabledSchemas;

    /**
     * The names of Attributes to index.
     *
     * @parameter
     */
    private String[] indexedAttributes;

    /**
     * Facotry used to create attributes objects from schema entities.
     */
    private AttributesFactory attributesFactory = new AttributesFactory();

    /**
     * Registries of objects used to load the schema partition.
     */
    private Registries registries;

    /**
     * The store to load schema entities into.
     */
    private JdbmStore store = new JdbmStore();

    /**
     * Map of schemas by name
     */
    private Map<String, Schema> schemas = new HashMap<String, Schema>();


    /**
     * Loads a bunch of bootstrap classes into memory then adds them to a new
     * schema partition within the target area.  The db files for this partition
     * are then packaged into the jar by the jar plugin.
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        File packageDirectory = new File( outputDirectory, outputPackage.replace( '.', File.separatorChar ) );
        
        if ( !packageDirectory.exists() )
        {
            packageDirectory.mkdirs();
        }

        // delete output directory if it exists
        File schemaDirectory = new File( packageDirectory, "schema" );
        
        if ( schemaDirectory.exists() )
        {
            try
            {
                FileUtils.forceDelete( schemaDirectory );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to delete old schema partition folder "
                        + schemaDirectory.getAbsolutePath() + ": " + e.getMessage() );
            }
        }

        initializeSchemas();
        initializePartition( schemaDirectory );

        try
        {
            LdapDN dn = new LdapDN( SchemaConstants.OU_AT + "=schema" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );

            if ( !hasEntry( dn ) )
            {
                Attributes entry = new AttributesImpl();
                entry.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC );
                entry.get( SchemaConstants.OBJECT_CLASS_AT ).add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
                entry.put( SchemaConstants.OU_AT, "schema" );
                store.add( dn, entry );
            }

            createSchemasAndContainers();

            addSyntaxCheckers();
            addSyntaxes();
            addNormalizers();
            addComparators();
            addMatchingRules();
            addAttributeTypes();
            addObjectClasses();

            if ( disabledSchemas != null && disabledSchemas.length > 0 )
            {
                getLog().info( "------------------------------------------------------------------------" );
                getLog().info( "Disabling schemas:" );
                getLog().info( "------------------------------------------------------------------------" );
                getLog().info( "" );

                for ( String disabledSchema : disabledSchemas )
                {
                    disableSchema( disabledSchema );
                    getLog().info( "\t\t o " + disabledSchema );
                }

                getLog().info( "" );
                getLog().info( "------------------------------------------------------------------------" );
            }
            
            createSchemaModificationAttributesEntry();
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
            throw new MojoFailureException( "Failed to add syntaxCheckers to partition: " + e.getMessage() );
        }

        try
        {
            store.sync();
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
        }

        // ------------------------------------------------------------------
        // Create db file listing and place it into the right package on disk
        // ------------------------------------------------------------------


        File listingFile = new File( packageDirectory, listingFileName );
        PrintWriter out = null;
        try
        {
            out = new PrintWriter( new FileWriter( listingFile ) );
            out.print( getDbFileListing().toString() );
            out.flush();
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to write file: " + e.getMessage() );
        }
        catch ( IndexNotFoundException e )
        {
            // never really thrown
            e.printStackTrace();
        }
        finally
        {
            if ( out != null )
            {
                out.close();
            }
        }
    }


    private static final String[] OTHER_SCHEMA_DEPENDENCIES = new String[] { "system", "core", "apache", "apachemeta" };
    
    private void createSchemasAndContainers() throws NamingException
    {
        Map<String, Schema> schemaMap = this.registries.getLoadedSchemas();
        
        for ( Schema schema:schemaMap.values() )
        {
            createSchemaAndContainers( schema );
        }
        
        Schema other = new Schema()
        {
            public String[] getDependencies()
            {
                return OTHER_SCHEMA_DEPENDENCIES;
            }

            public String getOwner()
            {
                return "uid=admin,ou=system";
            }

            public String getSchemaName()
            {
                return "other";
            }

            public boolean isDisabled()
            {
                return false;
            }
        };
        
        createSchemaAndContainers( other );
    }


    private void createSchemaAndContainers( Schema schema ) throws NamingException
    {
        LdapDN dn = new LdapDN( SchemaConstants.CN_AT + "=" 
            + schema.getSchemaName() + "," + SchemaConstants.OU_AT + "=schema" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );

        if ( hasEntry( dn ) )
        {
            return;
        }
        
        Attributes entry = attributesFactory.getAttributes( schema );
        store.add( dn, entry );
        
        dn.add( SchemaConstants.OU_AT + "=comparators" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
        
        dn.remove( dn.size() - 1 );
        dn.add( SchemaConstants.OU_AT + "=normalizers" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
        
        dn.remove( dn.size() - 1 );
        dn.add( SchemaConstants.OU_AT + "=syntaxCheckers" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
        
        dn.remove( dn.size() - 1 );
        dn.add( SchemaConstants.OU_AT + "=syntaxes" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
        
        dn.remove( dn.size() - 1 );
        dn.add( SchemaConstants.OU_AT + "=matchingRules" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
        
        dn.remove( dn.size() - 1 );
        dn.add( SchemaConstants.OU_AT + "=attributeTypes" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
        
        dn.remove( dn.size() - 1 );
        dn.add( SchemaConstants.OU_AT + "=objectClasses" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
        
        dn.remove( dn.size() - 1 );
        dn.add( SchemaConstants.OU_AT + "=nameForms" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
        
        dn.remove( dn.size() - 1 );
        dn.add( SchemaConstants.OU_AT + "=ditStructureRules" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
        
        dn.remove( dn.size() - 1 );
        dn.add( SchemaConstants.OU_AT + "=ditContentRules" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
        
        dn.remove( dn.size() - 1 );
        dn.add( SchemaConstants.OU_AT + "=matchingRuleUse" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        checkCreateContainer( dn );
    }

    
    private void addAttributeTypes() throws NamingException
    {
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( " Adding attributeTypes:" );
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( "" );

        AttributeTypeRegistry attributeTypeRegistry = registries.getAttributeTypeRegistry();
        
        Iterator<AttributeType> ii = attributeTypeRegistry.iterator();
        
        while ( ii.hasNext() )
        {
            AttributeType at = ii.next();
            String schemaName = attributeTypeRegistry.getSchemaName( at.getOid() );
            Schema schema = registries.getLoadedSchemas().get( schemaName );
            getLog().info( "\t\t o [" + schemaName + "] - " + getNameOrNumericoid( at ) );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( SchemaConstants.OU_AT + "=attributeTypes" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( at, schema );
            dn.add( MetaSchemaConstants.M_OID_AT + "=" + at.getOid() );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            store.add( dn, entry );
        }
        
        getLog().info( "" );
    }


    private void addObjectClasses() throws NamingException
    {
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( " Adding objectClasses:" );
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( "" );

        ObjectClassRegistry objectClassRegistry = registries.getObjectClassRegistry();
        Iterator<ObjectClass> ii = objectClassRegistry.iterator();
        
        while ( ii.hasNext() )
        {
            ObjectClass oc = ii.next();
            String schemaName = objectClassRegistry.getSchemaName( oc.getOid() );
            Schema schema = registries.getLoadedSchemas().get( schemaName );
            getLog().info( "\t\t o [" + schemaName + "] - " + getNameOrNumericoid( oc ) );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( SchemaConstants.OU_AT + "=objectClasses" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( oc, schema );
            dn.add( MetaSchemaConstants.M_OID_AT + "=" + oc.getOid() );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            store.add( dn, entry );
        }
        
        getLog().info( "" );
    }


    private void addMatchingRules() throws NamingException
    {
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( " Adding matchingRules:" );
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( "" );

        MatchingRuleRegistry matchingRuleRegistry = registries.getMatchingRuleRegistry();
        Iterator<MatchingRule> ii = matchingRuleRegistry.iterator();
        
        while ( ii.hasNext() )
        {
            MatchingRule mr = ii.next();
            String schemaName = matchingRuleRegistry.getSchemaName( mr.getOid() );
            Schema schema = registries.getLoadedSchemas().get( schemaName );
            getLog().info( "\t\t o [" + schemaName + "] - " + getNameOrNumericoid( mr ) );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( SchemaConstants.OU_AT + "=matchingRules" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( mr, schema );
            dn.add( MetaSchemaConstants.M_OID_AT + "=" + mr.getOid() );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            store.add( dn, entry );
        }
        
        getLog().info( "" );
    }


    private void addComparators() throws NamingException
    {
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( " Adding comparators:" );
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( "" );

        ComparatorRegistry comparatorRegistry = registries.getComparatorRegistry();
        Iterator<String> ii = comparatorRegistry.oidIterator();
        
        while ( ii.hasNext() )
        {
            String oid = ii.next();
            String schemaName = comparatorRegistry.getSchemaName( oid );
            Schema schema = registries.getLoadedSchemas().get( schemaName );
            getLog().info( "\t\t o [" + schemaName + "] - " + oid );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( SchemaConstants.OU_AT + "=comparators" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( oid, comparatorRegistry.lookup( oid ), schema );
            dn.add( MetaSchemaConstants.M_OID_AT + "=" + oid );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            store.add( dn, entry );
        }
        getLog().info( "" );
    }


    private void addNormalizers() throws NamingException
    {
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( " Adding normalizers:" );
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( "" );

        NormalizerRegistry normalizerRegistry = registries.getNormalizerRegistry();
        Iterator<String> ii = normalizerRegistry.oidIterator();
        
        while ( ii.hasNext() )
        {
            String oid = ii.next();
            String schemaName = normalizerRegistry.getSchemaName( oid );
            Schema schema = registries.getLoadedSchemas().get( schemaName );
            getLog().info( "\t\t o [" + schemaName + "] - " + oid );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( SchemaConstants.OU_AT + "=normalizers" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( oid, normalizerRegistry.lookup( oid ), schema );
            dn.add( MetaSchemaConstants.M_OID_AT + "=" + oid );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            store.add( dn, entry );
        }
        
        getLog().info( "" );
    }


    private void addSyntaxes() throws NamingException
    {
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( " Adding syntaxes:" );
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( "" );

        SyntaxRegistry syntaxRegistry = registries.getSyntaxRegistry();
        Iterator<Syntax> ii = syntaxRegistry.iterator();
        
        while ( ii.hasNext() )
        {
            Syntax syntax = ii.next();
            getLog().info( "\t\t o [" + syntax.getSchema() + "] - " + getNameOrNumericoid( syntax ) );
            LdapDN dn = checkCreateSchema( syntax.getSchema() );
            Schema schema = registries.getLoadedSchemas().get( syntax.getSchema() );
            dn.add( SchemaConstants.OU_AT + "=syntaxes" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( syntax, schema );
            dn.add( MetaSchemaConstants.M_OID_AT + "=" + syntax.getOid() );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            store.add( dn, entry );
        }
        getLog().info( "" );
    }


    private void addSyntaxCheckers() throws NamingException
    {
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( " Adding syntaxCheckers:" );
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( "" );

        SyntaxCheckerRegistry syntaxCheckerRegistry = registries.getSyntaxCheckerRegistry();
        Iterator<SyntaxChecker> ii = syntaxCheckerRegistry.iterator();
        
        while ( ii.hasNext() )
        {
            SyntaxChecker syntaxChecker = ii.next();
            String schemaName = syntaxCheckerRegistry.getSchemaName( syntaxChecker.getSyntaxOid() );
            Schema schema = registries.getLoadedSchemas().get( schemaName );
            getLog().info( "\t\t o [" + schemaName + "] - " + syntaxChecker.getSyntaxOid() );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( SchemaConstants.OU_AT + "=syntaxCheckers" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( syntaxChecker, schema );
            dn.add( MetaSchemaConstants.M_OID_AT + "=" + syntaxChecker.getSyntaxOid() );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            store.add( dn, entry );
        }
        
        getLog().info( "" );
    }


    /**
     * Creates the configuration and initializes the partition so we can start
     * adding entries into it.
     *
     * @throws MojoFailureException
     */
    private void initializePartition( File workingDirectory ) throws MojoFailureException
    {
        store.setCacheSize( 1000 );
        store.setEnableOptimizer( false );
        store.setName( "schema" );
        store.setSuffixDn( SchemaConstants.OU_AT + "=schema" );
        store.setSyncOnWrite( false );
        store.setWorkingDirectory( workingDirectory );

        // add the indices
        Set<JdbmIndex> userIndices = new HashSet<JdbmIndex>();
        
        for ( String indexedAttribute:indexedAttributes )
        {
            JdbmIndex index = new JdbmIndex();
            index.setAttributeId( indexedAttribute );
            userIndices.add( index );
        }
        
        store.setUserIndices( userIndices );

        Attributes rootEntry = new AttributesImpl( SchemaConstants.OBJECT_CLASS_AT, 
            SchemaConstants.ORGANIZATIONAL_UNIT_OC, true );
        rootEntry.put( SchemaConstants.OU_AT, "schema" );
        store.setContextEntry( rootEntry );

        try
        {
            store.init( this.registries.getOidRegistry(), this.registries.getAttributeTypeRegistry() );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
            throw new MojoFailureException( "Failed to initialize parition: " + e.getMessage() );
        }
    }


    /**
     * Creates the special schemaModificationsAttribute entry used to
     * store the modification attributes for the schema.  The current
     * time is used to create the initial values for the attributes in
     * this entry.
     * 
     * @throws NamingException if there is a failure to add the entry 
     */
    private void createSchemaModificationAttributesEntry() throws NamingException
    {
        Attributes entry = new AttributesImpl( 
            SchemaConstants.OBJECT_CLASS_AT, 
            ApacheSchemaConstants.SCHEMA_MODIFICATION_ATTRIBUTES_OC,
            true );
        entry.get( SchemaConstants.OBJECT_CLASS_AT ).add( SchemaConstants.TOP_OC );
        
        entry.put( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT, ADMIN_NORM_NAME );
        entry.put( SchemaConstants.MODIFIERS_NAME_AT, ADMIN_NORM_NAME );
        entry.put( SchemaConstants.CREATORS_NAME_AT, ADMIN_NORM_NAME );
        
        entry.put( ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        entry.put( SchemaConstants.MODIFY_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        entry.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
        
        entry.put( SchemaConstants.CN_AT, "schemaModifications" );
        entry.put( ApacheSchemaConstants.SUBSCHEMA_SUBENTRY_NAME_AT, "cn=schema" );
        
        LdapDN normName = new LdapDN( "cn=schemaModifications,ou=schema" );
        normName.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        store.add( normName, entry );
    }
    
    
    /**
     * Loads all the bootstrap schemas into the registries in preparation for
     * loading them into the schema partition.
     *
     * @throws MojoFailureException
     */
    private void initializeSchemas() throws MojoFailureException
    {
        // -------------------------------------------------------------------
        // load the bootstrap schemas to pre-load into the partition
        // -------------------------------------------------------------------

        // always include these core bootstrap schemas
        BootstrapSchema schema = new SystemSchema();
        schemas.put( schema.getSchemaName(), schema );

        schema = new ApacheSchema();
        schemas.put( schema.getSchemaName(), schema );

        schema = new ApachemetaSchema();
        schemas.put( schema.getSchemaName(), schema );

        schema = new CoreSchema();
        schemas.put( schema.getSchemaName(), schema );

        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( "Found bootstrap schemas: " );
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( "" );

        // start loading other schemas from the plugin's configuration section
        ClassLoader parent = getClass().getClassLoader();
        URL[] urls = new URL[classpathElements.size()];
        int i = 0;
        
        for ( String classpathElement:classpathElements )
        {
            try
            {
                urls[i++] = new File( classpathElement ).toURI().toURL();
            } 
            catch ( MalformedURLException e )
            {
                throw ( MojoFailureException ) new MojoFailureException( "Could not construct classloader: " ).initCause( e );
            }
        }
        
        ClassLoader cl = new URLClassLoader( urls, parent );
        
        for ( int ii = 0; ii < bootstrapSchemaClasses.length; ii++ )
        {
            try
            {
                Class schemaClass = cl.loadClass( bootstrapSchemaClasses[ii] );
                schema = ( BootstrapSchema ) schemaClass.newInstance();
                schemas.put( schema.getSchemaName(), schema );
            }
            catch ( ClassNotFoundException e )
            {
                getLog().info( "ClassLoader " + getClass().getClassLoader() );
                getLog().info( "ClassLoader URLs: " + Arrays.asList( ( ( URLClassLoader ) getClass().getClassLoader() ).getURLs() ) );
                e.printStackTrace();
                throw new MojoFailureException( "Could not find BootstrapSchema class: "
                        + bootstrapSchemaClasses[ii] );
            }
            catch ( InstantiationException e )
            {
                e.printStackTrace();
                throw new MojoFailureException( "Could not instantiate BootstrapSchema class: "
                        + bootstrapSchemaClasses[ii] );
            }
            catch ( IllegalAccessException e )
            {
                e.printStackTrace();
                throw new MojoFailureException( "Could not instantiate BootstrapSchema class due to security: "
                        + bootstrapSchemaClasses[ii] );
            }

            getLog().info( "\t" + bootstrapSchemaClasses[ii] );
        }
        
        getLog().info( "" );

        BootstrapSchemaLoader loader = new BootstrapSchemaLoader( cl );
        registries = new DefaultRegistries( "bootstrap", loader, new DefaultOidRegistry() );
        
        try
        {
            loader.loadWithDependencies( schemas.values(), registries );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
            throw new MojoFailureException( "Failed to load bootstrap registries with schemas: " + e.getMessage() );
        }

        SerializableComparator.setRegistry( registries.getComparatorRegistry() );
    }


    private void checkCreateContainer( LdapDN dn ) throws NamingException
    {
        if ( hasEntry( dn ) )
        {
            return;
        }

        Attributes entry = new AttributesImpl();
        entry.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC );
        entry.get( SchemaConstants.OBJECT_CLASS_AT ).add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( SchemaConstants.OU_AT, dn.getRdn().getValue() );
        store.add( dn, entry );
    }


    private LdapDN checkCreateSchema( String schemaName ) throws NamingException
    {
        Schema schema = schemas.get( schemaName );
        LdapDN dn = new LdapDN( SchemaConstants.CN_AT + "="
                + schemaName + "," + SchemaConstants.OU_AT + "=schema" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );

        if ( hasEntry( dn ) )
        {
            return dn;
        }

        Attributes entry = attributesFactory.getAttributes( schema );
        store.add( dn, entry );
        return dn;
    }


    private void disableSchema( String schemaName ) throws NamingException
    {
        LdapDN dn = new LdapDN( SchemaConstants.CN_AT + "=" + schemaName
                + "," + SchemaConstants.OU_AT + "=schema" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        
        ModificationItemImpl mod = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE,
                new AttributeImpl( MetaSchemaConstants.M_DISABLED_AT, "TRUE" ) );
        
        List<ModificationItemImpl> mods = new ArrayList<ModificationItemImpl>();
        mods.add( mod );
        store.modify( dn, mods );
    }


    private final String getNameOrNumericoid( SchemaObject object )
    {
        // first try to use userfriendly name if we can
        if ( object.getName() != null )
        {
            return object.getName();
        }

        return object.getOid();
    }


    private final boolean hasEntry( LdapDN dn ) throws NamingException
    {
        Long id = store.getEntryId( dn.toNormName() );
        
        return ( id != null );
    }


    private final StringBuffer getDbFileListing() throws IndexNotFoundException
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "schema/master.db\n" );

        Iterator<String> systemIndices = store.systemIndices();
        
        while ( systemIndices.hasNext() )
        {
            Index index = store.getSystemIndex(systemIndices.next() );
            buf.append( "schema/" );
            buf.append( index.getAttribute().getName() );
            buf.append( ".db\n" );
        }

        buf.append( "[USER INDICES]\n" );
        
        for ( String indexedAttribute:indexedAttributes )
        {
            buf.append( "schema/" );
            buf.append( indexedAttribute );
            buf.append( ".db\n" );
        }

        return buf;
    }
}
