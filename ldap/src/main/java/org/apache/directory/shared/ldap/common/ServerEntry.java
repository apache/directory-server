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
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * This is the interface to a collection of attributes associated with a
 * directory entry.
 * <p>
 * This interface defines the methods that are implemented by a collection of a
 * particular directory entry's attributes.
 * </p>
 * <p>
 * A directory entry can have zero or more attributes comprising its attributes
 * collection. The attributes can be identified by name. The names of attributes 
 * are case insensitive. Method names refer to attribute OID rather than name, 
 * for performance reasons, as we are internal to the server, where we manipulate
 * OIDs only.
 * </p>
 * <p>
 * The attribute collection is created when the directory entry is created.
 * </p>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ServerEntry extends Cloneable, Serializable
{
    /**
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    static final long serialVersionUID = 2L;


    /**
     * Removes all the attributes.
     */
    void clear();


    /**
     * Returns a deep copy of this <code>Attributes</code> instance. The
     * attribute objects <b>are</b> cloned.
     * 
     * @return a deep copy of this <code>Attributes</code> instance
     */
    ServerEntry clone();


    /**
     * Places a non-null attribute in the attribute collection. If there is
     * already an attribute with the same OID as the new attribute, the old one
     * is removed from the collection and is returned by this method. If there
     * was no attribute with the same OID the return value is <code>null</code>.
     * 
     * This method clone the attribute.
     * 
     * @param attribute the attribute to be copied
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     */
    ServerAttribute copy( ServerAttribute attr );


    /**
     * Returns the attribute with the specified OID. The return value
     * is <code>null</code> if no match is found.
     * 
     * @param oid attribute OID
     * @return the attribute with the specified OID
     */
    ServerAttribute get( OID oid );


    /**
     * Returns an enumeration containing the zero or more attributes in the
     * collection. The behaviour of the enumeration is not specified if the
     * attribute collection is changed.
     * 
     * @return an enumeration of all contained attributes
     */
    Iterator<ServerAttribute> getAll();

    
    /**
     * Get this entry's DN.
     *
     * @return The entry DN
     */
    public LdapDN getDN();
    

    /**
     * Set this entry's DN.
     * 
     * @param dn The LdapdN associated with this entry
     */
    public void setDN( LdapDN dn);
    

    /**
     * Returns an enumeration containing the zero or more OIDs of the
     * attributes in the collection. The behaviour of the enumeration is not
     * specified if the attribute collection is changed.
     * 
     * @return an enumeration of the OIDs of all contained attributes
     */
    Iterator<OID> getOIDs();
    
    
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
    ServerAttribute put( ServerAttribute attr );


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
     * @param oid the OID of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException If the oid is null.
     */
    ServerAttribute put( OID oid, Value<?> val ) throws NamingException;
    

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
     * @param oid the OID of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException If the oid is null.
     */
    ServerAttribute put( OID oid, String val ) throws NamingException;
    

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
     * @param oid the OID of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException If the oid is null.
     */
    ServerAttribute put( OID oid, byte[] val ) throws NamingException;
 
    
    /**
     * Removes the attribute with the specified OID. The removed attribute is
     * returned by this method. If there is no attribute with the specified OID,
     * the return value is <code>null</code>.
     * 
     * @param oid the OID of the attribute to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     */
     ServerAttribute remove( OID oid );


     /**
      * Removes the specified attribute. The removed attribute is
      * returned by this method. If there were no attribute the return value 
      * is <code>null</code>.
      * 
      * @param attribute the attribute to be removed
      * @return the removed attribute, if exists; otherwise <code>null</code>
      */
     ServerAttribute remove( ServerAttribute attribute );


     /**
      * Returns the number of attributes.
      * 
      * @return the number of attributes
      */
     int size();
}
