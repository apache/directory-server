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


import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.partition.impl.btree.IndexAssertion;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.filter.SubstringNode;

import javax.naming.directory.Attributes;


/**
 * A Cursor traversing candidates matching a Substring assertion expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class SubstringCursor extends AbstractCursor<IndexEntry<String, Attributes>>
{
    private static final String UNSUPPORTED_MSG =
        "SubstringCursors may not be ordered and do not support positioning by element.";
    private Cursor<IndexEntry<String,Attributes>> wrapped;
    private IndexAssertion<String,Attributes> indexAssertion;
    private boolean available = false;


    public SubstringCursor( Store<Attributes> db,
                            final SubstringEvaluator substringEvaluator ) throws Exception
    {
        SubstringNode node = substringEvaluator.getExpression();

        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            /*
             * Get the user index and return an index enumeration using the the
             * compiled regular expression.  Try to constrain even further if
             * an initial term is available in the substring expression.
             */
            //noinspection unchecked
            Index<String,Attributes> idx = db.getUserIndex( node.getAttribute() );

            if ( null == node.getInitial() )
            {
                wrapped = idx.forwardCursor();
            }
            else
            {
                wrapped = idx.forwardCursor( node.getInitial() );
            }

            /*
             * The Cursor used is over the index for this attribute so the
             * values are already normalized.  The value of the IndexEntry is
             * the value of the attribute (not the ndn as below when we do not
             * have an Index for the attribute).  All we have to do is see if
             * the value of the IndexEntry is matched by the filter and
             * return the result.  We reuse the regex Pattern already compiled
             * for the substringEvaluator instead of recompiling for this
             * Cursor.
             */
            indexAssertion = new IndexAssertion<String,Attributes>()
            {
                public boolean assertCandidate( final IndexEntry<String,Attributes> entry ) throws Exception
                {
                    return substringEvaluator.getPattern().matcher( entry.getValue() ).matches();
                }
            };
        }
        else
        {
            /*
             * There is no index on the attribute here.  We have no choice but
             * to perform a full table scan but need to leverage an index for the
             * wrapped Cursor.  We know that all entries are listed under
             * the ndn index and so this will enumerate over all entries.  The
             * substringEvaluator is used in an assertion to constrain the
             * result set to only those entries matching the pattern.  The
             * substringEvaluator handles all the details of normalization and
             * knows to use it, when it itself detects the lack of an index on
             * the node's attribute.
             */
            wrapped = db.getNdnIndex().forwardCursor();

            indexAssertion = new IndexAssertion<String,Attributes>()
            {
                public boolean assertCandidate( final IndexEntry<String,Attributes> entry ) throws Exception
                {
                    return substringEvaluator.evaluate( entry );
                }
            };
        }
    }


    public boolean available()
    {
        return false;
    }


    public void before( IndexEntry<String, Attributes> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void after( IndexEntry<String, Attributes> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeFirst() throws Exception
    {
        wrapped.beforeFirst();
        available = false;
    }


    public void afterLast() throws Exception
    {
        wrapped.afterLast();
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
        while ( wrapped.previous() )
        {
            IndexEntry<String,Attributes> entry = wrapped.get();
            if ( indexAssertion.assertCandidate( entry ) )
            {
                return available = true;
            }
        }

        return available = false;
    }


    public boolean next() throws Exception
    {
        while ( wrapped.next() )
        {
            IndexEntry<String,Attributes> entry = wrapped.get();
            if ( indexAssertion.assertCandidate( entry ) )
            {
                return available = true;
            }
        }

        return available = false;
    }


    public IndexEntry<String, Attributes> get() throws Exception
    {
        if ( available )
        {
            return wrapped.get();
        }

        throw new InvalidCursorPositionException( "Cursor has yet to be positioned." );
    }


    public boolean isElementReused()
    {
        return wrapped.isElementReused();
    }


    public void close() throws Exception
    {
        super.close();
        wrapped.close();
    }
}
