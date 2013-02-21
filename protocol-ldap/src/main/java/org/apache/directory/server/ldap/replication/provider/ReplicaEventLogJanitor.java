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


import java.io.File;
import java.util.Map;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.Csn;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.ldap.replication.ReplicaEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Deletes old entries from the replication event logs that are configured in refreshNPersist mode.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaEventLogJanitor extends Thread
{
    private static final Logger LOG = LoggerFactory.getLogger( ReplicaEventLogJanitor.class );

    private DirectoryService directoryService;

    private Map<Integer, ReplicaEventLog> replicaLogMap;

    private volatile boolean stop = false;

    /** A lock used to wait */
    final Object lock = new Object();

    /** time the janitor thread sleeps before successive cleanup attempts. Default value is 5 minutes */
    private long sleepTime = 5 * 60 * 1000L;

    private long thresholdTime = 2 * 60 * 60 * 1000L;


    public ReplicaEventLogJanitor( final DirectoryService directoryService,
        final Map<Integer, ReplicaEventLog> replicaLogMap )
    {
        // if log is in refreshNpersist mode, has more entries than the log's threshold count then 
        // all the entries before the last sent CSN and older than 2 hours will be purged
        this.directoryService = directoryService;
        this.replicaLogMap = replicaLogMap;
        setDaemon( true );
    }


    @Override
    public void run()
    {
        while ( !stop )
        {
            for ( ReplicaEventLog log : replicaLogMap.values() )
            {
                synchronized ( log ) // lock the log and clean
                {
                    try
                    {
                        String lastSentCsn = log.getLastSentCsn();

                        if ( lastSentCsn == null )
                        {
                            LOG.debug( "last sent CSN is null for the replica {}, skipping cleanup", log.getName() );
                            return;
                        }

                        long now = DateUtils.getDate( DateUtils.getGeneralizedTime() ).getTime();

                        long maxIdleTime = log.getMaxIdlePeriod() * 1000L;

                        long lastUpdatedTime = new Csn( lastSentCsn ).getTimestamp();

                        LOG.debug( "checking log idle time now={} lastUpdatedTime={} maxIdleTime={}", now,
                            lastUpdatedTime, maxIdleTime );

                        if ( ( now - lastUpdatedTime ) >= maxIdleTime )
                        {
                            //max idle time of the event log reached, delete it
                            removeEventLog( log );

                            // delete the associated entry from DiT, note that ConsumerLogEntryDeleteListener 
                            // will get called eventually but removeEventLog() will not be called cause by 
                            // that time this log will not be present in replicaLogMap
                            // The reason we don't call this method first is to guard against any rename
                            // operation performed on the log's entry in DiT
                            try
                            {
                                directoryService.getAdminSession().delete( log.getConsumerEntryDn() );
                            }
                            catch ( LdapException e )
                            {
                                LOG.warn( "Failed to delete the entry {} of replica event log {}",
                                    log.getConsumerEntryDn(), log.getName(), e );
                            }

                            continue;
                        }

                        long thresholdCount = log.getPurgeThresholdCount();

                        if ( log.count() < thresholdCount )
                        {
                            continue;
                        }

                        LOG.debug( "starting to purge the log entries that are older than {} milliseconds",
                            thresholdTime );

                        long deleteCount = 0;

                        ReplicaJournalCursor cursor = log.getCursor( null ); // pass no CSN
                        cursor.skipQualifyingWhileFetching();

                        while ( cursor.next() )
                        {
                            ReplicaEventMessage message = cursor.get();
                            String csnVal = message.getEntry().get( SchemaConstants.ENTRY_CSN_AT ).getString();

                            // skip if we reach the lastSentCsn or got past it
                            if ( csnVal.compareTo( lastSentCsn ) >= 0 )
                            {
                                break;
                            }

                            Csn csn = new Csn( csnVal );

                            if ( ( now - csn.getTimestamp() ) >= thresholdTime )
                            {
                                cursor.delete();
                                deleteCount++;
                            }
                        }

                        cursor.close();

                        LOG.debug( "purged {} messages from the log {}", deleteCount, log.getName() );
                    }
                    catch ( Exception e )
                    {
                        LOG.warn( "Failed to purge old entries from the log {}", log.getName(), e );
                    }
                }
            }

            try
            {
                synchronized ( lock )
                {
                    lock.wait( sleepTime );
                }
            }
            catch ( InterruptedException e )
            {
                LOG.warn( "ReplicaEventLogJanitor thread was interrupted, processing logs for cleanup", e );
            }
        }
    }


    public synchronized void removeEventLog( ReplicaEventLog replicaEventLog )
    {
        directoryService.getEventService().removeListener( replicaEventLog.getPersistentListener() );
        String name = replicaEventLog.getName();
        LOG.debug( "removed the persistent listener for replication event log {}", name );

        replicaLogMap.remove( replicaEventLog.getId() );

        try
        {
            replicaEventLog.stop();

            new File( directoryService.getInstanceLayout().getReplDirectory(), name + ".db" ).delete();
            new File( directoryService.getInstanceLayout().getReplDirectory(), name + ".lg" ).delete();
            LOG.info( "successfully removed replication event log {}", name );
        }
        catch ( Exception e )
        {
            LOG.warn(
                "Closing the replication event log of the entry {} was not successful, will be removed anyway",
                name, e );
        }
    }


    public void setSleepTime( long sleepTime )
    {
        this.sleepTime = sleepTime;
    }


    public long getSleepTime()
    {
        return sleepTime;
    }


    public void stopCleaning()
    {
        stop = true;

        synchronized ( lock )
        {
            lock.notify();
        }
    }
}
