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


import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.Arrays;


/**
 * A server side value which is also a StringValue.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerBinaryValue extends BinaryValue implements Value<byte[]>
{
    private static final Logger LOG = LoggerFactory.getLogger( ServerBinaryValue.class );

    @SuppressWarnings ( { "AnalyzingVariableNaming" } )
    static final long serialVersionUID = 2L;

    private byte[] normalizedValue;
    private AttributeType attributeType;


    public ServerBinaryValue( AttributeType attributeType ) throws NamingException
    {
        if ( attributeType == null )
        {
            throw new NullPointerException( "attributeType cannot be null" );
        }

        if ( attributeType.getSyntax().isHumanReadable() )
        {
            LOG.warn( "Treating a value of a human readible attribute {} as binary: ", attributeType.getName() );
        }

        this.attributeType = attributeType;
    }


    public ServerBinaryValue( AttributeType attributeType, byte[] wrapped )
    {
        if ( attributeType == null )
        {
            throw new NullPointerException( "attributeType cannot be null" );
        }

        this.attributeType = attributeType;
        super.set( wrapped );
    }


    public byte[] getNormalizedValue() throws NamingException
    {
        if ( get() == null )
        {
            return null;
        }

        if ( normalizedValue == null )
        {
            // search for matchingRules with a normalizer we can use based on
            // the parent attribute type this attribute value is intended for
            Normalizer normalizer = attributeType.getEquality().getNormalizer();

            if ( normalizer == null )
            {
                normalizer = attributeType.getOrdering().getNormalizer();
            }

            if ( normalizer == null )
            {
                normalizer = attributeType.getSubstr().getNormalizer();
            }

            // at this point if we still have no normalizer then presume as-is
            // normalization - the value is the same as the normalized value
            if ( normalizer == null )
            {
                normalizedValue = get();
            }
            else
            {
                normalizedValue = ( byte[] ) normalizer.normalize( get() );
            }
        }

        return normalizedValue;
    }


    public final void set( byte[] wrapped )
    {
        normalizedValue = null;
        super.set( wrapped );
    }


    public final boolean isValid() throws NamingException
    {
        return attributeType.getSyntax().getSyntaxChecker().isValidSyntax( get() );
    }


    @SuppressWarnings ( { "CloneDoesntCallSuperClone" } )
    public final ServerBinaryValue clone() throws CloneNotSupportedException
    {
        ServerBinaryValue copy = new ServerBinaryValue( attributeType, get() );
        copy.normalizedValue = normalizedValue;
        return copy;
    }


    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        // return zero if the value is null so only one null value can be
        // stored in an attribute - the string version does the same
        if ( get() == null )
        {
            return 0;
        }

        try
        {
            return getNormalizedValue().hashCode();
        }
        catch ( NamingException e )
        {
            LOG.warn( "Failed to get normalized value while trying to get hashCode: {}", toString() , e );

            // recover by using non-normalized values
            return get().hashCode();
        }
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

        if ( ! ( obj instanceof ServerBinaryValue ) )
        {
            return false;
        }

        ServerBinaryValue other = ( ServerBinaryValue ) obj;
        if ( get() == null && other.get() == null )
        {
            return true;
        }

        //noinspection SimplifiableIfStatement
        if ( get() == null && other.get() != null ||
             get() != null && other.get() == null )
        {
            return false;
        }

        // now unlike regular values we have to compare the normalized values
        try
        {
            return Arrays.equals( getNormalizedValue(), other.getNormalizedValue() );
        }
        catch ( NamingException e )
        {
            // 1st this is a warning because we're recovering from it and secondly
            // we build big string since waste is not an issue when exception handling
            LOG.warn( "Failed to get normalized value while trying to compare StringValues: "
                    + toString() + " and " + other.toString() , e );

            // recover by comparing non-normalized values
            return Arrays.equals( get(), other.get() );
        }
    }
}