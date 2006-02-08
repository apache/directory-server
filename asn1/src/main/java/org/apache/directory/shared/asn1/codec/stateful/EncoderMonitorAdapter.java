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
 * Document me.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev$
 */
public class EncoderMonitorAdapter implements EncoderMonitor
{
    /**
     * Receive notification of a recoverable error. This callback is used to
     * denote a failure to handle a unit of data to be encoded or decoded. The
     * entire [en|de]codable unit is lost but the [en|de]coding operation can
     * still proceed.
     * 
     * @param encoder
     *            the encoder that had the error
     * @param exception
     *            the error information encapsulated in an exception
     */
    public void error( StatefulEncoder encoder, Exception exception )
    {
    }


    /**
     * Receive notification of a non-recoverable error. The application must
     * assume that the stream data is unusable after the encoder has invoked
     * this method, and should continue (if at all) only for the sake of
     * collecting addition error messages: in fact, encoders are free to stop
     * reporting any other events once this method has been invoked.
     * 
     * @param encoder
     *            the encoder that had the failure
     * @param exception
     *            the warning information encapsulated in an exception
     */
    public void fatalError( StatefulEncoder encoder, Exception exception )
    {
    }


    /**
     * Receive notification of a warning. The encoder must continue to provide
     * normal callbacks after invoking this method: it should still be possible
     * for the application to process the encoded data through to the end.
     * 
     * @param encoder
     *            the encoder that had the error
     * @param exception
     *            the warning information encapsulated in an exception
     */
    public void warning( StatefulEncoder encoder, Exception exception )
    {
    }


    /**
     * Monitors callbacks that deliver a fully decoded object.
     * 
     * @param encoder
     *            the stateful encoder driving the callback
     * @param decoded
     *            the object that was decoded
     */
    public void callbackOccured( StatefulEncoder encoder, EncoderCallback cb, Object decoded )
    {
    }


    /**
     * Monitors changes to the callback.
     * 
     * @param encoder
     *            the encoder whose callback was set
     * @param oldcb
     *            the unset old callback, or null if none was set
     * @param newcb
     *            the newly set callback, or null if callback is cleared
     */
    public void callbackSet( StatefulEncoder encoder, EncoderCallback oldcb, EncoderCallback newcb )
    {
    }
}
