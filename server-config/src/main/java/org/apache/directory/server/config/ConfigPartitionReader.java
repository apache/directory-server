/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.config;


import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.ObjectClass;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.config.beans.AdsBaseBean;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.PartitionSearchResult;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class used for reading the configuration present in a Partition
 * and instantiate the necessary objects like DirectoryService, Interceptors etc.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigPartitionReader
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ConfigPartitionReader.class );

    /** the partition which holds the configuration data */
    private AbstractBTreePartition configPartition;

    /** the search engine of the partition */
    private SearchEngine se;

    /** the schema manager set in the config partition */
    private SchemaManager schemaManager;

    /** The prefix for all the configuration ObjectClass names */
    private static final String ADS_PREFIX = "ads-";

    /** The suffix for the bean */
    private static final String ADS_SUFFIX = "Bean";


    /**
     * 
     * Creates a new instance of ConfigPartitionReader.
     *
     * @param configPartition the non null config partition
     */
    public ConfigPartitionReader( AbstractBTreePartition configPartition )
    {
        if ( configPartition == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_503 ) );
        }

        if ( !configPartition.isInitialized() )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_504 ) );
        }

        this.configPartition = configPartition;
        se = configPartition.getSearchEngine();
        this.schemaManager = configPartition.getSchemaManager();
    }


    /**
     * Find the upper objectclass in a hierarchy. All the inherited ObjectClasses
     * will be removed.
     */
    private ObjectClass findObjectClass( Attribute objectClass ) throws Exception
    {
        Set<ObjectClass> candidates = new HashSet<>();

        // Create the set of candidates
        for ( Value ocValue : objectClass )
        {
            String ocName = ocValue.getValue();
            String ocOid = schemaManager.getObjectClassRegistry().getOidByName( ocName );
            ObjectClass oc = schemaManager.getObjectClassRegistry().get( ocOid );

            if ( oc.isStructural() )
            {
                candidates.add( oc );
            }
        }

        // Now find the parent OC
        for ( Value ocValue : objectClass )
        {
            String ocName = ocValue.getValue();
            String ocOid = schemaManager.getObjectClassRegistry().getOidByName( ocName );
            ObjectClass oc = schemaManager.getObjectClassRegistry().get( ocOid );

            for ( ObjectClass superior : oc.getSuperiors() )
            {
                if ( oc.isStructural() && candidates.contains( superior ) )
                {
                    candidates.remove( superior );
                }
            }
        }

        // The remaining OC in the candidates set is the one we are looking for
        ObjectClass result = candidates.toArray( new ObjectClass[]
            {} )[0];

        LOG.debug( "The top level object class is {}", result.getName() );
        return result;
    }


    /**
     * Create the base Bean from the ObjectClass name.
     * The bean name is constructed using the OjectClass name, by
     * removing the ADS prefix, upper casing the first letter and adding "Bean" at the end.
     * 
     * For instance, ads-directoryService wil become DirectoryServiceBean
     */
    private AdsBaseBean createBean( ObjectClass objectClass ) throws ConfigurationException
    {
        // The remaining OC in the candidates set is the one we are looking for
        String objectClassName = objectClass.getName();

        // Now, let's instantiate the associated bean. Get rid of the 'ads-' in front of the name,
        // and uppercase the first letter. Finally add "Bean" at the end and add the package.
        String beanName = this.getClass().getPackage().getName() + ".beans."
            + Character.toUpperCase( objectClassName.charAt( ADS_PREFIX.length() ) )
            + objectClassName.substring( ADS_PREFIX.length() + 1 ) + ADS_SUFFIX;

        try
        {
            Class<?> clazz = Class.forName( beanName );
            Constructor<?> constructor = clazz.getConstructor();
            AdsBaseBean bean = ( AdsBaseBean ) constructor.newInstance();

            LOG.debug( "Bean {} created for ObjectClass {}", beanName, objectClassName );

            return bean;
        }
        catch ( ClassNotFoundException cnfe )
        {
            String message = "Cannot find a Bean class for the ObjectClass name " + objectClassName;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( SecurityException e )
        {
            String message = "Cannot access to the class " + beanName;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( NoSuchMethodException nsme )
        {
            String message = "Cannot find a constructor for the class " + beanName;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( InvocationTargetException ite )
        {
            String message = "Cannot invoke the class " + beanName + ", " + ite.getMessage();
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( IllegalAccessException iae )
        {
            String message = "Cannot access to the constructor for class " + beanName;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( InstantiationException ie )
        {
            String message = "Cannot instantiate the class " + beanName + ", " + ie.getMessage();
            LOG.error( message );
            throw new ConfigurationException( message );
        }
    }


    /**
     * Read the single entry value for an AttributeType, and feed the Bean field with this value
     */
    private void readSingleValueField( AdsBaseBean bean, Field beanField, Attribute fieldAttr )
        throws ConfigurationException
    {
        if ( fieldAttr == null )
        {
            return;
        }
        
        
        Value value = fieldAttr.get();
        String valueStr = ""; 
        
        if ( value != null )
        {
            valueStr = value.getValue();
        }

        Class<?> type = beanField.getType();

        // Process the value accordingly to its type.
        try
        {
            if ( type == String.class )
            {
                beanField.set( bean, valueStr );
            }
            else if ( type == byte[].class )
            {
                if ( value != null )
                {
                    beanField.set( bean, value.getBytes() );
                }
                else
                {
                    beanField.set( bean, Strings.EMPTY_BYTES );
                }
            }
            else if ( type == int.class )
            {
                beanField.setInt( bean, Integer.parseInt( valueStr ) );
            }
            else if ( type == long.class )
            {
                beanField.setLong( bean, Long.parseLong( valueStr ) );
            }
            else if ( type == boolean.class )
            {
                beanField.setBoolean( bean, Boolean.parseBoolean( valueStr ) );
            }
            else if ( type == Dn.class )
            {
                try
                {
                    Dn dn = new Dn( valueStr );
                    beanField.set( bean, dn );
                }
                catch ( LdapInvalidDnException lide )
                {
                    String message = "The Dn '" + valueStr + "' for attribute " + fieldAttr.getId()
                        + " is not a valid Dn";
                    LOG.error( message );
                    throw new ConfigurationException( message );
                }
            }
        }
        catch ( IllegalArgumentException | IllegalAccessException e )
        {
            String message = "Cannot store '" + valueStr + "' into attribute " + fieldAttr.getId();
            LOG.error( message );
            throw new ConfigurationException( message );
        }
    }


    /**
     * Read the multiple entry value for an AttributeType, and feed the Bean field with this value
     */
    private void readMultiValuedField( AdsBaseBean bean, Field field, Attribute attribute )
        throws ConfigurationException
    {
        if ( attribute == null )
        {
            return;
        }

        Class<?> type = field.getType();

        String fieldName = field.getName();
        String addMethodName = "add" + Character.toUpperCase( fieldName.charAt( 0 ) ) + fieldName.substring( 1 );

        // loop on the values and inject them in the bean
        for ( Value value : attribute )
        {
            String valueStr = value.getValue();

            try
            {
                if ( type == String.class )
                {
                    field.set( bean, valueStr );
                }
                else if ( type == int.class )
                {
                    field.setInt( bean, Integer.parseInt( valueStr ) );
                }
                else if ( type == long.class )
                {
                    field.setLong( bean, Long.parseLong( valueStr ) );
                }
                else if ( type == boolean.class )
                {
                    field.setBoolean( bean, Boolean.parseBoolean( valueStr ) );
                }
                else if ( type == Dn.class )
                {
                    try
                    {
                        Dn dn = new Dn( valueStr );
                        field.set( bean, dn );
                    }
                    catch ( LdapInvalidDnException lide )
                    {
                        String message = "The Dn '" + valueStr + "' for attribute " + attribute.getId()
                            + " is not a valid Dn";
                        LOG.error( message );
                        throw new ConfigurationException( message );
                    }
                }
                else if ( ( type == Set.class ) || ( type == List.class ) )
                {
                    Type genericFieldType = field.getGenericType();
                    Class<?> fieldArgClass = null;

                    if ( genericFieldType instanceof ParameterizedType )
                    {
                        ParameterizedType parameterizedType = ( ParameterizedType ) genericFieldType;
                        Type[] fieldArgTypes = parameterizedType.getActualTypeArguments();

                        for ( Type fieldArgType : fieldArgTypes )
                        {
                            fieldArgClass = ( Class<?> ) fieldArgType;
                        }
                    }

                    Method method = bean.getClass().getMethod( addMethodName,
                        Array.newInstance( fieldArgClass, 0 ).getClass() );

                    method.invoke( bean, new Object[] { new String[] { valueStr } } );
                }
            }
            catch ( IllegalArgumentException | IllegalAccessException e )
            {
                String message = "Cannot store '" + valueStr + "' into attribute " + attribute.getId();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( SecurityException e )
            {
                String message = "Cannot access to the class " + bean.getClass().getName();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( NoSuchMethodException nsme )
            {
                String message = "Cannot find a method " + addMethodName + " in the class " + bean.getClass().getName();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( InvocationTargetException ite )
            {
                String message = "Cannot invoke the class " + bean.getClass().getName() + ", " + ite.getMessage();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( NegativeArraySizeException nase )
            {
                // No way that can happen...
            }
        }
    }


    private void readFieldValue( AdsBaseBean bean, Field field, Entry entry, String attributeTypeName, boolean mandatory )
        throws ConfigurationException
    {
        // Get the entry attribute for this attribute type
        Attribute attribute = entry.get( attributeTypeName );

        if ( attribute != null )
        {
            if ( attribute.size() > 0 )
            {
                if ( !isMultiple( field.getType() ) )
                {
                    readSingleValueField( bean, field, attribute );
                }
                else
                {
                    readMultiValuedField( bean, field, attribute );
                }
            }
            else if ( attribute.size() == 0 )
            {
                // No value ? May be valid
                readSingleValueField( bean, field, attribute );
            }
            else if ( mandatory )
            {
                // the requested element is mandatory so let's throw an exception
                String message = "No value was configured for entry with DN '"
                    + entry.getDn() + "' and attribute type '" + attributeTypeName + "'.";
                LOG.error( message );
                throw new ConfigurationException( message );
            }
        }
        else
        {
            if ( mandatory )
            {
                // the requested element is mandatory so let's throw an exception
                String message = "No value was configured for entry with DN '"
                    + entry.getDn() + "' and attribute type '" + attributeTypeName + "'.";
                LOG.error( message );
                throw new ConfigurationException( message );
            }
        }
    }


    /**
     * Read some configuration element from the DIT using its name
     * 
     * @param baseDn The base Dn in the DIT where the configuration is stored
     * @param name The element to read
     * @param scope The search scope
     * @param mandatory If the element is mandatory or not
     * @return The list of beans read
     * @throws ConfigurationException If the configuration cannot be read 
     */
    public List<AdsBaseBean> read( Dn baseDn, String name, SearchScope scope, boolean mandatory )
        throws ConfigurationException
    {
        LOG.debug( "Reading from '{}', objectClass '{}'", baseDn, name );

        // Search for the element starting at some point in the DIT
        // Prepare the search request
        AttributeType ocAt = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        EqualityNode<String> filter = null;
        
        try
        {
            filter = new EqualityNode<>( ocAt, new Value( ocAt, name ) );
        }
        catch ( LdapInvalidAttributeValueException liave )
        {
            throw new ConfigurationException( liave.getMessage() );
        }
        
        Cursor<IndexEntry<String, String>> cursor = null;

        // Create a container for all the read beans
        List<AdsBaseBean> beansList = new ArrayList<>();

        try
        {
            // Do the search
            
            try ( PartitionTxn partitionTxn = configPartition.beginReadTransaction() )
            {
                SearchOperationContext searchContext = new SearchOperationContext( null );
                searchContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );
                searchContext.setDn( baseDn );
                searchContext.setFilter( filter );
                searchContext.setScope( scope );
                searchContext.setPartition( configPartition );
                searchContext.setTransaction( partitionTxn );
                PartitionSearchResult searchResult = se.computeResult( partitionTxn, schemaManager, searchContext );
    
                cursor = searchResult.getResultSet();
    
                // First, check if we have some entries to process.
                if ( !cursor.next() )
                {
                    if ( mandatory )
                    {
                        cursor.close();
    
                        // the requested element is mandatory so let's throw an exception
                        String message = "No instance was configured under the DN '"
                            + baseDn + "' for the objectClass '" + name + "'.";
                        LOG.error( message );
                        throw new ConfigurationException( message );
                    }
                    else
                    {
                        return null;
                    }
                }
    
                // Loop on all the found elements
                do
                {
                    IndexEntry<String, String> forwardEntry = cursor.get();
    
                    // Now, get the entry
                    Entry entry = configPartition.fetch( partitionTxn, forwardEntry.getId() );
                    LOG.debug( "Entry read : {}", entry );
    
                    AdsBaseBean bean = readConfig( entry );
                    // Adding the bean to the list
                    beansList.add( bean );
                }
                while ( cursor.next() );
            }
        }
        catch ( ConfigurationException ce )
        {
            throw ce;
        }
        catch ( Exception e )
        {
            String message = "An error occured while reading the configuration DN '"
                + baseDn + "' for the objectClass '" + name + "':\n" + e.getMessage();
            LOG.error( message );
            throw new ConfigurationException( message, e );
        }
        finally
        {
            if ( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch ( Exception e )
                {
                    // So ??? If the cursor can't be close, there is nothing we can do
                    // but rethrow the exception
                    throw new ConfigurationException( e.getMessage(), e.getCause() );
                }
            }
        }

        return beansList;
    }


    /**
     * Creates a configuration bean from the given entry.
     * 
     * @param entry any configuration entry of the type "ads-base"
     * @return The ApacheDS base configuration
     * @throws Exception If the configuration cannot be read
     */
    public AdsBaseBean readConfig( Entry entry ) throws Exception
    {
        // Let's instantiate the bean we need. The upper ObjectClass's name
        // will be used to do that
        ObjectClass objectClass = findObjectClass( entry.get( SchemaConstants.OBJECT_CLASS_AT ) );

        // Instantiating the bean
        AdsBaseBean bean = createBean( objectClass );

        // Setting its DN
        bean.setDn( entry.getDn() );

        // Getting the class of the bean
        Class<?> beanClass = bean.getClass();

        // A flag to know when we reached the 'AdsBaseBean' class when 
        // looping on the class hierarchy of the bean
        boolean adsBaseBeanClassFound = false;

        // Looping until the 'AdsBaseBean' class has been found
        while ( !adsBaseBeanClassFound )
        {
            // Checking if we reached the 'AdsBaseBean' class
            if ( beanClass == AdsBaseBean.class )
            {
                adsBaseBeanClassFound = true;
            }

            // Looping on all fields of the bean
            Field[] fields = beanClass.getDeclaredFields();
            for ( Field field : fields )
            {
                // Making the field accessible (we get an exception if we don't do that)
                field.setAccessible( true );

                // Getting the class of the field
                Class<?> fieldClass = field.getType();

                // Looking for the @ConfigurationElement annotation
                ConfigurationElement configurationElement = field.getAnnotation( ConfigurationElement.class );
                if ( configurationElement != null )
                {
                    // Getting the annotation's values
                    String fieldAttributeType = configurationElement.attributeType();
                    String fieldObjectClass = configurationElement.objectClass();
                    String container = configurationElement.container();
                    boolean isOptional = configurationElement.isOptional();

                    // Checking if we have a value for the attribute type
                    if ( ( fieldAttributeType != null ) && ( !"".equals( fieldAttributeType ) ) )
                    {
                        readFieldValue( bean, field, entry, fieldAttributeType, !isOptional );
                    }
                    // Checking if we have a value for the object class
                    else if ( ( fieldObjectClass != null ) && ( !"".equals( fieldObjectClass ) ) )
                    {
                        // Checking if this is a multi-valued field (which values are stored in a container)
                        if ( isMultiple( fieldClass ) && ( container != null )
                            && ( !"".equals( container ) ) )
                        {
                            // Creating the DN of the container
                            Dn newBase = entry.getDn().add( "ou=" + container );

                            // Looking for the field values
                            Collection<AdsBaseBean> fieldValues = read( newBase, fieldObjectClass,
                                SearchScope.ONELEVEL, !isOptional );

                            // Setting the values to the field
                            if ( ( fieldValues != null ) && !fieldValues.isEmpty() )
                            {
                                field.set( bean, fieldValues );
                            }
                        }
                        // This is a single-value field
                        else
                        {
                            // Looking for the field values
                            List<AdsBaseBean> fieldValues = read( entry.getDn(), fieldObjectClass,
                                SearchScope.ONELEVEL, !isOptional );

                            // Setting the value to the field
                            if ( ( fieldValues != null ) && !fieldValues.isEmpty() )
                            {
                                field.set( bean, fieldValues.get( 0 ) );
                            }
                        }
                    }
                }
            }

            // Moving to the upper class in the class hierarchy
            beanClass = beanClass.getSuperclass();
        }
        
        return bean;
    }
    
    
    /**
     * Indicates the given type is multiple.
     *
     * @param clazz
     *      the class
     * @return
     *      <code>true</code> if the given is multiple,
     *      <code>false</code> if not.
     */
    private boolean isMultiple( Class<?> clazz )
    {
        return Collection.class.isAssignableFrom( clazz );
    }


    /**
     * Read the configuration from the DIT, returning a bean containing all of it.
     * <p>
     * This method implicitly uses <em>"ou=config"</em> as base Dn
     * 
     * @return The Config bean, containing the whole configuration
     * @throws ConfigurationException If we had some issue reading the configuration
     */
    public ConfigBean readConfig() throws LdapException
    {
        // The starting point is the DirectoryService element
        return readConfig( new Dn( new Rdn( SchemaConstants.OU_AT, "config" ) ) );
    }


    /**
     * Read the configuration from the DIT, returning a bean containing all of it.
     * 
     * @param baseDn The base Dn in the DIT where the configuration is stored
     * @return The Config bean, containing the whole configuration
     * @throws ConfigurationException If we had some issue reading the configuration
     */
    public ConfigBean readConfig( String baseDn ) throws LdapException
    {
        // The starting point is the DirectoryService element
        return readConfig( new Dn( baseDn ) );
    }


    /**
     * Read the configuration from the DIT, returning a bean containing all of it.
     * 
     * @param baseDn The base Dn in the DIT where the configuration is stored
     * @return The Config bean, containing the whole configuration
     * @throws ConfigurationException If we had some issue reading the configuration
     */
    public ConfigBean readConfig( Dn baseDn ) throws ConfigurationException
    {
        // The starting point is the DirectoryService element
        return readConfig( baseDn, ConfigSchemaConstants.ADS_DIRECTORY_SERVICE_OC.getValue() );
    }


    /**
     * Read the configuration from the DIT, returning a bean containing all of it.
     * 
     * @param baseDn The base Dn in the DIT where the configuration is stored
     * @param objectClass The element to read from the DIT
     * @return The bean containing the configuration for the required element
     * @throws ConfigurationException If the configuration cannot be read
     */
    public ConfigBean readConfig( String baseDn, String objectClass ) throws LdapException
    {
        return readConfig( new Dn( baseDn ), objectClass );
    }


    /**
     * Read the configuration from the DIT, returning a bean containing all of it.
     * 
     * @param baseDn The base Dn in the DIT where the configuration is stored
     * @param objectClass The element to read from the DIT
     * @return The bean containing the configuration for the required element
     * @throws ConfigurationException If the configuration cannot be read
     */
    public ConfigBean readConfig( Dn baseDn, String objectClass ) throws ConfigurationException
    {
        LOG.debug( "Reading configuration for the {} element, from {} ", objectClass, baseDn );
        ConfigBean configBean = new ConfigBean();

        if ( baseDn == null )
        {
            baseDn = configPartition.getSuffixDn();
        }

        List<AdsBaseBean> beans = read( baseDn, objectClass, SearchScope.ONELEVEL, true );

        if ( LOG.isDebugEnabled() )
        {
            if ( ( beans == null ) || beans.isEmpty() )
            {
                LOG.debug( "No {} element to read", objectClass );
            }
            else
            {
                LOG.debug( beans.get( 0 ).toString() );
            }
        }

        configBean.setDirectoryServiceBeans( beans );

        return configBean;
    }
}
