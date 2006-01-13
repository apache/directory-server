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
package org.apache.ldap.common.codec;


import java.nio.ByteBuffer;

import org.apache.asn1.Asn1Object;
import org.apache.asn1.codec.EncoderException;


/**
 * Control which allows for the management of referrals and other DSA 
 * specific entities without processing them: meaning the referrals are
 * treated as regular entries using this control.  More information is
 * available in <a href="">RFC 3296</a>.  Below we have included 
 * section 3 of the RFC describing this control:
 * <pre>
 * 3.  The ManageDsaIT Control
 *
 *  The client may provide the ManageDsaIT control with an operation to
 *  indicate that the operation is intended to manage objects within the
 *  DSA (server) Information Tree.  The control causes Directory-specific
 *  entries (DSEs), regardless of type, to be treated as normal entries
 *  allowing clients to interrogate and update these entries using LDAP
 *  operations.
 *
 *  A client MAY specify the following control when issuing an add,
 *  compare, delete, modify, modifyDN, search request or an extended
 *  operation for which the control is defined.
 *
 *  The control type is 2.16.840.1.113730.3.4.2.  The control criticality
 *  may be TRUE or, if FALSE, absent.  The control value is absent.
 *
 *  When the control is present in the request, the server SHALL NOT
 *  generate a referral or continuation reference based upon information
 *  held in referral objects and instead SHALL treat the referral object
 *  as a normal entry.  The server, however, is still free to return
 *  referrals for other reasons.  When not present, referral objects
 *  SHALL be handled as described above.
 *
 *  The control MAY cause other objects to be treated as normal entries
 *  as defined by subsequent documents.
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ManageDsaITControl extends Asn1Object
{
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate( 0 );
    
    
    /**
     * Returns 0 everytime.
     */
    public int computeLength()
    {
        return 0;
    }
    
    
    /**
     * Encodes the control: does nothing but returns an empty buffer.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        return EMPTY_BUFFER;
    }
}
