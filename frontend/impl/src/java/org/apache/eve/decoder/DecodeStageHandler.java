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
package org.apache.eve.decoder ;


import java.nio.ByteBuffer ;
import java.util.EventObject ;

import org.apache.commons.codec.DecoderException ;
import org.apache.commons.codec.stateful.StatefulDecoder ;

import org.apache.eve.event.InputEvent ;
import org.apache.eve.seda.StageHandler ;
import org.apache.eve.listener.ClientKey ;


/**
 * A decoder manager's decode StageHandler for use only with enqueued 
 * InputEvents.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DecodeStageHandler implements StageHandler
{
    /** reference to the decoder manager this handler is used by */
    private final DefaultDecoderManager manager ;

    
    /**
     * Creates the decoder manager's decode stage handler.
     * 
     * @param manager the decoder manager this handler is for
     */
    public DecodeStageHandler( DefaultDecoderManager manager )
    {
        this.manager = manager ;
    }
    

    /**
     * Uses the client key to have the decoder manager lookup the client's 
     * stateful decoder.  The decoder's decode method is called and control is
     * returned.  Error handling is left upto the decoder's monitor.
     * 
     * @see org.apache.eve.seda.StageHandler#handleEvent(java.util.EventObject)
     */
    public void handleEvent( EventObject event )
    {
        InputEvent e = ( InputEvent ) event ;
        ClientKey key = e.getClientKey() ;
        ByteBuffer buf = e.claimInterest( this ) ;
        StatefulDecoder decoder = ( StatefulDecoder ) manager.getDecoder( key );
        
        try
        {
            decoder.decode( buf ) ;
        }
        catch( DecoderException ex )
        {
            /*
             * monitor should be handling errors already for us and rethrowing
             * so we shouldn't have to do anything here but return control
             */
        }
        
        e.releaseInterest( this ) ;
        e.releaseInterest( manager ) ;
    }
}
