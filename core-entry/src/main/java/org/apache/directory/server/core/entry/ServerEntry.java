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


import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;

import java.util.Set;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ServerEntry extends Entry
{
    // -----------------------------------------------------------------------
    // Schema Related Methods
    // -----------------------------------------------------------------------


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
     * Gets all the auxiliary objectClasses that it can find within the entry.
     * If the entry is inconsistent and contains no objectClass attribute then
     * the empty set is returned.
     *
     * @return the set of auxiliary objectClasses found in this entry
     */
    Set<ObjectClass> getAuxiliaryObjectClasses();


    /**
     * Gets the objectClasses associated with this entry. If there is no
     * objectClass attribute contained within this entry then an empty set
     * is returned.
     *
     * @return the objectClasses which govern the structure of this entry
     */
    Set<ObjectClass> getObjectClasses();


    /**
     * Gets the combinded set of all required attributes for this entry across
     * all objectClasses.
     *
     * @return the combinded set of all required attributes
     */
    Set<ServerAttribute> getMustList();


    /**
     * Gets the combined set of all optional attributes for this entry across
     * all objectClasses.
     *
     * @return the combined set of all optional attributes
     */
    Set<ServerAttribute> getMayList();


    /**
     * Fail fast check performed to determine entry consistency according to schema
     * characteristics.
     *
     * @return true if the entry, it's attributes and their values are consistent
     * with the schema
     */
    boolean isValid();


    // -----------------------------------------------------------------------
    // Container (get/put/remove) Methods
    // -----------------------------------------------------------------------


    /**
     * Returns the attribute with the specified OID. The return value
     * is <code>null</code> if no match is found.
     *
     * @param attributeType the type of the attribute
     * @return the attribute with the specified OID
     */
    ServerAttribute get( AttributeType attributeType );


    /**
     * Places a non-null attribute in the attribute collection. If there is
     * already an attribute with the same OID as the new attribute, the old one
     * is removed from the collection and is returned by this method. If there
     * was no attribute with the same OID the return value is <code>null</code>.
     *
     * @param attribute the attribute to be put
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     */
    ServerAttribute put( ServerAttribute attribute );


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
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     */
    ServerAttribute put( AttributeType attributeType, Value<?> val );


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
     */
    ServerAttribute put( AttributeType attributeType, String val );


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
     */
    ServerAttribute put( AttributeType attributeType, byte[] val );


    /**
     * Removes the attribute with the specified alias. The removed attribute is
     * returned by this method. If there is no attribute with the specified OID,
     * the return value is <code>null</code>.
     *
     * @param oid the numeric object identifier of the attribute to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     */
    ServerAttribute remove( OID oid );
}
