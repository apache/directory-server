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
package org.apache.directory.server.core.integ.state;

import org.apache.directory.server.core.integ.SetupMode;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;


/**
 * The state of the service where it has been started and is dirty.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StartedDirtyState implements TestServiceState
{
    private static final Logger LOG = LoggerFactory.getLogger( StartedDirtyState.class );
    private final TestServiceContext context;


    public StartedDirtyState( TestServiceContext context )
    {
        this.context = context;
    }


    public void create()
    {
        throw new IllegalStateException( "The running service must be shutdown before creating a new one." );
    }


    public void destroy()
    {
        throw new IllegalStateException( "The running service must be shutdown before destroying it." );
    }


    public void cleanup()
    {
        throw new IllegalStateException( "The running service must be shutdown before cleaning it." );
    }


    public void startup()
    {
        throw new IllegalStateException( "The running service must be shutdown before starting it." );
    }


    public void shutdown()
    {
        try
        {
            context.getService().shutdown();
        }
        catch ( NamingException e )
        {
            context.getNotifier().fireTestFailure( new Failure( context.getDescription(), e ) );
        }
    }


    public void test()
    {
        if ( context.getMode().isStartedDirtyTestable() )
        {
            // run the test
        }
    }


    public void revert()
    {

    }
}