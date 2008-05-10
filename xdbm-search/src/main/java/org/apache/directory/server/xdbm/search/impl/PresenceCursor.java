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
package org.apache.directory.server.xdbm.search.impl;


import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * A returning candidates satisfying an attribute presence expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class PresenceCursor extends AbstractCursor<IndexEntry<?, ServerEntry>>
{
    private static final String UNSUPPORTED_MSG =
        "PresenceCursors do not support positioning by element without a user index on the presence attribute.";
    private final Cursor<IndexEntry<String,ServerEntry>> ndnCursor;
    private final Cursor<IndexEntry<String,ServerEntry>> presenceCursor;
    private final PresenceEvaluator presenceEvaluator;
    private boolean available = false;


    public PresenceCursor( Store<ServerEntry> db, PresenceEvaluator presenceEvaluator ) throws Exception
    {
        this.presenceEvaluator = presenceEvaluator;
        AttributeType type = presenceEvaluator.getAttributeType();

        if ( db.hasUserIndexOn( type.getOid() ) )
        {
            presenceCursor = db.getPresenceIndex().forwardCursor( type.getOid() );
            ndnCursor = null;
        }
        else
        {
            presenceCursor = null;
            ndnCursor = db.getNdnIndex().forwardCursor();
        }
    }


    public boolean available()
    {
        if ( presenceCursor != null )
        {
            return presenceCursor.available();
        }

        return available;
    }


    public void before( IndexEntry<?, ServerEntry> element ) throws Exception
    {
        if ( presenceCursor != null )
        {
            //noinspection unchecked
            presenceCursor.before( ( IndexEntry<String,ServerEntry> ) element );
            return;
        }

        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void after( IndexEntry<?, ServerEntry> element ) throws Exception
    {
        if ( presenceCursor != null )
        {
            //noinspection unchecked
            presenceCursor.after( ( IndexEntry<String,ServerEntry> ) element );
            return;
        }

        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeFirst() throws Exception
    {
        if ( presenceCursor != null )
        {
            presenceCursor.beforeFirst();
            return;
        }

        ndnCursor.beforeFirst();
        available = false;
    }


    public void afterLast() throws Exception
    {
        if ( presenceCursor != null )
        {
            presenceCursor.afterLast();
            return;
        }

        ndnCursor.afterLast();
        available = false;
    }


    public boolean first() throws Exception
    {
        if ( presenceCursor != null )
        {
            return presenceCursor.first();
        }

        beforeFirst();
        return next();
    }


    public boolean last() throws Exception
    {
        if ( presenceCursor != null )
        {
            return presenceCursor.last();
        }

        afterLast();
        return previous();
    }


    public boolean previous() throws Exception
    {
        if ( presenceCursor != null )
        {
            return presenceCursor.previous();
        }

        while ( ndnCursor.previous() )
        {
            IndexEntry<?,ServerEntry> candidate = ndnCursor.get();
            if ( presenceEvaluator.evaluate( candidate ) )
            {
                return available = true;
            }
        }
        
        return available = false;
    }


    public boolean next() throws Exception
    {
        if ( presenceCursor != null )
        {
            return presenceCursor.next();
        }

        while ( ndnCursor.next() )
        {
            IndexEntry<?,ServerEntry> candidate = ndnCursor.get();
            if ( presenceEvaluator.evaluate( candidate ) )
            {
                return available = true;
            }
        }

        return available = false;
    }


    public IndexEntry<String, ServerEntry> get() throws Exception
    {
        if ( presenceCursor != null )
        {
            if ( presenceCursor.available() )
            {
                return presenceCursor.get();
            }

            throw new InvalidCursorPositionException( "Cursor has not been positioned yet." );
        }

        if ( available )
        {
            /*
             * The value of NDN indices is the normalized dn and we want the
             * value to be the value of the attribute in question.  So we will
             * set that accordingly here.
             */
            IndexEntry<String, ServerEntry> indexEntry = ndnCursor.get();
            indexEntry.setValue( presenceEvaluator.getAttributeType().getOid() );
            return indexEntry;
        }

        throw new InvalidCursorPositionException( "Cursor has not been positioned yet." );
    }


    public boolean isElementReused()
    {
        if ( presenceCursor != null )
        {
            return presenceCursor.isElementReused();
        }

        return ndnCursor.isElementReused();
    }


    public void close() throws Exception
    {
        super.close();

        if ( presenceCursor != null )
        {
            presenceCursor.close();
        }
        else
        {
            ndnCursor.close();
        }
    }
}
