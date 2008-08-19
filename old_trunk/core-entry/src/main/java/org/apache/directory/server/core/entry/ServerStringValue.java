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
import java.util.Comparator;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
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
public class ServerStringValue extends ClientStringValue
{
    /** Used for serialization */
    private static final long serialVersionUID = 2L;
    
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( ServerStringValue.class );

    /** reference to the attributeType which is not serialized */
    private transient AttributeType attributeType;


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
     * Creates a ServerStringValue without an initial wrapped value.
     *
     * @param attributeType the schema type associated with this ServerStringValue
     */
    public ServerStringValue( AttributeType attributeType )
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
    /** No protection */ ServerStringValue( AttributeType attributeType, String wrapped, String normalizedValue, boolean valid )
    {
        super( wrapped );
        this.normalized = true;
        this.attributeType = attributeType;
        this.normalizedValue = normalizedValue;
        this.valid = valid;
    }


    // -----------------------------------------------------------------------
    // Value<String> Methods, overloaded
    // -----------------------------------------------------------------------
    /**
     * @return a copy of the current value
     */
    public ServerStringValue clone()
    {
        ServerStringValue clone = (ServerStringValue)super.clone();
        
        return clone;
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
        if ( normalized )
        {
            return;
        }
        
        Normalizer normalizer = getNormalizer();

        if ( normalizer == null )
        {
            normalizedValue = wrapped;
        }
        else
        {
            normalizedValue = ( String ) normalizer.normalize( wrapped );
        }

        normalized = true;
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
    public String getNormalizedValue() 
    {
        if ( isNull() )
        {
            normalized = true;
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
            valid = attributeType.getSyntax().getSyntaxChecker().isValidSyntax( get() );
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
     * @see Value#compareTo(Value)
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
                String message = "Cannot normalize the wrapped value " + stringValue; 
                LOG.error( message );
            }
            
            // Normalizes the value
            try
            {
                normalize();
            }
            catch ( NamingException ne )
            {
                String message = "Cannot normalize the wrapped value " + this;
                LOG.error( message );
            }

            try
            {
                //noinspection unchecked
                return getComparator().compare( getNormalizedValue(), stringValue.getNormalizedValue() );
            }
            catch ( NamingException e )
            {
                String msg = "Failed to compare normalized values for " + this + " and " + value;
                LOG.error( msg, e );
                throw new IllegalStateException( msg, e );
            }
        }

        String message = "I don't know what to do if value is not a ServerStringValue";
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
     * Checks to see if this ServerStringValue equals the supplied object.
     *
     * This equals implementation overrides the StringValue implementation which
     * is not schema aware.
     * 
     * Two ServerStringValues are equal if they have the same AttributeType,
     * they are both null, their value are equal or their normalized value 
     * are equal. If the AttributeType has a comparator, we use it to
     * compare both values.
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
        
        if ( !attributeType.equals( other.attributeType ) )
        {
            return false;
        }
        
        if ( isNull() )
        {
            return other.isNull();
        }

        // Shortcut : compare the values without normalization
        // If they are equal, we may avoid a normalization.
        // Note : if two values are equal, then their normalized
        // value are equal too if their attributeType are equal. 
        if ( get().equals( other.get() ) )
        {
            return true;
        }
        else 
        {
            try
            {
                Comparator<String> comparator = getComparator();

                // Compare normalized values
                if ( comparator == null )
                {
                    return getNormalizedValue().equals( other.getNormalizedValue() );
                }
                else
                {
                    return comparator.compare( getNormalizedValue(), other.getNormalizedValue() ) == 0;
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
     * Implement the hashCode method.
     * 
     * @see Object#hashCode()
     * @throws IllegalStateException on failures to extract the comparator, or the
     * normalizers needed to perform the required comparisons based on the schema
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        // return the OID hashcode if the value is null. 
        if ( isNull() )
        {
            return attributeType.getOid().hashCode();
        }

        // If the normalized value is null, will default to wrapped
        // which cannot be null at this point.
        int h = 17;
        
        String normalized = getNormalizedValue();
        
        if ( normalized != null )
        {
            h = h*37 + normalized.hashCode();
        }
        
        // Add the OID hashcode
        h = h*37 + attributeType.getOid().hashCode();
        
        return h;
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
