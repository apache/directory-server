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
package org.apache.eve.merlin ;


import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.merlin.unit.AbstractMerlinTestCase ;
import org.apache.eve.buffer.BufferPool;
import org.apache.eve.decoder.DecoderManager;
import org.apache.eve.encoder.EncoderManager;
import org.apache.eve.event.EventRouter;
import org.apache.eve.input.InputManager;
import org.apache.eve.listener.ListenerManager;
import org.apache.eve.output.OutputManager;
import org.apache.eve.processor.RequestProcessor;


/**
 * Tests the Merlin based frontend.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class MerlinFrontendTest extends AbstractMerlinTestCase
{
    private BufferPool bufferPool = null ;
    private EventRouter eventRouter = null ;
    private InputManager inputManager = null ;
    private OutputManager outputManager = null ;
    private ThreadManager threadManager = null ;
    private EncoderManager encoderManager = null ;
    private DecoderManager decoderManager = null ;
    private ListenerManager listenerManager = null ;
    private RequestProcessor requestProcessor = null ;
    
    /**
     * Creates a frontendt test case.
     * 
     * @param name
     */
    public MerlinFrontendTest( String name )
    {
        super( name ) ;
    }
    

    /*
     * @see AbstractMerlinTestCase#setUp()
     */
    public void setUp() throws Exception
    {
        super.setUp() ;

        bufferPool = ( BufferPool ) 
            resolve( "/server/buffer-pool" ) ;
        
        eventRouter = ( EventRouter )
            resolve( "/server/event-router" ) ; 
        
        inputManager = ( InputManager ) 
            resolve( "/server/input-manager" ) ;

        outputManager = ( OutputManager ) 
            resolve( "/server/output-manager" ) ;

        threadManager = ( ThreadManager ) 
            resolve( "/server/thread-manager" ) ;

        encoderManager = ( EncoderManager ) 
            resolve( "/server/encoder-manager" ) ;

        decoderManager = ( DecoderManager ) 
            resolve( "/server/decoder-manager" ) ;

        listenerManager = ( ListenerManager ) 
            resolve( "/server/listener-manager" ) ;

        requestProcessor = ( RequestProcessor ) 
            resolve( "/server/request-processor" ) ;
    }

    
    /*
     * @see AbstractMerlinTestCase#tearDown()
     */
    public void tearDown()
    {
        super.tearDown() ;
    }
    
    
    public void testPlacebo()
    {
        assertNotNull( this.bufferPool ) ;
        assertNotNull( this.decoderManager ) ;
        assertNotNull( this.encoderManager ) ;
        assertNotNull( this.eventRouter ) ;
        assertNotNull( this.inputManager ) ;
        assertNotNull( this.listenerManager ) ;
        assertNotNull( this.outputManager ) ;
        assertNotNull( this.requestProcessor ) ;
        assertNotNull( this.threadManager ) ;
    }
}
