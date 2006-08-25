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


import java.util.Stack;

import org.apache.directory.shared.asn1.codec.DecoderException;


/**
 * A stack of decoders used for the additive application of multiple decoders
 * forming a linear staged decoder pipeline.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public class DecoderStack extends AbstractStatefulDecoder
{
    /**
     * the top decoder callback which calls this decoders callback
     * 
     * @todo determine if this is even necessary - can't we just use cb
     */
    private final DecoderCallback topcb;

    /** a stack of StatefulDecoders */
    private Stack decoders = new Stack();


    /**
     * Creates an empty stack of chained decoders.
     */
    public DecoderStack()
    {
        topcb = new DecoderCallback()
        {
            public void decodeOccurred( StatefulDecoder decoder, Object decoded )
            {
                DecoderStack.this.decodeOccurred( decoded );
            }
        };
    }


    /**
     * Pushs a new terminal decoder onto the top of this DecoderStack. The old
     * top decoder is chained to feed its decoded object to the new top decoder.
     * The new pushed decoder will report decode events to this DecoderStacks
     * callback.
     * 
     * @param decoder
     *            the terminal decoder to push onto this stack
     */
    public synchronized void push( StatefulDecoder decoder )
    {
        decoder.setCallback( topcb );

        if ( !decoders.isEmpty() )
        {
            StatefulDecoder top = ( StatefulDecoder ) decoders.peek();
            ChainingCallback chaining = new ChainingCallback( top, decoder );
            top.setCallback( chaining );
        }

        decoders.push( decoder );
    }


    /**
     * Pops the terminal decoder off of this DecoderStack. The popped decoder
     * has its callback cleared. If the stack is empty nothing happens and this
     * StatefulDecoder, the DecoderStack, is returned to protect against null.
     * 
     * @return the top decoder that was popped, or this DecoderStack
     */
    public synchronized StatefulDecoder pop()
    {
        if ( decoders.isEmpty() )
        {
            return this;
        }

        StatefulDecoder popped = ( StatefulDecoder ) decoders.pop();
        popped.setCallback( null );

        if ( !decoders.isEmpty() )
        {
            StatefulDecoder top = ( StatefulDecoder ) decoders.peek();
            top.setCallback( this.topcb );
        }

        return popped;
    }


    /**
     * Decodes an encoded object by calling decode on the decoder at the bottom
     * of the stack. Callbacks are chained to feed the output of one decoder
     * into the input decode method of another. If the stack is empty then the
     * arguement is delivered without change to this StatefulDecoder's callback.
     * 
     * @see org.apache.directory.shared.asn1.codec.stateful.StatefulDecoder#
     *      decode(java.lang.Object)
     */
    public synchronized void decode( Object encoded ) throws DecoderException
    {
        if ( decoders.isEmpty() )
        {
            decodeOccurred( encoded );
            return;
        }

        ( ( StatefulDecoder ) decoders.get( 0 ) ).decode( encoded );
    }


    /**
     * Gets whether or not this stack is empty.
     * 
     * @return true if the stack is empty, false otherwise
     */
    public boolean isEmpty()
    {
        return decoders.isEmpty();
    }


    /**
     * Clears the stack popping all decoders setting their callbacks to null.
     */
    public synchronized void clear()
    {
        while ( !decoders.isEmpty() )
        {
            pop();
        }
    }

    /**
     * A callback used to chain decoders.
     * 
     * @author <a href="mailto:dev@directory.apache.org"> Apache Directory
     *         Project</a>
     * @version $Rev$
     */
    class ChainingCallback implements DecoderCallback
    {
        /** the source decoder calling this callback */
        private StatefulDecoder sink;

        /** the sink decoder recieving the src's decoded object */
        private StatefulDecoder src;


        /**
         * Creates a callback that chains the output of a src decoder to the
         * input of a sink decoder. No side-effects occur like setting the
         * callback of the src so this ChainingCallback must be set explicity as
         * the src decoders callback.
         * 
         * @param src
         *            the source decoder calling this callback
         * @param sink
         *            the sink decoder recieving the src's decoded object
         */
        ChainingCallback(StatefulDecoder src, StatefulDecoder sink)
        {
            this.src = src;
            this.sink = sink;
        }


        /**
         * Calls the {@link #decode(Object)} method of the sink if the decoder
         * argument is the source. Any failures that occur during the sink's
         * decode operation are reported to the monitor first then rethrown as
         * runtime exceptions with the root cause set to the faulting exception.
         * 
         * @see org.apache.directory.shared.asn1.codec.stateful.DecoderCallback#decodeOccurred
         *      (org.apache.directory.shared.asn1.codec.stateful.StatefulDecoder,
         *      java.lang.Object)
         */
        public void decodeOccurred( StatefulDecoder decoder, Object decoded )
        {
            if ( decoder != src )
            {
                return;
            }

            try
            {
                sink.decode( decoded );
            }
            catch ( DecoderException e )
            {
                if ( getDecoderMonitor() != null )
                {
                    getDecoderMonitor().fatalError( DecoderStack.this, e );
                }

                throw new RuntimeException( e );
            }
        }
    }
}
