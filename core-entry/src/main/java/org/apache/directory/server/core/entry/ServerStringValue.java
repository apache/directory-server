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


import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.entry.AbstractStringValue;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Comparator;


/**
 * A server side schema aware wrapper around a String attribute value.
 * This value wrapper uses schema information to syntax check values,
 * and to compare them for equality and ordering.  It caches results
 * and invalidates them when the wrapped value changes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerStringValue extends AbstractStringValue implements ServerValue<String>, Externalizable
{
    /** Used for serialization */
    public static final long serialVersionUID = 2L;
    
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( ServerStringValue.class );

    /** reference to the attributeType which is not serialized */
    private transient AttributeType attributeType;

    /** the canonical representation of the wrapped String value */
    private String normalizedValue;

    /** cached results of the isValid() method call */
    private transient Boolean valid;


    /**
     * Creates a ServerStringValue without an initial wrapped value.
     *
     * @param attributeType the schema type associated with this ServerStringValue
     */
    public ServerStringValue( AttributeType attributeType )
    {
        assert checkAttributeType( attributeType) == null : logAssert( checkAttributeType( attributeType ) );

        try
        {
            if ( ! attributeType.getSyntax().isHumanReadable() )
            {
                LOG.warn( "Treating a value of a binary attribute {} as a String: " +
                        "\nthis could cause data corruption!", attributeType.getName() );
            }
        }
        catch( NamingException e )
        {
            LOG.error( "Failed to resolve syntax for attributeType {}", attributeType, e );
        }

        this.attributeType = attributeType;
        setNormalized( false );
    }


    /**
     * Creates a ServerStringValue with an initial wrapped String value.
     *
     * @param attributeType the schema type associated with this ServerStringValue
     * @param wrapped the value to wrap which can be null
     */
    public ServerStringValue( AttributeType attributeType, String wrapped )
    {
        this( attributeType );
        set( wrapped );
        super.set( wrapped );
        setNormalized( false );
    }


    // -----------------------------------------------------------------------
    // Value<String> Methods
    // -----------------------------------------------------------------------


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

        setNormalized( false );
        valid = null;
        super.set( wrapped );

        /*
        try
        {
            Normalizer normalizer = getNormalizer();
            
            if ( normalizer != null )
            {
                normalizedValue = (String)getNormalizer().normalize( wrapped );
            }
            else
            {
                normalizedValue = wrapped;
            }
            
            // This is a special case : null values are allowed in LDAP 
            if ( normalizedValue == null )
            {
                valid = true;
                return;
            }

            valid = attributeType.getSyntax().getSyntaxChecker().isValidSyntax( normalizedValue );
            
            if ( !valid )
            {
                String message = "The wrapped value '" + wrapped + "'is not valid";
                LOG.warn( message );
            }
        }
        catch ( NamingException ne )
        {
            String message = "The wrapped value '" + wrapped + "'is not valid";
            try
            {
                Normalizer normalizer = getNormalizer();
                normalizedValue = (String)getNormalizer().normalize( wrapped );
                valid = attributeType.getSyntax().getSyntaxChecker().isValidSyntax( normalizedValue );
            }
            catch ( Exception e )
            {
                
            }
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        */
    }


    // -----------------------------------------------------------------------
    // ServerValue<String> Methods
    // -----------------------------------------------------------------------
    /**
     * Compute the normalized (canonical) representation for the wrapped string.
     * If the wrapped String is null, the normalized form will be null too.  
     *
     * @throws NamingException if the value cannot be properly normalized
     */
    public void normalize() throws NamingException
    {
        // If the value is already normalized, get out.
        if ( isNormalized() )
        {
            return;
        }
        
        Normalizer normalizer = getNormalizer();

        if ( normalizer == null )
        {
            normalizedValue = get();
            setNormalized( false );
        }
        else
        {
            normalizedValue = ( String ) normalizer.normalize( get() );
            setNormalized( true );
        }
    }

    /**
     * Gets the normalized (canonical) representation for the wrapped string.
     * If the wrapped String is null, null is returned, otherwise the normalized
     * form is returned.  If no the normalizedValue is null, then this method
     * will attempt to generate it from the wrapped value: repeated calls to
     * this method do not unnecessarily normalize the wrapped value.  Only changes
     * to the wrapped value result in attempts to normalize the wrapped value.
     *
     * @return gets the normalized value
     * @throws NamingException if the value cannot be properly normalized
     */
    public String getNormalized() throws NamingException
    {
        if ( isNull() )
        {
            return null;
        }

        if ( !isNormalized() )
        {
            normalize();
        }

        return normalizedValue;
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

        valid = attributeType.getSyntax().getSyntaxChecker().isValidSyntax( get() );
        return valid;
    }


    /**
     * @see ServerValue#compareTo(ServerValue)
     * @throws IllegalStateException on failures to extract the comparator, or the
     * normalizers needed to perform the required comparisons based on the schema
     */
    public int compareTo( ServerValue<String> value )
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

        if ( value instanceof ServerStringValue )
        {
            ServerStringValue stringValue = ( ServerStringValue ) value;
            
            // Normalizes the compared value
            try
            {
                stringValue.normalize();
            }
            catch ( NamingException ne )
            {
                LOG.error( "Cannot nnormalize the wrapped value '" + stringValue.get() + "'" );
            }
            
            // Normalizes the value
            try
            {
                normalize();
            }
            catch ( NamingException ne )
            {
                LOG.error( "Cannot normalize the wrapped value '" + get() + "'" );
            }

            try
            {
                //noinspection unchecked
                return getComparator().compare( getNormalized(), stringValue.getNormalized() );
            }
            catch ( NamingException e )
            {
                String msg = "Failed to compare normalized values for " + get() + " and " + value;
                LOG.error( msg, e );
                throw new IllegalStateException( msg, e );
            }
        }

        throw new NotImplementedException( "I don't know what to do if value is not a ServerStringValue" );
    }


    public AttributeType getAttributeType()
    {
        return attributeType;
    }


    /**
     * @see ServerValue#instanceOf(AttributeType)
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
     * @throws IllegalStateException on failures to extract the comparator, or the
     * normalizers needed to perform the required comparisons based on the schema
     */
    public int hashCode()
    {
        // return zero if the value is null so only one null value can be
        // stored in an attribute - the binary version does the same 
        if ( isNull() )
        {
            return 0;
        }

        try
        {
            return getNormalized().hashCode();
        }
        catch ( NamingException e )
        {
            String msg = "Failed to normalize \"" + get() + "\" while trying to get hashCode()";
            LOG.error( msg, e );
            throw new IllegalStateException( msg, e );
        }
    }


    /**
     * Checks to see if this ServerStringValue equals the supplied object.
     *
     * This equals implementation overrides the StringValue implementation which
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

        if ( ! ( obj instanceof ServerStringValue ) )
        {
            return false;
        }

        ServerStringValue other = ( ServerStringValue ) obj;
        
        if ( isNull() && other.isNull() )
        {
            return true;
        }

        if ( isNull() != other.isNull() )
        {
            return false;
        }

        // Shortcut : compare the values without normalization
        // If they are equal, we may avoid a normalization.
        // Note : if two values are equals, then their normalized
        // value is equal too.
        if ( get().equals( other.get() ) )
        {
            return true;
        }
        else 
        {
            try
            {
                // Compare normalized values
                if ( getComparator() == null )
                {
                    ServerStringValue stringValue = ( ServerStringValue )other;

                    return getNormalized().equals( stringValue.getNormalized() );
                }
            }
            catch ( NamingException ne )
            {
                return this.get().equals( other.get() );
            }

            return ( compareTo( other ) == 0 );
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
    public ServerStringValue clone()
    {
        try
        {
            ServerStringValue clone = (ServerStringValue)super.clone();
            
            return clone;
        }
        catch ( CloneNotSupportedException cnse )
        {
            return null;
        }
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
        if ( get() != null )
        {
            out.writeUTF( get() );
            
            try
            {
                normalize();
            }
            catch ( NamingException ne )
            {
                normalizedValue = null;
            }
            
            if ( get().equals( normalizedValue ) )
            {
                // If the normalized value is equal to the UP value,
                // don't save it
                out.writeUTF( "" );
            }
            else
            {
                out.writeUTF( normalizedValue );
            }
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
            String wrapped = in.readUTF();
            
            set( wrapped );
            
            normalizedValue = in.readUTF();
            
            if ( ( normalizedValue.length() == 0 ) &&  ( wrapped.length() != 0 ) )
            {
                // In this case, the normalized value is equal to the UP value
                normalizedValue = wrapped;
                setNormalized( true );
            }
            else
            {
                setNormalized( false );
            }
        }
    }
}
