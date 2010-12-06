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
package org.apache.directory.server.config;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.directory.server.config.beans.AdsBaseBean;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * TODO
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigWriter
{
    public static void writeConfiguration( SchemaManager schemaManager, ConfigBean configBean, String file )
        throws Exception
    {
        // The default configuration location is 'ou=config'
        DN dn = new DN( "ou=config" );

        List<LdifEntry> entries = new ArrayList<LdifEntry>();

        List<AdsBaseBean> directoryServiceBeans = configBean.getDirectoryServiceBeans();
        for ( AdsBaseBean adsBaseBean : directoryServiceBeans )
        {
            addBean( dn, schemaManager, adsBaseBean, entries );
        }

        System.out.println( entries );
    }


    private static void addBean( DN rootDn, SchemaManager schemaManager, AdsBaseBean bean, List<LdifEntry> entries )
        throws Exception
    {
        // Creating the entry to hold the bean and adding it to the list
        LdifEntry entry = new LdifEntry();
        entries.add( entry );
        entry.setDn( getDn( rootDn, bean ) );

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

                // Looking for the @AttributeType annotation
                AttributeType attributeTypeAnnotation = field.getAnnotation( AttributeType.class );
                if ( attributeTypeAnnotation != null )
                {
                    System.out.println( fieldClass.getName() + " " + attributeTypeAnnotation.value() + " "
                        + field.get( bean ) );
                    addPrimaryTypeFieldValue( attributeTypeAnnotation.value(), field.get( bean ), entry );
                    continue;
                }

                // Looking for the @Container annotation
                Container containerAnnotation = field.getAnnotation( Container.class );
                if ( containerAnnotation != null )
                {
                    DN containerDN = entry.getDn().add(
                        new org.apache.directory.shared.ldap.name.RDN( containerAnnotation.value() ) );

                    if ( Collection.class.isAssignableFrom( fieldClass ) )
                    {
                        // Looping on the Collection's objects
                        Collection<Object> collection = ( Collection<Object> ) field.get( bean );
                        for ( Object object : collection )
                        {
                            if ( object instanceof AdsBaseBean )
                            {
                                addBean( containerDN, schemaManager, ( AdsBaseBean ) object, entries );
                            }
                            else
                            {
                                AttributeType attributeType = field.getAnnotation( AttributeType.class );
                                if ( attributeType != null )
                                {
                                    System.out.println( fieldClass.getName() + " " + attributeType.value() + " "
                                                            + field.get( bean ) );
                                    addPrimaryTypeFieldValue( attributeType.value(), object, entry );
                                }
                            }
                        }
                    }
                    else
                    {
                        // TODO throw an error, if we have a container, the type must be a subtype of Collection
                    }

                }

                //                // Is the field a Collection object
                //                if ( Collection.class.isAssignableFrom( fieldClass ) )
                //                {
                //                    // Looping on the Collection's objects
                //                    Collection<Object> collection = ( Collection<Object> ) field.get( bean );
                //                    for ( Object object : collection )
                //                    {
                //                        if ( object instanceof AdsBaseBean )
                //                        {
                //                            addBean( entry.getDn(), schemaManager, ( AdsBaseBean ) object, entries );
                //                        }
                //                        else
                //                        {
                //                            AttributeType attributeType = field.getAnnotation( AttributeType.class );
                //                            if ( attributeType != null )
                //                            {
                //                                System.out.println( fieldClass.getName() + " " + attributeType.value() + " "
                //                                    + field.get( bean ) );
                //                                addPrimaryTypeFieldValue( attributeType.value(), object, entry );
                //                            }
                //                        }
                //                    }
                //                }
                //                else if ( AdsBaseBean.class.isAssignableFrom( fieldClass ) )
                //                {
                //                    AdsBaseBean newBean = ( AdsBaseBean ) field.get( bean );
                //                    if ( newBean != null )
                //                    {
                //                        addBean( entry.getDn(), schemaManager, newBean, entries );
                //                    }
                //                }
                //                else
                //                {
                //                    AttributeType attributeType = field.getAnnotation( AttributeType.class );
                //                    if ( attributeType != null )
                //                    {
                //                        System.out.println( fieldClass.getName() + " " + attributeType.value() + " "
                //                            + field.get( bean ) );
                //                        String attributeTypeId = attributeType.value();
                //
                //                        addPrimaryTypeFieldValue( attributeTypeId, field.get( bean ), entry );
                //
                //                    }
                //                }
            }

            // Moving to the upper class in the class hierarchy
            beanClass = beanClass.getSuperclass();
        }
    }


    /**
     * Gets the DN associated with the configuration bean based on the given base DN.
     *
     * @param baseDN
     *      the base DN
     * @param bean
     *      the configuration bean
     * @return
     *      the DN associated with the configuration bean based on the given base DN.
     * @throws LdapInvalidDnException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private static DN getDn( DN baseDN, AdsBaseBean bean ) throws LdapInvalidDnException, IllegalArgumentException,
        IllegalAccessException
    {
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

                // Looking for the AttributeType annotation 
                AttributeType attributeTypeAnnotation = field.getAnnotation( AttributeType.class );
                if ( attributeTypeAnnotation != null )
                {
                    String attributeTypeId = attributeTypeAnnotation.value();

                    // Looking for the RDN annotation         
                    RDN rdnAnnotation = field.getAnnotation( RDN.class );
                    if ( rdnAnnotation != null )
                    {
                        // Getting the value from the entry
                        return baseDN.add( new org.apache.directory.shared.ldap.name.RDN(
                            attributeTypeId, field.get( bean ).toString() ) );
                    }
                }
            }

            // Moving to the upper class in the class hierarchy
            beanClass = beanClass.getSuperclass();
        }

        return DN.EMPTY_DN; // TODO Throw an error when we reach that point
    }


    /**
     * Adds a primary type (String, int, long, boolean, byte[] ) field value to the given entry.
     *
     * @param attributeType
     *      the attribute type
     * @param value
     *      the value
     * @param entry
     *      the entry
     * @throws LdapException
     */
    private static void addPrimaryTypeFieldValue( String attributeType, Object value, LdifEntry entry )
        throws LdapException
    {
        // We don't store a 'null' value
        if ( value != null )
        {
            // Getting the attribute from the entry
            EntryAttribute attribute = entry.get( attributeType );

            // If no attribute has been found, we need to create it and add it to the entry
            if ( attribute == null )
            {
                attribute = new DefaultEntryAttribute( attributeType );
                entry.addAttribute( attribute );
            }

            // Storing the value to the attribute
            if ( value instanceof byte[] )
            {
                // Value is a byte[]
                attribute.add( ( byte[] ) value );
            }
            else
            {
                // Value is another type of object that we store as a String
                // (There will be an automatic translation for primary types like int, long, boolean, etc.)
                attribute.add( value.toString() );
            }
        }
    }
}
