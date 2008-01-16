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


import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.entry.AbstractBinaryValue;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.Arrays;
import java.util.Comparator;


/**
 * A server side schema aware wrapper around a binary attribute value.
 * This value wrapper uses schema information to syntax check values,
 * and to compare them for equality and ordering.  It caches results
 * and invalidates them when the wrapped value changes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ClientBinaryValue extends AbstractBinaryValue implements ClientValue<byte[]>
{
    /** Used for serialization */
    public static final long serialVersionUID = 2L;
    
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( ClientBinaryValue.class );

    /** the canonical representation of the wrapped binary value */
    private transient byte[] normalizedValue;

    /** cached results of the isValid() method call */
    private transient Boolean valid;


    /**
     * Creates a ServerBinaryValue without an initial wrapped value.
     *
     * @param attributeType the schema type associated with this ServerBinaryValue
     */
    public ClientBinaryValue()
    {
    }


    /**
     * Creates a ServerBinaryValue with an initial wrapped binary value.
     *
     * @param attributeType the schema type associated with this ServerBinaryValue
     * @param wrapped the binary value to wrap which may be null, or a zero length byte array
     */
    public ClientBinaryValue( byte[] wrapped )
    {
        super.set( wrapped );
    }


    // -----------------------------------------------------------------------
    // Value<String> Methods
    // -----------------------------------------------------------------------


    /**
     * Sets the wrapped binary value.  Has the side effect of setting the
     * normalizedValue and the valid flags to null if the wrapped value is
     * different than what is already set.  These cached values must be
     * recomputed to be correct with different values.
     *
     * @see ServerValue#set(Object)
     */
    public final void set( byte[] wrapped )
    {
        // Why should we invalidate the normalized value if it's we're setting the
        // wrapper to it's current value?
        byte[] value = getReference();
        
        if ( value != null )
        {
            if ( Arrays.equals( wrapped, value ) )
            {
                return;
            }
        }

        normalizedValue = null;
        valid = null;
        super.set( wrapped );
    }


    // -----------------------------------------------------------------------
    // ServerValue<String> Methods
    // -----------------------------------------------------------------------


    /**
     * Gets the normalized (cannonical) representation for the wrapped string.
     * If the wrapped String is null, null is returned, otherwise the normalized
     * form is returned.  If no the normalizedValue is null, then this method
     * will attempt to generate it from the wrapped value: repeated calls to
     * this method do not unnecessarily normalize the wrapped value.  Only changes
     * to the wrapped value result in attempts to normalize the wrapped value.
     *
     * @return a reference to the normalized version of the wrapped value
     * @throws NamingException with failures to normalize
     */
    public byte[] getNormalizedReference() throws NamingException
    {
        if ( isNull() )
        {
            return null;
        }

        if ( normalizedValue == null )
        {
        }

        return normalizedValue;
    }


    /**
     * Gets a direct reference to the normalized representation for the
     * wrapped value of this ServerValue wrapper. Implementations will most
     * likely leverage the attributeType this value is associated with to
     * determine how to properly normalize the wrapped value.
     *
     * @return the normalized version of the wrapped value
     * @throws NamingException if schema entity resolution fails or normalization fails
     */
    public byte[] getNormalizedCopy() throws NamingException
    {
        if ( normalizedValue == null )
        {
            getNormalizedReference();
        }

        byte[] copy = new byte[ normalizedValue.length ];
        System.arraycopy( normalizedValue, 0, copy, 0, normalizedValue.length );
        return copy;
    }


    /**
     * Uses the syntaxChecker associated with the attributeType to check if the
     * value is valid.  Repeated calls to this method do not attempt to re-check
     * the syntax of the wrapped value every time if the wrapped value does not
     * change. Syntax checks only result on the first check, and when the wrapped
     * value changes.
     *
     * @see ServerValue#isValid()
     */
    public final boolean isValid() throws NamingException
    {
        if ( valid != null )
        {
            return valid;
        }

        return valid;
    }


    /**
     *
     * @see ServerValue#compareTo(ServerValue)
     * @throws IllegalStateException on failures to extract the comparator, or the
     * normalizers needed to perform the required comparisons based on the schema
     */
    public int compareTo( ClientValue<byte[]> value )
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
        else
        {
            if ( ( value == null ) || value.isNull() ) 
            {
                return 1;
            }
        }

        if ( value instanceof ClientBinaryValue )
        {
            ClientBinaryValue binaryValue = ( ClientBinaryValue ) value;
        }

        throw new NotImplementedException( "I don't really know how to compare anything other " +
                "than ServerBinaryValues at this point in time." );
    }


    // -----------------------------------------------------------------------
    // Object Methods
    // -----------------------------------------------------------------------


    /**
     * @see Object#hashCode()
     * @throws IllegalStateException on failures to extract the comparator, or the
     * normalizers needed to perform the required comparisons based on the schema
     */
    public int hashCode()
    {
        // return zero if the value is null so only one null value can be
        // stored in an attribute - the string version does the same
        if ( isNull() )
        {
            return 0;
        }

        try
        {
            return Arrays.hashCode( getNormalizedReference() );
        }
        catch ( NamingException e )
        {
            String msg = "Failed to normalize \"" + toString() + "\" while trying to get hashCode()";
            LOG.error( msg, e );
            throw new IllegalStateException( msg, e );
        }
    }


    /**
     * Checks to see if this ServerBinaryValue equals the supplied object.
     *
     * This equals implementation overrides the BinaryValue implementation which
     * is not schema aware.
     * @throws IllegalStateException on failures to extract the comparator, or the
     * normalizers needed to perform the required comparisons based on the schema
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( ! ( obj instanceof ClientBinaryValue ) )
        {
            return false;
        }

        ClientBinaryValue other = ( ClientBinaryValue ) obj;
        
        if ( isNull() && other.isNull() )
        {
            return true;
        }

        if ( isNull() != other.isNull() )
        {
            return false;
        }

        // now unlike regular values we have to compare the normalized values
        try
        {
            return Arrays.equals( getNormalizedReference(), other.getNormalizedReference() );
        }
        catch ( NamingException e )
        {
            // 1st this is a warning because we're recovering from it and secondly
            // we build big string since waste is not an issue when exception handling
            LOG.warn( "Failed to get normalized value while trying to compare StringValues: "
                    + toString() + " and " + other.toString() , e );

            // recover by comparing non-normalized values
            return Arrays.equals( getReference(), other.getReference() );
        }
    }


    // -----------------------------------------------------------------------
    // Private Helper Methods (might be put into abstract base class)
    // -----------------------------------------------------------------------
    /**
     * @return a copy of the current value
     */
    public ClientBinaryValue clone()
    {
        ClientBinaryValue clone = (ClientBinaryValue)super.clone();
        
        if ( normalizedValue != null )
        {
            clone.normalizedValue = new byte[ normalizedValue.length ];
            System.arraycopy( normalizedValue, 0, clone.normalizedValue, 0, normalizedValue.length );
        }
        
        return clone;
    }
}