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
package org.apache.directory.server.core.api.event;


import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * Contains the set of notification criteria required for triggering the 
 * delivery of change notifications notifications to {@link DirectoryListener}s.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NotificationCriteria
{
    /** The scope to use (default to ONE_LEVEL) */
    private SearchScope scope = SearchScope.ONELEVEL;

    /** The AliasderefMode to use (default to DEREF_ALWAYS) */
    private AliasDerefMode aliasDerefMode = AliasDerefMode.DEREF_ALWAYS;

    /** The Base DN to search from (default to null) */
    private Dn base = null;

    /** The filter to use (default to '(ObjectClass=*)') */
    private ExprNode filter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT );

    /** The event mask to use (default to everything) */
    private int eventMask = EventType.ALL_EVENT_TYPES_MASK;


    /**
     * Create a new instance of a NotiticationCriteria
     */
    public NotificationCriteria()
    {
    }


    /**
     * Create a new instance of a NotiticationCriteria initialized with a search request
     */
    public NotificationCriteria( SearchRequest req )
    {
        this.scope = req.getScope();
        this.aliasDerefMode = req.getDerefAliases();
        this.base = req.getBase();
        this.filter = req.getFilter();
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
     * @param aliasDerefMode the aliasDerefMode to set
     */
    public void setAliasDerefMode( AliasDerefMode aliasDerefMode )
    {
        this.aliasDerefMode = aliasDerefMode;
    }


    /**
     * @return the aliasDerefMode
     */
    public AliasDerefMode getAliasDerefMode()
    {
        return aliasDerefMode;
    }


    /**
     * @param base the base to set
     */
    public void setBase( Dn base )
    {
        this.base = base;
    }


    /**
     * @return the base
     */
    public Dn getBase()
    {
        return base;
    }


    /**
     * @param filter the filter to set
     */
    public void setFilter( ExprNode filter )
    {
        this.filter = filter;
    }


    /**
     * @param filter the filter to set
     */
    public void setFilter( String filter ) throws Exception
    {
        this.filter = FilterParser.parse( filter );
    }


    /**
     * @return the filter
     */
    public ExprNode getFilter()
    {
        return filter;
    }


    /**
     * @param eventMask the eventMask to set
     */
    public void setEventMask( int eventMask )
    {
        this.eventMask = eventMask;
    }


    /**
     * @param eventTypes the eventTypes to set
     */
    public void setEventMask( EventType... eventTypes )
    {
        this.eventMask = EventType.getMask( eventTypes );
    }


    /**
     * @return the eventMask
     */
    public int getEventMask()
    {
        return eventMask;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Notification criteria : " );
        sb.append( '\'' ).append( base ).append( "', " );
        sb.append( '\'' ).append( filter ).append( "', " );
        sb.append( '\'' ).append( scope ).append( "', " );
        sb.append( '\'' ).append( aliasDerefMode ).append( "', " );
        sb.append( '\'' ).append( EventType.toString( eventMask ) ).append( '\'' );

        return sb.toString();
    }
}
