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
package org.apache.directory.server.core.interceptor.context;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITControl;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.schema.AttributeTypeOptions;


/**
 * A Search context used for Interceptors. It contains all the informations
 * needed for the search operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchOperationContext extends SearchingOperationContext
{
    /** The filter */
    private ExprNode filter;


    /**
     * Creates a new instance of SearchOperationContext.
     */
    public SearchOperationContext( CoreSession session )
    {
        super( session );
    }


    /**
     * Creates a new instance of SearchOperationContext.
     * @throws Exception 
     */
    public SearchOperationContext( CoreSession session, SearchRequest searchRequest ) throws LdapException
    {
        super( session );
        
        this.dn = searchRequest.getBase();
        this.filter = searchRequest.getFilter();
        this.abandoned = searchRequest.isAbandoned();
        this.aliasDerefMode = searchRequest.getDerefAliases();
        
        this.requestControls = searchRequest.getControls();
        this.scope = searchRequest.getScope();
        this.sizeLimit = searchRequest.getSizeLimit();
        this.timeLimit = searchRequest.getTimeLimit();
        this.typesOnly = searchRequest.getTypesOnly();
        
        List<String> ats = searchRequest.getAttributes();
        
        // section 4.5.1.8 of RFC 4511
        //1. An empty list with no attributes requests the return of all user attributes.
        if ( ats.isEmpty() )
        {
            ats = new ArrayList<String>();
            ats.add( SchemaConstants.ALL_USER_ATTRIBUTES );
            ats = Collections.unmodifiableList( ats ); 
        }
        
        setReturningAttributes( ats );
        
        if ( requestControls.containsKey( ManageDsaITControl.CONTROL_OID ) )
        {
            throwReferral = false;
        }
        else
        {
            throwReferral = true;
        }
    }


    /**
     * Creates a new instance of SearchOperationContext.
     * 
     * @param dn the dn of the search base
     * @param filter the filter AST to use for the search
     * @param searchControls the search controls
     */
    public SearchOperationContext( CoreSession session, Dn dn, ExprNode filter, SearchControls searchControls ) throws LdapException
    {
        super( session, dn );
        this.filter = filter;
        scope = SearchScope.getSearchScope( searchControls.getSearchScope() );
        timeLimit = searchControls.getTimeLimit();
        sizeLimit = searchControls.getCountLimit();
        typesOnly = searchControls.getReturningObjFlag();

        if ( searchControls.getReturningAttributes() != null )
        {
            setReturningAttributes( searchControls.getReturningAttributes() );
        }
        else
        {
            setReturningAttributes( SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
        }
    }


    /**
     * Creates a new instance of SearchOperationContext.
     * 
     * @param session the session this operation is associated with
     * @param dn the search base
     * @param scope the search scope
     * @param filter the filter AST to use for the search
     * @param aliasDerefMode the alias dereferencing mode
     * @param returningAttributes the attributes to return
     */
    public SearchOperationContext( CoreSession session, Dn dn, SearchScope scope,
        ExprNode filter, Set<AttributeTypeOptions> returningAttributes )
    {
        super( session, dn, returningAttributes );
        super.setScope( scope );
        this.filter = filter;
    }


    /**
     * Checks whether or not the ManageDsaITControl is present.  If not 
     * present then the filter is modified to force the return of all referral
     * entries regardless of whether or not the filter matches the referral
     * entry.
     */
    public boolean hasManageDsaItControl()
    {
        return super.hasRequestControl( ManageDsaITControl.CONTROL_OID );
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
     * @see Object#toString()
     */
    public String toString()
    {
        return "SearchContext for Dn '" + getDn().getName() + "', filter :'"
        + filter + "'"; 
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.SEARCH_REQUEST.name();
    }
}
