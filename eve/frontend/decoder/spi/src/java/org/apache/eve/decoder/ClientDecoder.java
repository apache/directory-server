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


import org.apache.commons.codec.DecoderException ;
import org.apache.commons.codec.stateful.DecoderMonitor ;
import org.apache.commons.codec.stateful.StatefulDecoder ;
import org.apache.commons.codec.stateful.DecoderCallback ;

import org.apache.eve.listener.ClientKey ;


/**
 * A stateful decoder dedicated to a specific client.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class ClientDecoder implements StatefulDecoder
{
    /** the key of the client this decoder is associated with */ 
    private final ClientKey key ;
    /** the actual decoder doing the work for us */
    private final StatefulDecoder decoder ;

    
    /**
     * Creates a client dedicated stateful decoder.
     * 
     * @param key the key of the client this decoder is for
     * @param decoder the underlying decoder doing the work
     */
    public ClientDecoder( ClientKey key, StatefulDecoder decoder )
    {
        this.key = key ;
        this.decoder = decoder ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.commons.codec.stateful.StatefulDecoder#decode(
     * java.lang.Object)
     */
    public void decode( Object encoded ) throws DecoderException
    {
        decoder.decode( encoded ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.commons.codec.stateful.StatefulDecoder#setCallback(
     * org.apache.commons.codec.stateful.DecoderCallback)
     */
    public void setCallback( DecoderCallback cb )
    {
        decoder.setCallback( cb ) ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.commons.codec.stateful.StatefulDecoder#setDecoderMonitor(
     * org.apache.commons.codec.stateful.DecoderMonitor)
     */
    public void setDecoderMonitor( DecoderMonitor monitor )
    {
        decoder.setDecoderMonitor( monitor ) ;
    }


    /**
     * Gets the key of the client this stateful decoder is dedicated to.
     * 
     * @return the key of the client for this stateful decoder 
     */
    public ClientKey getClientKey()
    {
        return key ;
    }
}
