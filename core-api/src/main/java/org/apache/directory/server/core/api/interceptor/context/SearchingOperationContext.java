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


import static org.apache.directory.shared.ldap.model.message.SearchScope.ONELEVEL;

import java.util.Set;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;


/**
 * A context used for search related operations and used by all
 * the Interceptors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class SearchingOperationContext extends FilteringOperationContext
{
    /** A flag describing the way alias should be handled */
    protected AliasDerefMode aliasDerefMode = AliasDerefMode.DEREF_ALWAYS;

    /** The sizeLimit for this search operation */
    protected long sizeLimit = 0;

    /** The timeLimit for this search operation */
    protected int timeLimit = 0;

    /** The scope for this search : default to One Level */
    protected SearchScope scope = ONELEVEL;

    /** A flag if the search operation is abandoned */
    protected boolean abandoned = false;


    /**
     * Creates a new instance of SearchingOperationContext.
     */
    public SearchingOperationContext( CoreSession session )
    {
        super( session );
    }


    /**
     * Creates a new instance of SearchingOperationContext.
     *
     * @param session The LDAP session we are in
     * @param dn The Dn to get the suffix from
     */
    public SearchingOperationContext( CoreSession session, Dn dn )
    {
        super( session, dn );
    }


    /**
     * Creates a new instance of a SearchingOperationContext using one level
     * scope, with attributes to return.
     *
     * @param session The LDAP session we are in
     * @param dn The Dn to get the suffix from
     * @param returningAttributes The list of attributes to return
     * @throws LdapException
     */
    public SearchingOperationContext( CoreSession session, Dn dn, Set<AttributeTypeOptions> returningAttributes )
    {
        super( session, dn, returningAttributes );
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
     * @param sizeLimit the sizeLimit to set
     */
    public void setSizeLimit( long sizeLimit )
    {
        this.sizeLimit = sizeLimit;
    }


    /**
     * @return the sizeLimit
     */
    public long getSizeLimit()
    {
        return sizeLimit;
    }


    /**
     * @param timeLimit the timeLimit to set
     */
    public void setTimeLimit( int timeLimit )
    {
        this.timeLimit = timeLimit;
    }


    /**
     * @return the timeLimit
     */
    public int getTimeLimit()
    {
        return timeLimit;
    }


    /**
     * @param scope the scope to set
     */
    public void setScope( SearchScope scope )
    {
        this.scope = scope;
    }


    /**
     * @return the scope
     */
    public SearchScope getScope()
    {
        return scope;
    }


    /**
     * @param abandoned the abandoned to set
     */
    public void setAbandoned( boolean abandoned )
    {
        this.abandoned = abandoned;
    }


    /**
     * @return the abandoned
     */
    public boolean isAbandoned()
    {
        return abandoned;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "SearchingOperationContext with Dn '" + getDn().getName() + "'";
    }
}
