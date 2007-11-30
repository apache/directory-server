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
package org.apache.directory.server.core.changelog;

import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.ListCursor;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.util.DateUtils;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A change log store that keeps it's information in memory.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MemoryChangeLogStore implements TaggableChangeLogStore
{
    private long currentRevision;
    private Tag latest;
    private final Map<Long,Tag> tags = new HashMap<Long,Tag>( 100 );
    private final List<ChangeLogEvent> events = new ArrayList<ChangeLogEvent>();


    public Tag tag( long revision ) throws NamingException
    {
        if ( tags.containsKey( revision ) )
        {
            return tags.get( revision );
        }

        return latest = new Tag( revision, null );
    }


    public Tag tag() throws NamingException
    {
        if ( latest != null && latest.getRevision() == currentRevision )
        {
            return latest;
        }

        return latest = new Tag( currentRevision, null );
    }


    public Tag tag( String description ) throws NamingException
    {
        if ( latest != null && latest.getRevision() == currentRevision )
        {
            return latest;
        }

        latest = new Tag( currentRevision, description );
        tags.put( currentRevision, latest );
        return latest;
    }


    public long getCurrentRevision()
    {
        return currentRevision;
    }


    public long log( LdapPrincipal principal, Entry forward, Entry reverse ) throws NamingException
    {
        currentRevision++;
        ChangeLogEvent event = new ChangeLogEvent( currentRevision, DateUtils.getGeneralizedTime(), 
                principal, forward, reverse );
        events.add( event );
        return currentRevision;
    }


    public ChangeLogEvent lookup( long revision ) throws NamingException
    {
        if ( revision < 0 )
        {
            throw new IllegalArgumentException( "revision must be greater than or equal to 0" );
        }

        if ( revision > getCurrentRevision() )
        {
            throw new IllegalArgumentException( "The revision must not be greater than the current revision" );
        }

        return events.get( ( int ) revision );
    }


    public Cursor<ChangeLogEvent> find() throws NamingException
    {
        return new ListCursor<ChangeLogEvent>( events );
    }


    public Cursor<ChangeLogEvent> findBefore( long revision ) throws NamingException
    {
        return new ListCursor<ChangeLogEvent>( events, ( int ) revision );
    }


    public Cursor<ChangeLogEvent> findAfter( long revision ) throws NamingException
    {
        return new ListCursor<ChangeLogEvent>( ( int ) revision, events );
    }


    public Cursor<ChangeLogEvent> find( long startRevision, long endRevision ) throws NamingException
    {
        return new ListCursor<ChangeLogEvent>( ( int ) startRevision, events, ( int ) ( endRevision + 1 ) );
    }


    public Tag getLatest()
    {
        return latest;
    }
}
