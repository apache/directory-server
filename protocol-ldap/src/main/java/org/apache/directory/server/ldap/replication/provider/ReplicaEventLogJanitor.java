/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ldap.replication.provider;

import java.util.Map;

import org.apache.directory.server.ldap.replication.ReplicaEventMessage;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.Csn;
import org.apache.directory.shared.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes old entries from the replication event logs that
 * are configured in refreshNPersist mode
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaEventLogJanitor extends Thread
{
    private static final Logger LOG = LoggerFactory.getLogger( ReplicaEventLogJanitor.class );
    
    private long thresholdCount;
    
    private long thresholdTime;
    
    private Map<Integer, ReplicaEventLog> replicaLogMap;
    
    private volatile boolean stop = false;
    
    public ReplicaEventLogJanitor( Map<Integer, ReplicaEventLog> replicaLogMap )
    {
        // if log is in refreshNpersist mode, has more than 10k entries then 
        // all the entries before the last sent CSN and older than 5 hours will be purged
        this( replicaLogMap, 10000, ( 5 * 60 * 60 * 1000 ) ); 
    }
    
    public ReplicaEventLogJanitor( Map<Integer, ReplicaEventLog> replicaLogMap, long thresholdCount, long thresholdTime )
    {
        this.replicaLogMap = replicaLogMap;
        this.thresholdCount = thresholdCount;
        this.thresholdTime = thresholdTime;
        setDaemon( true );
    }
    
    @Override
    public void run()
    {
        while( !stop )
        {
            for( ReplicaEventLog log : replicaLogMap.values() )
            {
                if( !log.isRefreshNPersist() )
                {
                    continue;
                }
                
                synchronized ( log ) // lock the log and clean
                {
                    try
                    {
                        String lastSentCsn = log.getLastSentCsn();
                        
                        if( lastSentCsn == null )
                        {
                            LOG.debug( "last sent CSN is null for the replica {}, skipping cleanup", log.getName() );
                            return;
                        }
                        
                        if( log.count() < thresholdCount )
                        {
                            return;
                        }
                        
                        LOG.debug( "starting to purge the log entries that are older than {} milliseconds", thresholdTime );
                        
                        long now = DateUtils.getDate( DateUtils.getGeneralizedTime() ).getTime();
                        
                        long deleteCount = 0;
                        
                        ReplicaJournalCursor cursor = log.getCursor( null ); // pass no CSN
                        cursor.skipQualifyingWhileFetching();
                        
                        while( cursor.next() )
                        {
                            ReplicaEventMessage message = cursor.get();
                            String csnVal = message.getEntry().get( SchemaConstants.ENTRY_CSN_AT ).getString();
                            
                            // skip if we reach the lastSentCsn or got past it
                            if( csnVal.compareTo( lastSentCsn ) >= 0 )
                            {
                                break;
                            }
                                
                            Csn csn = new Csn( csnVal );
                            
                            if( ( now - csn.getTimestamp() ) >= thresholdTime )
                            {
                                cursor.delete();
                                deleteCount++;
                            }
                        }
                        
                        cursor.close();
                        
                        LOG.debug( "purged {} messages from the log {}", deleteCount, log.getName() );
                    }
                    catch( Exception e )
                    {
                        LOG.warn( "Failed to purge old entries from the log {}", log.getName(), e );
                    }
                }
            }
            
            try
            {
                Thread.sleep( thresholdTime );
            }
            catch( InterruptedException e )
            {
                LOG.warn( "ReplicaEventLogJanitor thread was interrupted, stopping the thread", e );
                stop = true;
            }
        }
    }
    
    
    public void stopCleaning()
    {
        stop = true;
    }
}
