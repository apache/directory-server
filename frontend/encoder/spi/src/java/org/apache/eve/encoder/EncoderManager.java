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
package org.apache.eve.encoder ;

import org.apache.commons.codec.EncoderException ;

import org.apache.ldap.common.message.Response ;

/**
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface EncoderManager
{

    /**
     * Synchronously encodes an LDAPv3 protocol Response message into a byte
     * buffer that can be written to a Stream as an BER encoded PDU.
     *
     * @param a_response the LDAP Response message to be encoded.
     */
    public byte [] encode( Response a_response ) throws EncoderException ;
}
