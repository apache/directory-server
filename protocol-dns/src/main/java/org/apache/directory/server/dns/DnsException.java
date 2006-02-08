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

package org.apache.directory.server.dns;

import org.apache.directory.server.dns.messages.ResponseCode;

/**
 * The root of the DNS exception hierarchy.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DnsException extends Exception
{
    private static final long serialVersionUID = 7493573233290707069L;

    /**
     * The DNS response code associated with this exception
     */
    private final int responseCode;

    /**
     * Creates a DnsException with a response code.
     *
     * @param responseCode the response code associated with this DnsException
     */
    public DnsException( ResponseCode responseCode )
    {
        super( responseCode.getMessage() );

        this.responseCode = responseCode.getOrdinal();
    }

    /**
     * Gets the protocol response code associated with this DnsException.
     *
     * @return the response code associated with this DnsException
     */
    public int getResponseCode()
    {
        return this.responseCode;
    }
}
