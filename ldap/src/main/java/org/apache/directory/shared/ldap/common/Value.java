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
 * A common interface for values stored into a ServerAttribute. These values can
 * be a String or a byte[].
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface Value<T> extends Serializable, Cloneable
{
    /**
     * Tells if the Value is a StringValue or a BinaryValue.
     *
     * @return <code>true</code> if the Value is a BinaryVale, <code>false</code>
     * otherwise
     */
    boolean isBinary();

    /**
     * Get the non-normalized value.
     *
     * @todo ALEX - Remove after resolution:
     * If this value is normalized does this method return the original non-normalized
     * value or the normalized value? The semantics of this is very important.  Also
     * when we serialize values in the backend I guess we use the non-normalized values
     * within the master table for the entry.  Storing the normalized value in addition
     * to the original value will cost double the disk space and double the memory in
     * the entry cache and the master.db cache for partitions.  Should the on disk
     * representation be the same as the in memory representation?  So the question boils
     * down to is this class optimized for handling operations in the server or optimized
     * for storage in the partition?
     *
     * @return The stored value, in its original type (String or byte[])
     */
    T getValue();
    
    
    /**
     * Get the normalized value.
     *
     * @return The normalized value, as either a String or byte[]
     */
    T getNormalizedValue();
    
    
    /**
     * Sets the non-normalized value.
     *
     * @param value the value to set. Should be either a String or a byte[]
     */
    void setValue( T value );
    
    /**
     * Normalizes the value using the given normalizer
     *
     * @todo Alex - Remove after resolution:
     * Come back to this later - I have a feeling this may be a problem to normalize
     * this way.  Don't know for sure why yet but I'll try to figure it out.
     *
     * @param normalizer The normalizer to use
     * @throws NamingException If the normalization fails
     */
    void normalize( Normalizer normalizer ) throws NamingException;
    
    /**
     * Tells if the value has been normalized. It's a speedup.
     *
     * @return <code>true</code> if the Value has been normalized, <code>false</code> otherwise
     */
    boolean isNormalized();
    
    /**
     * Makes a copy of the Value. 
     *
     * @return A non-null copy of the Value.
     * @throws CloneNotSupportedException if the clone operation is not supported
     */
    Value<?> clone() throws CloneNotSupportedException;
}
