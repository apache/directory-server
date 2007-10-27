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


import org.apache.directory.shared.ldap.entry.StreamedValue;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.net.URI;
import java.util.Comparator;


/**
 * A large streamed value within the server.  For all practical purposes
 * values of this type are treated like referrals.  They reference some
 * value which could not fit without issue into main memory.  Instead the
 * value is referred to.  This has implications on the semantics of various
 * Value operations:
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
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerStreamedValue extends StreamedValue implements ServerValue<URI>
{
    private static final Logger LOG = LoggerFactory.getLogger( ServerStreamedValue.class );
    private URI normalizedValue;

    // use this to lookup the attributeType when deserializing
    @SuppressWarnings ( { "UnusedDeclaration" } )
    private final String oid;

    // do not serialize the schema entity graph associated with the type
    private transient AttributeType attributeType;

    
    public ServerStreamedValue( AttributeType attributeType )
    {
        if ( attributeType == null )
        {
            throw new NullPointerException( "attributeType cannot be null" );
        }
        this.attributeType = attributeType;
        this.oid = attributeType.getOid();
    }


    public ServerStreamedValue( AttributeType attributeType, URI wrapped )
    {
        if ( attributeType == null )
        {
            throw new NullPointerException( "attributeType cannot be null" );
        }
        this.attributeType = attributeType;
        this.oid = attributeType.getOid();
        super.set( wrapped );
    }


    public URI getNormalizedValue() throws NamingException
    {
        if ( get() == null )
        {
            return null;
        }

        if ( normalizedValue == null )
        {
            Normalizer normalizer = getNormalizer();

            if ( normalizer == null )
            {
                normalizedValue = get();
            }
            else
            {
                normalizedValue = ( URI ) normalizer.normalize( get() );
            }
        }

        return normalizedValue;
    }


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


    private Normalizer getNormalizer() throws NamingException
    {
        MatchingRule mr = getMatchingRule();

        if ( mr == null )
        {
            return null;
        }

        return mr.getNormalizer();
    }


    private Comparator getComparator() throws NamingException
    {
        MatchingRule mr = getMatchingRule();

        if ( mr == null )
        {
            return null;
        }

        return mr.getComparator();
    }


    public boolean isValid() throws NamingException
    {
        return attributeType.getSyntax().getSyntaxChecker().isValidSyntax( get() );
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

    public int compareTo( ServerValue<URI> value )
    {
        if ( value == null && get() == null )
        {
            return 0;
        }

        if ( value != null && get() == null )
        {
            if ( value.get() == null )
            {
                return 0;
            }
            return -1;
        }

        if ( value == null )
        {
            return 1;
        }


        try
        {
            if ( value instanceof ServerStreamedValue )
            {
                //noinspection unchecked
                return getComparator().compare( getNormalizedValue(), value.getNormalizedValue() );
            }

            //noinspection unchecked
            return getComparator().compare( getNormalizedValue(), value.get() );
        }
        catch ( NamingException e )
        {
            throw new IllegalStateException( "Normalization failed when it should have succeeded", e );
        }
    }
}
