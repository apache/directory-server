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
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.filter.ExprNode;


/**
 * A Cursor returning candidates satisfying a logical negation expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NotCursor<V, ID extends Comparable<ID>> extends AbstractIndexCursor<V, Entry, ID>
{
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_718 );
    private final IndexCursor<V, Entry, ID> ndnCursor;
    private final Evaluator<? extends ExprNode, Entry, ID> childEvaluator;
    private boolean available = false;


    @SuppressWarnings("unchecked")
    public NotCursor( Store<Entry, ID> db, Evaluator<? extends ExprNode, Entry, ID> childEvaluator )
        throws Exception
    {
        this.childEvaluator = childEvaluator;
        this.ndnCursor = ( IndexCursor<V, Entry, ID> ) db.getNdnIndex().forwardCursor();
    }


    public boolean available()
    {
        return available;
    }


    public void beforeValue( ID id, V value ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void before( IndexEntry<V, Entry, ID> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void after( IndexEntry<V, Entry, ID> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void afterValue( ID id, V value ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        ndnCursor.beforeFirst();
        available = false;
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
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
            checkNotClosed( "previous()" );
            IndexEntry<?, Entry, ID> candidate = ndnCursor.get();
            if ( !childEvaluator.evaluate( candidate ) )
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
            checkNotClosed( "next()" );
            IndexEntry<?, Entry, ID> candidate = ndnCursor.get();
            if ( !childEvaluator.evaluate( candidate ) )
            {
                return available = true;
            }
        }

        return available = false;
    }


    public IndexEntry<V, Entry, ID> get() throws Exception
    {
        checkNotClosed( "get()" );
        if ( available )
        {
            return ndnCursor.get();
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
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
