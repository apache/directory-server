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
import java.util.Iterator;

import org.apache.commons.lang.exception.ExceptionUtils ;

import org.apache.eve.event.EventRouter ;
import org.apache.eve.event.RequestEvent ;
import org.apache.eve.event.ResponseEvent ;
import org.apache.eve.event.RequestSubscriber ;
import org.apache.eve.event.AbstractSubscriber ;

import org.apache.eve.listener.ClientKey ;

import org.apache.eve.seda.DefaultStageConfig;
import org.apache.eve.seda.StageConfig ;
import org.apache.eve.seda.DefaultStage ;
import org.apache.eve.seda.StageHandler ;

import org.apache.eve.processor.NoReplyHandler ;
import org.apache.eve.processor.RequestHandler ;
import org.apache.eve.processor.HandlerRegistry ;
import org.apache.eve.processor.HandlerTypeEnum ;
import org.apache.eve.processor.ManyReplyHandler ;
import org.apache.eve.processor.RequestProcessor ;
import org.apache.eve.processor.SingleReplyHandler ;
import org.apache.eve.processor.RequestProcessorMonitor ;
import org.apache.eve.processor.RequestProcessorMonitorAdapter ;

import org.apache.ldap.common.message.Request ;
import org.apache.ldap.common.message.Response ;
import org.apache.ldap.common.message.LdapResult ;
import org.apache.ldap.common.message.ResultCodeEnum ;
import org.apache.ldap.common.message.ResultResponse ;
import org.apache.ldap.common.message.LdapResultImpl ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.message.AddResponseImpl ;
import org.apache.ldap.common.message.ManyReplyRequest ;
import org.apache.ldap.common.message.BindResponseImpl ;
import org.apache.ldap.common.message.ModifyResponseImpl ;
import org.apache.ldap.common.message.SingleReplyRequest ;
import org.apache.ldap.common.message.DeleteResponseImpl ;
import org.apache.ldap.common.message.CompareResponseImpl ;
import org.apache.ldap.common.message.ExtendedResponseImpl ;
import org.apache.ldap.common.message.ModifyDnResponseImpl ;


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
        
        DefaultStageConfig defaultConfig = ( DefaultStageConfig ) config ;
        defaultConfig.setHandler( new ProcessorStageHandler() ) ;
        
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

    
    class ProcessorStageHandler implements StageHandler
    {
        /**
         * Event handler method for processing RequestEvents.
         *
         * @param event the RequestEvent to process.
         */
        public void handleEvent( EventObject unspecific )
        {
            RequestEvent event = ( RequestEvent ) unspecific ;
            Request request = event.getRequest() ;
            ClientKey key = event.getClientKey() ;
            RequestHandler handler = hooks.lookup( request.getType() ) ;

            if( handler == null )
            {
                throw new IllegalArgumentException( 
                        "Unknown request message type: "
                        + request.getType().getName() ) ;
            }

            switch( handler.getHandlerType().getValue() )
            {
            case( HandlerTypeEnum.NOREPLY_VAL ):
                NoReplyHandler noreply = ( NoReplyHandler ) handler ;
                noreply.handle( request ) ;
                break ;
            case( HandlerTypeEnum.SINGLEREPLY_VAL ):
                SingleReplyHandler single = ( SingleReplyHandler ) handler ;
                reply( single, ( SingleReplyRequest ) request, key ) ;
                break ;
            case( HandlerTypeEnum.MANYREPLY_VAL ):
                ManyReplyHandler many = ( ManyReplyHandler ) handler ;
                reply( many, ( ManyReplyRequest ) request, key ) ;
                break ;
            default:
                throw new IllegalArgumentException( "Unrecognized type: "
                    + handler.getRequestType().getName() ) ;
            }
        }
    }
    

    /**
     * Handles the generation and return of multiple responses.
     * 
     * @param handler the handler that generates the responses
     * @param request the request responded to
     */
    private void reply( ManyReplyHandler handler, 
                        ManyReplyRequest request,
                        ClientKey key )
    {
        Response response = null ;
        LdapResult result = null ;

        try
         {
             Iterator list = handler.handle( request ) ;
             while ( list.hasNext() ) 
             {
                 response = ( Response ) list.next() ;
             }
         }

         // If the individual handlers do not do a global catch and report this
         // will sheild the server from complete failure on a request reporting
         // at a minimum the stack trace that cause the request to fail.
         catch( Throwable t )
         {
             monitor.failedOnSingleReply( key, request, t ) ;
            
             ResultResponse resultResponse = handler.getDoneResponse( 
                     request.getMessageId() ) ;
             result = new LdapResultImpl( response ) ;
             result.setMatchedDn( "" ) ;
             result.setErrorMessage( ExceptionUtils.getFullStackTrace( t ) ) ;
             result.setResultCode( ResultCodeEnum.OPERATIONSERROR ) ;
             resultResponse.setLdapResult( result ) ;
             router.publish( new ResponseEvent( this, key, resultResponse ) ) ;
         }
    }
    
    
    /**
     * Handles the generation and return of a single response.
     * 
     * @param handler the handler that generates the single response
     * @param request the request responded to
     */
    private void reply( SingleReplyHandler handler, SingleReplyRequest request,
                        ClientKey key )
    {
        int id = request.getMessageId() ;
        LdapResult result = null ;
        ResultResponse response = null ;

        try
        {
            response = handler.handle( request ) ;
        }

        // If the individual handlers do not do a global catch and report this
        // will sheild the server from complete failure on a request reporting
        // at a minimum the stack trace that cause the request to fail.
        catch( Throwable t )
        {
            switch( request.getResponseType().getValue() )
            {
            case( MessageTypeEnum.ADDRESPONSE_VAL ):
                response = new AddResponseImpl( id ) ;
                break ;
            case( MessageTypeEnum.BINDRESPONSE_VAL ):
                response = new BindResponseImpl( id ) ;
                break ;
            case( MessageTypeEnum.COMPARERESPONSE_VAL ):
                response = new CompareResponseImpl( id ) ;
                break ;
            case( MessageTypeEnum.DELRESPONSE_VAL ):
                response = new DeleteResponseImpl( id ) ;
                break ;
            case( MessageTypeEnum.EXTENDEDRESP_VAL ):
                response = new ExtendedResponseImpl( id ) ;
                break ;
            case( MessageTypeEnum.MODDNRESPONSE_VAL ):
                response = new ModifyDnResponseImpl( id ) ;
                break ;
            case( MessageTypeEnum.MODIFYRESPONSE_VAL ):
                response = new ModifyResponseImpl( id ) ;
                break ;
            }

            monitor.failedOnSingleReply( key, request, t ) ;
            
            result = new LdapResultImpl( response ) ;
            result.setMatchedDn( "" ) ;
            result.setErrorMessage( ExceptionUtils.getFullStackTrace( t ) ) ;
            result.setResultCode( ResultCodeEnum.OPERATIONSERROR ) ;
            response.setLdapResult( result ) ;
        }

        router.publish( new ResponseEvent( this, key, response ) ) ;
    }
}
