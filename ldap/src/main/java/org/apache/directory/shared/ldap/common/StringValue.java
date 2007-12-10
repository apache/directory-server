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


/**
 * A class storing a String value.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StringValue extends AbstractValue<String>
{
    /**
     * serialVersionUID 
     */
    static final long serialVersionUID = 2L;

    
    /**
     * Creates a new instance of StringValue with no value
     */
    public StringValue()
    {
        super( null );
    }
    
    
    /**
     * Creates a new instance of StringValue with a value
     */
    public StringValue( String value )
    {
        super( value );
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "StringValue : " + value;
    }
    
    
    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        return value.hashCode();
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
        
        StringValue value = (StringValue)obj;
        
        if ( this.value == null )
        {
            return value.value == null;
        }
        
        return this.value.equals( value.value );
    }
}
