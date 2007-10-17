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
package org.apache.directory.shared.ldap.entry.client;


import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.Value;


/**
 * An entry implementation intended for use by clients. Implementations of
 * this interface may treat attributes with different aliases of the same
 * attributeType as the same attribute or may treat them as separate
 * attributes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ClientEntry extends Entry
{
    /**
     * Returns the attribute with the specified alias. The return value
     * is <code>null</code> if no match is found.  An Attribute with an id
     * different from the supplied alias may be returned: for example a call
     * with 'cn' may in some implementations return a ClientAttribute whose
     * getId() feild returns 'commonName'.
     *
     * @param alias an aliased name of the attribute identifier
     * @return the attribute associated with the alias
     */
    ClientAttribute get( String alias );


    /**
     * Places a new attribute with the supplied alias and value into the
     * attribute collection. If there is already an attribute associated with
     * the alias, the old one is removed from the collection and is returned
     * by this method. If there was no attribute associated with the alias the
     * return value is <code>null</code>. An attribute with an id different
     * from the supplied alias may be returned: for example a call with 'cn'
     * may in some implementations return a ClientAttribute whose getId()
     * feild returns 'commonName'.
     *
     * This method provides a mechanism to put an attribute with a <code>null
     * </code> value: the value of <code>val</code> may be <code>null</code>.
     *
     * @param alias an aliased name of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with associated with the specified alias,
     * if exists; otherwise <code>null</code>
     */
    ClientAttribute put( String alias, Value<?> val );


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
     * @param alias an aliased name of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     *         <code>null</code>
     */
    ClientAttribute put( String alias, String val );


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
     * @param alias an aliased of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     *         <code>null</code>
     */
    ClientAttribute put( String alias, byte[] val );


    /**
     * Removes the attribute with the specified alias. The removed attribute is
     * returned by this method. If there is no attribute with the specified OID,
     * the return value is <code>null</code>.
     *
     * @param alias an aliased name of the attribute to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     */
    ClientAttribute remove( String alias );
}
