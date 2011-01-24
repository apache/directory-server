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

import javax.naming.directory.SearchControls;

import org.apache.directory.server.config.beans.AdsBaseBean;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.SearchScope;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.ObjectClass;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
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
    private BTreePartition<Long> configPartition;

    /** the search engine of the partition */
    private SearchEngine<Entry, Long> se;

    /** the schema manager set in the config partition */
    private SchemaManager schemaManager;

    /** The prefix for all the configuration ObjectClass names */
    private static final String ADS_PREFIX = "ads-";

    /** The suffix for the bean */
    private static final String ADS_SUFFIX = "Bean";

    /** Those two flags are used to tell the reader if an element of configuration is mandatory or not */
    private static final boolean MANDATORY = true;
    private static final boolean OPTIONNAL = false;


    /**
     * 
     * Creates a new instance of ConfigPartitionReader.
     *
     * @param configPartition the non null config partition
     */
    public ConfigPartitionReader( BTreePartition<Long> configPartition )
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
     * Fnd the upper objectclass in a hierarchy. All the inherited ObjectClasses
     * will be removed.
     */
    private ObjectClass findObjectClass( EntryAttribute objectClass ) throws Exception
    {
        Set<ObjectClass> candidates = new HashSet<ObjectClass>();

        try
        {
            // Create the set of candidates
            for ( Value<?> ocValue : objectClass )
            {
                String ocName = ocValue.getString();
                String ocOid = schemaManager.getObjectClassRegistry().getOidByName( ocName );
                ObjectClass oc = ( ObjectClass ) schemaManager.getObjectClassRegistry().get( ocOid );

                if ( oc.isStructural() )
                {
                    candidates.add( oc );
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw e;
        }

        // Now find the parent OC
        for ( Value<?> ocValue : objectClass )
        {
            String ocName = ocValue.getString();
            String ocOid = schemaManager.getObjectClassRegistry().getOidByName( ocName );
            ObjectClass oc = ( ObjectClass ) schemaManager.getObjectClassRegistry().get( ocOid );

            for ( ObjectClass superior : oc.getSuperiors() )
            {
                if ( oc.isStructural() )
                {
                    if ( candidates.contains( superior ) )
                    {
                        candidates.remove( superior );
                    }
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

        // Now, let's instanciate the associated bean. Get rid of the 'ads-' in front of the name,
        // and uppercase the first letter. Finally add "Bean" at the end and add the package.
        //String beanName = this.getClass().getPackage().getName() + "org.apache.directory.server.config.beans." + Character.toUpperCase( objectClassName.charAt( 4 ) ) + objectClassName.substring( 5 ) + "Bean";
        String beanName = this.getClass().getPackage().getName() + ".beans." +
            Character.toUpperCase( objectClassName.charAt( ADS_PREFIX.length() ) ) +
            objectClassName.substring( ADS_PREFIX.length() + 1 ) + ADS_SUFFIX;

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
        catch ( SecurityException se )
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
            String message = "Cannot instanciate the class " + beanName + ", " + ie.getMessage();
            LOG.error( message );
            throw new ConfigurationException( message );
        }
    }


    /**
     * Retrieve the Field associated with an AttributeType name, if any.
     */
    private static Field getField( Class<?> clazz, String attributeName, Class<?> originalClazz )
        throws ConfigurationException
    {
        // We will check all the fields, as the AT name is case insentitive
        // when the field is case sensitive
        Field[] fields = clazz.getDeclaredFields();

        for ( Field field : fields )
        {
            String fieldName = field.getName();

            if ( fieldName.equalsIgnoreCase( attributeName ) )
            {
                return field;
            }
        }

        // May be in the paren'ts class ?
        if ( clazz.getSuperclass() != null )
        {
            return getField( clazz.getSuperclass(), attributeName, originalClazz );
        }

        String message = "Cannot find a field named " + attributeName + " in class " + originalClazz.getName();
        LOG.error( message );
        throw new ConfigurationException( message );
    }


    /**
     * Read the single entry value for an AttributeType, and feed the Bean field with this value
     */
    private void readSingleValueField( AdsBaseBean bean, Field beanField, EntryAttribute fieldAttr, boolean mandatory )
        throws ConfigurationException
    {
        if ( fieldAttr == null )
        {
            return;
        }

        Value<?> value = fieldAttr.get();
        String valueStr = value.getString();
        Class<?> type = beanField.getType();

        // Process the value accordingly to its type.
        try
        {
            if ( type == String.class )
            {
                beanField.set( bean, value.getString() );
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
        catch ( IllegalArgumentException iae )
        {
            String message = "Cannot store '" + valueStr + "' into attribute " + fieldAttr.getId();
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( IllegalAccessException e )
        {
            String message = "Cannot store '" + valueStr + "' into attribute " + fieldAttr.getId();
            LOG.error( message );
            throw new ConfigurationException( message );
        }
    }


    /**
     * Read the multiple entry value for an AttributeType, and feed the Bean field with this value
     */
    private void readMultiValuedField( AdsBaseBean bean, Field beanField, EntryAttribute fieldAttr, boolean mandatory )
        throws ConfigurationException
    {
        if ( fieldAttr == null )
        {
            return;
        }

        Class<?> type = beanField.getType();

        String fieldName = beanField.getName();
        String addMethodName = "add" + Character.toUpperCase( fieldName.charAt( 0 ) ) + fieldName.substring( 1 );

        // loop on the values and inject them in the bean
        for ( Value<?> value : fieldAttr )
        {
            String valueStr = value.getString();

            try
            {
                if ( type == String.class )
                {
                    beanField.set( bean, value.getString() );
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
                else if ( type == Set.class )
                {
                    Type genericFieldType = beanField.getGenericType();
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

                    method.invoke( bean, new Object[]
                        { new String[]
                            { valueStr } } );
                }
                else if ( type == List.class )
                {
                    Type genericFieldType = beanField.getGenericType();
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

                    method.invoke( bean, new Object[]
                        { new String[]
                            { valueStr } } );
                }
            }
            catch ( IllegalArgumentException iae )
            {
                String message = "Cannot store '" + valueStr + "' into attribute " + fieldAttr.getId();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( IllegalAccessException e )
            {
                String message = "Cannot store '" + valueStr + "' into attribute " + fieldAttr.getId();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( SecurityException se )
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


    /**
     * Read all the required fields (AttributeTypes) for a given Entry.
     */
    private void readFields( AdsBaseBean bean, Entry entry, Set<AttributeType> attributeTypes, boolean mandatory )
        throws NoSuchFieldException, IllegalAccessException, Exception
    {
        for ( AttributeType attributeType : attributeTypes )
        {
            String fieldName = attributeType.getName();
            String beanFieldName = fieldName;

            // Remove the "ads-" from the beginning of the field name
            if ( fieldName.startsWith( ADS_PREFIX ) )
            {
                beanFieldName = fieldName.substring( ADS_PREFIX.length() );
            }

            // Get the field
            Field beanField = getField( bean.getClass(), beanFieldName, bean.getClass() );

            // The field is private, we need to modify it to be able to access it.
            beanField.setAccessible( true );

            // Get the entry attribute for this field
            EntryAttribute fieldAttr = entry.get( fieldName );

            if ( ( fieldAttr == null ) && ( mandatory ) )
            {
                String message = "Attribute " + fieldName + " is mandatory and is not present for the Entry "
                    + entry.getDn();
                LOG.error( message );
                throw new ConfigurationException( message );
            }

            // Get the associated AttributeType
            AttributeType beanAT = schemaManager.getAttributeType( fieldName );

            // Check if this AT has the ads-compositeElement as a superior
            AttributeType superior = beanAT.getSuperior();

            if ( ( superior != null )
                && superior.getOid().equals( ConfigSchemaConstants.ADS_COMPOSITE_ELEMENT_AT.getOid() ) )
            {
                // This is a composite element, we have to go one level down to read it.
                // First, check if it's a SingleValued element
                if ( beanAT.isSingleValued() )
                {
                    // Yes : get the first element
                    List<AdsBaseBean> beans = read( entry.getDn(), fieldName, SearchScope.ONELEVEL, mandatory );

                    // We may not have found an element, but if the attribute is mandatory,
                    // this is an error
                    if ( ( beans == null ) || ( beans.size() == 0 ) )
                    {
                        if ( mandatory )
                        {
                            // This is an error !
                            String message = "The composite " + beanAT.getName()
                                + " is mandatory, and was not found under the "
                                + "configuration entry " + entry.getDn();
                            LOG.error( message );
                            throw new ConfigurationException( message );
                        }
                    }
                    else
                    {
                        // We must take the first element
                        AdsBaseBean readBean = beans.get( 0 );

                        if ( beans.size() > 1 )
                        {
                            // Not allowed as the AT is singled-valued
                            String message = "We have more than one entry for " + beanAT.getName() + " under "
                                + entry.getDn();
                            LOG.error( message );
                            throw new ConfigurationException( message );
                        }

                        beanField.set( bean, readBean );
                    }
                }
                else
                {
                    // No : we have to loop recursively on all the elements which are
                    // under the ou=<element-name> branch
                    Dn newBase = entry.getDn().add( "ou=" + beanFieldName );

                    // We have to remove the 's' at the end of the field name
                    String attributeName = fieldName.substring( 0, fieldName.length() - 1 );

                    // Sometime, the plural of a noun takes 'es'
                    if ( !schemaManager.getObjectClassRegistry().contains( attributeName ) )
                    {
                        // Try by removing 'es'
                        attributeName = fieldName.substring( 0, fieldName.length() - 2 );

                        if ( !schemaManager.getObjectClassRegistry().contains( attributeName ) )
                        {
                            String message = "Cannot find the ObjectClass named " + attributeName + " in the schema";
                            LOG.error( message );
                            throw new ConfigurationException( message );
                        }
                    }

                    // This is a multi-valued element, it can be a Set or a List
                    Collection<AdsBaseBean> beans = read( newBase, attributeName, SearchScope.ONELEVEL, mandatory );

                    if ( ( beans == null ) || ( beans.size() == 0 ) )
                    {
                        // If the element is mandatory, this is an error
                        if ( mandatory )
                        {
                            String message = "The composite " + beanAT.getName()
                                + " is mandatory, and was not found under the "
                                + "configuration entry " + entry.getDn();
                            LOG.error( message );
                            throw new ConfigurationException( message );
                        }
                    }
                    else
                    {
                        // Update the field
                        beanField.set( bean, beans );
                    }
                }
            }
            else
            // A standard AttributeType (ie, boolean, long, int or String)
            {
                // Process the field accordingly to its cardinality
                if ( beanAT.isSingleValued() )
                {
                    readSingleValueField( bean, beanField, fieldAttr, mandatory );
                }
                else
                {
                    readMultiValuedField( bean, beanField, fieldAttr, mandatory );
                }
            }
        }
    }


    /**
     * Get the list of MUST AttributeTypes for an objectClass
     */
    private Set<AttributeType> getAllMusts( ObjectClass objectClass )
    {
        Set<AttributeType> musts = new HashSet<AttributeType>();

        // First, gets the direct MUST
        musts.addAll( objectClass.getMustAttributeTypes() );

        // then add all the superiors MUST (recursively)
        List<ObjectClass> superiors = objectClass.getSuperiors();

        if ( superiors != null )
        {
            for ( ObjectClass superior : superiors )
            {
                musts.addAll( getAllMusts( superior ) );
            }
        }

        return musts;
    }


    /**
     * Get the list of MAY AttributeTypes for an objectClass
     */
    private Set<AttributeType> getAllMays( ObjectClass objectClass )
    {
        Set<AttributeType> mays = new HashSet<AttributeType>();

        // First, gets the direct MAY
        mays.addAll( objectClass.getMayAttributeTypes() );

        // then add all the superiors MAY (recursively)
        List<ObjectClass> superiors = objectClass.getSuperiors();

        if ( superiors != null )
        {
            for ( ObjectClass superior : superiors )
            {
                mays.addAll( getAllMays( superior ) );
            }
        }

        return mays;
    }


    /**
     * Helper method to print a list of AT's names.
     */
    private String dumpATs( Set<AttributeType> attributeTypes )
    {
        if ( ( attributeTypes == null ) || ( attributeTypes.size() == 0 ) )
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        sb.append( '{' );

        for ( AttributeType attributeType : attributeTypes )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            sb.append( attributeType.getName() );
        }

        sb.append( '}' );

        return sb.toString();
    }


    /**
     * Read some configuration element from the DIT using its name 
     */
    private List<AdsBaseBean> read( Dn baseDn, String name, SearchScope scope, boolean mandatory )
        throws ConfigurationException
    {
        LOG.debug( "Reading from '{}', entry {}", baseDn, name );

        // Search for the element starting at some point in the DIT
        // Prepare the search request
        AttributeType adsdAt = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        EqualityNode<?> filter = new EqualityNode( adsdAt, new StringValue( name ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( scope.ordinal() );
        IndexCursor<Long, Entry, Long> cursor = null;

        // Create a container for all the read beans
        List<AdsBaseBean> beans = new ArrayList<AdsBaseBean>();

        try
        {
            // Do the search
            cursor = se.cursor( baseDn, AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

            // First, check if we have some entries to process.
            if ( !cursor.next() )
            {
                if ( mandatory )
                {
                    cursor.close();

                    // the requested element is mandatory so let's throw an exception
                    String message = "No directoryService instance was configured under the Dn "
                        + configPartition.getSuffix();
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
                ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
                    .get();

                // Now, get the entry
                Entry entry = configPartition.lookup( forwardEntry.getId() );
                LOG.debug( "Entry read : {}", entry );

                // Let's instanciate the bean we need. The upper ObjectClass's name
                // will be used to do that
                EntryAttribute objectClassAttr = entry.get( SchemaConstants.OBJECT_CLASS_AT );

                ObjectClass objectClass = findObjectClass( objectClassAttr );
                AdsBaseBean bean = createBean( objectClass );

                // Now, read the AttributeTypes and store the values into the bean fields
                // The MAY
                Set<AttributeType> mays = getAllMays( objectClass );
                LOG.debug( "Fetching the following MAY attributes : {}", dumpATs( mays ) );
                readFields( bean, entry, mays, OPTIONNAL );

                // The MUST
                Set<AttributeType> musts = getAllMusts( objectClass );
                LOG.debug( "Fetching the following MAY attributes : {}", dumpATs( musts ) );
                readFields( bean, entry, musts, MANDATORY );

                // Done, we can add the bean into the list
                beans.add( bean );
            }
            while ( cursor.next() );
        }
        catch ( ConfigurationException ce )
        {
            ce.printStackTrace();
            throw ce;
        }
        catch ( Exception e )
        {
            String message = "Cannot open a cursor to read the configuration on " + baseDn;
            LOG.error( message );
            throw new ConfigurationException( message );
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

        return beans;

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
     * @param base The base Dn in the DIT where the configuration is stored
     * @return The Config bean, containing the whole configuration
     * @throws ConfigurationException If we had some issue reading the configuration
     */
    public ConfigBean readConfig( String baseDn ) throws LdapException
    {
        // The starting point is the DirectoryService element
        return readConfig( new Dn( baseDn ), ConfigSchemaConstants.ADS_DIRECTORY_SERVICE_OC.getValue() );
    }


    /**
     * Read the configuration from the DIT, returning a bean containing all of it.
     * 
     * @param base The base Dn in the DIT where the configuration is stored
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
     * @throws ConfigurationException
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
     * @throws ConfigurationException
     */
    public ConfigBean readConfig( Dn baseDn, String objectClass ) throws ConfigurationException
    {
        LOG.debug( "Reading configuration for the {} element, from {} ", objectClass, baseDn );
        ConfigBean configBean = new ConfigBean();

        if ( baseDn == null )
        {
            baseDn = configPartition.getSuffix();
        }

        List<AdsBaseBean> beans = read( baseDn, objectClass, SearchScope.ONELEVEL, MANDATORY );

        if ( LOG.isDebugEnabled() )
        {
            if ( ( beans == null ) || ( beans.size() == 0 ) )
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
