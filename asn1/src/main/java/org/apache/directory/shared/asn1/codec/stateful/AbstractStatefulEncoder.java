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
package org.apache.directory.shared.asn1.codec.stateful;


/**
 * Convenience class to not have to reimplement the two setter methods everytime
 * one starts a new encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractStatefulEncoder implements StatefulEncoder
{
    /** this encoder's callback */
    private EncoderCallback cb = null;

    /** this encoder's monitor */
    private EncoderMonitor monitor = null;


    // ------------------------------------------------------------------------
    // constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a stateful encoder where the callback and monitor must be set.
     */
    public AbstractStatefulEncoder()
    {
    }


    /**
     * Creates a stateful encoder with a callback.
     * 
     * @param cb
     *            the callback to use for this encoder
     */
    public AbstractStatefulEncoder(EncoderCallback cb)
    {
        setCallback( cb );
    }


    /**
     * Creates a stateful encoder with a monitor but no callback.
     * 
     * @param monitor
     *            the monitor to use for this encoder
     */
    public AbstractStatefulEncoder(EncoderMonitor monitor)
    {
        this.monitor = monitor;
    }


    /**
     * Creates a stateful encoder.
     * 
     * @param cb
     *            the callback to use for this encoder
     * @param monitor
     *            the monitor to use for this encoder
     */
    public AbstractStatefulEncoder(EncoderCallback cb, EncoderMonitor monitor)
    {
        this.monitor = monitor;
        setCallback( cb );
    }


    // ------------------------------------------------------------------------
    // StatefulEncoder methods
    // ------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.asn1.codec.stateful.StatefulEncoder#setCallback(
     *      org.apache.asn1.codec.stateful.EncoderCallback)
     */
    public void setCallback( EncoderCallback cb )
    {
        EncoderCallback old = this.cb;
        this.cb = cb;

        if ( this.monitor != null )
        {
            this.monitor.callbackSet( this, old, cb );
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.apache.asn1.codec.stateful.StatefulEncoder#setEncoderMonitor(
     *      org.apache.asn1.codec.stateful.EncoderMonitor)
     */
    public void setEncoderMonitor( EncoderMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // protected methods
    // ------------------------------------------------------------------------

    /**
     * Notifies via the callback if one has been set that this encoder has
     * encoded a unit of encoded data.
     * 
     * @param encoded
     *            the encoded byproduct.
     */
    protected void encodeOccurred( Object encoded )
    {
        if ( cb != null )
        {
            cb.encodeOccurred( this, encoded );
        }
    }


    /**
     * Gets the encoder's monitor.
     * 
     * @return the monitor for this StatefulEncoder
     */
    protected EncoderMonitor getEncoderMonitor()
    {
        return monitor;
    }
}
