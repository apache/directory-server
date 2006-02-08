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

package org.apache.directory.server.dhcp;


public class DhcpException extends Exception
{
    private static final long serialVersionUID = 3985748516732135317L;

    /**
     * This empty constructor is used if no 
     * explanation of the DHCP exception is required.
     */
    public DhcpException()
    {
        super();
    }

    /**
     * This constructor is used if a description of the event
     * that caused the exception is required.
     * 
     * @param description this is a description of the exception
     */
    public DhcpException( String description )
    {
        super( description );
    }
}

