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
import org.apache.directory.shared.ldap.schema.AttributeType;

import javax.naming.directory.Attributes;


/**
 * A returning candidates satisfying an attribute presence expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class PresenceCursor extends AbstractCursor<IndexEntry<?, Attributes>>
{
    private static final String UNSUPPORTED_MSG =
        "PresenceCursors do not support positioning by element without a user index on the presence attribute.";
    private final Cursor<IndexEntry<String,Attributes>> ndnCursor;
    private final Cursor<IndexEntry<String,Attributes>> existenceCursor;
    private final PresenceEvaluator presenceEvaluator;
    private boolean available = false;


    public PresenceCursor( Store<Attributes> db, PresenceEvaluator presenceEvaluator ) throws Exception
    {
        this.presenceEvaluator = presenceEvaluator;
        AttributeType type = presenceEvaluator.getAttributeType();

        if ( db.hasUserIndexOn( type.getOid() ) )
        {
            existenceCursor = db.getExistanceIndex().forwardCursor( type.getOid() );
            ndnCursor = null;
        }
        else
        {
            existenceCursor = null;
            ndnCursor = db.getNdnIndex().forwardCursor();
        }
    }


    public boolean available()
    {
        if ( existenceCursor != null )
        {
            return existenceCursor.available();
        }

        return available;
    }


    public void before( IndexEntry<?, Attributes> element ) throws Exception
    {
        if ( existenceCursor != null )
        {
            //noinspection unchecked
            existenceCursor.before( ( IndexEntry<String,Attributes> ) element );
        }

        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void after( IndexEntry<?, Attributes> element ) throws Exception
    {
        if ( existenceCursor != null )
        {
            //noinspection unchecked
            existenceCursor.after( ( IndexEntry<String,Attributes> ) element );
        }

        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeFirst() throws Exception
    {
        if ( existenceCursor != null )
        {
            existenceCursor.beforeFirst();
        }

        ndnCursor.beforeFirst();
        available = false;
    }


    public void afterLast() throws Exception
    {
        if ( existenceCursor != null )
        {
            existenceCursor.afterLast();
        }

        ndnCursor.afterLast();
        available = false;
    }


    public boolean first() throws Exception
    {
        if ( existenceCursor != null )
        {
            return existenceCursor.first();
        }

        beforeFirst();
        return next();
    }


    public boolean last() throws Exception
    {
        if ( existenceCursor != null )
        {
            return existenceCursor.last();
        }

        afterLast();
        return previous();
    }


    public boolean previous() throws Exception
    {
        if ( existenceCursor != null )
        {
            return existenceCursor.previous();
        }

        while ( ndnCursor.previous() )
        {
            IndexEntry<?,Attributes> candidate = ndnCursor.get();
            if ( presenceEvaluator.evaluate( candidate ) )
            {
                return available = true;
            }
        }
        
        return available = false;
    }


    public boolean next() throws Exception
    {
        if ( existenceCursor != null )
        {
            return existenceCursor.next();
        }

        while ( ndnCursor.next() )
        {
            IndexEntry<?,Attributes> candidate = ndnCursor.get();
            if ( presenceEvaluator.evaluate( candidate ) )
            {
                return available = true;
            }
        }

        return available = false;
    }


    public IndexEntry<String, Attributes> get() throws Exception
    {
        if ( existenceCursor != null )
        {
            if ( existenceCursor.available() )
            {
                return existenceCursor.get();
            }

            throw new InvalidCursorPositionException( "Cursor has not been positioned yet." );
        }

        if ( available )
        {
            return ndnCursor.get();
        }

        throw new InvalidCursorPositionException( "Cursor has not been positioned yet." );
    }


    public boolean isElementReused()
    {
        if ( existenceCursor != null )
        {
            return existenceCursor.isElementReused();
        }

        return ndnCursor.isElementReused();
    }


    public void close() throws Exception
    {
        super.close();

        if ( existenceCursor != null )
        {
            existenceCursor.close();
        }
        else
        {
            ndnCursor.close();
        }
    }
}
