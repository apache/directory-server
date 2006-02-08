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
package org.apache.directory.shared.ldap.codec;


import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;


/**
 * A decoder for EntryChangeControls.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ManageDsaITControlDecoder implements ControlDecoder
{
    public String getControlType()
    {
        return ManageDsaITControl.CONTROL_OID;
    }


    public Asn1Object decode( byte[] controlBytes ) throws DecoderException
    {
        return new org.apache.directory.shared.ldap.codec.ManageDsaITControl();
    }
}
