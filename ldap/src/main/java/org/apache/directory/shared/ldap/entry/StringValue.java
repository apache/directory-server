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
package org.apache.directory.shared.ldap.entry;


/**
 * A warpper around an EntryAttribute's String value.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StringValue implements Value<String>
{
    @SuppressWarnings ( { "AnalyzingVariableNaming" } )
    static final long serialVersionUID = 2L;

    /** the wrapped string value */
    private String wrapped;


    /**
     * Creates a new instance of StringValue with no value.
     */
    public StringValue()
    {
    }


    /**
     * Creates a new instance of StringValue with a value.
     *
     * @param wrapped the actual String value to wrap
     */
    public StringValue( String wrapped )
    {
        this.wrapped = wrapped;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "StringValue : " + wrapped;
    }


    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        return wrapped.hashCode();
    }


    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        if ( ! ( obj instanceof StringValue ) )
        {
            return false;
        }

        StringValue stringValue = ( StringValue ) obj;
        if ( this.wrapped == null && stringValue.wrapped == null )
        {
            return true;
        }

        //noinspection SimplifiableIfStatement
        if ( this.wrapped == null && stringValue.wrapped != null ||
             this.wrapped != null && stringValue.wrapped == null )
        {
            return false;
        }

        //noinspection ConstantConditions
        return this.wrapped.equals( stringValue.wrapped );
    }


    public String get()
    {
        return wrapped;
    }


    public void set( String wrapped )
    {
        this.wrapped = wrapped;
    }


    /**
     * This is a shallow copy but in this case since the String is immutable
     * and thus it makes no difference if the clone is shallow or deep.
     *
     * @return a shallow copy
     */
    @SuppressWarnings ( { "CloneDoesntCallSuperClone" } )
    public Value<String> clone() throws CloneNotSupportedException
    {
        if ( wrapped == null )
        {
            return new StringValue();
        }

        return new StringValue( wrapped );
    }
}