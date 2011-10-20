
package org.apache.directory.server.core.partition.index;

import java.util.Comparator;

public interface IndexComparator<V,ID> extends Comparator<IndexEntry<V,ID>>
{
    public Comparator<V> getValueComparator();
    
    
    public Comparator<ID> getIDComparator();
}
