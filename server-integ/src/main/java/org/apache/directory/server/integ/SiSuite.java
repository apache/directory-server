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
package org.apache.directory.server.integ;


import java.util.List;

import static org.apache.directory.server.integ.state.TestServerContext.cleanup;
import static org.apache.directory.server.integ.state.TestServerContext.destroy;
import static org.apache.directory.server.integ.state.TestServerContext.shutdown;

import org.apache.directory.server.core.integ.Level;
import org.junit.internal.requests.IgnoredClassRunner;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;


/**
 * A replacement for standard JUnit 4 suites. Note that this test suite
 * will not startup an DirectoryService instance but will clean it up if
 * one remains.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SiSuite extends Suite
{
    private InheritableServerSettings settings;


    public SiSuite( Class<?> clazz ) throws InitializationError
    {
        super( clazz );
        settings = new InheritableServerSettings( getDescription() );
    }


    public void addAll( List<? extends Runner> runners )
    {
        for ( Runner runner : getRunners() )
        {
            if ( runner instanceof SiRunner )
            {
                SiRunner cir = ( SiRunner) runner;
                cir.setSuite( this );
            }
            else if ( runner instanceof IgnoredClassRunner )
            {
                // allow this one
            }
            else
            {
                throw new IllegalArgumentException( String.format( "Unexpected runner type \"%s\".  " +
                        "Test classes within CiSuites must use CiRunners.", runner ) );
            }
        }

        super.addAll( runners );
    }


    public void add( Runner runner )
    {
        if ( runner instanceof SiRunner )
        {
            SiRunner cir = ( SiRunner) runner;
            cir.setSuite( this );
            super.add( runner );
        }
        else if ( runner instanceof IgnoredClassRunner )
        {
            // allow this one
        }
        else
        {
            throw new IllegalArgumentException( String.format( "Unexpected runner type \"%s\".  " +
                    "Test classes within CiSuites must use CiRunners.", runner ) );
        }
    }


    @Override
    public void run( final RunNotifier notifier )
    {
        super.run( notifier );

        /*
         * For any service scope other than test system scope, we must have to
         * shutdown the sevice and cleanup the working directory.  Failures to
         * do this without exception shows that something is wrong with the
         * server and so the entire test should be marked as failed.  So we
         * presume that tests have failed in the suite if the fixture is in an
         * inconsistent state.  Who knows if this inconsistent state of the
         * service could have made it so false results were acquired while
         * running tests.
         */

        if ( settings.getCleanupLevel() != Level.SYSTEM )
        {
            try
            {
                shutdown();
                cleanup();
                destroy();
            }
            catch ( Exception e )
            {
                notifier.fireTestFailure( new Failure( getDescription(), e ) );
            }
        }
    }


    public InheritableServerSettings getSettings()
    {
        return settings;
    }
}
