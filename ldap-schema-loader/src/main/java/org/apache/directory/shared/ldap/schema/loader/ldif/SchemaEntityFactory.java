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
package org.apache.directory.shared.ldap.schema.loader.ldif;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.EntityFactory;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.LoadableSchemaObject;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.registries.DefaultSchema;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Showing how it's done ...
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaEntityFactory implements EntityFactory
{
    /** Slf4j logger */
    private final static Logger LOG = LoggerFactory.getLogger( SchemaEntityFactory.class );

    /** for fast debug checks */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** Used for looking up the setRegistries(Registries) method */
    private final static Class<?>[] parameterTypes = new Class[]
        { Registries.class };

    private static final List<String> EMPTY_LIST = new ArrayList<String>();
    private static final String[] EMPTY_ARRAY = new String[]
        {};

    /** A special ClassLoader that loads a class from the bytecode attribute */
    private final AttributeClassLoader classLoader;


    public SchemaEntityFactory() throws Exception
    {
        this.classLoader = new AttributeClassLoader();
    }


    /**
     * Get an OID from an entry. Handles the bad cases (null OID, 
     * not a valid OID, ...)
     */
    private String getOid( Entry entry, String objectType ) throws NamingException
    {
        // The OID
        EntryAttribute mOid = entry.get( MetaSchemaConstants.M_OID_AT );

        if ( mOid == null )
        {
            String msg = objectType + " entry must have a valid " + MetaSchemaConstants.M_OID_AT
                + " attribute, it's null";
            LOG.warn( msg );
            throw new NullPointerException( msg );
        }

        String oid = mOid.getString();

        if ( !OID.isOID( oid ) )
        {
            String msg = "Comparator OID " + oid + " is not a valid OID ";
            LOG.warn( msg );
            throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
        }

        return oid;
    }


    /**
     * Get an OID from an entry. Handles the bad cases (null OID, 
     * not a valid OID, ...)
     */
    private String getOid( SchemaObject description, String objectType ) throws NamingException
    {
        // The OID
        String oid = description.getOid();

        if ( oid == null )
        {
            String msg = objectType + " entry must have a valid " + MetaSchemaConstants.M_OID_AT
                + " attribute, it's null";
            LOG.warn( msg );
            throw new NullPointerException( msg );
        }

        if ( !OID.isOID( oid ) )
        {
            String msg = "Comparator OID " + oid + " is not a valid OID ";
            LOG.warn( msg );
            throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
        }

        return oid;
    }


    /**
     * Check that the Entry is not null
     */
    private void checkEntry( Entry entry, String schemaEntity )
    {
        if ( entry == null )
        {
            String msg = schemaEntity + " entry cannot be null";
            LOG.warn( msg );
            throw new NullPointerException( msg );
        }
    }


    /**
     * Check that the Description is not null
     */
    private void checkDescription( SchemaObject description, String schemaEntity )
    {
        if ( description == null )
        {
            String msg = schemaEntity + " Schema description cannot be null";
            LOG.warn( msg );
            throw new NullPointerException( msg );
        }
    }


    /**
     * Get the schema from its name. Return the Other reference if there
     * is no schema name. Throws a NPE if the schema is not loaded.
     */
    private Schema getSchema( String schemaName, Registries registries )
    {
        if ( StringTools.isEmpty( schemaName ) )
        {
            schemaName = MetaSchemaConstants.SCHEMA_OTHER;
        }

        Schema schema = registries.getLoadedSchema( schemaName );

        if ( schema == null )
        {
            String msg = "The schema " + schemaName + " does not exists or is not loaded";
            LOG.error( msg );
        }

        return schema;
    }


    /**
     * {@inheritDoc}
     */
    public Schema getSchema( Entry entry ) throws Exception
    {
        String name;
        String owner;
        String[] dependencies = EMPTY_ARRAY;
        boolean isDisabled = false;

        if ( entry == null )
        {
            throw new NullPointerException( "entry cannot be null" );
        }

        if ( entry.get( SchemaConstants.CN_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid cn attribute" );
        }

        name = entry.get( SchemaConstants.CN_AT ).getString();

        if ( entry.get( SchemaConstants.CREATORS_NAME_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid " + SchemaConstants.CREATORS_NAME_AT
                + " attribute" );
        }

        owner = entry.get( SchemaConstants.CREATORS_NAME_AT ).getString();

        if ( entry.get( MetaSchemaConstants.M_DISABLED_AT ) != null )
        {
            String value = entry.get( MetaSchemaConstants.M_DISABLED_AT ).getString();
            value = value.toUpperCase();
            isDisabled = value.equals( "TRUE" );
        }

        if ( entry.get( MetaSchemaConstants.M_DEPENDENCIES_AT ) != null )
        {
            Set<String> depsSet = new HashSet<String>();
            EntryAttribute depsAttr = entry.get( MetaSchemaConstants.M_DEPENDENCIES_AT );

            for ( Value<?> value : depsAttr )
            {
                depsSet.add( value.getString() );
            }

            dependencies = depsSet.toArray( EMPTY_ARRAY );
        }

        return new DefaultSchema( name, owner, dependencies, isDisabled );
    }


    /**
     * Class load a syntaxChecker instance
     */
    private SyntaxChecker classLoadSyntaxChecker( SchemaManager schemaManager, String oid, String className,
        EntryAttribute byteCode, Registries targetRegistries ) throws Exception
    {
        // Try to class load the syntaxChecker
        Class<?> clazz = null;
        SyntaxChecker syntaxChecker = null;
        String byteCodeStr = StringTools.EMPTY;

        if ( byteCode == null )
        {
            clazz = Class.forName( className );
        }
        else
        {
            classLoader.setAttribute( byteCode );
            clazz = classLoader.loadClass( className );
            byteCodeStr = new String( Base64.encode( byteCode.getBytes() ) );
        }

        // Create the syntaxChecker instance
        syntaxChecker = ( SyntaxChecker ) clazz.newInstance();

        // Update the common fields
        syntaxChecker.setBytecode( byteCodeStr );
        syntaxChecker.setFqcn( className );

        // Inject the new OID, as the loaded syntaxChecker might have its own
        syntaxChecker.setOid( oid );

        return syntaxChecker;
    }


    /**
     * {@inheritDoc}
     */
    public SyntaxChecker getSyntaxChecker( SchemaManager schemaManager, Entry entry, Registries targetRegistries,
        String schemaName ) throws Exception
    {
        checkEntry( entry, SchemaConstants.SYNTAX_CHECKER );

        // The SyntaxChecker OID
        String oid = getOid( entry, SchemaConstants.SYNTAX_CHECKER );

        // Get the schema
        if ( !schemaManager.isSchemaLoaded( schemaName ) )
        {
            // The schema is not loaded. We can't create the requested Normalizer
            String msg = "Cannot add the SyntaxChecker " + entry.getDn().getName() + ", as the associated schema ("
                + schemaName + " is not loaded";
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( schemaName, targetRegistries );

        if ( schema == null )
        {
            // The schema is disabled. We still have to update the backend
            String msg = "Cannot add the SyntaxChecker " + entry.getDn().getName() + " into the registries, "
                + "as the associated schema (" + schemaName + ") is disabled";
            LOG.info( msg );
            schema = schemaManager.getLoadedSchema( schemaName );
        }

        // The FQCN
        String className = getFqcn( entry, SchemaConstants.SYNTAX_CHECKER );

        // The ByteCode
        EntryAttribute byteCode = entry.get( MetaSchemaConstants.M_BYTECODE_AT );

        // Class load the syntaxChecker
        SyntaxChecker syntaxChecker = classLoadSyntaxChecker( schemaManager, oid, className, byteCode, targetRegistries );

        // Update the common fields
        setSchemaObjectProperties( syntaxChecker, entry, schema );

        // return the resulting syntaxChecker
        return syntaxChecker;
    }


    /**
     * {@inheritDoc}
     */
    public SyntaxChecker getSyntaxChecker( SchemaManager schemaManager,
        SyntaxCheckerDescription syntaxCheckerDescription, Registries targetRegistries, String schemaName )
        throws Exception
    {
        checkDescription( syntaxCheckerDescription, SchemaConstants.SYNTAX_CHECKER );

        // The Comparator OID
        String oid = getOid( syntaxCheckerDescription, SchemaConstants.SYNTAX_CHECKER );

        // Get the schema
        Schema schema = getSchema( schemaName, targetRegistries );

        if ( schema == null )
        {
            // The schema is not loaded. We can't create the requested SyntaxChecker
            String msg = "Cannot add the SyntaxChecker " + syntaxCheckerDescription.getName()
                + ", as the associated schema (" + schemaName + " is not loaded";
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        // The FQCN
        String fqcn = getFqcn( syntaxCheckerDescription, SchemaConstants.SYNTAX_CHECKER );

        // get the byteCode
        EntryAttribute byteCode = getByteCode( syntaxCheckerDescription, SchemaConstants.SYNTAX_CHECKER );

        // Class load the SyntaxChecker
        SyntaxChecker syntaxChecker = classLoadSyntaxChecker( schemaManager, oid, fqcn, byteCode, targetRegistries );

        // Update the common fields
        setSchemaObjectProperties( syntaxChecker, syntaxCheckerDescription, schema );

        return syntaxChecker;
    }


    /**
     * Class load a comparator instances
     */
    private LdapComparator<?> classLoadComparator( SchemaManager schemaManager, String oid, String className,
        EntryAttribute byteCode, Registries targetRegistries ) throws Exception
    {
        // Try to class load the comparator
        LdapComparator<?> comparator = null;
        Class<?> clazz = null;
        String byteCodeStr = StringTools.EMPTY;

        if ( byteCode == null )
        {
            clazz = Class.forName( className );
        }
        else
        {
            classLoader.setAttribute( byteCode );
            clazz = classLoader.loadClass( className );
            byteCodeStr = new String( Base64.encode( byteCode.getBytes() ) );
        }

        // Create the comparator instance. Either we have a no argument constructor,
        // or we have one which takes an OID. Lets try the one with an OID argument first
        try
        {
            Constructor<?> constructor = clazz.getConstructor( new Class[]
                { String.class } );
            comparator = ( LdapComparator<?> ) constructor.newInstance( new Object[]
                { oid } );
        }
        catch ( NoSuchMethodException nsme )
        {
            // Ok, let's try with the constructor without argument.
            // In this case, we will have to check that the OID is the same than
            // the one we got in the Comparator entry
            Constructor<?> constructor = clazz.getConstructor();
            comparator = ( LdapComparator<?> ) clazz.newInstance();
            
            if ( !comparator.getOid().equals( oid ) )
            {
                String msg = "The Comparator's OID (" + oid + ") is different from the loaded class' OID (" + comparator.getOid();
                throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
        }

        // Update the loadable fields
        comparator.setBytecode( byteCodeStr );
        comparator.setFqcn( className );

        // Inject the SchemaManager for the comparator who needs it
        comparator.setSchemaManager( schemaManager );

        return comparator;
    }


    /**
     * {@inheritDoc}
     */
    public LdapComparator<?> getLdapComparator( SchemaManager schemaManager,
        LdapComparatorDescription comparatorDescription, Registries targetRegistries, String schemaName )
        throws Exception
    {
        checkDescription( comparatorDescription, SchemaConstants.COMPARATOR );

        // The Comparator OID
        String oid = getOid( comparatorDescription, SchemaConstants.COMPARATOR );

        // Get the schema
        Schema schema = getSchema( schemaName, targetRegistries );

        if ( schema == null )
        {
            // The schema is not loaded. We can't create the requested Comparator
            String msg = "Cannot add the Comparator " + comparatorDescription.getName()
                + ", as the associated schema (" + schemaName + " is not loaded";
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        // The FQCN
        String fqcn = getFqcn( comparatorDescription, SchemaConstants.COMPARATOR );

        // get the byteCode
        EntryAttribute byteCode = getByteCode( comparatorDescription, SchemaConstants.COMPARATOR );

        // Class load the comparator
        LdapComparator<?> comparator = classLoadComparator( schemaManager, oid, fqcn, byteCode, targetRegistries );

        // Update the common fields
        setSchemaObjectProperties( comparator, comparatorDescription, schema );

        return comparator;
    }


    /**
     * {@inheritDoc}
     */
    public LdapComparator<?> getLdapComparator( SchemaManager schemaManager, Entry entry, Registries targetRegistries,
        String schemaName ) throws Exception
    {
        checkEntry( entry, SchemaConstants.COMPARATOR );

        // The Comparator OID
        String oid = getOid( entry, SchemaConstants.COMPARATOR );

        // Get the schema
        if ( !schemaManager.isSchemaLoaded( schemaName ) )
        {
            // The schema is not loaded. We can't create the requested Comparator
            String msg = "Cannot add the Comparator " + entry.getDn().getName() + ", as the associated schema ("
                + schemaName + " is not loaded";
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( schemaName, targetRegistries );

        if ( schema == null )
        {
            // The schema is disabled. We still have to update the backend
            String msg = "Cannot add the Comparator " + entry.getDn().getName() + " into the registries, "
                + "as the associated schema (" + schemaName + ") is disabled";
            LOG.info( msg );
            schema = schemaManager.getLoadedSchema( schemaName );
        }

        // The FQCN
        String fqcn = getFqcn( entry, SchemaConstants.COMPARATOR );

        // The ByteCode
        EntryAttribute byteCode = entry.get( MetaSchemaConstants.M_BYTECODE_AT );

        // Class load the comparator
        LdapComparator<?> comparator = classLoadComparator( schemaManager, oid, fqcn, byteCode, targetRegistries );

        // Update the common fields
        setSchemaObjectProperties( comparator, entry, schema );

        // return the resulting comparator
        return comparator;
    }


    /**
     * Class load a normalizer instances
     */
    private Normalizer classLoadNormalizer( SchemaManager schemaManager, String oid, String className,
        EntryAttribute byteCode, Registries targetRegistries ) throws Exception
    {
        // Try to class load the normalizer
        Class<?> clazz = null;
        Normalizer normalizer = null;
        String byteCodeStr = StringTools.EMPTY;

        if ( byteCode == null )
        {
            clazz = Class.forName( className );
        }
        else
        {
            classLoader.setAttribute( byteCode );
            clazz = classLoader.loadClass( className );
            byteCodeStr = new String( Base64.encode( byteCode.getBytes() ) );
        }

        // Create the normalizer instance
        normalizer = ( Normalizer ) clazz.newInstance();
        
        // Update the common fields
        normalizer.setBytecode( byteCodeStr );
        normalizer.setFqcn( className );

        // Inject the new OID, as the loaded normalizer might have its own
        normalizer.setOid( oid );

        // Inject the SchemaManager for the normalizer who needs it
        normalizer.setSchemaManager( schemaManager );

        return normalizer;
    }


    /**
     * {@inheritDoc}
     */
    public Normalizer getNormalizer( SchemaManager schemaManager, NormalizerDescription normalizerDescription,
        Registries targetRegistries, String schemaName ) throws Exception
    {
        checkDescription( normalizerDescription, SchemaConstants.NORMALIZER );

        // The Comparator OID
        String oid = getOid( normalizerDescription, SchemaConstants.NORMALIZER );

        // Get the schema
        Schema schema = getSchema( schemaName, targetRegistries );

        if ( schema == null )
        {
            // The schema is not loaded. We can't create the requested Normalizer
            String msg = "Cannot add the Normalizer " + normalizerDescription.getName()
                + ", as the associated schema (" + schemaName + " is not loaded";
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        // The FQCN
        String fqcn = getFqcn( normalizerDescription, SchemaConstants.NORMALIZER );

        // get the byteCode
        EntryAttribute byteCode = getByteCode( normalizerDescription, SchemaConstants.NORMALIZER );

        // Class load the normalizer
        Normalizer normalizer = classLoadNormalizer( schemaManager, oid, fqcn, byteCode, targetRegistries );

        // Update the common fields
        setSchemaObjectProperties( normalizer, normalizerDescription, schema );

        return normalizer;
    }


    /**
     * {@inheritDoc}
     */
    public Normalizer getNormalizer( SchemaManager schemaManager, Entry entry, Registries targetRegistries,
        String schemaName ) throws Exception
    {
        checkEntry( entry, SchemaConstants.NORMALIZER );

        // The Normalizer OID
        String oid = getOid( entry, SchemaConstants.NORMALIZER );

        // Get the schema
        if ( !schemaManager.isSchemaLoaded( schemaName ) )
        {
            // The schema is not loaded. We can't create the requested Normalizer
            String msg = "Cannot add the Normalizer " + entry.getDn().getName() + ", as the associated schema ("
                + schemaName + " is not loaded";
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( schemaName, targetRegistries );

        if ( schema == null )
        {
            // The schema is disabled. We still have to update the backend
            String msg = "Cannot add the Normalizer " + entry.getDn().getName() + " into the registries, "
                + "as the associated schema (" + schemaName + ") is disabled";
            LOG.info( msg );
            schema = schemaManager.getLoadedSchema( schemaName );
        }

        // The FQCN
        String className = getFqcn( entry, SchemaConstants.NORMALIZER );

        // The ByteCode
        EntryAttribute byteCode = entry.get( MetaSchemaConstants.M_BYTECODE_AT );

        // Class load the Normalizer
        Normalizer normalizer = classLoadNormalizer( schemaManager, oid, className, byteCode, targetRegistries );

        // Update the common fields
        setSchemaObjectProperties( normalizer, entry, schema );

        // return the resulting Normalizer
        return normalizer;
    }


    /**
     * Uses reflection to see if a setRegistries( Registries ) method exists on the
     * object's class.  If so then the registries are dependency injected into the 
     * new schema object.
     * 
     * @param obj a schema object to have a Registries dependency injected.
     */
    private void injectRegistries( Object obj, Registries targetRegistries ) throws Exception
    {
        Method method = null;

        try
        {
            method = obj.getClass().getMethod( "setRegistries", parameterTypes );
        }
        catch ( NoSuchMethodException e )
        {
            if ( IS_DEBUG )
            {
                LOG.debug( obj.getClass() + " has no setRegistries() method." );
            }

            return;
        }

        if ( method == null )
        {
            return;
        }

        Object[] args = new Object[]
            { targetRegistries };
        method.invoke( obj, args );
    }


    /**
     * {@inheritDoc}
     */
    public LdapSyntax getSyntax( SchemaManager schemaManager, Entry entry, Registries targetRegistries,
        String schemaName ) throws NamingException
    {
        checkEntry( entry, SchemaConstants.SYNTAX );

        // The Syntax OID
        String oid = getOid( entry, SchemaConstants.SYNTAX );

        // Get the schema
        if ( !schemaManager.isSchemaLoaded( schemaName ) )
        {
            // The schema is not loaded. We can't create the requested Syntax
            String msg = "Cannot add the Syntax " + entry.getDn().getName() + ", as the associated schema ("
                + schemaName + " is not loaded";
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( schemaName, targetRegistries );

        if ( schema == null )
        {
            // The schema is disabled. We still have to update the backend
            String msg = "Cannot add the SyntaxChecker " + entry.getDn().getName() + " into the registries, "
                + "as the associated schema (" + schemaName + ") is disabled";
            LOG.info( msg );
            schema = schemaManager.getLoadedSchema( schemaName );
        }

        // Create the new LdapSyntax instance
        LdapSyntax syntax = new LdapSyntax( oid );

        // The isHumanReadable field
        EntryAttribute mHumanReadable = entry.get( MetaSchemaConstants.X_HUMAN_READABLE_AT );

        if ( mHumanReadable != null )
        {
            String val = mHumanReadable.getString();
            syntax.setHumanReadable( val.toUpperCase().equals( "TRUE" ) );
        }

        // Common properties
        setSchemaObjectProperties( syntax, entry, schema );

        return syntax;
    }


    /**
     * {@inheritDoc}
     */
    public MatchingRule getMatchingRule( SchemaManager schemaManager, Entry entry, Registries targetRegistries,
        String schemaName ) throws NamingException
    {
        checkEntry( entry, SchemaConstants.MATCHING_RULE );

        // The MatchingRule OID
        String oid = getOid( entry, SchemaConstants.MATCHING_RULE );

        // Get the schema
        if ( !schemaManager.isSchemaLoaded( schemaName ) )
        {
            // The schema is not loaded. We can't create the requested MatchingRule
            String msg = "Cannot add the MatchingRule " + entry.getDn().getName() + ", as the associated schema ("
                + schemaName + " is not loaded";
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( schemaName, targetRegistries );

        if ( schema == null )
        {
            // The schema is disabled. We still have to update the backend
            String msg = "Cannot add the MatchingRule " + entry.getDn().getName() + " into the registries, "
                + "as the associated schema (" + schemaName + ") is disabled";
            LOG.info( msg );
            schema = schemaManager.getLoadedSchema( schemaName );
        }

        MatchingRule matchingRule = new MatchingRule( oid );

        // The syntax field
        EntryAttribute mSyntax = entry.get( MetaSchemaConstants.M_SYNTAX_AT );

        if ( mSyntax != null )
        {
            matchingRule.setSyntaxOid( mSyntax.getString() );
        }

        // The normalizer and comparator fields will be updated when we will
        // apply the registry 

        // Common properties
        setSchemaObjectProperties( matchingRule, entry, schema );

        return matchingRule;
    }


    /**
     * Create a list of string from a multivalued attribute's values
     */
    private List<String> getStrings( EntryAttribute attr ) throws NamingException
    {
        if ( attr == null )
        {
            return EMPTY_LIST;
        }

        List<String> strings = new ArrayList<String>( attr.size() );

        for ( Value<?> value : attr )
        {
            strings.add( value.getString() );
        }

        return strings;
    }


    /**
     * {@inheritDoc}
     */
    public ObjectClass getObjectClass( SchemaManager schemaManager, Entry entry, Registries targetRegistries,
        String schemaName ) throws Exception
    {
        checkEntry( entry, SchemaConstants.OBJECT_CLASS );

        // The ObjectClass OID
        String oid = getOid( entry, SchemaConstants.OBJECT_CLASS );

        // Get the schema
        if ( !schemaManager.isSchemaLoaded( schemaName ) )
        {
            // The schema is not loaded. We can't create the requested ObjectClass
            String msg = "Cannot add the ObjectClass " + entry.getDn().getName() + ", as the associated schema ("
                + schemaName + " is not loaded";
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( schemaName, targetRegistries );

        if ( schema == null )
        {
            // The schema is disabled. We still have to update the backend
            String msg = "Cannot add the ObjectClass " + entry.getDn().getName() + " into the registries, "
                + "as the associated schema (" + schemaName + ") is disabled";
            LOG.info( msg );
            schema = schemaManager.getLoadedSchema( schemaName );
        }

        // Create the ObjectClass instance
        ObjectClass oc = new ObjectClass( oid );

        // The Sup field
        EntryAttribute mSuperiors = entry.get( MetaSchemaConstants.M_SUP_OBJECT_CLASS_AT );

        if ( mSuperiors != null )
        {
            oc.setSuperiorOids( getStrings( mSuperiors ) );
        }

        // The May field
        EntryAttribute mMay = entry.get( MetaSchemaConstants.M_MAY_AT );

        if ( mMay != null )
        {
            oc.setMayAttributeTypeOids( getStrings( mMay ) );
        }

        // The Must field
        EntryAttribute mMust = entry.get( MetaSchemaConstants.M_MUST_AT );

        if ( mMust != null )
        {
            oc.setMustAttributeTypeOids( getStrings( mMust ) );
        }

        // The objectClassType field
        EntryAttribute mTypeObjectClass = entry.get( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT );

        if ( mTypeObjectClass != null )
        {
            String type = mTypeObjectClass.getString();
            oc.setType( ObjectClassTypeEnum.getClassType( type ) );
        }

        // Common properties
        setSchemaObjectProperties( oc, entry, schema );

        return oc;
    }


    /**
     * {@inheritDoc}
     */
    public AttributeType getAttributeType( SchemaManager schemaManager, Entry entry, Registries targetRegistries,
        String schemaName ) throws NamingException
    {
        checkEntry( entry, SchemaConstants.ATTRIBUTE_TYPE );

        // The AttributeType OID
        String oid = getOid( entry, SchemaConstants.ATTRIBUTE_TYPE );

        // Get the schema
        if ( !schemaManager.isSchemaLoaded( schemaName ) )
        {
            // The schema is not loaded, this is an error
            String msg = "Cannot add the AttributeType " + entry.getDn().getName() + ", as the associated schema ("
                + schemaName + " is not loaded";
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( schemaName, targetRegistries );

        if ( schema == null )
        {
            // The schema is disabled. We still have to update the backend
            String msg = "Cannot add the AttributeType " + entry.getDn().getName() + " into the registries, "
                + "as the associated schema (" + schemaName + ") is disabled";
            LOG.info( msg );
            schema = schemaManager.getLoadedSchema( schemaName );
        }

        // Create the new AttributeType
        AttributeType attributeType = new AttributeType( oid );

        // Syntax
        EntryAttribute mSyntax = entry.get( MetaSchemaConstants.M_SYNTAX_AT );

        if ( ( mSyntax != null ) && ( mSyntax.get() != null ) )
        {
            attributeType.setSyntaxOid( mSyntax.getString() );
        }

        // Syntax Length
        EntryAttribute mSyntaxLength = entry.get( MetaSchemaConstants.M_LENGTH_AT );

        if ( mSyntaxLength != null )
        {
            attributeType.setSyntaxLength( Integer.parseInt( mSyntaxLength.getString() ) );
        }

        // Equality
        EntryAttribute mEquality = entry.get( MetaSchemaConstants.M_EQUALITY_AT );

        if ( mEquality != null )
        {
            attributeType.setEqualityOid( mEquality.getString() );
        }

        // Ordering
        EntryAttribute mOrdering = entry.get( MetaSchemaConstants.M_ORDERING_AT );

        if ( mOrdering != null )
        {
            attributeType.setOrderingOid( mOrdering.getString() );
        }

        // Substr
        EntryAttribute mSubstr = entry.get( MetaSchemaConstants.M_SUBSTR_AT );

        if ( mSubstr != null )
        {
            attributeType.setSubstringOid( mSubstr.getString() );
        }

        EntryAttribute mSupAttributeType = entry.get( MetaSchemaConstants.M_SUP_ATTRIBUTE_TYPE_AT );

        // Sup
        if ( mSupAttributeType != null )
        {
            attributeType.setSuperiorOid( mSupAttributeType.getString() );
        }

        // isCollective
        EntryAttribute mCollective = entry.get( MetaSchemaConstants.M_COLLECTIVE_AT );

        if ( mCollective != null )
        {
            String val = mCollective.getString();
            attributeType.setCollective( val.equalsIgnoreCase( "TRUE" ) );
        }

        // isSingleValued
        EntryAttribute mSingleValued = entry.get( MetaSchemaConstants.M_SINGLE_VALUE_AT );

        if ( mSingleValued != null )
        {
            String val = mSingleValued.getString();
            attributeType.setSingleValued( val.equalsIgnoreCase( "TRUE" ) );
        }

        // isReadOnly
        EntryAttribute mNoUserModification = entry.get( MetaSchemaConstants.M_NO_USER_MODIFICATION_AT );

        if ( mNoUserModification != null )
        {
            String val = mNoUserModification.getString();
            attributeType.setUserModifiable( !val.equalsIgnoreCase( "TRUE" ) );
        }

        // Usage
        EntryAttribute mUsage = entry.get( MetaSchemaConstants.M_USAGE_AT );

        if ( mUsage != null )
        {
            attributeType.setUsage( UsageEnum.getUsage( mUsage.getString() ) );
        }

        // Common properties
        setSchemaObjectProperties( attributeType, entry, schema );

        return attributeType;
    }


    /**
     * Process the FQCN attribute
     */
    private String getFqcn( Entry entry, String objectType ) throws NamingException
    {
        // The FQCN
        EntryAttribute mFqcn = entry.get( MetaSchemaConstants.M_FQCN_AT );

        if ( mFqcn == null )
        {
            String msg = objectType + " entry must have a valid " + MetaSchemaConstants.M_FQCN_AT + " attribute";
            LOG.warn( msg );
            throw new NullPointerException( msg );
        }

        return mFqcn.getString();
    }


    /**
     * Process the FQCN attribute
     */
    private String getFqcn( LoadableSchemaObject description, String objectType ) throws NamingException
    {
        // The FQCN
        String mFqcn = description.getFqcn();

        if ( mFqcn == null )
        {
            String msg = objectType + " entry must have a valid " + MetaSchemaConstants.M_FQCN_AT + " attribute";
            LOG.warn( msg );
            throw new NullPointerException( msg );
        }

        return mFqcn;
    }


    /**
     * Process the ByteCode attribute
     */
    private EntryAttribute getByteCode( Entry entry, String objectType ) throws NamingException
    {
        EntryAttribute byteCode = entry.get( MetaSchemaConstants.M_BYTECODE_AT );

        if ( byteCode == null )
        {
            String msg = objectType + " entry must have a valid " + MetaSchemaConstants.M_BYTECODE_AT + " attribute";
            LOG.warn( msg );
            throw new NullPointerException( msg );
        }

        return byteCode;
    }


    /**
     * Process the ByteCode attribute
     */
    private EntryAttribute getByteCode( LoadableSchemaObject description, String objectType ) throws NamingException
    {
        String byteCodeString = description.getBytecode();

        if ( byteCodeString == null )
        {
            String msg = objectType + " entry must have a valid " + MetaSchemaConstants.M_BYTECODE_AT + " attribute";
            LOG.warn( msg );
            throw new NullPointerException( msg );
        }

        byte[] bytecode = Base64.decode( byteCodeString.toCharArray() );
        EntryAttribute attr = new DefaultClientAttribute( MetaSchemaConstants.M_BYTECODE_AT, bytecode );

        return attr;
    }


    /**
     * Process the common attributes to all SchemaObjects :
     *  - obsolete
     *  - description
     *  - names
     *  - schemaName
     *  - specification (if any)
     *  - extensions
     *  - isReadOnly
     *  - isEnabled
     */
    private void setSchemaObjectProperties( SchemaObject schemaObject, Entry entry, Schema schema )
        throws NamingException
    {
        // The isObsolete field
        EntryAttribute mObsolete = entry.get( MetaSchemaConstants.M_OBSOLETE_AT );

        if ( mObsolete != null )
        {
            String val = mObsolete.getString();
            schemaObject.setObsolete( val.equalsIgnoreCase( "TRUE" ) );
        }

        // The description field
        EntryAttribute mDescription = entry.get( MetaSchemaConstants.M_DESCRIPTION_AT );

        if ( mDescription != null )
        {
            schemaObject.setDescription( mDescription.getString() );
        }

        // The names field
        EntryAttribute names = entry.get( MetaSchemaConstants.M_NAME_AT );

        if ( names != null )
        {
            List<String> values = new ArrayList<String>();

            for ( Value<?> name : names )
            {
                values.add( name.getString() );
            }

            schemaObject.setNames( values );
        }

        // The isEnabled field
        EntryAttribute mDisabled = entry.get( MetaSchemaConstants.M_DISABLED_AT );

        // If the SchemaObject has an explicit m-disabled attribute, then use it.
        // Otherwise, inherit it from the schema
        if ( mDisabled != null )
        {
            String val = mDisabled.getString();
            schemaObject.setEnabled( !val.equalsIgnoreCase( "TRUE" ) );
        }
        else
        {
            schemaObject.setEnabled( schema != null && schema.isEnabled() );
        }

        // The isReadOnly field
        EntryAttribute mIsReadOnly = entry.get( MetaSchemaConstants.M_NO_USER_MODIFICATION_AT );

        if ( mIsReadOnly != null )
        {
            String val = mIsReadOnly.getString();
            schemaObject.setReadOnly( val.equalsIgnoreCase( "TRUE" ) );
        }

        // The specification field
        /*
         * TODO : create the M_SPECIFICATION_AT
        EntryAttribute mSpecification = entry.get( MetaSchemaConstants.M_SPECIFICATION_AT );
        
        if ( mSpecification != null )
        {
            so.setSpecification( mSpecification.getString() ); 
        }
        */

        // The schemaName field
        schemaObject.setSchemaName( schema.getSchemaName() );

        // The extensions field
        /*
         * TODO create the M_EXTENSION_AT AT
        EntryAttribute extensions = entry.get( MetaSchemaConstants.M_EXTENSION_AT );
        
        if ( extensions != null )
        {
            List<String> extensions = new ArrayList<String>();
            
            for ( Value<?> extension:extensions )
            {
                values.add( extension() );
            }
            
            so.setExtensions( values );
        }
        */
    }


    /**
     * Process the common attributes to all SchemaObjects :
     *  - obsolete
     *  - description
     *  - names
     *  - schemaName
     *  - specification (if any)
     *  - extensions
     *  - isReadOnly
     *  - isEnabled
     */
    private void setSchemaObjectProperties( SchemaObject schemaObject, SchemaObject description, Schema schema )
        throws NamingException
    {
        // The isObsolete field
        schemaObject.setObsolete( description.isObsolete() );

        // The description field
        schemaObject.setDescription( description.getDescription() );

        // The names field
        schemaObject.setNames( description.getNames() );

        // The isEnabled field. Has the description does not hold a 
        // Disable field, we will inherit from the schema enable field
        schemaObject.setEnabled( schema.isEnabled() );

        // The isReadOnly field. We don't have this data in the description,
        // so set it to false
        // TODO : should it be a X-READONLY extension ?
        schemaObject.setReadOnly( false );

        // The specification field
        schemaObject.setSpecification( description.getSpecification() );

        // The schemaName field
        schemaObject.setSchemaName( schema.getSchemaName() );

        // The extensions field
        schemaObject.setExtensions( description.getExtensions() );
    }
}
