/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.entry;

import java.util.Iterator;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;


/**
 * A generic interface mocking the Attribute JNDI interface. This interface
 * will be the base interface for the ServerAttribute and ClientAttribute.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface EntryAttribute<T extends Value<?>>
{
    /**
     * Adds a value to this attribute. If the new value is already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new value is added at the end of list of values.
     * </p>
     * <p>
     * This method returns true or false to indicate whether a value was added.
     * </p>
     *
     * @param val a new value to be added which may be null
     * @return true if a value was added, otherwise false
     * 
     * @exception InvalidAttributeValueException if the added value is not valid
     */
    boolean add( String val ) throws InvalidAttributeValueException, NamingException;


    /**
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     *
     * @param val some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    int add( String... vals ) throws InvalidAttributeValueException, NamingException;


    /**
     * Adds a value to this attribute. If the new value is already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new value is added at the end of list of values.
     * </p>
     * <p>
     * This method returns true or false to indicate whether a value was added.
     * </p>
     *
     * @param val a new value to be added which may be null
     * @return true if a value was added, otherwise false
     */
    boolean add( byte[] val ) throws InvalidAttributeValueException, NamingException;


    /**
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     *
     * @param val some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    int add( byte[]... vals ) throws InvalidAttributeValueException, NamingException;


    /**
     * Removes all values of this attribute.
     */
    void clear();


    /**
     * Indicates whether the specified value is one of the attribute's values.
     *
     * @param val the value which may be null
     * @return true if this attribute contains the value, otherwise false
     */
    boolean contains( String val );


    /**
     * Indicates whether the specified values are some of the attribute's values.
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    boolean contains( String... vals );


    /**
     * Indicates whether the specified value is one of the attribute's values.
     *
     * @param val the value which may be null
     * @return true if this attribute contains the value, otherwise false
     */
    boolean contains( byte[] val );


    /**
     * Indicates whether the specified values are some of the attribute's values.
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    boolean contains( byte[]... vals );


   /**
      * Retrieves the number of values in this attribute.
      *
      * @return the number of values in this attribute, including any values
      * wrapping a null value if there is one
      */
    int size();


    /**
     * Removes a value that is equal to the given value.
     * <p>
     * Returns true if a value is removed. If there is no value equal to <code>
     * val</code> this method simply returns false.
     * </p>
     *
     * @param val the value to be removed
     * @return true if the value is removed, otherwise false
     */
    boolean remove( byte[] val );


    /**
     * Removes all the  values that are equal to the given values.
     * <p>
     * Returns true if a value is removed. If there is no value equal to <code>
     * val</code> this method simply returns false.
     * </p>
     *
     * @param vals the values to be removed
     * @return true if all the values are removed, otherwise false
     */
    boolean remove( byte[]... val );


    /**
     * Removes a value that is equal to the given value.
     * <p>
     * Returns true if a value is removed. If there is no value equal to <code>
     * val</code> this method simply returns false.
     * </p>
     *
     * @param val the value to be removed
     * @return true if the value is removed, otherwise false
     */
    boolean remove( String val );
    
    
    /**
     * Removes all the  values that are equal to the given values.
     * <p>
     * Returns true if a value is removed. If there is no value equal to <code>
     * val</code> this method simply returns false.
     * </p>
     *
     * @param vals the values to be removed
     * @return true if all the values are removed, otherwise false
     */
    boolean remove( String... vals );
    
    
    /**
     * Gets the first value of this attribute. <code>null</code> is a valid value.
     *
     * <p>
     * If the attribute has no values this method throws
     * <code>NoSuchElementException</code>.
     * </p>
     *
     * @return a value of this attribute
     */
    T get();


    /**
     * Returns an iterator over all the attribute's values.
     * <p>
     * The effect on the returned enumeration of adding or removing values of
     * the attribute is not specified.
     * </p>
     * <p>
     * This method will throw any <code>NamingException</code> that occurs.
     * </p>
     *
     * @return an enumeration of all values of the attribute
     */
    Iterator<T> getAll();


    /**
     * Removes a value that is equal to the given value.
     * <p>
     * Returns true if a value is removed. If there is no value equal to <code>
     * val</code> this method simply returns false.
     * </p>
     *
     * @param val the value to be removed
     * @return true if the value is removed, otherwise false
     */
    boolean remove( T val );


    /**
     * Removes all the  values that are equal to the given values.
     * <p>
     * Returns true if a value is removed. If there is no value equal to <code>
     * val</code> this method simply returns false.
     * </p>
     *
     * @param vals the values to be removed
     * @return true if all the values are removed, otherwise false
     */
    boolean remove( T... vals );

    
    /**
     * Indicates whether the specified value is one of the attribute's values.
     *
     * @param val the value which may be null
     * @return true if this attribute contains the value, otherwise false
     */
    boolean contains( T val );


    /**
     * Indicates whether the specified values are some of the attribute's values.
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    boolean contains( T... vals );

    
    /**
     * Adds a value to this attribute. If the new value is already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new value is added at the end of list of values.
     * </p>
     * <p>
     * This method returns true or false to indicate whether a value was added.
     * </p>
     *
     * @param val a new value to be added which may be null
     * @return true if a value was added, otherwise false
     */
    boolean add( T val ) throws InvalidAttributeValueException, NamingException;
    
    
    /**
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     *
     * @param val some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    int add( T... val ) throws InvalidAttributeValueException, NamingException;
}
