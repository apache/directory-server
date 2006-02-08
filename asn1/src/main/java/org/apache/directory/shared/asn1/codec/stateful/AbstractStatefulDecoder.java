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
package org.apache.directory.shared.asn1.codec.stateful ;


/**
 * Convenience class to not have to reimplement the two setter methods everytime
 * one starts a new decoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractStatefulDecoder implements StatefulDecoder
{
    /** this decoder's callback */
    private DecoderCallback cb = null ;
    /** this decoder's monitor */
    private DecoderMonitor monitor = null ;
    
    
    // ------------------------------------------------------------------------
    // constructors
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates a stateful decoder where the callback and monitor must be set.
     */
    public AbstractStatefulDecoder() 
    { 
    }
    
    
    /**
     * Creates a stateful decoder with a callback.
     * 
     * @param cb the callback to use for this decoder
     */
    public AbstractStatefulDecoder( DecoderCallback cb )
    {
        setCallback( cb ) ;
    }

    
    /**
     * Creates a stateful decoder with a monitor but no callback.
     * 
     * @param monitor the monitor to use for this decoder
     */
    public AbstractStatefulDecoder( DecoderMonitor monitor )
    {
        this.monitor = monitor ;
    }
    
    
    /**
     * Creates a stateful decoder.
     * 
     * @param cb the callback to use for this decoder
     * @param monitor the monitor to use for this decoder
     */
    public AbstractStatefulDecoder( DecoderCallback cb, DecoderMonitor monitor )
    {
        this.monitor = monitor ;
        setCallback( cb ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // StatefulDecoder methods
    // ------------------------------------------------------------------------
    
    
    /* (non-Javadoc)
     * @see org.apache.asn1.codec.stateful.StatefulDecoder#setCallback(
     * org.apache.asn1.codec.stateful.DecoderCallback)
     */
    public void setCallback( DecoderCallback cb )
    {
        DecoderCallback old = this.cb ;
        this.cb = cb ;

        if ( this.monitor != null )
        {
            this.monitor.callbackSet( this, old, cb );
        }
    }
    

    /* (non-Javadoc)
     * @see org.apache.asn1.codec.stateful.StatefulDecoder#setDecoderMonitor(
     * org.apache.asn1.codec.stateful.DecoderMonitor)
     */
    public void setDecoderMonitor( DecoderMonitor monitor )
    {
        this.monitor = monitor ;
    }
    
    
    // ------------------------------------------------------------------------
    // protected methods
    // ------------------------------------------------------------------------
    
    
    /**
     * Notifies via the callback if one has been set that this decoder has 
     * decoded a unit of encoded data.
     * 
     * @param decoded the decoded byproduct.
     */
    protected void decodeOccurred( Object decoded ) 
    {
        if ( cb != null )
        {    
            cb.decodeOccurred( this, decoded ) ;
        }
    }
    
    
    /**
     * Gets the decoder's monitor.
     * 
     * @return the monitor for this StatefulDecoder
     */
    protected DecoderMonitor getDecoderMonitor()
    {
        return monitor ;
    }
}
