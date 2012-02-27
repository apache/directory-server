/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.server.core.shared.txn.logedit;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.server.core.api.txn.logedit.AbstractLogEdit;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TxnStateChange extends AbstractLogEdit
{
    /** State to record for the txn */
    ChangeState txnState;

    private static final long serialVersionUID = 1;


    // For deserialization
    public TxnStateChange()
    {
        super( Long.MIN_VALUE );
    }


    public TxnStateChange( long txnID, ChangeState txnState )
    {
        super( txnID );
        this.txnState = txnState;
    }


    public ChangeState getTxnState()
    {
        return txnState;
    }


    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        txnID = in.readLong();
        txnState = ChangeState.values()[in.readInt()];
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        out.writeLong( txnID );
        out.writeInt( txnState.ordinal() );
    }

    public enum ChangeState
    {
        TXN_BEGIN,
        TXN_COMMIT,
        TXN_ABORT
    }
}
