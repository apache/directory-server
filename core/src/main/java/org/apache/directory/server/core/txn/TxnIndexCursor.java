
package org.apache.directory.server.core.txn;

import org.apache.directory.server.core.partition.index.IndexCursor;
import org.apache.directory.server.core.partition.index.AbstractIndexCursor;
import org.apache.directory.server.core.partition.index.IndexEntry;

import org.apache.directory.server.core.partition.index.ForwardIndexEntry;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.Tuple;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.NavigableSet;

public class TxnIndexCursor <V, O, ID> extends AbstractIndexCursor<V, O, ID>
{
    /** list of changed index entries */
    private NavigableSet<IndexEntry<V,ID>> changedEntries;
    
    /** forward or reverse index */
    private boolean forwardIndex;
    
    /** whether cursor is explicitly positioned */
    private boolean positioned;
    
    /** whether the moving direction is next */
    private boolean movingNext = true;
    
    /** Iterator to move over the set */
    private Iterator<IndexEntry<V,ID>> it;
    
    /** currently available value */
    IndexEntry<V,ID> availableValue;
    
    /** unsupported operation message */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_722 );
   
    
    public TxnIndexCursor( NavigableSet<IndexEntry<V,ID>> changedEntries, boolean forwardIndex )
    {
        this.changedEntries = changedEntries;
        this.forwardIndex = forwardIndex;
        
        if ( changedEntries.size()  < 1 )
        {
            throw new IllegalArgumentException("TxnIndexCursor should not be constructed with no index  changes");
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<V, ID> element ) throws Exception
    {
        positioned = true;
        availableValue = null;
        movingNext = true;
        it = changedEntries.tailSet( element, false ).iterator();
    }
    
    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<V, ID> element ) throws Exception
    {
        positioned = true;
        availableValue = null;
        movingNext = true;
        it = changedEntries.tailSet( element, true ).iterator();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void afterValue( ID id, V value ) throws Exception
    {
        ForwardIndexEntry<V,ID> indexEntry = new ForwardIndexEntry();
        indexEntry.setId( id );
        indexEntry.setValue( value );
        this.after( indexEntry );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( ID id, V value ) throws Exception
    {
        ForwardIndexEntry<V,ID> indexEntry = new ForwardIndexEntry();
        indexEntry.setId( id );
        indexEntry.setValue( value );
        this.before( indexEntry );
    }
   
    
    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        positioned = true;
        availableValue = null;
        movingNext = true;
        it = changedEntries.iterator();
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        positioned = true;
        availableValue = null;
        movingNext = false;
        it = changedEntries.descendingIterator();
    }

    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        this.beforeFirst();
        return this.next();
    }

    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        this.afterLast();
        return this.previous();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        if ( positioned == false )
        {
            afterLast();
        }
        
        if ( movingNext == true )
        {
            if ( availableValue == null )
            {
                if ( it.hasNext() )
                {
                    availableValue = it.next();
                }
            }
            
            if ( availableValue == null )
            {
                it = changedEntries.descendingIterator();
            }
            else
            {
                it = changedEntries.headSet( availableValue, false ).descendingIterator();
            }
            
            availableValue = null;
            movingNext = false;
        }

        if ( it.hasNext() )
        {
            availableValue = it.next();
            return true;
        }
        else
        {
            availableValue = null;
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        if ( positioned == false )
        {
            afterLast();
        }
        
        if ( movingNext == false )
        {
            if ( availableValue == null )
            {
                if ( it.hasNext() )
                {
                    availableValue = it.next();
                }
            }
            
            if ( availableValue == null )
            {
                it = changedEntries.iterator();
            }
            else
            {
                it = changedEntries.tailSet( availableValue, false ).descendingIterator();
            }
            
            availableValue = null;
            movingNext = true;
        }

        if ( it.hasNext() )
        {
            availableValue = it.next();
            return true;
        }
        else
        {
            availableValue = null;
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public IndexEntry<V, ID> get() throws Exception
    {
        if ( availableValue != null )
        {
            return availableValue;
        }

        throw new InvalidCursorPositionException();
    }
    
    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }

}
