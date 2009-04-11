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
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;

/**
 * Tests the DecoderStack.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public class DecoderStackTest
{
    /**
     * Tests the push method.
     */
    @Test
    public void testPush()
    {
        DecoderStack stack = new DecoderStack();
        assertNotNull( stack );
        assertTrue( "expecting empty stack after creation", stack.isEmpty() );
        PassThroDecoder decoder = new PassThroDecoder();
        stack.push( decoder );
        assertFalse( "expecting non-empty stack after push", stack.isEmpty() );
    }


    /**
     * Tests the pop method.
     */
    @Test
    public void testPop()
    {
        DecoderStack stack = new DecoderStack();
        assertNotNull( stack );
        assertTrue( "expecting empty stack after creation", stack.isEmpty() );
        PassThroDecoder decoder = new PassThroDecoder();
        stack.push( decoder );
        assertFalse( "expecting non-empty stack after push", stack.isEmpty() );
        StatefulDecoder popped = stack.pop();
        assertTrue( "expecting empty stack after last pop", stack.isEmpty() );
        assertNotNull( popped );
        assertSame( "expecting last popped == last pushed", popped, decoder );
        StatefulDecoder empty = stack.pop();
        assertNotNull( "expecting empty pop to be non-null", empty );
        assertNotSame( "expecting empty pop != last popped", popped, empty );
        assertSame( "expecting empty pop == stack decoder", stack, empty );
        assertTrue( "expecting empty stack after empty pop", stack.isEmpty() );
    }


    @Test
    public void testDecode() throws Exception
    {
        DecoderStack stack = new DecoderStack();
        CallbackHistory history = new CallbackHistory();
        stack.setCallback( history );
        assertNotNull( stack );
        assertTrue( "expecting empty stack after creation", stack.isEmpty() );
        PassThroDecoder decoder = new PassThroDecoder();
        stack.push( decoder );
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 0 ), history.getMostRecent() );

        assertFalse( "expecting non-empty stack after push", stack.isEmpty() );

        stack.push( new IncrementingDecoder() );
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 1 ), history.getMostRecent() );

        stack.push( new IncrementingDecoder() );
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 2 ), history.getMostRecent() );

        stack.push( new IncrementingDecoder() );
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 3 ), history.getMostRecent() );

        stack.push( new IncrementingDecoder() );
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 4 ), history.getMostRecent() );

        stack.push( new IncrementingDecoder() );
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 5 ), history.getMostRecent() );

        stack.push( new IncrementingDecoder() );
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 6 ), history.getMostRecent() );

        stack.push( new IncrementingDecoder() );
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 7 ), history.getMostRecent() );

        // start popping and decrementing now

        stack.pop();
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 6 ), history.getMostRecent() );

        stack.pop();
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 5 ), history.getMostRecent() );

        stack.pop();
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 4 ), history.getMostRecent() );

        stack.pop();
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 3 ), history.getMostRecent() );

        stack.pop();
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 2 ), history.getMostRecent() );

        stack.pop();
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 1 ), history.getMostRecent() );

        stack.pop();
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 0 ), history.getMostRecent() );

        assertFalse( "expecting stack with passthrodecoder", stack.isEmpty() );

        stack.pop();
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 0 ), history.getMostRecent() );

        assertTrue( "expecting empty stack after last pop", stack.isEmpty() );

        stack.pop();
        stack.decode( Integer.valueOf( 0 ) );
        assertEquals( Integer.valueOf( 0 ), history.getMostRecent() );

        assertTrue( "expecting empty stack after empty pop", stack.isEmpty() );
    }


    @Test
    public void testFailure() throws Exception
    {
        DecoderStack stack = new DecoderStack();
        CallbackHistory history = new CallbackHistory();
        stack.setCallback( history );
        assertNotNull( stack );
        assertTrue( "expecting empty stack after creation", stack.isEmpty() );
        PassThroDecoder decoder = new PassThroDecoder();
        stack.push( decoder );

        stack.push( new FaultingDecoder() );

        try
        {
            stack.decode( new Object() );
            fail( "should never reach here due to exception throws" );
        }
        catch ( RuntimeException e )
        {
            assertNotNull( e );
            assertTrue( "testing keyword should be in the message", e.getMessage().indexOf( "testing" ) > 0 );
            assertTrue( "RuntimeException cause should be a DecoderException", e.getCause().getClass().equals(
                DecoderException.class ) );
        }
    }

    /**
     * A do nothing decoder.
     */
    class PassThroDecoder extends AbstractStatefulDecoder
    {
        public void decode( Object encoded ) throws DecoderException
        {
            super.decodeOccurred( encoded );
        }
    }

    /**
     * A decoder that increments an Integer passed in as an argument. We're
     * using this for verifying the additive (hehe) effects of decoder chaining.
     */
    class IncrementingDecoder extends AbstractStatefulDecoder
    {
        public void decode( Object encoded ) throws DecoderException
        {
            Integer value = ( Integer ) encoded;
            value = Integer.valueOf( value.intValue() + 1 );
            super.decodeOccurred( value );
        }
    }

    /**
     * A decoder that throws an exception on decode calls. We're using this for
     * verifying the failure of the chain.
     */
    class FaultingDecoder extends AbstractStatefulDecoder
    {
        public void decode( Object encoded ) throws DecoderException
        {
            throw new DecoderException( "testing" );
        }
    }
}
