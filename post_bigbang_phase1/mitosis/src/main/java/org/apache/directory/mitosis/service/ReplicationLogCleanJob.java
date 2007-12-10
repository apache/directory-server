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
package org.apache.directory.mitosis.service;


import org.apache.directory.mitosis.common.Constants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.naming.NamingException;
import java.util.Map;


/**
 * A <a href="http://www.opensymphony.com/quartz/">OpenSymphony Quartz</a>
 * {@link Job} that purges old replication logs and the old entries marked as
 * 'deleted' (i.e. {@link Constants#ENTRY_DELETED} is <tt>TRUE</tt>).  This
 * {@link Job} just calls {@link ReplicationInterceptor#purgeAgedData()} to
 * purge old data. 
 * 
 * @author The Apache Directory Project Team
 */
public class ReplicationLogCleanJob implements Job
{
    public static final String INSTANCE_ID = "instanceId";
    private Map<String,DirectoryService> services;


    public ReplicationLogCleanJob( Map<String,DirectoryService> services )
    {
        this.services = services;
    }


    public void execute( JobExecutionContext ctx ) throws JobExecutionException
    {
        String instanceId = ctx.getJobDetail().getJobDataMap().getString( INSTANCE_ID );
        if ( instanceId == null )
        {
            // Execute for all instances in the VM if instanceId is not specified.
            for ( DirectoryService service : services.values() )
            {
                execute0( service );
            }
        }
        else
        {
            // Execute for the instance with the specified instanceId if
            // it is specified.

            execute0( services.get( instanceId ) );
        }
    }


    private void execute0( DirectoryService service ) throws JobExecutionException
    {
        for ( Interceptor interceptor : service.getInterceptorChain().getAll() )
        {
            if ( interceptor instanceof ReplicationInterceptor )
            {
                try
                {
                    ( ( ReplicationInterceptor ) interceptor ).purgeAgedData();
                }
                catch ( NamingException e )
                {
                    throw new JobExecutionException( e );
                }
            }
        }
    }
}
