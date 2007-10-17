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

import java.util.Iterator;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface Entry
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
    Iterator<? extends EntryAttribute> getAll();


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
    EntryAttribute put( EntryAttribute attribute );


    /**
      * Removes the specified attribute. The removed attribute is
      * returned by this method. If there were no attribute the return value
      * is <code>null</code>.
      *
      * @param attribute the attribute to be removed
      * @return the removed attribute, if exists; otherwise <code>null</code>
      */
    EntryAttribute remove( EntryAttribute attribute );


    /**
      * Returns the number of attributes.
      *
      * @return the number of attributes
      */
    int size();
}
