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
 * An event used to denote output to send to a client.  The output event
 * only connotates that data is available for output but not yet delivered.  
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class OutputEvent extends ClientEvent
{
    /** the data to send */
    private final ByteBuffer buf ;

    
    public OutputEvent( Object source, ClientKey clientKey, ByteBuffer buf )
    {
        super( source, clientKey ) ;
        this.buf = buf ;
    }
    

    public ByteBuffer getBuffer()
    {
        return buf ;
    }
}
