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

package org.apache.directory.server.dns.protocol;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class DnsProtocolCodecFactory implements ProtocolCodecFactory
{
    private static final DnsProtocolCodecFactory INSTANCE = 
        new DnsProtocolCodecFactory();
    
    public static DnsProtocolCodecFactory getInstance()
    {
        return INSTANCE;
    }

    private DnsProtocolCodecFactory()
    {
    }

    public ProtocolEncoder getEncoder()
    {
        // Create a new encoder.
        return new DnsEncoder();
    }

    public ProtocolDecoder getDecoder()
    {
        // Create a new decoder.
        return new DnsDecoder();
    }
}
