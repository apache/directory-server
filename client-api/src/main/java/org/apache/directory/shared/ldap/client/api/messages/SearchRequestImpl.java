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
 * SearchRequest implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 */
public class SearchRequestImpl extends AbstractRequest implements SearchRequest
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
    public SearchRequestImpl()
    {
        super();
    }
    
    
    // ------------------------------------------------------------------------
    // SearchRequest Interface Method Implementations
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public Set<String> getAttributes()
    {
        return attributes;
    }


    /**
     * {@inheritDoc}
     */
    public String getBaseDn()
    {
        return baseDn;
    }


    /**
     * {@inheritDoc}
     */
    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }


    /**
     * {@inheritDoc}
     */
    public AliasDerefMode getDerefAliases()
    {
        return aliasDerefMode;
    }


    /**
     * {@inheritDoc}
     */
    public void setDerefAliases( AliasDerefMode aliasDerefAliases )
    {
        this.aliasDerefMode = aliasDerefAliases;
    }


    /**
     * {@inheritDoc}
     */
    public String getFilter()
    {
        return filter;
    }


    /**
     * {@inheritDoc}
     */
    public void setFilter( String filter )
    {
        this.filter = filter;
    }


    /**
     * {@inheritDoc}
     */
    public SearchScope getScope()
    {
        return scope;
    }


    /**
     * {@inheritDoc}
     */
    public void setScope( SearchScope scope )
    {
        this.scope = scope;
    }


    /**
     * {@inheritDoc}
     */
    public int getSizeLimit()
    {
        return sizeLimit;
    }


    /**
     * {@inheritDoc}
     */
    public void setSizeLimit( int entriesMax )
    {
        sizeLimit = entriesMax;
    }


    /**
     * {@inheritDoc}
     */
    public int getTimeLimit()
    {
        return timeLimit;
    }


    /**
     * {@inheritDoc}
     */
    public void setTimeLimit( int secondsMax )
    {
        timeLimit = secondsMax;
    }


    /**
     * {@inheritDoc}
     */
    public boolean getTypesOnly()
    {
        return typesOnly;
    }


    /**
     * {@inheritDoc}
     */
    public void setTypesOnly( boolean typesOnly )
    {
        this.typesOnly = typesOnly;
    }


    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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