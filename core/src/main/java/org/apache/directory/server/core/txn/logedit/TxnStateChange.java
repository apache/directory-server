
package org.apache.directory.server.core.txn.logedit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class TxnStateChange<ID> extends AbstractLogEdit<ID>
{
    /** ID of the txn associated with this change */
    long txnID;
    
    /** State to record for the txn */
    State txnState;
    
    private static final long serialVersionUID = 1;
    
    // For deserialization
    public TxnStateChange()
    {
        
    }
    
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
