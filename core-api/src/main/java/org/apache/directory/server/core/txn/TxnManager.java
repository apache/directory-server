
package org.apache.directory.server.core.txn;

import java.io.IOException;

public interface TxnManager
{
    public void beginTransaction( boolean readOnly ) throws IOException;
   
    public void commitTransaction() throws IOException;
    
    public void abortTransaction() throws IOException;
}
