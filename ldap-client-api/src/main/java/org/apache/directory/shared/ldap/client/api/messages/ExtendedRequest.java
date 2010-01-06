/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.ldap.client.api.messages;


import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Class for representing client's extended operation request.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ExtendedRequest extends AbstractRequest implements RequestWithResponse, AbandonableRequest
{
    /** requested extended operation's OID (a.k.a request name) */
    private OID oid;

    /** requested extended operation's value (a.k.a request value) */
    private byte[] value;


    public ExtendedRequest( String oid )
    {
        try
        {
            this.oid = new OID( oid );
        }
        catch ( DecoderException e )
        {
            throw new IllegalArgumentException( e );
        }
    }


    public ExtendedRequest( OID oid )
    {
        this.oid = oid;
    }


    public byte[] getValue()
    {
        return value;
    }


    public void setValue( String value )
    {
        this.value = StringTools.getBytesUtf8( value );
    }


    public void setValue( byte[] value )
    {
        this.value = value;
    }


    public OID getOid()
    {
        return oid;
    }
}
