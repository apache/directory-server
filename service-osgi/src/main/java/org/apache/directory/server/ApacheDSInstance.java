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
package org.apache.directory.server;


import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * IPojo Component that represents live ApacheDS instance
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory
 *         Project</a>
 */
@Component(name = "ApacheDSInstance")
@Instantiate(name = "ApacheDSInstance-Default")
public class ApacheDSInstance
{
    /** A logger for this class */
    private final Logger LOG = LoggerFactory
        .getLogger( ApacheDSInstance.class );

    /** Property for specifying instance directory. It is "default" by default */
    @Property(name = "apacheds.instance.dir", value = "default")
    private String instanceDir;

    /* ApacheDSService reference */
    private ApacheDsService service;


    /**
     * Will be called, when this component instance is validated,
     * Means all of its requirements are satisfied.
     *
     */
    @Validate
    public void validated()
    {
        /**
         * Calls the initialization and running on new thread
         * to seperate the execution from the IPojo management thread.
         */
        new Thread( new Runnable()
        {

            @Override
            public void run()
            {
                initAndStartADS();
            }
        } ).start();
    }


    /**
     * Will be called when this component instance is invalidated,
     * means one of its requirements is lost.
     *
     */
    @Invalidate
    public void invalidated()
    {
        new Thread( new Runnable()
        {

            @Override
            public void run()
            {
                stopADS();
            }
        } ).start();
    }


    /**
     * Inits and run()'s the ApacheDS blocking.
     *
     */
    private void initAndStartADS()
    {
        service = new ApacheDsService();

        // Creating instance layouts from the argument
        InstanceLayout instanceLayout = new InstanceLayout( instanceDir );

        // Initializing the service
        try
        {
            service.start( instanceLayout );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            LOG.error( "Failed to start the service.", e );
            System.exit( 1 );
        }
    }


    /*
     * Stops the ApacheDS instance.
     */
    private void stopADS()
    {
        //Stopping the service
        try
        {
            service.stop();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            LOG.error( "Failed to stop the service.", e );
            System.exit( 1 );
        }
    }

}
