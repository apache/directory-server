/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.directory.shared.ldap.codec;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.message.Message;


/**
 * Thrown when a Decoder has encountered a failure condition during a decode.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ResponseCarryingException extends DecoderException
{
    /**
     * Declares the Serial Version Uid.
     * 
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    /** The response with the error cause */
    private Message response;
    
    /**
     * Creates a DecoderException
     * 
     * @param message
     *            A message with meaning to a human
     */
    public ResponseCarryingException(String message)
    {
        super( message );
    }


    /**
     * Creates a DecoderException
     * 
     * @param message
     *            A message with meaning to a human
     * @param cause
     *            The Exception which caused the error
     */
    public ResponseCarryingException(String message, Throwable cause)
    {
        super( message, cause );
    }
    
    /**
     * Set a response if we get an exception while parsing the message
     * @param response the constructed response
     */
    public void setResponse( Message response ) 
    {
        this.response = response;
    }
    
    /**
     * Get the constructed response
     * @return The constructed response
     */
    public Message getResponse()
    {
        return response;
    }

}
