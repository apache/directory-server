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
package org.apache.eve.event ;


import java.nio.ByteBuffer ;

import org.apache.eve.listener.ClientKey ;


/**
 * Input event used to indicate the availability of more data from the client.
 * The data has already been read in from the client.
 * 
 * Note that this is an abstract event whose methods to claim and release 
 * interest in the buffer payload are abstract.  This has purposefully been 
 * left that way so concrete subclasses created by stages can manage the 
 * interactions required with the buffer pool without creating a dependency
 * on the BufferPool spi or its implementation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class InputEvent extends ClientEvent
{
    /** the buffer used to store the read input */
    private final ByteBuffer m_buffer ;
    

    /**
     * Creates an InputEvent 
     * 
     * @param client the key of the client 
     * @param buffer the buffer containing the input chunk
     */
    public InputEvent( Object source, ClientKey client, ByteBuffer buffer )
    {
        super( source, client ) ;
        m_buffer = buffer ;
    }

    
    /**
     * Gets a handle on a read-only buffer using the original as the backing 
     * store and registers the accessing party as interested in the buffer.
     * 
     * @param party the party interested in the buffer
     * @return the buffer with the partial input data
     */
    public abstract ByteBuffer claimInterest( Object party ) ;

    
    /**
     * Releases the interest for the ByteBuffer held within this InputEvent.  
     * Once all interested parties have released their interest in the buffer it
     * is reclaimed.
     * 
     * @param party the party that originally claimed interest
     */
    public abstract void releaseInterest( Object party ) ;

    
    /**
     * Gets the underlying byte buffer associated with this InputEvent.
     * 
     * @return the underlying byte buffer
     */
    protected ByteBuffer getBuffer()
    {
        return m_buffer ;
    }
}

