package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.util.Comparator;

import jdbm.btree.BTree;

import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.core.avltree.AvlTreeCursor;
import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over a BTree which manages duplicate keys.
 */
public class JdbmDupsCursor<K,V> extends AbstractCursor<Tuple<K,V>>
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmDupsCursor.class.getSimpleName() );
    
    /**
     * The JDBM backed table this Cursor traverses over.
     */
    private final JdbmTable<K,V> table;

    /**
     * The Tuple that is used to return values via the get() method. This
     * same Tuple instance will be returned every time.  At different
     * positions it may return different values for the same key if the table
     * supports duplicate keys.
     */
    private final Tuple<K,V> dupsTuple = new Tuple<K,V>();

    /**
     * The underlying Cursor which is simply returns Tuples with key value
     * pairs in the btree of the JDBM Table.  It does not return different
     * values for the same key: hence it is not duplicate key aware.  So for
     * tables supporting duplicate keys, it's Tuple vlaues may contain
     * AvlTree or BTreeRedirect objects.  These objects are processed by
     * this outer Cursor to mimic traversal over the Table as if duplicate
     * keys are natively allowed by JDBM.
     *
     * In essence the AvlTree and the BTreeRedirect are used to store multiple
     * values for the same key.
     */
    private final JdbmNoDupsCursor<K,V> noDupsCursor;

    /**
     * A Cursor over a set of value objects for the current key.  A new Cursor
     * will be created for each new key as we traverse table's that allow for
     * duplicate keys.  The Cursor traverses over either a AvlTree object full
     * of values in a multi-valued key or it traverses over a BTree which
     * contains the values in it's keys.
     */
    private Cursor<V> dupCursor;

    /**
     * The current Tuple returned from the underlying JdbmNoDupsCursor which
     * may contain a AvlTree or BTreeRedirect for Tuple values.  A
     * JdbmNoDupsCursor on a Table that allows duplicates returns AvlTrees or
     * BTreeRedirect objects for Tuple values.
     */
    private Tuple<K,V> noDupsTuple;

    /**
     * Whether or not a value is available when get() is called.
     */
    private boolean valueAvailable;


    public JdbmDupsCursor( JdbmTable<K,V> table ) throws Exception
    {
        this.table = table;
        this.noDupsCursor = new JdbmNoDupsCursor<K,V>( table );
    }


    public boolean available()
    {
        return valueAvailable;
    }


    /**
     * Advances this Cursor just before the record with key and value equal to 
     * those provided in the Tuple argument.  If the key is not present the 
     * Cursor advances to just before the greatest value of the key less than 
     * but not greater than the one provided by the Tuple argument.  If the 
     * key is present but the value is not present the Cursor advances to the 
     * element with the same key containing a value less than but not greater
     * than the value in the Tuple.
     */
    public void before( Tuple<K,V> element ) throws Exception
    {
        noDupsCursor.before( element );
        
        if ( noDupsCursor.next() )
        {
            noDupsTuple = noDupsCursor.get();
            
            if ( noDupsTuple.getValue() instanceof byte[] )
            {
                LOG.debug( "Duplicates tuple {} stored in a AvlTree", noDupsTuple );
                AvlTree<V> set = ( AvlTree<V> ) table.getMarshaller().deserialize( ( byte[] ) noDupsTuple.getValue() );
            }
            else 
            {
                LOG.debug( "Duplicates tuple {} are stored in a BTree", noDupsTuple );
                BTreeRedirect redirect = ( BTreeRedirect ) noDupsTuple.getValue();
                
                
            }
        }
//        throw new NotImplementedException();
    }


    public void after( Tuple<K,V> element ) throws Exception
    {
        throw new NotImplementedException();
    }


    public void beforeFirst() throws Exception
    {
        throw new NotImplementedException();
    }


    public void afterLast() throws Exception
    {
        throw new NotImplementedException();
    }


    public boolean first() throws Exception
    {
        throw new NotImplementedException();
    }


    private void clearValue()
    {
        dupsTuple.setKey( null );
        dupsTuple.setValue( null );
        valueAvailable = false;
    }


    public boolean last() throws Exception
    {
        if ( noDupsCursor.last() )
        {
            noDupsTuple = noDupsCursor.get();
            Object values = noDupsTuple.getValue();

            if ( values instanceof AvlTree)
            {
                //noinspection unchecked
                AvlTree<V> set = ( AvlTree<V> ) noDupsTuple.getValue();
                dupCursor = new AvlTreeCursor<V>( set );
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
                dupCursor = new KeyCursor<V>( tree, ( Comparator<V> ) table.getComparator().getKeyComparator() );
                if ( ! dupCursor.previous() )
                {
                    clearValue();
                    return false;
                }
            }

            /*
             * If we get to this point then cursor has more elements and
             * noDupsTuple holds the Tuple containing the key and the btree or
             * AvlTree of values for that key which the Cursor traverses.  All we
             * need to do is populate our tuple object with the key and the value
             * in the cursor.
             */
            dupsTuple.setKey( noDupsTuple.getKey() );
            dupsTuple.setValue( dupCursor.get() );
            return valueAvailable = true;
        }

        clearValue();
        return false;
    }


    public boolean previous() throws Exception
    {
        /*
         * If the iterator over the values of the current key is null or is
         * extinguished then we need to advance to the previous key.
         */
        while ( null == dupCursor || ! dupCursor.previous() )
        {
            /*
             * If the underlying cursor has more elements we get the previous
             * key/AvlTree Tuple to work with and get a cursor over it's
             * values.
             */
            if ( noDupsCursor.previous() )
            {
                noDupsTuple = noDupsCursor.get();
                Object values = noDupsTuple.getValue();

                if ( values instanceof AvlTree )
                {
                    //noinspection unchecked
                    AvlTree<V> set = ( AvlTree ) noDupsTuple.getValue();
                    dupCursor = new AvlTreeCursor<V>( set );
                    dupCursor.previous();
                }
                else if ( values instanceof BTreeRedirect )
                {
                    BTree tree = table.getBTree( ( BTreeRedirect ) values );
                    //noinspection unchecked
                    dupCursor = new KeyCursor<V>( tree, ( Comparator<V> ) table.getComparator().getKeyComparator() );
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
         * noDupsTuple holds the Tuple containing the key and the btree or
         * AvlTree of values for that key which the Cursor traverses.  All we
         * need to do is populate our tuple object with the key and the value
         * in the cursor.
         */
        dupsTuple.setKey( noDupsTuple.getKey() );
        dupsTuple.setValue( dupCursor.get() );
        return valueAvailable = true;
    }


    public boolean next() throws Exception
    {
        /*
         * If the iterator over the values of the current key is null or is
         * extinguished then we need to advance to the next key.
         */
        while ( null == dupCursor || ! dupCursor.next() )
        {
            /*
             * If the underlying cursor has more elements we get the next
             * key/AvlTree Tuple to work with and get a cursor over it.
             */
            if ( noDupsCursor.next() )
            {
                noDupsTuple = noDupsCursor.get();
                Object values = noDupsTuple.getValue();

                if ( values instanceof AvlTree)
                {
                    //noinspection unchecked
                    AvlTree<V> set = ( AvlTree<V> ) noDupsTuple.getValue();
                    dupCursor = new AvlTreeCursor<V>( set );
                    dupCursor.next();
                }
                else if ( values instanceof BTreeRedirect )
                {
                    BTree tree = table.getBTree( ( BTreeRedirect ) values );
                    //noinspection unchecked
                    dupCursor = new KeyCursor<V>( tree, ( Comparator<V> ) table.getComparator().getKeyComparator() );
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
         * noDupsTuple holds the Tuple containing the key and the btree or
         * AvlTree of values for that key which the Cursor traverses.  All we
         * need to do is populate our tuple object with the key and the value
         * in the cursor.
         */
        dupsTuple.setKey( noDupsTuple.getKey() );
        dupsTuple.setValue( dupCursor.get() );
        return valueAvailable = true;
    }


    public Tuple<K,V> get() throws Exception
    {
        checkClosed( "get()" );

        if ( ! valueAvailable )
        {
            throw new InvalidCursorPositionException();
        }

        return dupsTuple;
    }


    public boolean isElementReused()
    {
        return true;
    }
}
