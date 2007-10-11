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
package org.apache.directory.shared.ldap.common;

import java.io.Serializable;
import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * This interface defines the valid operations on a particular attribute of a
 * directory entry.
 * <p>
 * An attribute can have zero or more values. The value may be null. Values are
 * not ordered
 * </p>
 * <p>
 * The indexed operations work as if the values
 * added previously to the attribute had been done using ordered semantics. For
 * example, if the values "a", "b" and "c" were previously added to an unordered
 * attribute using "<code>add("a"); add("b"); add("c");</code>", it is
 * equivalent to adding the same objects to an ordered attribute using "<code>add(0,"a"); add(1,"b"); add(2,"c");</code>".
 * In this case, if we do "<code>remove(1)</code>" on the unordered list,
 * the value "b" is removed, changing the index of "c" to 1.
 * </p>
 * <p>
 * Multiple null values can be added to an attribute. It is not the same as
 * having no values on an attribute. If a null value is added to an unordered
 * attribute which already has a null value, the <code>add</code> method has
 * no effect.
 * </p>
 * <p>
 * Note that updates to the attribute via this interface do not affect the
 * directory directly.
 * 
 * </p>
 * This interface represents an attribute used internally by the
 * server. It's a subset of the javax.naming.directory.Attribute, where
 * some methods have been removed, and which manipulates Value instead
 * of Object
 * 
 * @todo ALEX - Remove after resolution:
 * Why not track the AttributeType associated with the ServerAttribute as we
 * discussed a while back at LDAPCon?  It makes sense to access the attributeType
 * directly from the ServerAttribute instead of having to fish for it in the
 * registries.  Perhaps the attributeType can be dynamically looked up from within
 * implementors of this interface (to respond to schema changes) to dynamically
 * resolve their respective type information.  Internally checks should be performed
 * while adding values.
 * @todo ALEX - Remove after resolution:
 * Also I thought we would use polymorphism for the different kinds of attributes:
 * Binary verses NonBinary.  Is there value in this?
 *
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 499013 $
 */
public interface ServerAttribute extends Cloneable, Serializable 
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
     */
    boolean add( Value<?> val );
    
    
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
    boolean add( String val );
    
    
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
    boolean add( byte[] val );
    
    
    /**
     * Removes all values of this attribute.
     */
    void clear();


    /**
     * Returns a deep copy of the attribute containing all the same values. The
     * values <b>are</b> cloned.
     * 
     * @return a deep clone of this attribute
     */
    ServerAttribute clone();

   
    /**
     * Indicates whether the specified value is one of the attribute's values.
     * 
     * @param val the value which may be null
     * @return true if this attribute contains the value, otherwise false
     */
    boolean contains( Value<?> val );
    

    /**
     * Indicates whether the specified value is one of the attribute's values.
     * 
     * @param val the value which may be null
     * @return true if this attribute contains the value, otherwise false
     */
    boolean contains( String val );
    

    /**
     * Indicates whether the specified value is one of the attribute's values.
     * 
     * @param val the value which may be null
     * @return true if this attribute contains the value, otherwise false
     */
    boolean contains( byte[] val );
    

    /**
     * Gets a value of this attribute. Returns the first one, if many. 
     * <code>null</code> is a valid value.
     * <p>
     * 
     * If the attribute has no values this method throws
     * <code>NoSuchElementException</code>.
     * </p>
     * 
     * @return a value of this attribute
     * @throws NamingException If the attribute has no value.
     */
    Value<?> get() throws NamingException;


    /**
     * Returns an enumeration of all the attribute's values. 
     * <p>
     * The effect on the returned enumeration of adding or removing values of
     * the attribute is not specified.
     * </p>
     * <p>
     * This method will throw any <code>NamingException</code> that occurs.
     * </p>
     * 
     * @return an enumeration of all values of the attribute
     * @throws NamingException If any <code>NamingException</code> occurs.
     */
    Iterator<Value<?>> getAll() throws NamingException;


    /**
     * Returns the identity of this attribute. This method is not expected to
     * return null.
     *
     * @return The id of this attribute
     */
    String getID();

   
    /**
     * Returns the OID of this attribute. This method is not expected to
     * return null.
     *
     * @return The OID of this attribute
     */
    OID getOid();

   
   /** 
      * Retrieves the number of values in this attribute.
      *
      * @return The number of values in this attribute, including the null value
      * if there is one.
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
    boolean remove( Value<?> val );
    

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
     * Normalize the attribute, setting the OID and normalizing the values 
     *
     * @param oid The attribute OID
     * @param normalizer The normalizer
     * @throws NamingException when normalization fails
     */
    void normalize( OID oid, Normalizer normalizer ) throws NamingException;
}
