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

package org.apache.directory.server.dhcp.options.misc;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * This option specifies the name of the client's NIS domain.  The
 * domain is formatted as a character string consisting of characters
 * from the NVT ASCII character set.
 * 
 * The code for this option is 40.  Its minimum length is 1.
 */
public class NisDomain extends DhcpOption
{
    private byte[] nisDomain;


    public NisDomain(byte[] nisDomain)
    {
        super( 40, 1 );
        this.nisDomain = nisDomain;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( nisDomain );
    }
}
