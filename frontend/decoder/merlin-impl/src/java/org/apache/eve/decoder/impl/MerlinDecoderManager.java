/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.decoder.impl ;


import java.nio.ByteBuffer ;

import org.apache.commons.codec.DecoderException ;
import org.apache.commons.codec.stateful.DecoderMonitor ;
import org.apache.commons.codec.stateful.DecoderCallback ;

import org.apache.eve.thread.ThreadPool ;
import org.apache.eve.event.EventRouter ;
import org.apache.eve.listener.ClientKey ;
import org.apache.eve.decoder.DecoderManager ;
import org.apache.eve.seda.DefaultStageConfig ;

import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.logger.LogEnabled ;
import org.apache.avalon.framework.activity.Startable ;
import org.apache.avalon.framework.service.Serviceable ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.activity.Initializable ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.configuration.Configurable ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;

import org.apache.avalon.cornerstone.services.threads.ThreadManager ;


/**
 * A Merlin stateful decoder manager service. 
 * 
 * @avalon.component name="decoder-manager" lifestyle="singleton"
 * @avalon.service type="org.apache.eve.decoder.DecoderManager" version="1.0"
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class MerlinDecoderManager
    implements 
    DecoderManager, 
    LogEnabled, 
    Initializable, 
    Configurable,
    Serviceable,
    Startable
{
    /** the Avalon logger enabled monitor for the DecoderManager */
    private AvalonLoggingMonitor monitor = new AvalonLoggingMonitor() ;
    /** underlying wrapped DecoderManager implementation */
    private DefaultDecoderManager decoderManager = null ;
    /** the stage configuration bean for the underlying DecoderManager */
    private DefaultStageConfig stageConfig = null ;
    /** the event router we depend on to recieve and publish events */
    private EventRouter router = null ;
    /** the thread manager used to access the thread pool used by the stage */
    private ThreadManager tm = null ;
    
    
    // ------------------------------------------------------------------------
    // DecoderManager Interface Methods
    // ------------------------------------------------------------------------


    /* (non-Javadoc)
     * @see org.apache.eve.decoder.DecoderManager#decode(java.nio.ByteBuffer)
     */
    public Object decode( ByteBuffer buffer ) throws DecoderException
    {
        return decoderManager.decode( buffer ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.decoder.DecoderManager#decode(
     * org.apache.eve.listener.ClientKey, java.nio.ByteBuffer)
     */
    public void decode( ClientKey key, ByteBuffer buffer ) 
        throws DecoderException
    {
        decoderManager.decode( key, buffer ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.decoder.DecoderManager#disable(
     * org.apache.eve.listener.ClientKey)
     */
    public boolean disable( ClientKey key )
    {
        return decoderManager.disable( key ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.decoder.DecoderManager#setCallback(
     * org.apache.eve.listener.ClientKey, 
     * org.apache.commons.codec.stateful.DecoderCallback)
     */
    public void setCallback( ClientKey key, DecoderCallback cb )
    {
        decoderManager.setCallback( key, cb ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.decoder.DecoderManager#setDecoderMonitor(
     * org.apache.eve.listener.ClientKey, 
     * org.apache.commons.codec.stateful.DecoderMonitor)
     */
    public void setDecoderMonitor( ClientKey key, DecoderMonitor monitor )
    {
        decoderManager.setDecoderMonitor( key, monitor ) ;
    }
    
    
    
    // ------------------------------------------------------------------------
    // Avalon Life Cycle Methods ( in order of occurrence )
    // ------------------------------------------------------------------------

    
    /* (non-Javadoc)
     * @see org.apache.avalon.framework.logger.LogEnabled#
     * enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging( Logger logger )
    {
        monitor.enableLogging( logger ) ;
    }
    
   
    /**
     * @avalon.dependency type="org.apache.eve.event.EventRouter"
     *      key="event-router" version="1.0"
     * @avalon.dependency key="thread-manager"
     *      type="org.apache.avalon.cornerstone.services.threads.ThreadManager"
     *  
     * @see org.apache.avalon.framework.service.Serviceable#service(
     * org.apache.avalon.framework.service.ServiceManager)
     */
    public void service( ServiceManager manager ) throws ServiceException
    {
        tm = ( ThreadManager ) manager.lookup( "thread-manager" ) ;
        router = ( EventRouter ) manager.lookup( "event-router" ) ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.avalon.framework.configuration.Configurable#configure(
     * org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure( Configuration config ) 
        throws ConfigurationException
    {
        String name = config.getChild( "stage-name" ).getValue() ;
        String pool = config.getChild( "thread-pool" ).getValue() ;
        
        final org.apache.excalibur.thread.ThreadPool excaliburPool = 
            tm.getThreadPool( pool ) ;
        ThreadPool tp = new ThreadPool()
        {
            public void execute( Runnable runnable )
            {
                excaliburPool.execute( runnable ) ;
            }
        } ;
        
        stageConfig = new DefaultStageConfig( name, tp ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception
    {
        decoderManager = new DefaultDecoderManager( router, stageConfig ) ;
        DecodeStageHandler handler = new DecodeStageHandler( decoderManager ) ;
        stageConfig.setHandler( handler ) ;
        decoderManager.setMonitor( monitor ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.avalon.framework.activity.Startable#start()
     */
    public void start() throws Exception
    {
        decoderManager.start() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.avalon.framework.activity.Startable#stop()
     */
    public void stop() throws Exception
    {
        decoderManager.stop() ;
    }
}
