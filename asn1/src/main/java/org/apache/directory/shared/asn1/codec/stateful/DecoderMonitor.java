/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.asn1.codec.stateful;


/**
 * Monitors decoder activity. This class borrowed some from the <code>
 * org.xml.sax.ErrorHandler</code>
 * interface and its documentation. A monitor is a generalized callback for any
 * sort of activity used for tracking both successes and failures. So you'll
 * realize similarities between monitors and callbacks especially where the
 * callbackOccurred() method is concerned.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public interface DecoderMonitor
{
    /**
     * Receive notification of a recoverable error. This callback is used to
     * denote a failure to handle a unit of data to be encoded or decoded. The
     * entire [en|de]codable unit is lost but the [en|de]coding operation can
     * still proceed.
     * 
     * @param decoder
     *            the decoder that had the error
     * @param exception
     *            the error information encapsulated in an exception
     */
    void error( StatefulDecoder decoder, Exception exception );


    /**
     * Receive notification of a non-recoverable error. The application must
     * assume that the stream data is unusable after the decoder has invoked
     * this method, and should continue (if at all) only for the sake of
     * collecting addition error messages: in fact, decoders are free to stop
     * reporting any other events once this method has been invoked.
     * 
     * @param decoder
     *            the decoder that had the failure
     * @param exception
     *            the warning information encapsulated in an exception
     */
    void fatalError( StatefulDecoder decoder, Exception exception );


    /**
     * Receive notification of a warning. The decoder must continue to provide
     * normal callbacks after invoking this method: it should still be possible
     * for the application to process the encoded data through to the end.
     * 
     * @param decoder
     *            the decoder that had the error
     * @param exception
     *            the warning information encapsulated in an exception
     */
    void warning( StatefulDecoder decoder, Exception exception );


    /**
     * Monitors callbacks that deliver a fully decoded object.
     * 
     * @param decoder
     *            the stateful decoder driving the callback
     * @param decoded
     *            the object that was decoded
     */
    void callbackOccured( StatefulDecoder decoder, DecoderCallback cb, Object decoded );


    /**
     * Monitors changes to the callback.
     * 
     * @param decoder
     *            the decoder whose callback was set
     * @param oldcb
     *            the unset old callback, or null if none was set
     * @param newcb
     *            the newly set callback, or null if callback is cleared
     */
    void callbackSet( StatefulDecoder decoder, DecoderCallback oldcb, DecoderCallback newcb );
}
