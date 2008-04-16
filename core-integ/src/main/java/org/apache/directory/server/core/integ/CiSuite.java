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
package org.apache.directory.server.core.integ;


import static org.apache.directory.server.core.integ.state.TestServiceContext.shutdown;
import static org.apache.directory.server.core.integ.state.TestServiceContext.cleanup;
import static org.apache.directory.server.core.integ.state.TestServiceContext.destroy;
import org.junit.internal.requests.IgnoredClassRunner;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;

import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;


/**
 * A replacement for standard JUnit 4 suites. Note that this test suite
 * will not startup an DirectoryService instance but will clean it up if
 * one remains.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CiSuite extends Suite
{
    private InheritableSettings settings;


    public CiSuite( Class<?> clazz ) throws InitializationError
    {
        super( clazz );
        settings = new InheritableSettings( getDescription() );
    }


    public void addAll( List<? extends Runner> runners )
    {
        for ( Runner runner : getRunners() )
        {
            if ( runner instanceof CiRunner )
            {
                CiRunner cir = ( CiRunner) runner;
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
        if ( runner instanceof CiRunner )
        {
            CiRunner cir = ( CiRunner) runner;
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
            catch ( NamingException e )
            {
                notifier.fireTestFailure( new Failure( getDescription(), e ) );
            }
            catch ( IOException e )
            {
                notifier.fireTestFailure( new Failure( getDescription(), e ) );
            }
        }
    }


    public InheritableSettings getSettings()
    {
        return settings;
    }
}
