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

import org.apache.eve.listener.ClientKey ;

import org.apache.commons.codec.stateful.ErrorHandler ;
import org.apache.commons.codec.stateful.DecoderCallback ;


/**
 * The DecoderManager creates, maintains and destroys StatefulDecoder instances,
 * one dedicated per client.  The StatefulDecoder implementation decodes BER 
 * encoded LDAPv3 messages into Message instances.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface DecoderManager
{
    /** Avalon likes to have the ROLE associated with the service interface */
    String ROLE = DecoderManager.class.getName() ;

    /**
     * Sets a client decoder's callback.
     * 
     * @param key the unique key associated with the client
     * @param cb the decoder callback used to deliver decode events to
     */
    void setCallback( ClientKey key, DecoderCallback cb ) ;

    /**
     * Sets a client decoder's error handler.
     * 
     * @param key the unique key associated with the client
     * @param cb the callback used to deliver error events
     */
    void setErrorHandler( ClientKey key, ErrorHandler handler ) ;

    /**
     * Disables callback events for a client destroying decoding state if any.
     * 
     * @param key the unique key associated with the client
     */
    boolean disable( ClientKey key ) ;

    /**
     * Decodes a buffer of encoded data.
     * 
     * @param key the unique key associated with the client
     * @param buffer the buffer of encoded data
     * @return the set of keys for decoding sessions
     */
    void decode( ClientKey key, ByteBuffer buffer ) ;
    
    /**
     * All in one shot synchronous decode operation requiring entire set of data
     * 
     * @param buffer the buffer containing all the encoded data
     * @return the decoded object
     */
    Object decode( ByteBuffer buffer ) ;
}
