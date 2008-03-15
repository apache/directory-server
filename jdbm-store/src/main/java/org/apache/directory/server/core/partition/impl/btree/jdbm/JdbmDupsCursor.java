package org.apache.directory.server.core.partition.impl.btree.jdbm;


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
     * An underlying Cursor which returns Tuples whose values are
     * DupsContainer objects representing either AvlTrees or BTreeRedirect
     * objects used to store the values of duplicate keys.  It does not return
     * different values for the same key.
     */
    private final DupsContainerCursor<K,V> containerCursor;

    /**
     * The current Tuple returned from the underlying DupsContainerCursor.
     */
    private final Tuple<K,DupsContainer<V>> containerTuple = new Tuple<K, DupsContainer<V>>();

    /**
     * A Cursor over a set of value objects for the current key held in the
     * containerTuple.  A new Cursor will be set for each new key as we
     * traverse.  The Cursor traverses over either a AvlTree object full
     * of values in a multi-valued key or it traverses over a BTree which
     * contains the values in the key field of it's Tuples.
     */
    private Cursor<V> dupsCursor;

    /**
     * The Tuple that is used to return values via the get() method. This
     * same Tuple instance will be returned every time.  At different
     * positions it may return different values for the same key.
     */
    private final Tuple<K,V> returnedTuple = new Tuple<K,V>();

    /**
     * Whether or not a value is available when get() is called.
     */
    private boolean valueAvailable;


    public JdbmDupsCursor( JdbmTable<K,V> table ) throws Exception
    {
        this.table = table;
        this.containerCursor = new DupsContainerCursor<K,V>( table );
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
        DupsContainer<V> container = new DupsContainer<V>( element.getValue() );
        containerCursor.before( new Tuple<K,DupsContainer<V>>( element.getKey(), container ) );
        
        if ( containerCursor.next() )
        {
            containerTuple.setBoth( containerCursor.get() );
            
            if ( containerTuple.getValue().isAvlTree() )
            {
                LOG.debug( "Duplicates tuple {} stored in a AvlTree", containerTuple );
                AvlTree<V> set = containerTuple.getValue().getAvlTree();
            }
            else 
            {
                LOG.debug( "Duplicates tuple {} are stored in a BTree", containerTuple );
                BTreeRedirect redirect = containerTuple.getValue().getBTreeRedirect();
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
        clearValue();
        containerCursor.beforeFirst();
        containerTuple.setKey( null );
        containerTuple.setValue( null );
        dupsCursor = null;
    }


    public void afterLast() throws Exception
    {
        clearValue();
        containerCursor.afterLast();
        containerTuple.setKey( null );
        containerTuple.setValue( null );
        dupsCursor = null;
    }


    public boolean first() throws Exception
    {
        clearValue();
        dupsCursor = null;

        if ( containerCursor.first() )
        {
            containerTuple.setBoth( containerCursor.get() );

            if ( containerTuple.getValue().isAvlTree() )
            {
                dupsCursor = new AvlTreeCursor<V>( containerTuple.getValue().getAvlTree() );
            }
            else
            {
                BTree bt = table.getBTree( containerTuple.getValue().getBTreeRedirect() );
                dupsCursor = new KeyCursor<V>( bt, table.getValueComparator() );
            }

            if ( ! dupsCursor.next() )
            {
                clearValue();
                return false;
            }

            returnedTuple.setKey( containerTuple.getKey() );
            returnedTuple.setValue( dupsCursor.get() );
            return valueAvailable = true;
        }

        clearValue();
        return false;
    }


    public boolean last() throws Exception
    {
        clearValue();
        dupsCursor = null;
        return valueAvailable = containerCursor.last();
    }


    private void clearValue()
    {
        returnedTuple.setKey( null );
        returnedTuple.setValue( null );
        valueAvailable = false;
    }


    public boolean previous() throws Exception
    {
        /*
         * If the iterator over the values of the current key is null or is
         * extinguished then we need to advance to the previous key.
         */
        while ( null == dupsCursor || ! dupsCursor.previous() )
        {
            /*
             * If the underlying cursor has more elements we get the previous
             * key/AvlTree Tuple to work with and get a cursor over it's
             * values.
             */
            if ( containerCursor.previous() )
            {
                containerTuple.setBoth( containerCursor.get() );
                DupsContainer<V> values = containerTuple.getValue();

                if ( values.isAvlTree() )
                {
                    AvlTree<V> set = values.getAvlTree();
                    dupsCursor = new AvlTreeCursor<V>( set );
                }
                else if ( values.isBTreeRedirect() )
                {
                    BTree tree = table.getBTree( values.getBTreeRedirect() );
                    dupsCursor = new KeyCursor<V>( tree, table.getValueComparator() );
                }

                dupsCursor.afterLast();
                if ( dupsCursor.previous() )
                {
                    break;
                }
            }
            else
            {
                dupsCursor = null;
                return false;
            }
        }

        returnedTuple.setKey( containerTuple.getKey() );
        returnedTuple.setValue( dupsCursor.get() );
        return valueAvailable = true;
    }


    public boolean next() throws Exception
    {
        /*
         * If the iterator over the values of the current key is null or is
         * extinguished then we need to advance to the next key.
         */
        while ( null == dupsCursor || ! dupsCursor.next() )
        {
            /*
             * If the underlying cursor has more elements we get the next
             * key/AvlTree Tuple to work with and get a cursor over it.
             */
            if ( containerCursor.next() )
            {
                containerTuple.setBoth( containerCursor.get() );
                DupsContainer<V> values = containerTuple.getValue();

                if ( values.isAvlTree() )
                {
                    AvlTree<V> set = values.getAvlTree();
                    dupsCursor = new AvlTreeCursor<V>( set );
                }
                else if ( values.isBTreeRedirect() )
                {
                    BTree tree = table.getBTree( values.getBTreeRedirect() );
                    dupsCursor = new KeyCursor<V>( tree, table.getValueComparator() );
                }

                dupsCursor.beforeFirst();
                if ( dupsCursor.next() )
                {
                    break;
                }
            }
            else
            {
                dupsCursor = null;
                return false;
            }
        }

        /*
         * If we get to this point then cursor has more elements and
         * containerTuple holds the Tuple containing the key and the btree or
         * AvlTree of values for that key which the Cursor traverses.  All we
         * need to do is populate our tuple object with the key and the value
         * in the cursor.
         */
        returnedTuple.setKey( containerTuple.getKey() );
        returnedTuple.setValue( dupsCursor.get() );
        return valueAvailable = true;
    }


    public Tuple<K,V> get() throws Exception
    {
        checkClosed( "get()" );

        if ( ! valueAvailable )
        {
            throw new InvalidCursorPositionException();
        }

        return returnedTuple;
    }


    public boolean isElementReused()
    {
        return true;
    }
}
