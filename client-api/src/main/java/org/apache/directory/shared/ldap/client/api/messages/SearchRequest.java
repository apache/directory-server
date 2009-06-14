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
package org.apache.directory.shared.ldap.client.api.messages;


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;


/**
 * Search request protocol message interface.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 760724 $
 */
public class SearchRequest extends AbstractRequest implements AbandonableRequest, RequestWithResponse
{
    /** Search base distinguished name */
    private String baseDn;

    /** Search filter expression tree's root node */
    private String filter;

    /** Search scope enumeration value */
    private SearchScope scope;

    /** Types only return flag */
    private boolean typesOnly;

    /** Max size in entries to return */
    private int sizeLimit;

    /** Max seconds to wait for search to complete */
    private int timeLimit;

    /** Alias dereferencing mode enumeration value */
    private AliasDerefMode aliasDerefMode;

    /** Attributes to return */
    private Set<String> attributes = new HashSet<String>();

    
    /**
     * 
     * Creates a new instance of SearchRequestImpl.
     *
     * @param messageId The message ID
     */
    public SearchRequest()
    {
        super();
    }
    
    
    // ------------------------------------------------------------------------
    // SearchRequest Interface Method Implementations
    // ------------------------------------------------------------------------
    /**
     * Gets a list of the attributes to be returned from each entry which
     * matches the search filter. There are two special values which may be
     * used: an empty list with no attributes, and the attribute description
     * string "*". Both of these signify that all user attributes are to be
     * returned. (The "*" allows the client to request all user attributes in
     * addition to specific operational attributes). Attributes MUST be named at
     * most once in the list, and are returned at most once in an entry. If
     * there are attribute descriptions in the list which are not recognized,
     * they are ignored by the server. If the client does not want any
     * attributes returned, it can specify a list containing only the attribute
     * with OID "1.1". This OID was chosen arbitrarily and does not correspond
     * to any attribute in use. Client implementors should note that even if all
     * user attributes are requested, some attributes of the entry may not be
     * included in search results due to access control or other restrictions.
     * Furthermore, servers will not return operational attributes, such as
     * objectClasses or attributeTypes, unless they are listed by name, since
     * there may be extremely large number of values for certain operational
     * attributes.
     * 
     * @return the attributes to return for this request
     */
    public Set<String> getAttributes()
    {
        return attributes;
    }


    /**
     * Gets the search base as a distinguished name.
     * 
     * @return the search base
     */
    public String getBaseDn()
    {
        return baseDn;
    }


    /**
     * Sets the search base as a distinguished name.
     * 
     * @param baseDn the search base
     */
    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }


    /**
     * Gets the alias handling parameter.
     * 
     * @return the alias handling parameter enumeration.
     */
    public AliasDerefMode getDerefAliases()
    {
        return aliasDerefMode;
    }


    /**
     * Sets the alias handling parameter.
     * 
     * @param aliasDerefAliases the alias handling parameter enumeration.
     */
    public void setDerefAliases( AliasDerefMode aliasDerefAliases )
    {
        this.aliasDerefMode = aliasDerefAliases;
    }


    /**
     * Gets the search filter associated with this search request.
     * 
     * @return the expression node for the root of the filter expression tree.
     */
    public String getFilter()
    {
        return filter;
    }


    /**
     * Sets the search filter associated with this search request.
     * 
     * @param filter the expression node for the root of the filter 
     * expression tree.
     */
    public void setFilter( String filter )
    {
        this.filter = filter;
    }


    /**
     * Gets the search scope parameter enumeration.
     * 
     * @return the scope enumeration parameter.
     */
    public SearchScope getScope()
    {
        return scope;
    }


    /**
     * Sets the search scope parameter enumeration.
     * 
     * @param scope the scope enumeration parameter.
     */
    public void setScope( SearchScope scope )
    {
        this.scope = scope;
    }


    /**
     * A sizelimit that restricts the maximum number of entries to be returned
     * as a result of the search. A value of 0 in this field indicates that no
     * client-requested sizelimit restrictions are in effect for the search.
     * Servers may enforce a maximum number of entries to return.
     * 
     * @return search size limit.
     */
    public int getSizeLimit()
    {
        return sizeLimit;
    }


    /**
     * Sets sizelimit that restricts the maximum number of entries to be
     * returned as a result of the search. A value of 0 in this field indicates
     * that no client-requested sizelimit restrictions are in effect for the
     * search. Servers may enforce a maximum number of entries to return.
     * 
     * @param entriesMax maximum search result entries to return.
     */
    public void setSizeLimit( int entriesMax )
    {
        sizeLimit = entriesMax;
    }


    /**
     * Gets the timelimit that restricts the maximum time (in seconds) allowed
     * for a search. A value of 0 in this field indicates that no client-
     * requested timelimit restrictions are in effect for the search.
     * 
     * @return the search time limit in seconds.
     */
    public int getTimeLimit()
    {
        return timeLimit;
    }


    /**
     * Sets the timelimit that restricts the maximum time (in seconds) allowed
     * for a search. A value of 0 in this field indicates that no client-
     * requested timelimit restrictions are in effect for the search.
     * 
     * @param secondsMax the search time limit in seconds.
     */
    public void setTimeLimit( int secondsMax )
    {
        timeLimit = secondsMax;
    }


    /**
     * An indicator as to whether search results will contain both attribute
     * types and values, or just attribute types. Setting this field to TRUE
     * causes only attribute types (no values) to be returned. Setting this
     * field to FALSE causes both attribute types and values to be returned.
     * 
     * @return true for only types, false for types and values.
     */
    public boolean getTypesOnly()
    {
        return typesOnly;
    }


    /**
     * An indicator as to whether search results will contain both attribute
     * types and values, or just attribute types. Setting this field to TRUE
     * causes only attribute types (no values) to be returned. Setting this
     * field to FALSE causes both attribute types and values to be returned.
     * 
     * @param typesOnly
     *            true for only types, false for types and values.
     */
    public void setTypesOnly( boolean typesOnly )
    {
        this.typesOnly = typesOnly;
    }


    /**
     * Adds attributes to the set of entry attributes to return.
     * 
     * @param attributes the attributes description or identifier.
     * @return The object itself, to allow chaining
     */
    public SearchRequest addAttributes( String... attributes )
    {
        if ( ( attributes == null ) || ( attributes.length == 0 ) )
        {
            return this;
        }
        
        if ( this.attributes == null )
        {
            this.attributes = new HashSet<String>( attributes.length );
        }
        
        for ( String attribute:attributes )
        {
            this.attributes.add( attribute );
        }
        
        return this;
    }


    /**
     * Removes attributes to the set of entry attributes to return.
     * 
     * @param attributes the attributes description or identifier.
     * @return The object itself, to allow chaining
     */
    public SearchRequest removeAttributes( String... attributes )
    {
        if ( ( attributes == null ) || ( attributes.length == 0 ) )
        {
            return this;
        }
        
        if ( this.attributes == null )
        {
            this.attributes = new HashSet<String>( attributes.length );
        }
        
        for ( String attribute:attributes )
        {
            this.attributes.remove( attribute );
        }
        
        return this;
    }
}
