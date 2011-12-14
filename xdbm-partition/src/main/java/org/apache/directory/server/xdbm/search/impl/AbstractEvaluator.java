
package org.apache.directory.server.xdbm.search.impl;

import java.util.UUID;

import org.apache.directory.server.core.api.partition.OperationExecutionManager;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.name.Dn;


public abstract class AbstractEvaluator<T extends ExprNode> implements Evaluator<T>
{
    /** The backend */
    protected final Partition db;
    
    /** Txn log manager */
    protected TxnLogManager txnLogManager;
    
    /** Master table */
    private MasterTable masterTable;
    
    /** Operation execution manager */
    protected OperationExecutionManager executionManager;
    
    /** Txn and Operation Execution Factories */
    protected TxnManagerFactory txnManagerFactory;
    protected OperationExecutionManagerFactory executionManagerFactory;
    
    
    public AbstractEvaluator( Partition db, TxnManagerFactory txnManagerFactory,
        OperationExecutionManagerFactory executionManagerFactory ) throws Exception
    {
        this.db = db;
        txnLogManager = txnManagerFactory.txnLogManagerInstance();
        masterTable = txnLogManager.wrap( db.getSuffixDn(), db.getMasterTable() );
        executionManager = executionManagerFactory.instance();
        
        this.txnManagerFactory = txnManagerFactory;
        this.executionManagerFactory = executionManagerFactory;
    }
    
    
    public AbstractEvaluator()
    {
        // If no partition is there, we wont initialize the txn and operation execution manager
        db = null;
    }
    
    
    protected Entry getEntry( UUID id ) throws Exception
    {
        Entry entry = masterTable.get( id );
        
        if ( entry != null )
        {
            Dn dn = executionManager.buildEntryDn( db, id );
            entry.setDn( dn );
        }
        
        return entry;
    }
}
