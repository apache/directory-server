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
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.filter.ExprNode;


/**
 * A Cursor returning candidates satisfying a logical negation expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class NotCursor<V> extends AbstractIndexCursor<V, ServerEntry>
{
    private static final String UNSUPPORTED_MSG =
        "NotCursors are not ordered and do not support positioning by element.";
    private final IndexCursor<V,ServerEntry> ndnCursor;
    private final Evaluator<? extends ExprNode, ServerEntry> childEvaluator;
    private boolean available = false;


    public NotCursor( Store<ServerEntry> db,
                      Evaluator<? extends ExprNode, ServerEntry> childEvaluator ) throws Exception
    {
        this.childEvaluator = childEvaluator;
        //noinspection unchecked
        this.ndnCursor = ( IndexCursor<V,ServerEntry> ) db.getNdnIndex().forwardCursor();
    }


    public boolean available()
    {
        return available;
    }


    public void beforeValue( Long id, V value ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void before( IndexEntry<V, ServerEntry> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void after( IndexEntry<V, ServerEntry> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void afterValue( Long id, V value ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeFirst() throws Exception
    {
        ndnCursor.beforeFirst();
        available = false;
    }


    public void afterLast() throws Exception
    {
        ndnCursor.afterLast();
        available = false;
    }


    public boolean first() throws Exception
    {
        beforeFirst();
        return next();
    }


    public boolean last() throws Exception
    {
        afterLast();
        return previous();
    }


    public boolean previous() throws Exception
    {
        while ( ndnCursor.previous() )
        {
            IndexEntry<?,ServerEntry> candidate = ndnCursor.get();
            if ( ! childEvaluator.evaluate( candidate ) )
            {
                return available = true;
            }
        }

        return available = false;
    }


    public boolean next() throws Exception
    {
        while ( ndnCursor.next() )
        {
            IndexEntry<?,ServerEntry> candidate = ndnCursor.get();
            if ( ! childEvaluator.evaluate( candidate ) )
            {
                return available = true;
            }
        }

        return available = false;
    }


    public IndexEntry<V, ServerEntry> get() throws Exception
    {
        if ( available )
        {
            return ndnCursor.get();
        }

        throw new InvalidCursorPositionException( "Cursor has not been positioned yet." );
    }


    public boolean isElementReused()
    {
        return ndnCursor.isElementReused();
    }


    public void close() throws Exception
    {
        super.close();
        ndnCursor.close();
    }
}
