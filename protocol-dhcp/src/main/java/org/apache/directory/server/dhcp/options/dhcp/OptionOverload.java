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

package org.apache.directory.server.dhcp.options.dhcp;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * This option is used to indicate that the DHCP 'sname' or 'file'
 * fields are being overloaded by using them to carry DHCP options. A
 * DHCP server inserts this option if the returned parameters will
 * exceed the usual space allotted for options.
 * 
 * If this option is present, the client interprets the specified
 * additional fields after it concludes interpretation of the standard
 * option fields.
 * 
 * The code for this option is 52, and its length is 1.  Legal values
 * for this option are:
 * 
 *         Value   Meaning
 *         -----   --------
 *           1     the 'file' field is used to hold options
 *           2     the 'sname' field is used to hold options
 *           3     both fields are used to hold options
 */
public class OptionOverload extends DhcpOption
{
    private byte[] optionOverload;


    public OptionOverload(byte[] optionOverload)
    {
        super( 52, 1 );
        this.optionOverload = optionOverload;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( optionOverload );
    }
}
