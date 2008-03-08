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


import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;


/**
 * The server specific interface extending the EntryAttribute interface. It adds
 * three more methods which are Server side.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ClientAttribute extends EntryAttribute<Value<?>>
{
    /**
     * Checks to see if this attribute is valid along with the values it contains.
     *
     * @return true if the attribute and it's values are valid, false otherwise
     * @throws NamingException if there is a failure to check syntaxes of values
     */
    boolean isValid() throws NamingException;


    /**
     * Get the byte[] value, if and only if the value is known to be Binary,
     * otherwise a InvalidAttributeValueException will be thrown
     *
     * @return The value as a String
     * @throws InvalidAttributeValueException If the value is a String
     */
    byte[] getBytes() throws InvalidAttributeValueException;
    
    
    /**
     * Get the String value, if and only if the value is known to be a String,
     * otherwise a InvalidAttributeValueException will be thrown
     *
     * @return The value as a String
     * @throws InvalidAttributeValueException If the value is a byte[]
     */
    String getString() throws InvalidAttributeValueException;
}
