
package org.apache.directory.server.core.txn;

import java.io.IOException;
import org.apache.directory.server.core.api.partition.index.Serializer;
import java.util.Comparator;

public interface TxnManager<ID>
{
    public void beginTransaction( boolean readOnly ) throws IOException;
   
    public void commitTransaction() throws IOException;
    
    public void abortTransaction() throws IOException;
    
    public Comparator<ID> getIDComparator();
    
    public Serializer getIDSerializer();
}
