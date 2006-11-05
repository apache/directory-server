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


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


public class ReplicationLogCleanJob implements Job
{
    public static final String INSTANCE_ID = "instanceId";


    public ReplicationLogCleanJob()
    {
    }


    public void execute( JobExecutionContext ctx ) throws JobExecutionException
    {
        String instanceId = ctx.getJobDetail().getJobDataMap().getString( INSTANCE_ID );
        if ( instanceId == null )
        {
            // Execute for all instances in the VM if instanceId is not specified.
            Iterator it = DirectoryService.getAllInstances().iterator();
            while ( it.hasNext() )
            {
                DirectoryService service = ( DirectoryService ) it.next();
                execute0( service.getConfiguration().getInstanceId() );
            }
        }
        else
        {
            // Execute for the instance with the specified instanceId if
            // it is specified.
            execute0( instanceId );
        }
    }


    private void execute0( String instanceId ) throws JobExecutionException
    {
        DirectoryService service = DirectoryService.getInstance( instanceId );
        Iterator it = service.getConfiguration().getInterceptorChain().getAll().iterator();
        while ( it.hasNext() )
        {
            Interceptor interceptor = ( Interceptor ) it.next();
            if ( interceptor instanceof ReplicationService )
            {
                try
                {
                    ( ( ReplicationService ) interceptor ).purgeAgedData();
                }
                catch ( NamingException e )
                {
                    throw new JobExecutionException( e );
                }
            }
        }
    }
}
