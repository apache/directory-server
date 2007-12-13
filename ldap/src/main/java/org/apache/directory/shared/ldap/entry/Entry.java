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


import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.NamingException;
import java.util.Iterator;
import java.util.List;


/**
 * This interface represent a LDAP entry. An LDAP entry contains :
 * - A distinguished name (DN)
 * - A list of attributes
 * 
 * The available methods on this object are described in this interface.
 * 
 * This interface is used by the serverEntry and clientEntry interfaces.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface Entry<T extends EntryAttribute<?>>
{
    /**
     * Removes all the attributes.
     */
    void clear();


    /**
     * Get this entry's DN.
     *
     * @return The entry DN
     */
    LdapDN getDn();


    /**
     * Set this entry's DN.
     *
     * @param dn The LdapdN associated with this entry
     */
    void setDn( LdapDN dn);


    /**
     * Returns an enumeration containing the zero or more attributes in the
     * collection. The behaviour of the enumeration is not specified if the
     * attribute collection is changed.
     *
     * @return an enumeration of all contained attributes
     */
    Iterator<T> iterator();


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
    T put( T attribute ) throws NamingException;


    /**
      * Removes the specified attributes. The removed attributes are
      * returned by this method. If there were no attribute the return value
      * is <code>null</code>.
      *
      * @param attributes the attributes to be removed
      * @return the removed attribute, if exists; otherwise <code>null</code>
      */
    List<T> remove( T... attributes ) throws NamingException;


    /**
     * Removes the specified attribute. The removed attribute is
     * returned by this method. If there were no attribute the return value
     * is <code>null</code>.
     *
     * @param attribute the attribute to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     */
    T remove( T attribute ) throws NamingException;
    
    
    /**
      * Returns the number of attributes.
      *
      * @return the number of attributes
      */
    int size();
}
