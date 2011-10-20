
package org.apache.directory.server.core.api.partition.index;

import java.util.Comparator;

public interface IndexComparator<V,ID> extends Comparator<IndexEntry<V,ID>>
{
    public Comparator<V> getValueComparator();
    
    
    public Comparator<ID> getIDComparator();
}
