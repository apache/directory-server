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
package org.apache.eve.processor.impl ;


import java.util.EventObject ;

import org.apache.eve.event.AbstractSubscriber ;
import org.apache.eve.event.EventRouter ;
import org.apache.eve.event.RequestEvent ;
import org.apache.eve.event.RequestSubscriber ;
import org.apache.eve.listener.ClientKey;
import org.apache.eve.processor.HandlerRegistry;
import org.apache.eve.processor.HandlerTypeEnum;
import org.apache.eve.processor.NoReplyHandler;
import org.apache.eve.processor.RequestHandler;
import org.apache.eve.processor.RequestProcessor ;
import org.apache.eve.processor.RequestProcessorMonitor;
import org.apache.eve.processor.RequestProcessorMonitorAdapter;
import org.apache.eve.processor.SingleReplyHandler;
import org.apache.eve.seda.DefaultStage ;
import org.apache.eve.seda.StageConfig ;
import org.apache.eve.seda.StageHandler;
import org.apache.ldap.common.message.Request;
import org.apache.ldap.common.message.SingleReplyRequest;


/**
 * Default RequestProcessor service implemented as a POJO.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultRequestProcessor extends DefaultStage
    implements RequestProcessor, RequestSubscriber
{
    private final HandlerRegistry hooks ;
    private final EventRouter router ;
    private RequestProcessorMonitor monitor = null ;
    
    
    /**
     * Creates a default RequestProcessor.
     * 
     * @param router the event router we subscribe and publish to
     * @param config the configuration for this stage 
     * @param hooks the handler registry to use for setting the request hooks
     */
    public DefaultRequestProcessor( EventRouter router, StageConfig config, 
                                    HandlerRegistry hooks )
    {
        super( config ) ;
        
        this.hooks = hooks ;
        this.router = router ;
        this.router.subscribe( RequestEvent.class, this ) ;
        this.monitor = new RequestProcessorMonitorAdapter() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.RequestSubscriber#inform(
     * org.apache.eve.event.RequestEvent)
     */
    public void inform( RequestEvent event )
    {
        // @todo do something with the monitor here
        enqueue( event ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     */
    public void inform( EventObject event )
    {
        try
        {
            AbstractSubscriber.inform( this, event ) ;
        }
        catch ( Throwable t )
        {
            monitor.failedOnInform( this, event, t ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.processor.RequestProcessor#dummy()
     */
    public void dummy()
    {
    }
    
    
    class ProcessorStageHandler implements StageHandler
    {
        /**
         * Event handler method for processing RequestEvents.
         *
         * @param event the RequestEvent to process.
         */
        public void handleEvent( EventObject event )
        {
            Request l_request = null ;
            ClientKey l_clientKey = null ;

            // Throw protocol exception if the event is not a request event.
            if( ! ( event instanceof RequestEvent ) )
            {
                throw new ProtocolException( "Unrecognized event: " + event ) ;
            }

            // Extract the ClientKey and Request parameters from the event
            l_request = ( ( RequestEvent ) event ).getRequest() ;
            l_clientKey = ( ClientKey )
                ( ( RequestEvent ) event ).getSource() ;

            // Get the handler if we have one defined.
            RequestHandler l_handler = ( RequestHandler )
                m_handlers.get( l_request.getType() ) ;
            if( l_handler == null )
            {
                throw new ProtocolException( "Unknown request message type: "
                    + l_request.getType().getName() ) ;
            }

            // Based on the handler type start request handling.
            switch( l_handler.getHandlerType().getValue() )
            {
            case( HandlerTypeEnum.NOREPLY_VAL ):
                NoReplyHandler l_noreply = ( NoReplyHandler ) l_handler ;
                l_noreply.handle( l_request ) ;
                break ;
            case( HandlerTypeEnum.SINGLEREPLY_VAL ):
                SingleReplyHandler l_single = ( SingleReplyHandler ) l_handler ;
                doSingleReply( l_single, ( SingleReplyRequest ) l_request ) ;
                break ;
            case( HandlerTypeEnum.SEARCH_VAL ):
                SearchHandler l_search = ( SearchHandler ) l_handler ;
                l_search.handle( ( SearchRequest ) l_request ) ;
                break ;
            default:
                throw new ProtocolException( "Unrecognized handler type: "
                    + l_handler.getRequestType().getName() ) ;
            }

        }
    }
}
