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
package org.apache.directory.server.core.entry;


import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;

import javax.naming.NamingException;

import java.util.List;
import java.util.Set;


/**
 * A server side entry which is schema aware.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ServerEntry<T extends ServerAttribute<ServerValue<?>>> extends Entry<T>, Iterable<T>
{
    // -----------------------------------------------------------------------
    // Schema Related Methods
    // -----------------------------------------------------------------------


    /**
     * Adds an objectClass to the objectClass attribute of this ServerEntry using
     * a specific alias name with the case provided by the user.
     *
     * @param objectClass the objectClass to add to this ServerEntry
     * @param alias the optional user provided alias to use
     * @return true if the objectClass is added, false otherwise
     * @throws NamingException if there are problems resolving entities while
     * adding the objectClass and its ancestors
     */
    boolean addObjectClass( ObjectClass objectClass, String alias ) throws NamingException;


    /**
     * Adds an objectClass to the objectClass attribute of this ServerEntry using
     * the first alias it can find.  If no alias name exists the numeric OID of the
     * objectClass is added as a value to the objectClass attribute.
     *
     * @param objectClass the objectClass to add to this ServerEntry
     * @return true if the objectClass is added, false otherwise
     * @throws NamingException if there are problems resolving entities while
     * adding the objectClass and its ancestors
     */
    boolean addObjectClass( ObjectClass objectClass ) throws NamingException;


    /**
     * Checks to see if this entry is of the objectClass.
     *
     * @param objectClass the objectClass to check for in this ServerEntry
     * @return true if this entry is of the objectClass, false otherwise
     */
    boolean hasObjectClass( ObjectClass objectClass );


    /**
     * Gets the first structural objectClass that it can find within the entry.
     * If the entry is inconsistent and contains no objectClass attribute then
     * null is returned.  If the entry is inconsistent and contains more than
     * one structural objectClass which is illegal, then the first to be found
     * will be returned.
     *
     * @return the first structural objectClass found in this entry
     */
    ObjectClass getStructuralObjectClass();


    /**
     * Gets all the structural objectClasses that are found within the entry
     * even though such a condition is considered invalid.  Only one structural
     * objectClass can be present within a valid entry.  The entry can also be
     * inconsistent by having no structural objectClasses then an empty set is
     * returned.
     *
     * @return all the structural objectClasses found in this entry
     */
    Set<ObjectClass> getStructuralObjectClasses();


    /**
     * Gets all the auxiliary objectClasses that it can find within the entry.
     * If the entry is inconsistent and contains no objectClass attribute then
     * the empty set is returned.
     *
     * @return the set of auxiliary objectClasses found in this entry
     */
    Set<ObjectClass> getAuxiliaryObjectClasses();


    /**
     * Gets all the abstract objectClasses that it can find within the entry.
     * If the entry is inconsistent and contains no objectClass attribute then
     * the empty set is returned.
     *
     * @return the set of abstract objectClasses found in this entry
     */
    Set<ObjectClass> getAbstractObjectClasses();


    /**
     * Gets the objectClasses associated with this entry. If there is no
     * objectClass attribute contained within this entry then an empty set
     * is returned.
     *
     * @return the objectClasses which govern the structure of this entry
     */
    Set<ObjectClass> getAllObjectClasses();


    /**
     * Gets the combinded set of all required attributes for this entry across
     * all objectClasses.
     *
     * @return the combinded set of all required attributes
     */
    Set<AttributeType> getMustList();


    /**
     * Gets the combined set of all optional attributes for this entry across
     * all objectClasses.
     *
     * @return the combined set of all optional attributes
     */
    Set<AttributeType> getMayList();

    
    /**
     * Gets all the attributes type (ObjectClasses, May and Must)
     *
     * @return The combined set of all the attributes, including ObjectClass.
     */
    Set<AttributeType> getAttributeTypes();
    

    /**
     * Fail fast check performed to determine entry consistency according to schema
     * characteristics.
     *
     * @return true if the entry, it's attributes and their values are consistent
     * with the schema
     */
    boolean isValid();


    /**
     * Check performed to determine entry consistency according to the schema
     * requirements of a particular objectClass.  The entry must be of that objectClass
     * to return true: meaning if the entry's objectClass attribute does not contain
     * the objectClass argument, then false should be returned.
     *
     * @param objectClass the objectClass to use while checking for validity
     * @return true if the entry, it's attributes and their values are consistent
     * with the objectClass
     */
    boolean isValid( ObjectClass objectClass );


    // -----------------------------------------------------------------------
    // Container (get/put/remove) Methods
    // -----------------------------------------------------------------------


    /**
     * Returns the attribute with the specified attributeType. The return
     * value is <code>null</code> if no match is found.
     *
     * @param attributeType the type of the attribute
     * @return the attribute of the specified type
     */
    T get( AttributeType attributeType );


    /**
     * Places a non-null attribute into this ServerEntry. If there an attribute
     * of the same exists, the existing one is removed from the set and is
     * returned by this method. If there was no attribute of the same type the
     * return value is <code>null</code>.
     *
     * @param attribute the attribute to be put into this ServerEntry
     * @return the existing attribute of the same type if it exists; otherwise
     * <code>null</code>
     */
    T put( T attribute ) throws NamingException;

    
    /**
     * Places non-null attributes in the attribute collection. If there is
     * already an attribute with the same OID as any of the new attributes, 
     * the old ones are removed from the collection and are returned by this 
     * method. If there was no attribute with the same OID the return value 
     * is <code>null</code>.
     *
     * @param attributes the attributes to be put
     * @return the old attributes with the same OID, if exist; otherwise
     *         <code>null</code>
     */
    List<T> put( T... attributes ) throws NamingException;

    // no value put'ters

    T put( String upId, AttributeType attributeType ) throws NamingException;

    T put( AttributeType attributeType ) throws NamingException;


    /**
     * Places a new attribute of the supplied type and value into the attribute
     * collection. The identifier used for the attribute is the first alias found
     * from the attributeType and if no aliases are available then the
     * attributeType's numric OID is used instead.  If there is already an attribute
     * of the same type, the old attribute is removed from the collection and is
     * returned by this method.  The user provided identifier of the existing
     * attribute will be used for the new one.  If there was no attribute with the same
     * type the return value is <code>null</code>.
     *
     * This method provides a mechanism to put an attribute with a <code>null</code>
     * value: the value of <code>val</code> may be <code>null</code>.
     *
     * @param attributeType the type of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute of the same type, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException if there are resolution issues
     */
    T put( AttributeType attributeType, ServerValue<?> val ) throws NamingException;

    /**
     * Places a new attribute with the supplied attributeType and value into this
     * ServerEntry. If there already exists attribute of the same type, the existing
     * one is removed from this ServerEntry and is returned. If there was no existing
     * attribute the <code>null</code> is returned instead.
     *
     * This method provides a mechanism to put an attribute with a <code>null</code>
     * value: the value of <code>obj</code> may be <code>null</code>.
     *
     * @param upId the user provided identifier for the new attribute
     * @param attributeType the type of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute of the same type, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException if there are failures
     */
    T put( String upId, AttributeType attributeType, ServerValue<?> val ) throws NamingException;


    /**
     * Places a new attribute with the supplied OID and value into the attribute
     * collection. If there is already an attribute with the same OID, the old
     * one is removed from the collection and is returned by this method. If
     * there was no attribute with the same OID the return value is
     * <code>null</code>.
     *
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value of <code>obj</code> may be
     * <code>null</code>.
     *
     * @param attributeType the type of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException if there are failures
     */
    T put( AttributeType attributeType, String val ) throws NamingException;


    T put( String upId, AttributeType attributeType, String val ) throws NamingException;


    /**
     * Places a new attribute with the supplied OID and value into the attribute
     * collection. If there is already an attribute with the same OID, the old
     * one is removed from the collection and is returned by this method. If
     * there was no attribute with the same OID the return value is
     * <code>null</code>.
     *
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value of <code>obj</code> may be
     * <code>null</code>.
     *
     * @param attributeType the type of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException if there are failures
     */
    T put( AttributeType attributeType, byte[] val ) throws NamingException;


    T put( String upId, AttributeType attributeType, byte[] val ) throws NamingException;


    /**
     * Removes the attribute with the specified alias. The removed attribute is
     * returned by this method. If there is no attribute with the specified OID,
     * the return value is <code>null</code>.
     *
     * @param attributeType the type of the attribute to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     * @throws NamingException if there are failures
     */
    T remove( AttributeType attributeType ) throws NamingException;

    /**
     * Places a non-null attribute into this ServerEntry. If there an attribute
     * of the same exists, the existing one is removed from the set and is
     * returned by this method. If there was no attribute of the same type the
     * return value is <code>null</code>.
     *
     * @param attribute the attribute to be put into this ServerEntry
     * @return the existing attribute of the same type if it exists; otherwise
     * <code>null</code>
     */
    T remove( T attribute ) throws NamingException;

    /**
     * Removes the specified attributes. The removed attributes are
     * returned by this method. If there were no attribute the return value
     * is <code>null</code>.
     *
     * @param attributes the attributes to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     */
    List<T> remove( T... attributes ) throws NamingException;
}
