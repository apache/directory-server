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

package org.apache.directory.server.dns.io.encoder;


import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.store.DnsAttribute;


/**
 * 3.3.14. TXT RDATA format
 * 
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     /                   TXT-DATA                    /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 
 * where:
 * 
 * TXT-DATA        One or more <character-string>s.
 * 
 * TXT RRs are used to hold descriptive text.  The semantics of the text
 * depends on the domain where it is found.
 */
public class TextRecordEncoder extends ResourceRecordEncoder
{
    protected byte[] encodeResourceData( ResourceRecord record )
    {
        return encodeCharacterString( record.get( DnsAttribute.CHARACTER_STRING ) );
    }
}
