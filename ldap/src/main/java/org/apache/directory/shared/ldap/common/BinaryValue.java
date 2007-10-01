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

import java.util.Arrays;

import org.apache.directory.shared.ldap.util.StringTools;

/**
 * A class storing a byte[] value.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BinaryValue extends AbstractValue<byte[]>
{
    /**
     * serialVersionUID 
     */
    static final long serialVersionUID = 2L;

    
    /**
     * Creates a new instance of StringValue with no value
     */
    public BinaryValue()
    {
        super( null );
    }
    
    
    /**
     * Creates a new instance of StringValue with a value
     */
    public BinaryValue( byte[] value )
    {
        super( value );
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "BinaryValue : " + StringTools.dumpBytes( value );
    }
    

    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        return Arrays.hashCode( value );
    }

    
    /**
     * Makes a copy of the Value. 
     *
     * @return A non-null copy of the Value.
     */
    public Object clone() throws CloneNotSupportedException
    {
        // Clone the superclass
        BinaryValue cloned = (BinaryValue)super.clone();
        
        // Clone the byte[] value, if it's not null
        byte[] clonedValue = null;
        
        if ( value != null )
        {
            clonedValue = ( value == null ? null : (byte[])value.clone() );
        
            System.arraycopy( value, 0, clonedValue, 0, value.length );
        }
        
        cloned.value = clonedValue;
        
        // We can return the cloned result
        return cloned;
    }
    
    
    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object obj )
    {
        if ( !super.equals( obj ) )
        {
            return false;
        }
        
        BinaryValue value = (BinaryValue)obj;
        
        if ( this.value == null )
        {
            return value.value == null;
        }
        
        return Arrays.equals( this.value, value.value );
    }
}
