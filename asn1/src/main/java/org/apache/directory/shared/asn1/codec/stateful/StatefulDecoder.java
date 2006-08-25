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


import org.apache.directory.shared.asn1.codec.DecoderException;


/**
 * A decoder which decodes encoded data as it arrives in peices while
 * maintaining the state of the decode operation between the arrival of encoded
 * chunks. As chunks of encoded data arrive the decoder processes each chunk of
 * encoded data and maintains decoding state in between arrivals: it is hence
 * stateful and should be associated with a single channel or encoded data
 * producer. When an arbitrary unit of encoding, to be determined by the
 * encoding scheme, has been decoded, the <code>decode()</code> method of the
 * registered DecoderCallback is called.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface StatefulDecoder
{
    /**
     * Decodes a peice of encoded data. The nature of this call, synchronous
     * verses asynchonous, with respect to driving the actual decoding of the
     * encoded data argument is determined by an implementation. A return from
     * this method does not guarrantee any callbacks: zero or more callbacks may
     * occur during this call.
     * 
     * @param encoded
     *            an object representing a peice of encoded data
     */
    void decode( Object encoded ) throws DecoderException;


    /**
     * Sets the callback for this StatefulDecoder.
     * 
     * @param cb
     *            the callback to inform of a complete decode operation
     */
    void setCallback( DecoderCallback cb );


    /**
     * Monitors all kinds of events that occur during processing.
     * 
     * @param monitor
     *            to set for this StatefulDecoder
     */
    void setDecoderMonitor( DecoderMonitor monitor );
}
