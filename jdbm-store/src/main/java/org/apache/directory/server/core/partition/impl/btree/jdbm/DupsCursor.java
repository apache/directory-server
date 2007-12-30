package org.apache.directory.server.core.partition.impl.btree.jdbm;


import jdbm.helper.Tuple;
import jdbm.btree.BTree;
import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.ListCursor;
import org.apache.directory.server.core.partition.impl.btree.TupleComparator;
import org.apache.directory.shared.ldap.NotImplementedException;

import java.io.IOException;
import java.util.*;


/**
 * A Cursor over a BTree which manages duplicate keys.
 */
public class DupsCursor extends AbstractCursor<Tuple> 
{
    private final JdbmTable table;
    private final TupleComparator comparator;
    private final TupleCursor wrapped;
    private final Tuple tuple = new Tuple();

    /**
     * A Cursor over a set of value objects for the same key.  It traverses
     * over either a TreeSet of values in a multi valued key or it traverses
     * over a BTree of values.
     */
    private Cursor<Object> dupCursor;

    /**
     * The current Tuple returned from the underlying TupleCursor which may
     * contain a TreeSet for Tuple values.  A TupleCursor on a Table that
     * allows duplicates essentially returns Strings for keys and TreeSets or
     * BTreeRedirect objects for their values.
     */
    private Tuple duplicates;
    private boolean valueAvailable;


    public DupsCursor( JdbmTable table, TupleCursor wrapped, TupleComparator comparator )
    {
        this.table = table;
        this.wrapped = wrapped;
        this.comparator = comparator;
    }


    public boolean available()
    {
        return valueAvailable;
    }


    public void before( Tuple element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public void after(Tuple element) throws IOException
    {
        throw new NotImplementedException();
    }


    public void beforeFirst() throws IOException
    {
        throw new NotImplementedException();
    }


    public void afterLast() throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean first() throws IOException
    {
        throw new NotImplementedException();
    }


    private void clearValue()
    {
        tuple.setKey( null );
        tuple.setValue( null );
        valueAvailable = false;
    }


    public boolean last() throws IOException
    {
        if ( wrapped.last() )
        {
            duplicates = wrapped.get();
            Object values = duplicates.getValue();

            if ( values instanceof TreeSet)
            {
                //noinspection unchecked
                TreeSet<Object> set = ( TreeSet ) duplicates.getValue();
                List<Object> list = new ArrayList<Object>( set.size() );
                list.addAll( set );
                dupCursor = new ListCursor<Object>( list );
                if ( ! dupCursor.previous() )
                {
                    clearValue();
                    return false;
                }
            }
            else if ( values instanceof BTreeRedirect )
            {
                BTree tree = table.getBTree( ( BTreeRedirect ) values );
                //noinspection unchecked
                dupCursor = new KeyCursor( tree, comparator.getKeyComparator() );
                if ( ! dupCursor.previous() )
                {
                    clearValue();
                    return false;
                }
            }

            /*
             * If we get to this point then cursor has more elements and
             * duplicates holds the Tuple containing the key and the btree or
             * TreeSet of values for that key which the Cursor traverses.  All we
             * need to do is populate our tuple object with the key and the value
             * in the cursor.
             */
            tuple.setKey( duplicates.getKey() );
            tuple.setValue( dupCursor.get() );
            return valueAvailable = true;
        }

        clearValue();
        return false;
    }


    public boolean previous() throws IOException
    {
        /*
         * If the iterator over the values of the current key is null or is
         * extinguished then we need to advance to the previous key.
         */
        while ( null == dupCursor || ! dupCursor.previous() )
        {
            /*
             * If the underlying cursor has more elements we get the previous
             * key/TreeSet Tuple to work with and get a cursor over it's
             * values.
             */
            if ( wrapped.previous() )
            {
                duplicates = wrapped.get();
                Object values = duplicates.getValue();

                if ( values instanceof TreeSet )
                {
                    //noinspection unchecked
                    TreeSet<Object> set = ( TreeSet ) duplicates.getValue();
                    List<Object> list = new ArrayList<Object>( set.size() );
                    list.addAll( set );
                    dupCursor = new ListCursor<Object>( list );
                    dupCursor.previous();
                }
                else if ( values instanceof BTreeRedirect )
                {
                    BTree tree = table.getBTree( ( BTreeRedirect ) values );
                    //noinspection unchecked
                    dupCursor = new KeyCursor( tree, comparator.getKeyComparator() );
                    dupCursor.previous();
                }
            }
            else
            {
                return false;
            }
        }

        /*
         * If we get to this point then cursor has more elements and
         * duplicates holds the Tuple containing the key and the btree or
         * TreeSet of values for that key which the Cursor traverses.  All we
         * need to do is populate our tuple object with the key and the value
         * in the cursor.
         */
        tuple.setKey( duplicates.getKey() );
        tuple.setValue( dupCursor.get() );
        return valueAvailable = true;
    }


    public boolean next() throws IOException
    {
        /*
         * If the iterator over the values of the current key is null or is
         * extinguished then we need to advance to the next key.
         */
        while ( null == dupCursor || ! dupCursor.next() )
        {
            /*
             * If the underlying cursor has more elements we get the next
             * key/TreeSet Tuple to work with and get a cursor over it.
             */
            if ( wrapped.next() )
            {
                duplicates = wrapped.get();
                Object values = duplicates.getValue();

                if ( values instanceof TreeSet)
                {
                    //noinspection unchecked
                    TreeSet<Object> set = ( TreeSet ) duplicates.getValue();
                    List<Object> list = new ArrayList<Object>( set.size() );
                    list.addAll( set );
                    dupCursor = new ListCursor<Object>( list );
                    dupCursor.next();
                }
                else if ( values instanceof BTreeRedirect )
                {
                    BTree tree = table.getBTree( ( BTreeRedirect ) values );
                    //noinspection unchecked
                    dupCursor = new KeyCursor( tree, comparator.getKeyComparator() );
                    dupCursor.next();
                }
            }
            else
            {
                return false;
            }
        }

        /*
         * If we get to this point then cursor has more elements and
         * duplicates holds the Tuple containing the key and the btree or
         * TreeSet of values for that key which the Cursor traverses.  All we
         * need to do is populate our tuple object with the key and the value
         * in the cursor.
         */
        tuple.setKey( duplicates.getKey() );
        tuple.setValue( dupCursor.get() );
        return valueAvailable = true;
    }


    public Tuple get() throws IOException
    {
        checkClosed( "get()" );

        if ( ! valueAvailable )
        {
            throw new InvalidCursorPositionException();
        }

        return tuple;
    }


    public boolean isElementReused()
    {
        return true;
    }
}
