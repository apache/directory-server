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
import org.apache.commons.codec.stateful.DecoderCallback ;
import org.apache.commons.codec.stateful.StatefulDecoder ;


/**
 * A decoder that does not really do anything but return the data you give it
 * calling the callback if one exists with every call to decode.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class NoOpDecoder implements StatefulDecoder
{
    /** the callback for this decoder */
    private DecoderCallback cb = null ;
    
    
    /* (non-Javadoc)
     * @see org.apache.commons.codec.stateful.StatefulDecoder#decode(
     * java.lang.Object)
     */
    public void decode( Object encoded ) throws DecoderException
    {
        if ( cb == null )
        {
            return ;
        }
        
        cb.decodeOccurred( this, encoded ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.commons.codec.stateful.StatefulDecoder#setCallback(
     * org.apache.commons.codec.stateful.DecoderCallback)
     */
    public void setCallback( DecoderCallback cb )
    {
        this.cb = cb ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.commons.codec.stateful.StatefulDecoder#setDecoderMonitor(
     * org.apache.commons.codec.stateful.DecoderMonitor)
     */
    public void setDecoderMonitor( DecoderMonitor monitor )
    {
        // don't care this does nothing anyway
    }
}
