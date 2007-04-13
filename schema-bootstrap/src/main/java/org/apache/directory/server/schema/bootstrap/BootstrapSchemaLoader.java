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
package org.apache.directory.server.schema.bootstrap;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import javax.naming.NamingException;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchema;
import org.apache.directory.server.schema.bootstrap.ProducerTypeEnum;
import org.apache.directory.server.schema.bootstrap.AbstractBootstrapProducer.BootstrapAttributeType;
import org.apache.directory.server.schema.bootstrap.AbstractBootstrapProducer.BootstrapMatchingRule;
import org.apache.directory.server.schema.bootstrap.AbstractBootstrapProducer.BootstrapObjectClass;
import org.apache.directory.server.schema.bootstrap.AbstractBootstrapProducer.BootstrapSyntax;
import org.apache.directory.server.schema.registries.AbstractSchemaLoader;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.ComparatorRegistry;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.server.schema.registries.MatchingRuleRegistry;
import org.apache.directory.server.schema.registries.NormalizerRegistry;
import org.apache.directory.server.schema.registries.ObjectClassRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.schema.registries.SyntaxCheckerRegistry;
import org.apache.directory.server.schema.registries.SyntaxRegistry;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.ComparatorDescription;
import org.apache.directory.shared.ldap.schema.syntax.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxCheckerDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class which handles bootstrap schema class file loading.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapSchemaLoader extends AbstractSchemaLoader
{
    private static final Logger log = LoggerFactory.getLogger( BootstrapSchemaLoader.class );

    private ClassLoader cl = getClass().getClassLoader();

    /** stores schemas of producers for callback access */
    private ThreadLocal<BootstrapSchema> schemas;
    /** stores registries associated with producers for callback access */
    private ThreadLocal<Registries> registries;
    /** the callback that just calls register() */
    private final ProducerCallback cb = new ProducerCallback()
    {
        public void schemaObjectProduced( BootstrapProducer producer, String registryKey, Object schemaObject )
            throws NamingException
        {
            register( producer.getType(), registryKey, schemaObject );
        }
    };


    /**
     * Creates a BootstrapSchema loader.
     */
    public BootstrapSchemaLoader()
    {
        schemas = new ThreadLocal<BootstrapSchema>();
        registries = new ThreadLocal<Registries>();
    }


    public BootstrapSchemaLoader( ClassLoader cl )
    {
        this();
        this.cl = cl;
    }

    public final void loadWithDependencies( Schema schema, Registries registries ) throws NamingException
    {
        if ( ! ( schema instanceof BootstrapSchema ) )
        {
            throw new NamingException( "Expecting schema to be of sub-type BootstrapSchema" );
        }
        
        Map<String, Schema> notLoaded = new HashMap<String, Schema>();
        notLoaded.put( schema.getSchemaName(), schema );
        Properties props = new Properties();
        props.put( "package", ( ( BootstrapSchema ) schema ).getPackageName() );
        loadDepsFirst( schema, new Stack<String>(), notLoaded, schema, registries, props );
    }

    
    /**
     * Loads a set of schemas by loading and running all producers for each
     * dependent schema first.
     *
     * @param bootstrapSchemas Collection of {@link BootstrapSchema}s to load
     * @param registries the registries to fill with producer created objects
     * @throws NamingException if there are any failures during this process
     */
    public final void loadWithDependencies( Collection<Schema> bootstrapSchemas, Registries registries ) throws NamingException
    {
        BootstrapSchema[] schemas = new BootstrapSchema[bootstrapSchemas.size()];
        schemas = ( BootstrapSchema[] ) bootstrapSchemas.toArray( schemas );
        HashMap<String,Schema> loaded = new HashMap<String,Schema>();
        HashMap<String,Schema> notLoaded = new HashMap<String,Schema>();

        for ( int ii = 0; ii < schemas.length; ii++ )
        {
            notLoaded.put( schemas[ii].getSchemaName(), schemas[ii] );
        }

        BootstrapSchema schema;

        // Create system schema and kick it off by loading system which
        // will never depend on anything.
        schema = new SystemSchema();
        load( schema, registries, false );
        notLoaded.remove( schema.getSchemaName() ); // Remove if user specified it.
        loaded.put( schema.getSchemaName(), schema );

        Iterator list = notLoaded.values().iterator();
        while ( list.hasNext() )
        {
            schema = ( BootstrapSchema ) list.next();
            Properties props = new Properties();
            props.put( "package", schema.getPackageName() );
            loadDepsFirst( schema, new Stack<String>(), notLoaded, schema, registries, props );
            list = notLoaded.values().iterator();
        }
    }


    /**
     * Loads a schema by loading and running all producers for the schema.
     *
     * @param schema the schema to load
     * @param registries the registries to fill with producer created objects
     * @throws NamingException if there are any failures during this process
     */
    public final void load( Schema schema, Registries registries, boolean isDepLoad ) throws NamingException
    {
        if ( registries.getLoadedSchemas().containsKey( schema.getSchemaName() ) )
        {
            return;
        }
        
        if ( ! ( schema instanceof BootstrapSchema ) )
        {
            throw new NamingException( "Expecting schema to be of sub-type BootstrapSchema" );
        }
        
        this.registries.set( registries );
        this.schemas.set( ( BootstrapSchema ) schema );

        for ( ProducerTypeEnum producerType:ProducerTypeEnum.getList() )
        {
            BootstrapProducer producer = getProducer( ( BootstrapSchema ) schema, producerType.getName() );
            producer.produce( registries, cb );
        }

        notifyListenerOrRegistries( schema, registries );
    }


    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------

    /**
     * Registers objects
     *
     * @param type the type of the producer which determines the type of object produced
     * @param id the primary key identifying the created object in a registry
     * @param schemaObject the object being registered
     * @throws NamingException if there are problems when registering the object
     * in any of the registries
     */
    private void register( ProducerTypeEnum type, String id, Object schemaObject ) throws NamingException
    {
        BootstrapSchema schema = this.schemas.get();
        DefaultRegistries registries = ( DefaultRegistries ) this.registries.get();
        List<String> values = new ArrayList<String>(1);
        values.add( schema.getSchemaName() );

        switch ( type )
        {
            case NORMALIZER_PRODUCER :
                Normalizer normalizer = ( Normalizer ) schemaObject;
                NormalizerRegistry normalizerRegistry;
                normalizerRegistry = registries.getNormalizerRegistry();
                
                NormalizerDescription normalizerDescription = new NormalizerDescription();
                normalizerDescription.setNumericOid( id );
                normalizerDescription.setFqcn( normalizer.getClass().getName() );
                normalizerDescription.addExtension( MetaSchemaConstants.X_SCHEMA, values );
                
                normalizerRegistry.register( normalizerDescription, normalizer );
                break;
                
            case COMPARATOR_PRODUCER :
                Comparator comparator = ( Comparator ) schemaObject;
                ComparatorRegistry comparatorRegistry;
                comparatorRegistry = registries.getComparatorRegistry();
                
                ComparatorDescription comparatorDescription = new ComparatorDescription();
                comparatorDescription.addExtension( MetaSchemaConstants.X_SCHEMA, values );
                comparatorDescription.setFqcn( comparator.getClass().getName() );
                comparatorDescription.setNumericOid( id );
                
                comparatorRegistry.register( comparatorDescription, comparator );
                break;
                
            case SYNTAX_CHECKER_PRODUCER :
                SyntaxChecker syntaxChecker = ( SyntaxChecker ) schemaObject;
                SyntaxCheckerRegistry syntaxCheckerRegistry;
                syntaxCheckerRegistry = registries.getSyntaxCheckerRegistry();
                
                SyntaxCheckerDescription syntaxCheckerDescription = new SyntaxCheckerDescription();
                syntaxCheckerDescription.addExtension( MetaSchemaConstants.X_SCHEMA, values );
                syntaxCheckerDescription.setFqcn( syntaxChecker.getClass().getName() );
                syntaxCheckerDescription.setNumericOid( id );
                
                syntaxCheckerRegistry.register( syntaxCheckerDescription, syntaxChecker );
                break;
                
            case SYNTAX_PRODUCER :
                Syntax syntax = ( Syntax ) schemaObject;
                
                if ( schemaObject instanceof BootstrapSyntax )
                {
                    ( ( BootstrapSyntax ) syntax ).setSchema( schema.getSchemaName() );
                }

                SyntaxRegistry syntaxRegistry = registries.getSyntaxRegistry();
                syntaxRegistry.register( syntax );
                break;
                
            case MATCHING_RULE_PRODUCER :
                MatchingRule matchingRule = ( MatchingRule ) schemaObject;
                
                if ( schemaObject instanceof BootstrapMatchingRule )
                {
                    ( ( BootstrapMatchingRule ) matchingRule ).setSchema( schema.getSchemaName() );
                }

                MatchingRuleRegistry matchingRuleRegistry;
                matchingRuleRegistry = registries.getMatchingRuleRegistry();
                matchingRuleRegistry.register( matchingRule );
                break;
                
            case ATTRIBUTE_TYPE_PRODUCER :
                AttributeType attributeType = ( AttributeType ) schemaObject;
                
                if ( attributeType instanceof BootstrapAttributeType )
                {
                    ( ( BootstrapAttributeType ) attributeType ).setSchema( schema.getSchemaName() );
                }
                
                AttributeTypeRegistry attributeTypeRegistry;
                attributeTypeRegistry = registries.getAttributeTypeRegistry();
                attributeTypeRegistry.register( attributeType );
                break;
                
            case OBJECT_CLASS_PRODUCER :
                ObjectClass objectClass = ( ObjectClass ) schemaObject;
                
                if ( objectClass instanceof BootstrapObjectClass )
                {
                    ( ( BootstrapObjectClass ) objectClass ).setSchema( schema.getSchemaName() );
                }
                
                ObjectClassRegistry objectClassRegistry;
                objectClassRegistry = registries.getObjectClassRegistry();
                objectClassRegistry.register( objectClass );
                break;
                
            default:
                throw new IllegalStateException( "ProducerTypeEnum value is invalid: " + type );
        }
    }


    /**
     * Attempts first to try to load the target class for the Producer,
     * then tries for the default if the target load fails.
     *
     * @param schema the bootstrap schema
     * @param producerBase the producer's base name
     * @throws NamingException if there are failures loading classes
     */
    private BootstrapProducer getProducer( BootstrapSchema schema, String producerBase ) throws NamingException
    {
        Class clazz = null;
        boolean failedTargetLoad = false;
        String defaultClassName;
        String targetClassName = schema.getBaseClassName() + producerBase;

        try
        {
            clazz = Class.forName( targetClassName, true, cl );
        }
        catch ( ClassNotFoundException e )
        {
            failedTargetLoad = true;
            log.debug( "Failed to load '" + targetClassName + "'.  Trying the alternative.", e );
        }

        if ( failedTargetLoad )
        {
            defaultClassName = schema.getDefaultBaseClassName() + producerBase;

            try
            {
                clazz = Class.forName( defaultClassName, true, cl );
            }
            catch ( ClassNotFoundException e )
            {
                NamingException ne = new NamingException( "Failed to load " + producerBase + " for "
                    + schema.getSchemaName() + " schema using following classes: " + targetClassName + ", "
                    + defaultClassName );
                ne.setRootCause( e );
                throw ne;
            }
        }

        try
        {
            return ( BootstrapProducer ) clazz.newInstance();
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException( "Failed to create " + clazz );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( InstantiationException e )
        {
            NamingException ne = new NamingException( "Failed to create " + clazz );
            ne.setRootCause( e );
            throw ne;
        }
    }


    public Schema getSchema( String schemaName ) throws NamingException
    {
        return getSchema( schemaName, null );
    }
    
    
    public Schema getSchema( String schemaName, Properties schemaProperties ) throws NamingException
    {
        String baseName = schemaName;
        schemaName = schemaName.toLowerCase();
        StringBuffer buf = new StringBuffer();

        
        if ( schemaProperties == null || schemaProperties.getProperty( "package" ) == null )
        {
            // first see if we can load a schema object using the default bootstrap package
            Properties props = new Properties();
            props.put( "package", "org.apache.directory.server.schema.bootstrap" );
            
            try
            {
                Schema schema = getSchema( baseName, props );
                return schema;
            }
            catch( NamingException e )
            {
                throw new NamingException( "Can't find the bootstrap schema class in the default " +
                        "\n bootstrap schema package.  I need a package name property with key \"package\"." );
            }
        }
        
        buf.append( schemaProperties.getProperty( "package" ) );
        buf.append( '.' );
        buf.append( Character.toUpperCase( schemaName.charAt( 0 ) ) );
        buf.append( schemaName.substring( 1 ) );
        schemaName = buf.toString();
        
        Schema schema = null;
        try
        {
            schema = ( Schema ) Class.forName( schemaName, true, cl ).newInstance();
        }
        catch ( InstantiationException e )
        {
            NamingException ne = new NamingException( "Failed to instantiate schema object: " + schemaName );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = 
                new NamingException( "Failed to access default constructor of schema object: " + schemaName );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( ClassNotFoundException e )
        {
            NamingException ne = new NamingException( "Schema class not found: " + schemaName );
            ne.setRootCause( e );
            throw ne;
        }
        
        return schema;
    }
}
