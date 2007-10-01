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

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.Normalizer;

/**
 * A common interface for values stored into a ServerAttribute. Thos values can 
 * be String or byte[] 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface Value<T> extends Serializable, Cloneable
{
    /**
     * Tells if the Value is a StringValue or a BinaryValue
     *
     * @return <code>true</code> if the Value is a BinaryVale, <code>false</code>
     * otherwise
     */
    boolean isBinary();
    
    /**
     * Get the inner value
     *
     * @return The stored value, in its original type (String or byte[])
     */
    T getValue();
    
    
    /**
     * Get the normalized inner value
     *
     * @return The normalized stored value, in its original type (String or byte[])
     */
    T getNormValue();
    
    
    /**
     * Store a value into the object
     *
     * @param value The value to store. Should be either a String or a byte[]
     */
    void setValue( T value );
    
    /**
     * 
     * Normalize the value using the given normalizer
     *
     * @param normalizer The normalizer to use
     * @throws NamingException If the normalization fail
     */
    void normalize( Normalizer<T> normalizer ) throws NamingException;
    
    /**
     * Tells if the value has been normalized. It's a speedup.
     *
     * @return <code>true</code> if the Value has been normalized, <code>false</code>
     * otherwise
     */
    boolean isNormalized();
    
    /**
     * Makes a copy of the Value. 
     *
     * @return A non-null copy of the Value.
     */
    Object clone() throws CloneNotSupportedException;
}
