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
package org.apache.directory.server.xdbm.search.cursor;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.impl.ScanCountComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor returning candidates satisfying a logical conjunction expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AndCursor<V> extends AbstractIndexCursor<V>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /** The message for unsupported operations */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_707 );

    /** */
    private final Cursor<IndexEntry<V, String>> wrapped;

    /** The evaluators used for the members of the And filter */
    private final List<Evaluator<? extends ExprNode>> evaluators;


    /**
     * Creates an instance of a AndCursor. It wraps an index cursor and the list
     * of evaluators associated with all the elements connected by the And.
     * 
     * @param wrapped The encapsulated IndexCursor
     * @param evaluators The list of evaluators associated wth the elements
     */
    public AndCursor( PartitionTxn partitionTxn, Cursor<IndexEntry<V, String>> wrapped,
        List<Evaluator<? extends ExprNode>> evaluators )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating AndCursor {}", this );
        }

        this.wrapped = wrapped;
        this.evaluators = optimize( evaluators );
        this.partitionTxn = partitionTxn;
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed();
        wrapped.beforeFirst();
        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed();
        wrapped.afterLast();
        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        beforeFirst();

        return next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        afterLast();

        return previous();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous( PartitionTxn partitionTxn ) throws LdapException, CursorException
    {
        while ( wrapped.previous() )
        {
            checkNotClosed();

            IndexEntry<V, String> candidate = wrapped.get();

            if ( matches( partitionTxn, candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean next( PartitionTxn partitionTxn ) throws LdapException, CursorException
    {
        while ( wrapped.next() )
        {
            checkNotClosed();
            IndexEntry<V, String> candidate = wrapped.get();

            if ( matches( partitionTxn, candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<V, String> get() throws CursorException
    {
        checkNotClosed();

        if ( available() )
        {
            return wrapped.get();
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing AndCursor {}", this );
        }

        super.close();
        wrapped.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing AndCursor {}", this );
        }

        super.close( cause );
        wrapped.close( cause );
    }


    /**
     * Takes a set of Evaluators and copies then sorts them in a new list with
     * increasing scan counts on their expression nodes.  This is done to have
     * the Evaluators with the least scan count which have the highest
     * probability of rejecting a candidate first.  That will increase the
     * chance of shorting the checks on evaluators early so extra lookups and
     * comparisons are avoided.
     *
     * @param unoptimized the unoptimized list of Evaluators
     * @return optimized Evaluator list with increasing scan count ordering
     */
    private List<Evaluator<? extends ExprNode>> optimize(
        List<Evaluator<? extends ExprNode>> unoptimized )
    {
        List<Evaluator<? extends ExprNode>> optimized = new ArrayList<>(
            unoptimized.size() );
        optimized.addAll( unoptimized );

        Collections.sort( optimized, new ScanCountComparator() );

        return optimized;
    }


    /**
     * Checks if the entry is a valid candidate by using the evaluators.
     */
    private boolean matches( PartitionTxn partitionTxn, IndexEntry<V, String> indexEntry ) throws LdapException
    {
        for ( Evaluator<?> evaluator : evaluators )
        {
            if ( !evaluator.evaluate( partitionTxn, indexEntry ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Dumps the evaluators
     */
    private String dumpEvaluators( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        for ( Evaluator<? extends ExprNode> evaluator : evaluators )
        {
            sb.append( evaluator.toString( tabs + "  >>" ) );
        }

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "AndCursor (" );

        if ( available() )
        {
            sb.append( "available) :\n" );
        }
        else
        {
            sb.append( "absent) :\n" );
        }

        if ( ( evaluators != null ) && !evaluators.isEmpty() )
        {
            sb.append( dumpEvaluators( tabs ) );
        }

        sb.append( wrapped.toString( tabs + "  " ) );

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
