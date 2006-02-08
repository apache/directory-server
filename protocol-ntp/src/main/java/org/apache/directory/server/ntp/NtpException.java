/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.server.ntp;

/**
 * The root of the NTP exception hierarchy.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NtpException extends Exception
{
    private static final long serialVersionUID = -225862469671550203L;

    /**
     * Creates an NtpException.
     */
    public NtpException()
    {
        super();
    }

    /**
     * Creates an NtpException with a message and cause.
     * 
     * @param message the message
     * @param cause the cause
     */
    public NtpException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Creates an NtpException with a message.
     * 
     * @param message the message
     */
    public NtpException( String message )
    {
        super( message );
    }

    /**
     * @param cause the cause
     */
    public NtpException( Throwable cause )
    {
        super( cause );
    }
}
