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
package org.apache.directory.server.core.api.interceptor.context;


import static org.apache.directory.api.ldap.model.message.SearchScope.ONELEVEL;

import javax.naming.directory.SearchControls;

import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.StringConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;


/**
 * A Search context used for Interceptors. It contains all the informations
 * needed for the search operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchOperationContext extends FilteringOperationContext
{
    /** A flag describing the way alias should be handled */
    private AliasDerefMode aliasDerefMode = AliasDerefMode.DEREF_ALWAYS;

    /** The sizeLimit for this search operation */
    private long sizeLimit = 0;

    /** The timeLimit for this search operation */
    private int timeLimit = 0;

    /** The scope for this search : default to One Level */
    private SearchScope scope = ONELEVEL;

    /** A flag if the search operation is abandoned */
    protected boolean abandoned = false;

    /** The filter */
    private ExprNode filter;


    /** flag to indicate if this search is done for replication */
    private boolean syncreplSearch;
    
    /**
     * Creates a new instance of SearchOperationContext.
     * 
     * @param session The session to use
     */
    public SearchOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.SEARCH ) );
        }
    }


    /**
     * Creates a new instance of SearchOperationContext.
     * 
     * @param session The session to use
     * @param searchRequest The SearchRequest to process
     */
    public SearchOperationContext( CoreSession session, SearchRequest searchRequest )
    {
        super( session, searchRequest.getBase(), searchRequest.getAttributes().toArray( StringConstants.EMPTY_STRINGS ) );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.SEARCH ) );
        }

        this.filter = searchRequest.getFilter();
        this.abandoned = searchRequest.isAbandoned();
        this.aliasDerefMode = searchRequest.getDerefAliases();

        this.requestControls = searchRequest.getControls();
        this.scope = searchRequest.getScope();
        this.sizeLimit = searchRequest.getSizeLimit();
        this.timeLimit = searchRequest.getTimeLimit();
        this.typesOnly = searchRequest.getTypesOnly();

        throwReferral = !requestControls.containsKey( ManageDsaIT.OID );
    }


    /**
     * Creates a new instance of SearchOperationContext.
     * 
     * @param session The session to use
     * @param dn the dn of the search base
     * @param filter the filter AST to use for the search
     * @param searchControls the search controls
     */
    public SearchOperationContext( CoreSession session, Dn dn, ExprNode filter, SearchControls searchControls )
    {
        super( session, dn, searchControls.getReturningAttributes() );
        this.filter = filter;
        scope = SearchScope.getSearchScope( searchControls.getSearchScope() );
        timeLimit = searchControls.getTimeLimit();
        sizeLimit = searchControls.getCountLimit();
        typesOnly = searchControls.getReturningObjFlag();

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.SEARCH ) );
        }
    }


    /**
     * Creates a new instance of SearchOperationContext.
     * 
     * @param session the session this operation is associated with
     * @param dn the search base
     * @param scope the search scope
     * @param filter the filter AST to use for the search
     * @param returningAttributes the attributes to return
     */
    public SearchOperationContext( CoreSession session, Dn dn, SearchScope scope,
        ExprNode filter, String... returningAttributes )
    {
        super( session, dn, returningAttributes );
        this.scope = scope;
        this.filter = filter;

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.SEARCH ) );
        }
    }


    /**
     * Checks whether or not the ManageDsaITControl is present.  If not
     * present then the filter is modified to force the return of all referral
     * entries regardless of whether or not the filter matches the referral
     * entry.
     * 
     * @return <tt>true</tt> if the ManageDSAIt control is present
     */
    public boolean hasManageDsaItControl()
    {
        return super.hasRequestControl( ManageDsaIT.OID );
    }


    /**
     * @return The filter
     */
    public ExprNode getFilter()
    {
        return filter;
    }


    /**
     * Set the filter into the context.
     *
     * @param filter The filter to set
     */
    public void setFilter( ExprNode filter )
    {
        this.filter = filter;
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.SEARCH_REQUEST.name();
    }


    /**
     * @return true if this is a syncrepl specific search
     */
    public boolean isSyncreplSearch()
    {
        return syncreplSearch;
    }


    /**
     * Sets the flag to indicate if this is a syncrepl specific search or not
     * 
     * @param syncreplSearch The flag indicating it's a syncrepl search
     */
    public void setSyncreplSearch( boolean syncreplSearch )
    {
        this.syncreplSearch = syncreplSearch;
    }


    /**
     * @return The alias dereferencing mode
     */
    public AliasDerefMode getAliasDerefMode()
    {
        return aliasDerefMode;
    }


    /**
     * Set the Alias dereferencing mode
     * @param aliasDerefMode Th erequested mode
     */
    public void setAliasDerefMode( AliasDerefMode aliasDerefMode )
    {
        this.aliasDerefMode = aliasDerefMode;
    }


    /**
     * @return the sizeLimit
     */
    public long getSizeLimit()
    {
        return sizeLimit;
    }


    /**
     * @param sizeLimit the sizeLimit to set
     */
    public void setSizeLimit( long sizeLimit )
    {
        this.sizeLimit = sizeLimit;
    }


    /**
     * @return the timeLimit
     */
    public int getTimeLimit()
    {
        return timeLimit;
    }


    /**
     * @param timeLimit the timeLimit to set
     */
    public void setTimeLimit( int timeLimit )
    {
        this.timeLimit = timeLimit;
    }


    /**
     * @return the scope
     */
    public SearchScope getScope()
    {
        return scope;
    }


    /**
     * @param scope the scope to set
     */
    public void setScope( SearchScope scope )
    {
        this.scope = scope;
    }


    /**
     * @return the abandoned
     */
    public boolean isAbandoned()
    {
        return abandoned;
    }


    /**
     * @param abandoned the abandoned to set
     */
    public void setAbandoned( boolean abandoned )
    {
        this.abandoned = abandoned;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return "SearchContext for Dn '" + getDn().getName() + "', filter :'"
            + filter + "'";
    }
}
