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


/**
 * Response object for extended operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ExtendedResponse extends AbstractResponseWithResult
{
    /** extended operation response OID */
    private OID oid;

    /** extended operation response value */
    private Object value;

    public ExtendedResponse()
    {
    }
    
    public ExtendedResponse( String oid )
    {
        super();
        try
        {
            this.oid = new OID( oid );
        }
        catch ( DecoderException e )
        {
            throw new IllegalArgumentException( e );
        }
    }

    
    public ExtendedResponse( OID oid )
    {
        super();
        this.oid = oid;
    }

    
    public Object getValue()
    {
        return value;
    }


    public void setValue( Object value )
    {
        this.value = value;
    }


    public void setOid( OID oid )
    {
        this.oid = oid;
    }

    public OID getOid()
    {
        return oid;
    }
}
