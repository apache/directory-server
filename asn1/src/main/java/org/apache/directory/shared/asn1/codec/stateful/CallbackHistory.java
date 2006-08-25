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


import java.util.LinkedList;


/**
 * A convenience callback which collects decoded or encoded objects to audit a
 * codecs's activity. The callback also comes in handy when data is to be pushed
 * through a codec and grabed immediately afterwords to serialize codec
 * operation.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public class CallbackHistory implements DecoderCallback, EncoderCallback
{
    /** history of decoded objects in cronological order */
    private final LinkedList history;

    /** the length of callback history stored */
    private final int length;


    /**
     * Creates an auditing callback that manages a history of indefinite length.
     */
    public CallbackHistory()
    {
        this( -1 );
    }


    /**
     * Creates an auditing callback that manages a history of fixed or
     * indefinite length. If the length is fixed the history effectively becomes
     * a FIFO structure.
     * 
     * @param length
     *            the maximum length of callback history to store before
     *            dropping decoded items, a length of zero or 1 corresponds to
     *            indefinite history
     */
    public CallbackHistory(int length)
    {
        this.length = length;
        history = new LinkedList();
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.apache.asn1.codec.stateful.DecoderCallback#decodeOccurred(
     *      org.apache.asn1.codec.stateful.StatefulDecoder, java.lang.Object)
     */
    public void decodeOccurred( StatefulDecoder decoder, Object decoded )
    {
        if ( length > 0 )
        {
            while ( history.size() >= length )
            {
                history.removeLast();
            }
        }

        history.addFirst( decoded );
    }


    /**
     * Callback to deliver a fully encoded object.
     * 
     * @param encoder
     *            the stateful encoder driving the callback
     * @param encoded
     *            the object that was encoded
     */
    public void encodeOccurred( StatefulEncoder encoder, Object encoded )
    {
        if ( length > 0 )
        {
            while ( history.size() >= length )
            {
                history.removeLast();
            }
        }

        history.addFirst( encoded );
    }


    /**
     * Gets the most recent decoded object if one exists.
     * 
     * @return the most recent decoded object
     * @throws java.util.NoSuchElementException
     *             if the history is empty
     */
    public Object getMostRecent()
    {
        return history.getFirst();
    }


    /**
     * Gets the oldest decoded object if one exists.
     * 
     * @return the oldest decoded object
     * @throws java.util.NoSuchElementException
     *             if the history is empty
     */
    public Object getOldest()
    {
        return history.getLast();
    }


    /**
     * Tests to see if the history is empty.
     * 
     * @return true if the history is empty, false otherwise
     */
    public boolean isEmpty()
    {
        return history.isEmpty();
    }


    /**
     * Clears the history of decoded items.
     */
    public void clear()
    {
        history.clear();
    }


    /**
     * Gets the number of decoded items in the callback history.
     * 
     * @return the number of decoded items in the callback history
     */
    public int size()
    {
        return history.size();
    }
}
