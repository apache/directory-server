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
package org.apache.eve.encoder.impl ;


import java.nio.ByteBuffer;
import java.util.EventObject;

import org.apache.commons.codec.EncoderException;
import org.apache.eve.encoder.EncoderManager ;
import org.apache.eve.encoder.EncoderManagerMonitor;
import org.apache.eve.event.AbstractSubscriber;
import org.apache.eve.event.EventRouter;
import org.apache.eve.event.OutputEvent;
import org.apache.eve.event.ResponseEvent;
import org.apache.eve.event.ResponseSubscriber;
import org.apache.eve.seda.DefaultStage;
import org.apache.eve.seda.DefaultStageConfig;
import org.apache.eve.seda.StageHandler;
import org.apache.eve.seda.LoggingStageMonitor;
import org.apache.ldap.common.message.MessageEncoder;
import org.apache.ldap.common.message.Response;


/**
 * The default EncoderManager.  For now we're modeling this as a simple one
 * shot encode module but this will change as well build stateful encoders which
 * will fragment a response into multiple output events.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultEncoderManager extends DefaultStage 
    implements EncoderManager, ResponseSubscriber
{
    /** the event router used to publish and subscribe to events on */
    private final EventRouter router ;
    private EncoderManagerMonitor monitor ;
    
    
    /**
     * Creates the default EncoderManager.
     * 
     * @param router the event router used to publish and subscribe to events on
     */
    public DefaultEncoderManager( EventRouter router, 
                                  DefaultStageConfig config )
    {
        super( config ) ;
        super.setMonitor( new LoggingStageMonitor( this.getClass() ) ) ;
        config.setHandler( new EncoderStageHandler() ) ;
        this.router = router ;
        this.router.subscribe( ResponseEvent.class, this ) ;
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
        catch( Throwable t )
        {
            monitor.failedOnInform( this, event, t ) ;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.ResponseSubscriber#inform(
     * org.apache.eve.event.ResponseEvent)
     */
    public void inform( ResponseEvent event )
    {
        super.enqueue( event ) ;
    }


    /* (non-Javadoc)
     * @see org.apache.eve.encoder.EncoderManager#encode(
     * org.apache.ldap.common.message.Response)
     */
    public byte[] encode( Response response ) throws EncoderException
    {
        MessageEncoder encoder = new MessageEncoder() ;
        return encoder.encode( response ) ;
    }
    
    
    class EncoderStageHandler implements StageHandler
    {
        /* (non-Javadoc)
         * @see org.apache.eve.seda.StageHandler#handleEvent(
         * java.util.EventObject)
         */
        public void handleEvent( EventObject generic )
        {
            ByteBuffer buf = null ;
            ResponseEvent event = ( ResponseEvent ) generic ;
            
            try
            {
                buf = ByteBuffer.wrap( encode( event.getResponse() ) ) ;
            }
            catch ( EncoderException e )
            {
                monitor.failedOnEncode( DefaultEncoderManager.this, event, e ) ;
            }
            
            OutputEvent outEvent = new OutputEvent( DefaultEncoderManager.this, 
                    event.getClientKey(), buf ) ;
            router.publish( outEvent ) ;
        }
    }
}
