/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.api.filtering;


import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.directory.server.core.api.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.api.txn.TxnHandle;
import org.apache.directory.server.core.api.txn.TxnManager;
import org.apache.directory.shared.ldap.model.exception.OperationAbandonedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractEntryFilteringCursor implements EntryFilteringCursor
{
    /** A logger for ths class */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractEntryFilteringCursor.class );

    /** the parameters associated with the search operation */
    protected final SearchingOperationContext searchContext;

    /** The associated TxnManager */
    protected TxnManager txnManager;

    /** The associated transaction */
    protected TxnHandle transaction;
    
    /** True if a thread is using the txn */
    protected AtomicBoolean txnBusy = new AtomicBoolean(false);


    /**
     * An instance for this class
     * @param searchContext The associated search context
     */
    protected AbstractEntryFilteringCursor( SearchingOperationContext searchContext )
    {
        this.searchContext = searchContext;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setTxnManager( TxnManager txnManager )
    {
        this.txnManager = txnManager;
        transaction = txnManager.getCurTxn() ;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public TxnHandle getTransaction()
    {
        return transaction;
    }


    /**
     * {@inheritDoc}
     */
    public SearchingOperationContext getOperationContext()
    {
        return searchContext;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAbandoned()
    {
        return searchContext.isAbandoned();
    }


    /**
     * {@inheritDoc}
     */
    public void setAbandoned( boolean abandoned )
    {
        searchContext.setAbandoned( abandoned );

        if ( abandoned )
        {
            LOG.info( "Cursor has been abandoned." );
        }
    }
    
    protected boolean maybeSetCurTxn() throws Exception
    {
        if ( transaction != null )
        {
            TxnHandle curTxn = txnManager.getCurTxn();
            
            if ( curTxn != null )
            {
                if ( curTxn != transaction )
                {
                    throw new 
                        IllegalStateException("Shouldnt Have another txn running if cursor has a txn ");
                }
            }
            else
            {
                boolean busy = !txnBusy.compareAndSet( false, true );
                
                // This can happen if the abondon listener sneaked in and
                // closed the cursor. return immediately
                if ( busy )
                {
                    throw new OperationAbandonedException();
                }
                
                txnManager.setCurTxn( transaction );
                
                return true;
            }
        }
        
        return false;
    }
    
    protected void endTxnAtClose(boolean abort) throws Exception
    {
        if ( transaction == null )
        {
            return;
        }
        
        // If this thread already owns the txn, then end it and return
        TxnHandle curTxn = txnManager.getCurTxn();
        
        if ( curTxn != null )
        {
            if ( curTxn != transaction )
            {
                throw new 
                    IllegalStateException("Shouldnt Have another txn running if cursor has a txn ");
            }
            
            if ( abort == false )
            {
                txnManager.commitTransaction();
            }
            else
            {
                txnManager.abortTransaction();
            }
            
            txnBusy.set( false );
            txnManager.setCurTxn( null );
        }
        else
        {
            while ( !(transaction.getState() == TxnHandle.State.COMMIT || 
                transaction.getState() == TxnHandle.State.ABORT) )
            {
                boolean busy = !txnBusy.compareAndSet( false, true );
                
                // This can happen if the abondon listener sneaked in and
                // closed the cursor. return immediately
                if ( busy )
                {
                    try
                    {
                        System.out.println("sleeping " + this);
                        Thread.sleep( 100 );
                    }
                    catch( Exception e )
                    {
                        //ignore
                    }
                    continue;
                }
                
                if (transaction.getState() == TxnHandle.State.COMMIT || 
                    transaction.getState() == TxnHandle.State.ABORT)
                {
                    txnBusy.set( false );
                    break;
                }
                
                txnManager.setCurTxn( transaction ); 
                
                if ( abort == false )
                {
                    txnManager.commitTransaction();
                }
                else
                {
                    txnManager.abortTransaction();
                }
                
                txnBusy.set( false );
                txnManager.setCurTxn( null );
            }
        }
    }
}
