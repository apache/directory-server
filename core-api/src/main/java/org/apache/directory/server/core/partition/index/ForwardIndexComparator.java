
package org.apache.directory.server.core.partition.index;

import java.util.Comparator;

public class ForwardIndexComparator<V, ID> implements Comparator<IndexEntry<V,ID>>
{
    Comparator<V> keyComparator;
    Comparator<ID> valueComparator;
    
    public ForwardIndexComparator( Comparator<V> keyComparator, Comparator<ID> valueComparator )
    {
        this.keyComparator = keyComparator;
        this.valueComparator = valueComparator;
    }
    
    public int compare( IndexEntry<V, ID> entry1, IndexEntry<V, ID> entry2 )
    {
        V value1 = entry1.getValue();
        V value2 = entry2.getValue();
        ID id1 = entry1.getId();
        ID id2 = entry2.getId();
        
        int result = keyComparator.compare( value1, value2 );
        
        if ( result == 0 )
        {
            if ( id1 == id2 )
            {
                result = 0;
            }
            else if ( id1 == null )
            {
                result = -1;
            }
            else if ( id2 == null )
            {
                result = 1;
            }
            else
            {
                result = valueComparator.compare( id1, id2 );
            }
        }
        
        return result;
    }
}
