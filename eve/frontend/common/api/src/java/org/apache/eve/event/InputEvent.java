/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.eve.event ;


import java.nio.ByteBuffer ;
import java.util.EventObject ;

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
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public abstract class InputEvent extends EventObject
{
    /** the buffer used to store the read input */
    protected final ByteBuffer m_buffer ;
    

    /**
     * Creates an InputEvent 
     * 
     * @param a_client
     * @param a_buffer
     */
    public InputEvent( ClientKey a_client, ByteBuffer a_buffer )
    {
        super( a_client ) ;
        m_buffer = a_buffer ;
    }

    
    /**
     * Gets a handle on a read-only buffer using the original as the backing 
     * store and registers the accessing party as interested in the buffer.
     * 
     * @param a_party the party interested in the buffer
     * @return the buffer with the partial input data
     */
    public abstract ByteBuffer claimInterest( Object a_party ) ;

    
    /**
     * Releases the interest for the ByteBuffer held within this InputEvent.  
     * Once all interested parties have released their interest in the buffer it
     * is reclaimed.
     * 
     * @param a_party the party that originally claimed interest
     */
    public abstract void releaseInterest( Object a_party ) ;
}

