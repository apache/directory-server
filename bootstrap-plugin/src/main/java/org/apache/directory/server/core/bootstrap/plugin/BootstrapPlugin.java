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


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.constants.CoreSchemaConstants;
import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.IndexNotFoundException;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStore;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStoreConfiguration;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.ApachemetaSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.ComparatorRegistry;
import org.apache.directory.server.schema.registries.DefaultOidRegistry;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.server.schema.registries.MatchingRuleRegistry;
import org.apache.directory.server.schema.registries.NormalizerRegistry;
import org.apache.directory.server.schema.registries.ObjectClassRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.schema.registries.SyntaxCheckerRegistry;
import org.apache.directory.server.schema.registries.SyntaxRegistry;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;


/**
 * A plugin used to pre-load meta schema entries into the schema partition.
 *
 * @goal load 
 * @description creates and pre-loads ApacheDS schema partition
 * @phase compile
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapPlugin extends AbstractMojo
{
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
    
    /** Facotry used to create attributes objects from schema entities. */ 
    private AttributesFactory attributesFactory = new AttributesFactory();
    
    /** Registries of objects used to load the schema partition. */
    private Registries registries;

    /** The store to load schema entities into. */
    private JdbmStore store = new JdbmStore();
    
    /** Map of schemas by name */
    private Map schemas = new HashMap();
    
    
    /**
     * Loads a bunch of bootstrap classes into memory then adds them to a new 
     * schema partition within the target area.  The db files for this partition
     * are then packaged into the jar by the jar plugin.
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        File packageDirectory = new File( outputDirectory, outputPackage.replace( '.', File.separatorChar ) );
        if ( ! packageDirectory.exists() )
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
            LdapDN dn = new LdapDN( CoreSchemaConstants.OU_AT + "=schema" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            
            if ( ! hasEntry( dn ) )
            {
                Attributes entry = new AttributesImpl();
                entry.put( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
                entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( "organizationalUnit" );
                entry.put( CoreSchemaConstants.OU_AT, "schema" );
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

                for ( int ii = 0; ii < disabledSchemas.length; ii++ )
                {
                    disableSchema( disabledSchemas[ii] );
                    getLog().info( "\t\t o " + disabledSchemas[ii] );
                }
                
                getLog().info( "" );
                getLog().info( "------------------------------------------------------------------------" );
            }
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

    
    private void createSchemasAndContainers() throws NamingException
    {
        Map schemaMap = this.registries.getLoadedSchemas();
        Iterator schemas = schemaMap.values().iterator();
        while ( schemas.hasNext() )
        {
            Schema schema = ( Schema ) schemas.next();
            LdapDN dn = new LdapDN( SystemSchemaConstants.CN_AT + "=" 
                + schema.getSchemaName() + "," + CoreSchemaConstants.OU_AT + "=schema" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );

            if ( hasEntry( dn ) )
            {
                continue;
            }
            
            Attributes entry = attributesFactory.getAttributes( schema );
            store.add( dn, entry );
            dn.add( CoreSchemaConstants.OU_AT + "=comparators" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            dn.remove( dn.size() - 1 );
            dn.add( CoreSchemaConstants.OU_AT + "=normalizers" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            dn.remove( dn.size() - 1 );
            dn.add( CoreSchemaConstants.OU_AT + "=syntaxCheckers" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            dn.remove( dn.size() - 1 );
            dn.add( CoreSchemaConstants.OU_AT + "=syntaxes" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            dn.remove( dn.size() - 1 );
            dn.add( CoreSchemaConstants.OU_AT + "=matchingRules" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            dn.remove( dn.size() - 1 );
            dn.add( CoreSchemaConstants.OU_AT + "=attributeTypes" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            dn.remove( dn.size() - 1 );
            dn.add( CoreSchemaConstants.OU_AT + "=objectClasses" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
        }
    }


    private void addAttributeTypes() throws NamingException
    {
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( " Adding attributeTypes:" );
        getLog().info( "------------------------------------------------------------------------" );
        getLog().info( "" );

        AttributeTypeRegistry attributeTypeRegistry = registries.getAttributeTypeRegistry();
        Iterator ii = attributeTypeRegistry.iterator();
        while ( ii.hasNext() )
        {
            AttributeType at = ( AttributeType ) ii.next();
            String schemaName = attributeTypeRegistry.getSchemaName( at.getOid() );
            getLog().info( "\t\t o [" + schemaName + "] - " + getNameOrNumericoid( at ) );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( CoreSchemaConstants.OU_AT + "=attributeTypes" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( at );
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
        Iterator ii = objectClassRegistry.iterator();
        while ( ii.hasNext() )
        {
            ObjectClass oc = ( ObjectClass ) ii.next();
            String schemaName = objectClassRegistry.getSchemaName( oc.getOid() );
            getLog().info( "\t\t o [" + schemaName + "] - " + getNameOrNumericoid( oc ) );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( CoreSchemaConstants.OU_AT + "=objectClasses" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( oc );
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
        Iterator ii = matchingRuleRegistry.iterator();
        while ( ii.hasNext() )
        {
            MatchingRule mr = ( MatchingRule ) ii.next();
            String schemaName = matchingRuleRegistry.getSchemaName( mr.getOid() );
            getLog().info( "\t\t o [" + schemaName + "] - " + getNameOrNumericoid( mr ) );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( CoreSchemaConstants.OU_AT + "=matchingRules" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( mr );
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
        Iterator ii = comparatorRegistry.oidIterator();
        while ( ii.hasNext() )
        {
            String oid = ( String ) ii.next();
            String schemaName = comparatorRegistry.getSchemaName( oid );
            getLog().info( "\t\t o [" + schemaName + "] - " + oid );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( CoreSchemaConstants.OU_AT + "=comparators" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( oid, comparatorRegistry.lookup( oid ) );
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
        Iterator ii = normalizerRegistry.oidIterator();
        while ( ii.hasNext() )
        {
            String oid = ( String ) ii.next();
            String schemaName = normalizerRegistry.getSchemaName( oid );
            getLog().info( "\t\t o [" + schemaName + "] - " + oid );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( CoreSchemaConstants.OU_AT + "=normalizers" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( oid, normalizerRegistry.lookup( oid ) );
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
        Iterator ii = syntaxRegistry.iterator();
        while ( ii.hasNext() )
        {
            Syntax syntax = ( Syntax ) ii.next();
            String schemaName = syntaxRegistry.getSchemaName( syntax.getOid() );
            getLog().info( "\t\t o [" + schemaName + "] - " + getNameOrNumericoid( syntax ) );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( CoreSchemaConstants.OU_AT + "=syntaxes" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( syntax );
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
        Iterator ii = syntaxCheckerRegistry.iterator();
        while ( ii.hasNext() )
        {
            SyntaxChecker syntaxChecker = ( SyntaxChecker ) ii.next();
            String schemaName = syntaxCheckerRegistry.getSchemaName( syntaxChecker.getSyntaxOid() );
            getLog().info( "\t\t o [" + schemaName + "] - " + syntaxChecker.getSyntaxOid() );
            LdapDN dn = checkCreateSchema( schemaName );
            dn.add( CoreSchemaConstants.OU_AT + "=syntaxCheckers" );
            dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            checkCreateContainer( dn );
            Attributes entry = attributesFactory.getAttributes( syntaxChecker );
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
        JdbmStoreConfiguration storeConfig = new JdbmStoreConfiguration();
        storeConfig.setAttributeTypeRegistry( registries.getAttributeTypeRegistry() );
        storeConfig.setCacheSize( 1000 );
        storeConfig.setEnableOptimizer( false );
        storeConfig.setName( "schema" );
        storeConfig.setOidRegistry( registries.getOidRegistry() );
        storeConfig.setSuffixDn( CoreSchemaConstants.OU_AT + "=schema" );
        storeConfig.setSyncOnWrite( false );
        storeConfig.setWorkingDirectory( workingDirectory );

        // add the indices
        Set indexSet = new HashSet();
        for ( int ii = 0; ii < indexedAttributes.length; ii++ )
        {
            indexSet.add( indexedAttributes[ii] );
        }
        storeConfig.setIndexedAttributes( indexSet );

        Attributes rootEntry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "organizationalUnit", true );
        rootEntry.put( CoreSchemaConstants.OU_AT, "schema" );
        storeConfig.setContextEntry( rootEntry );
        
        try
        {
            store.init( storeConfig );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
            throw new MojoFailureException( "Failed to initialize parition: " + e.getMessage() );
        }
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
        for ( int ii = 0; ii < bootstrapSchemaClasses.length; ii++ )
        {
            try
            {
                Class schemaClass = Class.forName( bootstrapSchemaClasses[ii] );
                schema = ( BootstrapSchema ) schemaClass.newInstance();
                schemas.put( schema.getSchemaName(), schema );
            }
            catch ( ClassNotFoundException e )
            {
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
        
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
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
        entry.put( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( "organizationalUnit" );
        entry.put( CoreSchemaConstants.OU_AT, dn.getRdn().getValue() );
        store.add( dn, entry );
    }
    
    
    private LdapDN checkCreateSchema( String schemaName ) throws NamingException
    {
        Schema schema = ( Schema ) schemas.get( schemaName );
        LdapDN dn = new LdapDN( SystemSchemaConstants.CN_AT + "=" 
            + schemaName + "," + CoreSchemaConstants.OU_AT + "=schema" );
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
        LdapDN dn = new LdapDN( SystemSchemaConstants.CN_AT + "=" + schemaName 
            + "," + CoreSchemaConstants.OU_AT + "=schema" );
        dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        ModificationItemImpl mod = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, 
            new AttributeImpl( MetaSchemaConstants.M_DISABLED_AT, "TRUE" ) );
        ModificationItemImpl[] mods = new ModificationItemImpl[] { mod };
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
        BigInteger id = store.getEntryId( dn.toNormName() );
        if ( id == null )
        {
            return false;
        }
        return true;
    }
    
    
    private final StringBuffer getDbFileListing() throws IndexNotFoundException
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "schema/master.db\n" );
        
        Iterator systemIndices = store.getSystemIndices();
        while( systemIndices.hasNext() )
        {
            Index index = store.getSystemIndex( ( String ) systemIndices.next() );
            buf.append( "schema/" );
            buf.append( index.getAttribute().getName() );
            buf.append( ".db\n" );
        }

        buf.append( "[USER INDICES]\n" );
        for ( int ii = 0; ii < indexedAttributes.length; ii++ )
        {
            buf.append( "schema/" );
            buf.append( indexedAttributes[ii] );
            buf.append( ".db\n" );
        }
        
        return buf;
    }
}
