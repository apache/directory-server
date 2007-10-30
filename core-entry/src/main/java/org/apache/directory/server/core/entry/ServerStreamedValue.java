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
import org.apache.directory.shared.ldap.entry.StreamedValue;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.net.URI;


/**
 * A server side schema aware wrapper around a large streamed value.
 * For all practical purposes values of this type are treated like
 * referrals.  They reference some value which could not fit without
 * issue into main memory.  Instead the value is referred to using a
 * URI.  This has implications on the semantics of various Value
 * operations:
 *
 * <ul>
 *   <li>
 *     URI get() does not return the value but a URI referring to the value located
 *     in some streamable target location.
 *   </li>
 *   <li>
 *     set(URI) likewise sets the location target where the value can be streamed
 *     from rather than setting the actual value.
 *   </li>
 *   <li>
 *     @todo need to reflect upon this some more
 *     Don't know if getNormalizedValue() is even relavent but I guess the
 *     same URI can be represented in many ways if it is not already in canonical form
 *   </li>
 *   <li>
 *     @todo need to reflect upon this some more
 *     isValid() is also uncessary since really this does not mean the actual value
 *     stored where pointed to by the URI is actually valid.  I think this is a reason
 *     why the server must suspend certain checks for blobs like this.  Blobs should be
 *     binary and cannot be used for naming.  Hence because they cannot be used for
 *     naming then they should not need to have matchingRules and can just use the
 *     default binary matching.
 *   </li>
 *   <li>
 *     compareTo() is relative to the URI and not the actual streamed value stored at
 *     the URI target.  This means another means is needed to determine ordering for
 *     the wrapped streamed values.
 *   </li>
 * </ul>
 *
 * NOTE: this implementation of ServerValue<T> is still a work in progress!!!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerStreamedValue extends StreamedValue implements ServerValue<URI>
{
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( ServerStreamedValue.class );

    // use this to lookup the attributeType when deserializing
    @SuppressWarnings ( { "UnusedDeclaration", "FieldCanBeLocal" } )
    private final String oid;

    // do not serialize the schema entity graph associated with the type
    @SuppressWarnings ( { "UnusedDeclaration", "FieldCanBeLocal" } )
    private transient AttributeType attributeType;


    /**
     * Creates a ServerStreamedValue without an initial wrapped value.
     *
     * @param attributeType the schema type associated with this ServerBinaryValue
     */
    public ServerStreamedValue( AttributeType attributeType )
    {
        if ( attributeType == null )
        {
            throw new NullPointerException( "attributeType cannot be null" );
        }
        this.attributeType = attributeType;
        this.oid = attributeType.getOid();
    }


    /**
     * Creates a ServerStreamedValue with a wrapped URI referring to the
     * streamed binary value.
     *
     * @param attributeType the schema type associated with this ServerStreamedValue
     * @param wrapped the URI value to wrap which may be null
     */
    public ServerStreamedValue( AttributeType attributeType, URI wrapped )
    {
        this( attributeType );
        super.set( wrapped );
    }


    // -----------------------------------------------------------------------
    // Value<String> Methods
    // -----------------------------------------------------------------------


    /**
     * Sets the wrapped URI value.  Has the side effect of setting the
     * normalizedValue and the valid flags to null if the wrapped value is
     * different than what is already set.  These cached values must be
     * recomputed to be correct with different values.
     *
     * @see ServerValue#set(Object)
     */
    public final void set( URI wrapped )
    {
        // Why should we invalidate the normalized value if it's we're setting the
        // wrapper to it's current value?
        if ( wrapped.equals( get() ) )
        {
            return;
        }

        super.set( wrapped );
    }


    // -----------------------------------------------------------------------
    // ServerValue<String> Methods
    // -----------------------------------------------------------------------


    /**
     * Throws NotImplementedException. To normalize the value and return it
     * we would have to work on the data on disk.  Normalizing the URI probably
     * has no value since the Normalizer will not be geared for it but rather
     * the data stored for streaming.  Perhaps this is a completely wrong value
     * representation paradigmn for streamed values.
     *
     * @todo if URI is already cannonical this method may be useless
     * @todo consider this while figuring out how to deal with streamed values
     * @see ServerValue#getNormalizedValue()
     */
    public URI getNormalizedValue() throws NamingException
    {
        if ( get() == null )
        {
            return null;
        }

        throw new NotImplementedException();
    }


    /**
     * Throws NotImplementedException always.
     *
     * We cannot attempt to check the syntax of what is wrapped since what is
     * wrapped is the URI referring to the actual value.  Perhaps the only
     * allowable syntax should be the binary syntax?  But that does not make
     * sense since we want to switch to streaming for values (which can be any
     * kind) in memory when they are greater than some threshold.
     *
     * @todo this is totally bogus
     * @see ServerValue#isValid()
     */
    public boolean isValid() throws NamingException
    {
        throw new NotImplementedException();
    }


    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        // return zero if the value is null so only one null value can be
        // stored in an attribute - the binary version does the same
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
     * Again the semantics of this operation are completely screwed up since with
     * normal processing the comparators and normalizers used would be for the data
     * type streamed to disk rather than the URI value we have in memory.  So the
     * comparisons would fail.
     */
    public int compareTo( ServerValue<URI> value )
    {
        throw new NotImplementedException();
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

        //noinspection RedundantIfStatement
        if ( this.attributeType.isDescentantOf( attributeType ) )
        {
            return true;
        }

        return false;
    }
}
