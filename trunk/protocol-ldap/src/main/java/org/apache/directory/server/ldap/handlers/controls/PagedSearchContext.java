/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ldap.handlers.controls;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.asn1.ber.tlv.BerValue;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.Strings;


/**
 * The structure which stores the informations relative to the pagedSearch control.
 * They are associated to a cookie, stored into the session and associated to an
 * instance of this class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PagedSearchContext
{
    /** The previous search request */
    private SearchRequest previousSearchRequest;

    /** The current position in the cursor */
    private int currentPosition;

    /** The cookie key */
    private byte[] cookie;

    /** The integer value for the cookie */
    private AtomicInteger cookieValue;

    /** The associated cursor for the current search request */
    private EntryFilteringCursor cursor;


    /**
     * Creates a new instance of this class, storing the SearchRequest into it.
     */
    public PagedSearchContext( SearchRequest searchRequest )
    {
        previousSearchRequest = searchRequest;
        currentPosition = 0;

        // We compute a key for this cookie. It combines the search request
        // and some time seed, in order to avoid possible collisions, as
        // a user may send more than one PagedSearch on the same session.
        cookieValue = new AtomicInteger( searchRequest.getMessageId() << 16 );

        cookie = BerValue.getBytes( cookieValue.get() );
    }


    /**
     * Compute a new key for this cookie, based on the current searchRequest
     * hashCode and the current position. This value will be stored into the
     * session, and will permit the retrieval of this instance.
     *
     * @return The new cookie's key
     */
    public byte[] getCookie()
    {
        return cookie;
    }


    public int getCookieValue()
    {
        return cookieValue.get();
    }


    /**
     * Compute a new cookie, if the previous one already exists. This
     * is unlikely, as we are based on some time seed, but just in case,
     * this method will generate a new one.
     * @return The new cookie
     */
    public byte[] getNewCookie()
    {
        cookie = BerValue.getBytes( cookieValue.incrementAndGet() );

        return cookie;
    }


    /**
     * Build a set of OIDs from the list of attributes we have in the search request
     */
    private Set<String> buildAttributeSet( SearchRequest request,
        SchemaManager schemaManager )
    {
        Set<String> requestSet = new HashSet<String>();

        // Build the set of attributeType from the attributes
        for ( String attribute : request.getAttributes() )
        {
            try
            {
                AttributeType at = schemaManager.lookupAttributeTypeRegistry( attribute );
                requestSet.add( at.getOid() );
            }
            catch ( LdapException le )
            {
                // Deal with special attributes : '*', '+' and '1.1'
                if ( attribute.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) ||
                    attribute.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) ||
                    attribute.equals( SchemaConstants.NO_ATTRIBUTE ) )
                {
                    requestSet.add( attribute );
                }

                // Otherwise, don't add the attribute to the set
            }
        }

        return requestSet;
    }


    /**
     * Compare the previous search request and the new one, and return
     * true if they are equal. We compare every field but the MessageID.
     *
     * @param request The new SearchRequest
     * @return true if both request are equal.
     */
    public boolean hasSameRequest( SearchRequest request, LdapSession session )
    {
        // Compares the scope
        if ( request.getScope() != previousSearchRequest.getScope() )
        {
            return false;
        }

        // Compares the sizeLimit
        if ( request.getSizeLimit() != previousSearchRequest.getSizeLimit() )
        {
            return false;
        }

        // Compares the timeLimit
        if ( request.getTimeLimit() != previousSearchRequest.getTimeLimit() )
        {
            return false;
        }

        // Compares the TypesOnly
        if ( request.getTypesOnly() != previousSearchRequest.getTypesOnly() )
        {
            return false;
        }

        // Compares the deref aliases mode
        if ( request.getDerefAliases() != previousSearchRequest.getDerefAliases() )
        {
            return false;
        }

        SchemaManager schemaManager =
            session.getLdapServer().getDirectoryService().getSchemaManager();

        // Compares the attributes
        if ( request.getAttributes() == null )
        {
            if ( previousSearchRequest.getAttributes() != null )
            {
                return false;
            }
        }
        else
        {
            if ( previousSearchRequest.getAttributes() == null )
            {
                return false;
            }
            else
            {
                // We have to normalize the attributes in order to compare them
                if ( request.getAttributes().size() != previousSearchRequest.getAttributes().size() )
                {
                    return false;
                }

                // Build the set of attributeType from both requests
                Set<String> requestSet = buildAttributeSet( request, schemaManager );
                Set<String> previousRequestSet = buildAttributeSet( previousSearchRequest, schemaManager );

                // Check that both sets have the same size again after having converted
                // the attributes to OID
                if ( requestSet.size() != previousRequestSet.size() )
                {
                    return false;
                }

                for ( String attribute : requestSet )
                {
                    previousRequestSet.remove( attribute );
                }

                // The other set must be empty
                if ( !previousRequestSet.isEmpty() )
                {
                    return false;
                }
            }
        }

        // Compare the baseDN
        try
        {
            request.getBase().apply( schemaManager );

            if ( !previousSearchRequest.getBase().isSchemaAware() )
            {
                previousSearchRequest.getBase().apply( schemaManager );
            }

            if ( !request.getBase().equals( previousSearchRequest.getBase() ) )
            {
                return false;
            }
        }
        catch ( LdapException le )
        {
            return false;
        }

        // Compare the filters
        // Here, we assume the user hasn't changed the filter's order or content,
        // as the filter is not normalized. This is a real problem, as the normalization
        // phase is done in the interceptor chain, which is a bad decision wrt what we
        // do here.
        return true; //request.getFilter().equals( previousSearchRequest.getFilter() );
    }


    /**
     * @return The current position in the cursor. This value is updated
     * after each successful search request.
     */
    public int getCurrentPosition()
    {
        return currentPosition;
    }


    /**
     * Set the new current position, incrementing it with the
     * number of returned entries.
     *
     * @param returnedEntries The number of returned entries
     */
    public void incrementCurrentPosition( int returnedEntries )
    {
        this.currentPosition += returnedEntries;
    }


    /**
     * @return The previous search request
     */
    public SearchRequest getPreviousSearchRequest()
    {
        return previousSearchRequest;
    }


    /**
     * @return The associated cursor
     */
    public EntryFilteringCursor getCursor()
    {
        return cursor;
    }


    /**
     * Set the new cursor for this search request
     * @param cursor The associated cursor
     */
    public void setCursor( EntryFilteringCursor cursor )
    {
        this.cursor = cursor;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "PagedSearch context : <" );
        sb.append( Strings.dumpBytes( cookie ) );
        sb.append( ", " );
        sb.append( currentPosition );
        sb.append( ">" );

        return sb.toString();
    }
}
