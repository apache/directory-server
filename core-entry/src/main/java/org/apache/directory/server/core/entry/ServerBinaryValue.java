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


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Comparator;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientBinaryValue;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ByteArrayComparator;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A server side schema aware wrapper around a binary attribute value.
 * This value wrapper uses schema information to syntax check values,
 * and to compare them for equality and ordering.  It caches results
 * and invalidates them when the wrapped value changes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerBinaryValue extends ClientBinaryValue
{
    /** Used for serialization */
    private static final long serialVersionUID = 2L;
    
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( ServerBinaryValue.class );

    /** reference to the attributeType which is not serialized */
    private transient AttributeType attributeType;

    /** A flag set if the normalized data is different from the wrapped data */
    private transient boolean same;


    // -----------------------------------------------------------------------
    // utility methods
    // -----------------------------------------------------------------------
    /**
     * Utility method to get some logs if an assert fails
     */
    protected String logAssert( String message )
    {
        LOG.error(  message );
        return message;
    }

    
    /**
     *  Check the attributeType member. It should not be null, 
     *  and it should contains a syntax.
     */
    protected String checkAttributeType( AttributeType attributeType )
    {
        try
        {
            if ( attributeType == null )
            {
                return "The AttributeType parameter should not be null";
            }
            
            if ( attributeType.getSyntax() == null )
            {
                return "There is no Syntax associated with this attributeType";
            }

            return null;
        }
        catch ( NamingException ne )
        {
            return "This AttributeType is incorrect";
        }
    }

    
    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------
    /**
     * Creates a ServerBinaryValue without an initial wrapped value.
     *
     * @param attributeType the schema type associated with this ServerBinaryValue
     */
    public ServerBinaryValue( AttributeType attributeType )
    {
        super();
        
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( "The AttributeType parameter should not be null" );
        }

        try
        {
            if ( attributeType.getSyntax() == null )
            {
                throw new IllegalArgumentException( "There is no Syntax associated with this attributeType" );
            }

            if ( attributeType.getSyntax().isHumanReadable() )
            {
                LOG.warn( "Treating a value of a human readible attribute {} as binary: ", attributeType.getName() );
            }
        }
        catch( NamingException e )
        {
            LOG.error( "Failed to resolve syntax for attributeType {}", attributeType, e );
        }

        this.attributeType = attributeType;
    }


    /**
     * Creates a ServerBinaryValue with an initial wrapped binary value.
     *
     * @param attributeType the schema type associated with this ServerBinaryValue
     * @param wrapped the binary value to wrap which may be null, or a zero length byte array
     */
    public ServerBinaryValue( AttributeType attributeType, byte[] wrapped )
    {
        this( attributeType );
        this.wrapped = wrapped;
    }


    /**
     * Creates a ServerStringValue with an initial wrapped String value and
     * a normalized value.
     *
     * @param attributeType the schema type associated with this ServerStringValue
     * @param wrapped the value to wrap which can be null
     * @param normalizedValue the normalized value
     */
    /** No protection */ 
    ServerBinaryValue( AttributeType attributeType, byte[] wrapped, byte[] normalizedValue, boolean same, boolean valid )
    {
        super( wrapped );
        this.normalized = true;
        this.attributeType = attributeType;
        this.normalizedValue = normalizedValue;
        this.valid = valid;
        this.same = same;
    }


    // -----------------------------------------------------------------------
    // ServerValue<byte[]> Methods
    // -----------------------------------------------------------------------
    public void normalize() throws NamingException
    {
        if ( isNormalized() )
        {
            // Bypass the normalization if it has already been done. 
            return;
        }
        
        if ( getReference() != null )
        {
            Normalizer normalizer = getNormalizer();
    
            if ( normalizer == null )
            {
                normalizedValue = getCopy();
                setNormalized( false );
            }
            else
            {
                normalizedValue = ( byte[] ) normalizer.normalize( getCopy() );
                setNormalized( true );
            }
            
            if ( Arrays.equals( super.getReference(), normalizedValue ) )
            {
                same = true;
            }
            else
            {
                same = false;
            }
        }
        else
        {
            normalizedValue = null;
            same = true;
            setNormalized( false );
        }
    }

    
    /**
     * Gets the normalized (cannonical) representation for the wrapped string.
     * If the wrapped String is null, null is returned, otherwise the normalized
     * form is returned.  If no the normalizedValue is null, then this method
     * will attempt to generate it from the wrapped value: repeated calls to
     * this method do not unnecessarily normalize the wrapped value.  Only changes
     * to the wrapped value result in attempts to normalize the wrapped value.
     *
     * @return a reference to the normalized version of the wrapped value
     */
    public byte[] getNormalizedValueReference()
    {
        if ( isNull() )
        {
            return null;
        }

        if ( !isNormalized() )
        {
            try
            {
                normalize();
            }
            catch ( NamingException ne )
            {
                String message = "Cannot normalize the value :" + ne.getMessage();
                LOG.warn( message );
                normalized = false;
            }
        }

        return normalizedValue;
    }


    /**
     * Gets the normalized (canonical) representation for the wrapped byte[].
     * If the wrapped byte[] is null, null is returned, otherwise the normalized
     * form is returned.  If no the normalizedValue is null, then this method
     * will attempt to generate it from the wrapped value: repeated calls to
     * this method do not unnecessarily normalize the wrapped value.  Only changes
     * to the wrapped value result in attempts to normalize the wrapped value.
     *
     * @return gets the normalized value
     */
    public byte[] getNormalizedValue() 
    {
        if ( isNull() )
        {
            return null;
        }

        if ( !normalized )
        {
            try
            {
                normalize();
            }
            catch ( NamingException ne )
            {
                String message = "Cannot normalize the value :" + ne.getMessage();
                LOG.warn( message );
                normalized = false;
            }
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
     */
    public byte[] getNormalizedValueCopy()
    {
        if ( isNull() )
        {
            return null;
        }

        if ( normalizedValue == null )
        {
            try
            {
                normalize();
            }
            catch ( NamingException ne )
            {
                String message = "Cannot normalize the value :" + ne.getMessage();
                LOG.warn( message );
                normalized = false;
            }
        }

        if ( normalizedValue != null )
        {
            byte[] copy = new byte[ normalizedValue.length ];
            System.arraycopy( normalizedValue, 0, copy, 0, normalizedValue.length );
            return copy;
        }
        else
        {
            return null;
        }
    }


    /**
     * Uses the syntaxChecker associated with the attributeType to check if the
     * value is valid.  Repeated calls to this method do not attempt to re-check
     * the syntax of the wrapped value every time if the wrapped value does not
     * change. Syntax checks only result on the first check, and when the wrapped
     * value changes.
     *
     * @see Value#isValid()
     */
    public final boolean isValid()
    {
        if ( valid != null )
        {
            return valid;
        }

        try
        {
            valid = attributeType.getSyntax().getSyntaxChecker().isValidSyntax( getReference() );
        }
        catch ( NamingException ne )
        {
            String message = "Cannot check the syntax : " + ne.getMessage();
            LOG.error( message );
            valid = false;
        }
        
        return valid;
    }

    
    /**
     * @return Tells if the wrapped value and the normalized value are the same 
     */
    public final boolean isSame()
    {
        return same;
    }
    

    /**
     *
     * @see Value#compareTo(Value)
     * @throws IllegalStateException on failures to extract the comparator, or the
     * normalizers needed to perform the required comparisons based on the schema
     */
    public int compareTo( Value<byte[]> value )
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

        if ( value instanceof ServerBinaryValue )
        {
            ServerBinaryValue binaryValue = ( ServerBinaryValue ) value;

            try
            {
                Comparator<? super Value<byte[]>> comparator = getComparator();
                
                if ( comparator != null )
                {
                    return getComparator().compare( getNormalizedValueReference(), binaryValue.getNormalizedValueReference() );
                }
                else
                {
                    return ByteArrayComparator.INSTANCE.compare( getNormalizedValueReference(), 
                        binaryValue.getNormalizedValueReference() );
                }
            }
            catch ( NamingException e )
            {
                String msg = "Failed to compare normalized values for " + Arrays.toString( getReference() )
                        + " and " + value;
                LOG.error( msg, e );
                throw new IllegalStateException( msg, e );
            }
        }

        String message = "I don't really know how to compare anything other " +
        "than ServerBinaryValues at this point in time.";
        LOG.error( message );
        throw new NotImplementedException( message );
    }


    /**
     * Get the associated AttributeType
     * @return The AttributeType
     */
    public AttributeType getAttributeType()
    {
        return attributeType;
    }


    /**
     * Check if the value is stored into an instance of the given 
     * AttributeType, or one of its ascendant.
     * 
     * For instance, if the Value is associated with a CommonName,
     * checking for Name will match.
     * 
     * @param attributeType The AttributeType we are looking at
     * @return <code>true</code> if the value is associated with the given
     * attributeType or one of its ascendant
     */
    public boolean instanceOf( AttributeType attributeType ) throws NamingException
    {
        if ( this.attributeType.equals( attributeType ) )
        {
            return true;
        }

        return this.attributeType.isDescentantOf( attributeType );
    }


    // -----------------------------------------------------------------------
    // Object Methods
    // -----------------------------------------------------------------------
    /**
     * @see Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        // return zero if the value is null so only one null value can be
        // stored in an attribute - the string version does the same
        if ( isNull() )
        {
            return 0;
        }

        return Arrays.hashCode( getNormalizedValueReference() );
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

        if ( ! ( obj instanceof ServerBinaryValue ) )
        {
            return false;
        }

        ServerBinaryValue other = ( ServerBinaryValue ) obj;
        
        if ( !attributeType.equals( other.attributeType ) )
        {
            return false;
        }
        
        if ( isNull() )
        {
            return other.isNull();
        }

        // Shortcut : if the values are equals, no need to compare
        // the normalized values
        if ( Arrays.equals( wrapped, other.get() ) )
        {
            return true;
        }
        else
        {
            try
            {
                Comparator<byte[]> comparator = getComparator();

                // Compare normalized values
                if ( comparator == null )
                {
                    return Arrays.equals( getNormalizedValueReference(), other.getNormalizedValueReference() );
                }
                else
                {
                    return comparator.compare( getNormalizedValueReference(), other.getNormalizedValueReference() ) == 0;
                }
            }
            catch ( NamingException ne )
            {
                return false;
            }
        }
    }


    // -----------------------------------------------------------------------
    // Private Helper Methods (might be put into abstract base class)
    // -----------------------------------------------------------------------
    /**
     * Find a matchingRule to use for normalization and comparison.  If an equality
     * matchingRule cannot be found it checks to see if other matchingRules are
     * available: SUBSTR, and ORDERING.  If a matchingRule cannot be found null is
     * returned.
     *
     * @return a matchingRule or null if one cannot be found for the attributeType
     * @throws NamingException if resolution of schema entities fail
     */
    private MatchingRule getMatchingRule() throws NamingException
    {
        MatchingRule mr = attributeType.getEquality();

        if ( mr == null )
        {
            mr = attributeType.getOrdering();
        }

        if ( mr == null )
        {
            mr = attributeType.getSubstr();
        }

        return mr;
    }


    /**
     * Gets a normalizer using getMatchingRule() to resolve the matchingRule
     * that the normalizer is extracted from.
     *
     * @return a normalizer associated with the attributeType or null if one cannot be found
     * @throws NamingException if resolution of schema entities fail
     */
    private Normalizer getNormalizer() throws NamingException
    {
        MatchingRule mr = getMatchingRule();

        if ( mr == null )
        {
            return null;
        }

        return mr.getNormalizer();
    }


    /**
     * Gets a comparator using getMatchingRule() to resolve the matching
     * that the comparator is extracted from.
     *
     * @return a comparator associated with the attributeType or null if one cannot be found
     * @throws NamingException if resolution of schema entities fail
     */
    private Comparator getComparator() throws NamingException
    {
        MatchingRule mr = getMatchingRule();

        if ( mr == null )
        {
            return null;
        }

        return mr.getComparator();
    }
    
    
    /**
     * @return a copy of the current value
     */
    public ServerBinaryValue clone()
    {
        ServerBinaryValue clone = (ServerBinaryValue)super.clone();
        
        if ( normalizedValue != null )
        {
            clone.normalizedValue = new byte[ normalizedValue.length ];
            System.arraycopy( normalizedValue, 0, clone.normalizedValue, 0, normalizedValue.length );
        }
        
        return clone;
    }


    /**
     * @see Externalizable#writeExternal(ObjectOutput)
     * 
     * We will write the value and the normalized value, only
     * if the normalized value is different.
     * 
     * The data will be stored following this structure :
     * 
     *  [UP value]
     *  [Norm value] (will be null if normValue == upValue)
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        if ( getReference() != null )
        {
            out.writeInt( getReference().length );
            out.write( getReference() );
            
            if ( same )
            {
                // If the normalized value is equal to the UP value,
                // don't save it
                out.writeInt( 0 );
            }
            else
            {
                out.writeInt( normalizedValue.length );
                out.write( normalizedValue );
            }
        }
        else
        {
            out.writeInt( -1 );
        }
        
        out.flush();
    }

    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        if ( in.available() == 0 )
        {
            set( null );
            normalizedValue = null;
        }
        else
        {
            int wrappedLength = in.readInt();
            byte[] wrappedBytes = null;
            
            switch ( wrappedLength )
            {
                case -1 :
                    // No value, no normalized value
                    same = true;
                    setNormalized( false );
                    break;
                    
                case 0 :
                    // Empty value, so is the normalized value
                    wrappedBytes = StringTools.EMPTY_BYTES;
                    normalizedValue = wrappedBytes;
                    setNormalized( true );
                    same = true;
                    break;
                    
                default :
                    wrappedBytes = new byte[wrappedLength];
                    in.readFully( wrappedBytes );
                    
                    int normalizedLength = in.readInt();
                    
                    // The normalized length should be either 0 or N, 
                    // but it can't be -1
                    switch ( normalizedLength )
                    {
                        case -1 :
                            String message = "The normalized value cannot be null when the User Provide value is not";
                            LOG.error(  message  );
                            throw new IOException( message );
                            
                        case 0 :
                            normalizedValue = StringTools.EMPTY_BYTES;
                            same = true;
                            setNormalized( false );
                            break;
                            
                        default :
                            same = false;
                            normalizedValue = new byte[normalizedLength];
                            in.readFully( normalizedValue );
                            setNormalized( true );
                            break;
                    }
                    
                    break;
            }
            
            set( wrappedBytes );
        }
    }
}