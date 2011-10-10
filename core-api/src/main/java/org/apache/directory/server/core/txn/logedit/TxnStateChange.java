
package org.apache.directory.server.core.txn.logedit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class TxnStateChange extends AbstractLogEdit
{
    long txnID;
    State txnState;
    
    public TxnStateChange( long txnID, State txnState )
    {
        this.txnID = txnID;
        this.txnState = txnState;
    }
    
    public long getTxnID()
    {
        return this.txnID;
    }
    
    public State getTxnState()
    {
        return this.txnState;
    }
    
    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        txnID = in.readLong();
        txnState = State.values()[in.readInt()];
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        out.writeLong( txnID );
        out.writeInt( txnState.ordinal() );
    }
    
    public enum State
    {
        TXN_BEGIN,
        TXN_COMMIT,
        TXN_ABORT
    }

}
