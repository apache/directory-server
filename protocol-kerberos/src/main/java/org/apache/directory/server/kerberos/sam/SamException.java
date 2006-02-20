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
package org.apache.directory.server.kerberos.sam;


import org.apache.directory.server.kerberos.shared.messages.value.SamType;


/**
 * Base class for all SAM subsystem errors.
 *
 * @warning this should extend from KerberosException in o.a.k.exception.
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SamException extends Exception
{
    private static final long serialVersionUID = -677444708375928227L;

    /** the SAM type that caused this exception */
    private final SamType type;


    /**
     * Creates a SamException for a specific SamType.
     *
     * @param type the type value for the SAM algorithm associated with this exception
     */
    public SamException(SamType type)
    {
        super();

        this.type = type;
    }


    /**
     * Creates a SamException for a specific SamType, with message.
     *
     * @param type the type value for the SAM algorithm associated with this exception
     * @param message a message regarding the nature of the fault
     */
    public SamException(SamType type, String message)
    {
        super( message );

        this.type = type;
    }


    /**
     * Creates a SamException for a specific SamType, with the cause resulted in
     * this exception.
     *
     * @param type the type value for the SAM algorithm associated with this exception
     * @param cause the throwable that resulted in this exception being thrown
     */
    public SamException(SamType type, Throwable cause)
    {
        super( cause );

        this.type = type;
    }


    /**
     * Creates a SamException for a specific SamType, with a message and the
     * cause that resulted in this exception.
     *
     *
     * @param type the type value for the SAM algorithm associated with this exception
     * @param message a message regarding the nature of the fault
     * @param cause the throwable that resulted in this exception being thrown
     */
    public SamException(SamType type, String message, Throwable cause)
    {
        super( message, cause );

        this.type = type;
    }


    /**
     * Gets the registered SAM algorithm type associated with this SamException.
     *
     * @return the type value for the SAM algorithm associated with this exception
     */
    public SamType getSamType()
    {
        return this.type;
    }
}
