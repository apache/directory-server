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


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.entry.AbstractValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A server side schema aware wrapper around a String attribute value.
 * This value wrapper uses schema information to syntax check values,
 * and to compare them for equality and ordering.  It caches results
 * and invalidates them when the wrapped value changes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ClientStringValue extends AbstractValue<String>
{
    /** Used for serialization */
    private static final long serialVersionUID = 2L;
    
    
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( ClientStringValue.class );

    
    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------
    /**
     * Creates a ServerStringValue without an initial wrapped value.
     */
    public ClientStringValue()
    {
        normalized = false;
        valid = null;
    }


    /**
     * Creates a ServerStringValue with an initial wrapped String value.
     *
     * @param wrapped the value to wrap which can be null
     */
    public ClientStringValue( String wrapped )
    {
        this.wrapped = wrapped;
        normalized = false;
        valid = null;
    }


    // -----------------------------------------------------------------------
    // Value<String> Methods
    // -----------------------------------------------------------------------
    /**
     * Get the stored value.
     *
     * @return The stored value
     */
    public String get()
    {
        return wrapped;
    }


    /**
     * Get a copy of the stored value.
     *
     * @return A copy of the stored value.
     */
    public String getCopy()
    {
        // The String is immutable, we can safely return the internal
        // object without copying it.
        return wrapped;
    }
    
    
    /**
     * Sets the wrapped String value.  Has the side effect of setting the
     * normalizedValue and the valid flags to null if the wrapped value is
     * different than what is already set.  These cached values must be
     * recomputed to be correct with different values.
     *
     * @see ServerValue#set(Object)
     */
    public final void set( String wrapped )
    {
        // Why should we invalidate the normalized value if it's we're setting the
        // wrapper to it's current value?
        if ( ( wrapped != null ) && wrapped.equals( get() ) )
        {
            return;
        }

        normalizedValue = null;
        normalized = false;
        valid = null;
        this.wrapped = wrapped;
    }


    /**
     * Gets the normalized (canonical) representation for the wrapped string.
     * If the wrapped String is null, null is returned, otherwise the normalized
     * form is returned.  If the normalizedValue is null, then this method
     * will attempt to generate it from the wrapped value: repeated calls to
     * this method do not unnecessarily normalize the wrapped value.  Only changes
     * to the wrapped value result in attempts to normalize the wrapped value.
     *
     * @return gets the normalized value
     */
    public String getNormalizedValue()
    {
        if ( isNull() )
        {
            return null;
        }

        if ( normalizedValue == null )
        {
            return wrapped;
        }

        return normalizedValue;
    }


    /**
     * Gets a copy of the the normalized (canonical) representation 
     * for the wrapped value.
     *
     * @return gets a copy of the normalized value
     */
    public String getNormalizedValueCopy()
    {
        return getNormalizedValue();
    }


    /**
     * Normalize the value. For a client String value, applies the given normalizer.
     * 
     * It supposes that the client has access to the schema in order to select the
     * appropriate normalizer.
     * 
     * @param Normalizer The normalizer to apply to the value
     * @exception NamingException If the value cannot be normalized
     */
    public final void normalize( Normalizer normalizer ) throws NamingException
    {
        if ( normalizer != null )
        {
            normalizedValue = (String)normalizer.normalize( wrapped );
            normalized = true;
        }
    }

    
    // -----------------------------------------------------------------------
    // Comparable<String> Methods
    // -----------------------------------------------------------------------
    /**
     * @see ServerValue#compareTo(ServerValue)
     * @throws IllegalStateException on failures to extract the comparator, or the
     * normalizers needed to perform the required comparisons based on the schema
     */
    public int compareTo( Value<String> value )
    {
        if ( isNull() )
        {
            if ( ( value == null ) || value.isNull() )
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
        else if ( ( value == null ) || value.isNull() )
        {
            return 1;
        }

        if ( value instanceof ClientStringValue )
        {
            ClientStringValue stringValue = ( ClientStringValue ) value;
            
            return getNormalizedValue().compareTo( stringValue.getNormalizedValue() );
        }
        else 
        {
            String message = "Cannot compare " + toString() + " with the unknown value " + value.getClass();
            LOG.error( message );
            throw new NotImplementedException( message );
        }
    }


    // -----------------------------------------------------------------------
    // Cloneable methods
    // -----------------------------------------------------------------------
    /**
     * Get a clone of the Client Value
     * 
     * @return a copy of the current value
     */
    public ClientStringValue clone()
    {
        return (ClientStringValue)super.clone();
    }


    // -----------------------------------------------------------------------
    // Object Methods
    // -----------------------------------------------------------------------
    /**
     * @see Object#hashCode()
     * @return the instance's hashcode 
     */
    public int hashCode()
    {
        // return zero if the value is null so only one null value can be
        // stored in an attribute - the binary version does the same 
        if ( isNull() )
        {
            return 0;
        }

        // If the normalized value is null, will default to wrapped
        // which cannot be null at this point.
        return getNormalizedValue().hashCode();
    }


    /**
     * @see Object#equals(Object)
     * 
     * Two ClientStringValue are equals if their normalized values are equal
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( ! ( obj instanceof ClientStringValue ) )
        {
            return false;
        }

        ClientStringValue other = ( ClientStringValue ) obj;
        
        if ( this.isNull() )
        {
            return other.isNull();
        }
        
        // Test the normalized values
        return this.getNormalizedValue().equals( other.getNormalizedValue() );
    }
    
    
    /**
     * Tells if the current value is Binary or String
     * 
     * @return <code>true</code> if the value is Binary, <code>false</code> otherwise
     */
    public boolean isBinary()
    {
        return false;
    }


    /**
     * @see Externalizable#readExternal(ObjectInput)
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the wrapped value, if it's not null
        if ( in.readBoolean() )
        {
            wrapped = in.readUTF();
        }
        
        // Read the isNormalized flag
        normalized = in.readBoolean();
        
        if ( normalized )
        {
            // Read the normalized value, if not null
            if ( in.readBoolean() )
            {
                normalizedValue = in.readUTF();
            }
        }
    }

    
    /**
     * @see Externalizable#writeExternal(ObjectOutput)
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        // Write the wrapped value, if it's not null
        if ( wrapped != null )
        {
            out.writeBoolean( true );
            out.writeUTF( wrapped );
        }
        else
        {
            out.writeBoolean( false );
        }
        
        // Write the isNormalized flag
        if ( normalized )
        {
            out.writeBoolean( true );
            
            // Write the normalized value, if not null
            if ( normalizedValue != null )
            {
                out.writeBoolean( true );
                out.writeUTF( normalizedValue );
            }
            else
            {
                out.writeBoolean( false );
            }
        }
        else
        {
            out.writeBoolean( false );
        }
        
        // and flush the data
        out.flush();
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return wrapped == null ? "null": wrapped;
    }
}
