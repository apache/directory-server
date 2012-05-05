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
package org.apache.directory.server.benchmarks;


import com.sun.slamd.job.JobClass;
import com.sun.slamd.job.UnableToRunException;
import com.sun.slamd.parameter.BooleanParameter;
import com.sun.slamd.parameter.ParameterList;
import com.sun.slamd.stat.IncrementalTracker;
import com.sun.slamd.stat.StatTracker;


public class BogusBenchmark extends JobClass
{
    IncrementalTracker incremental = null;
    
    
    public String getJobDescription()
    {
        return "Does a bind using a single user name then unbinds.";
    }

    
    public String getJobName()
    {
        return "Simple Bind";
    }
    
    
    public String getJobCategoryName()
    {
        return "ApacheDS";
    }

    
    public ParameterList getParameterStubs()
    {
        ParameterList list = new ParameterList();
        list.addParameter( new BooleanParameter( "TestParam", "Test Parameter", "just for testing", true ) );
        return list;
    }


    public StatTracker[] getStatTrackerStubs( String clientId, String threadId, int interval )
    {
        return new StatTracker[] { new IncrementalTracker( clientId, threadId, "test tracker", interval ) };
    }

    
    public StatTracker[] getStatTrackers()
    {
        return new StatTracker[] { incremental };
    }

    
    public void initializeThread( String clientId, String threadId, int interval, ParameterList params ) 
        throws UnableToRunException
    {
        super.logMessage( "initializeThread() called" );
        incremental = new IncrementalTracker( clientId, threadId, "test tracker", interval );
    }


    public void runJob()
    {
        incremental.startTracker();
        while ( !shouldStop() )
        {
            try
            {
                Thread.sleep( 200 );
                incremental.increment();
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        }
        incremental.stopTracker();
    }
}
