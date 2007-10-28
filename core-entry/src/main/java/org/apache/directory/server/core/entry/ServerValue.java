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


import org.apache.directory.shared.ldap.entry.Value;

import javax.naming.NamingException;


/**
 * A server side entry value wrapper.  Server side value wrappers are
 * schema aware, knowing the attributeType of the attributes which contain them,
 * and hence they are capable of things like:
 *
 * <ul>
 *   <li>producing, caching and invalidate normalized versions of their wrapped values</li>
 *   <li>
 *     determining if the wrapped value complies with the syntax of their associated
 *     attributeType
 *   </li>
 *   <li>
 *     comparing wrapped values on the basis of their cannonical representation which
 *     utilizes the matchingRules of the attributeType associated with these server
 *     side values
 *   </li>
 * </ul>
 *
 * These characteristics have a major impact on how these objects are treated in
 * containers: compared (ordered), hashed, added, removed, and looked up. Furthermore
 * a great advantage is gained in simplifying code which must deal with values based
 * on their associated schema.  A large portion of the value managing code which is
 * setup to compare and test values can now be avoided.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ServerValue<T> extends Value<T>
{
    /**
     * Gets the normalized representation for the wrapped value of this
     * ServerValue wrapper. Implementations will most likely leverage the
     * attributeType this value is associated with to determine how to
     * properly normalize the wrapped value.
     *
     * @return the normalized version of the wrapped value
     * @throws NamingException if schema entity resolution fails or normalization fails
     */
    T getNormalizedValue() throws NamingException;


    /**
     * Checks to see if this ServerValue is valid according to the syntax
     * of the ServerAttribute which contains it.
     *
     * @return true if valid according to syntax, false otherwise
     * @throws NamingException if schema entity resolution fails or the syntaxChecker fails
     */
    boolean isValid() throws NamingException;


    /**
     * Compares two ServerValue objects for ordering based on the matchingRules
     * associated with the ServerAttribute containing this ServerValue.
     * Implementations should be using the normalized versions of the wrapped
     * value when conducting comparisons.
     *
     * @param value the ServerValue object to compare this ServerValue to
     * @return 0 if the objects are equivalent according to equals(), 1 or greater
     * if this ServerValue is greater than the supplied ServerValue, -1 or lesser
     * if this ServerValue is less than the supplied ServerValue.
     */
    int compareTo( ServerValue<T> value );
}
