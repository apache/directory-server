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

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.Normalizer;

/**
 * Abstract implementation for Value subclasses.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractValue<T> implements Value<T>
{
    /** The stored value */
    protected T value;
    
    /** The normalized value */
    protected T normValue;
    

    /**
     * Creates a new instance of StringValue with no value
     */
    public AbstractValue( T value )
    {
        this.value = value;
        normValue = null;
    }
    
    
    /**
     * 
     * Set a new value
     *
     * @param value The value to set
     */
    public void setValue( T value )
    {
        this.value = value;
        normValue = null;
    }
    
    
    /**
     * 
     * Get the stored value
     *
     * @return The stored value
     */
    public T getValue()
    {
        return value;
    }
    
    
    /**
     * 
     * Get the stored normalized value
     *
     * @return The stored normalized value
     */
    public T getNormValue()
    {
        return normValue;
    }
    
    
    /**
     * Tells if this value is binary or not
     *
     * @return True if the value is binary, false otherwise
     */
    public final boolean isBinary()
    {
        return value instanceof byte[];
    }


    /**
     * Tells if this value is ormalized or not
     *
     * @return True if the value is normalized, false otherwise
     */
    public final boolean isNormalized()
    {
        return normValue != null;
    }
    
    
    /**
     * Normalize the value
     */
    public void normalize( Normalizer<T> normalizer ) throws NamingException
    {
        normValue = normalizer.normalize( value );
    }
    
    
    /**
     * Clone the value.
     */
    public Object clone() throws CloneNotSupportedException
    {
        // Simply clone the object
        return (Value)super.clone();
    }
    
    
    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( ( obj == null ) || !( obj instanceof Value ) )
        {
            return false;
        }
        
        if ( obj.getClass() != this.getClass() )
        {
            return false;
        }
        
        return true;
    }
}
