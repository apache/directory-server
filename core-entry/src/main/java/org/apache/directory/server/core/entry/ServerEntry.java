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
import org.apache.directory.shared.ldap.entry.Value;
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
public interface ServerEntry extends Entry<ServerAttribute>, Cloneable
{
    // -----------------------------------------------------------------------
    // Schema Related Methods
    // -----------------------------------------------------------------------
    /**
     * Checks to see if this entry is of the objectClass.
     *
     * @param objectClass the objectClass to check for in this ServerEntry
     * @return true if this entry is of the objectClass, false otherwise
     */
    boolean hasObjectClass( String objectClass );

    
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
    ServerAttribute get( AttributeType attributeType );


    /**
     * Returns the attribute with the specified ID. The return
     * value is <code>null</code> if no match is found.
     *
     * @param upId the ID of the attribute
     * @return the attribute of the specified ID
     */
    ServerAttribute get( String upId ) throws NamingException;


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
    List<ServerAttribute> put( ServerAttribute... attributes ) throws NamingException;

    ServerAttribute put( String upId, String... values ) throws NamingException;

    ServerAttribute put( String upId, byte[]... values ) throws NamingException;
    
    List<ServerAttribute> set( String... upIds ) throws NamingException;

    List<ServerAttribute> set( AttributeType... attributeTypes ) throws NamingException;

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
     * @param values the values of the new attribute to be put
     * @return the old attribute of the same type, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException if there are resolution issues
     */
    ServerAttribute put( AttributeType attributeType, Value<?>... values ) throws NamingException;

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
     * @param values the value of the new attribute to be put
     * @return the old attribute of the same type, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException if there are failures
     */
    ServerAttribute put( String upId, Value<?>... values ) throws NamingException;


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
     * @param values the values of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException if there are failures
     */
    ServerAttribute put( AttributeType attributeType, String... values ) throws NamingException;


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
     * @param values the values of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException if there are failures
     */
    ServerAttribute put( AttributeType attributeType, byte[]... values ) throws NamingException;


    /**
     * Removes the attribute with the specified alias. The removed attribute is
     * returned by this method. If there is no attribute with the specified OID,
     * the return value is <code>null</code>.
     *
     * @param attributeTypes the types of the attribute to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     * @throws NamingException if there are failures
     */
    List<ServerAttribute> remove( AttributeType... attributeTypes ) throws NamingException;


    /**
     * Removes the attribute with the specified alias. The removed attribute is
     * returned by this method. If there is no attribute with the specified OID,
     * the return value is <code>null</code>.
     *
     * @param ids the IDs of the attribute to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     * @throws NamingException if there are failures
     */
    List<ServerAttribute> remove( String... ids ) throws NamingException;


    /**
     * Removes the specified attributes. The removed attributes are
     * returned by this method. If there were no attribute the return value
     * is <code>null</code>.
     *
     * @param attributes the attributes to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     */
    List<ServerAttribute> remove( ServerAttribute... attributes ) throws NamingException;
    
    /**
     * A clone method to produce a clone of the current object
     */
    public Object clone();
}
