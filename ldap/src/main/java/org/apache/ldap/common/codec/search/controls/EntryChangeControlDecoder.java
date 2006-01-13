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
package org.apache.ldap.common.codec.search.controls;


import java.nio.ByteBuffer;

import org.apache.asn1.Asn1Object;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.codec.DecoderException;
import org.apache.ldap.common.codec.ControlDecoder;


/**
 * A decoder for EntryChangeControls.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryChangeControlDecoder implements ControlDecoder
{
    private final static String CONTROL_TYPE_OID = "2.16.840.1.113730.3.4.7";
    
    private static final Asn1Decoder decoder = new Asn1Decoder();
    
    public String getControlType()
    {
        return CONTROL_TYPE_OID;
    }


    public Asn1Object decode( byte[] controlBytes ) throws DecoderException
    {
        ByteBuffer bb = ByteBuffer.wrap( controlBytes );
        EntryChangeControlContainer container = new EntryChangeControlContainer();
        decoder.decode( bb, container );
        return container.getEntryChangeControl();
    }
}
