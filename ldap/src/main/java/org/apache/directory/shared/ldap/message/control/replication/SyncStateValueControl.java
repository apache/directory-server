/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.message.control.replication;

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControlCodec;
import org.apache.directory.shared.ldap.message.control.InternalAbstractControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the SyncStateValue Control, as described by RFC 4533.
 * The structure for this control is :
 * 
 *  syncStateValue ::= SEQUENCE {
 *       state ENUMERATED {
 *            present (0),
 *            add (1),
 *            modify (2),
 *            delete (3)
 *       },
 *       entryUUID syncUUID,
 *       cookie    syncCookie OPTIONAL
 *  }
 * 
 * This control OID is 1.3.6.1.4.1.4203.1.9.1.2
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc4533.html">RFC 4533</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: $
 *
 */
public class SyncStateValueControl extends InternalAbstractControl
{
    /** As this class is serializable, defined its serialVersionUID */ 
    private static final long serialVersionUID = 1L;

    /** The Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SyncStateValueControl.class );

    /** This control OID */
    public static final String CONTROL_OID = "1.3.6.1.4.1.4203.1.9.1.2";

    /** The synchronization state type */
    private SyncStateTypeEnum syncStateType;
    
    /** the entryUUID */
    private byte[] entryUUID;
    
    /** The cookie */
    private byte[] cookie;
    

    /**
     * @return the syncState's type
     */
    public SyncStateTypeEnum getSyncStateType()
    {
        return syncStateType;
    }


    /**
     * set the syncState's type
     * 
     * @param syncStateType the syncState's type
     */
    public void setSyncStateType( SyncStateTypeEnum syncStateType )
    {
        this.syncStateType = syncStateType;
    }


    /**
     * @return the entryUUID
     */
    public byte[] getEntryUUID()
    {
        return entryUUID;
    }


    /**
     * set the entryUUID
     * 
     * @param entryUUID the entryUUID
     */
    public void setEntryUUID( byte[] entryUUID )
    {
        this.entryUUID = entryUUID;
    }


    /**
     * @return the cookie
     */
    public byte[] getCookie()
    {
        return cookie;
    }

    
    /**
     * @param syncCookie the syncCookie to set
     */
    public void setCookie( byte[] cookie )
    {
        this.cookie = cookie;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getID()
    {
        return CONTROL_OID;
    }

    
    /**
     * {@inheritDoc}
     */
    public byte[] getEncodedValue()
    {
        SyncStateValueControlCodec syncStateValueCtlCodec = new SyncStateValueControlCodec();
        syncStateValueCtlCodec.setSyncStateType( syncStateType );
        syncStateValueCtlCodec.setEntryUUID( entryUUID );
        syncStateValueCtlCodec.setCookie( cookie );

        try
        {
            return syncStateValueCtlCodec.encode( null ).array();
        }
        catch ( EncoderException e )
        {
            LOG.error( "Failed to encode syncStateValue control", e );
            throw new IllegalStateException( "Failed to encode control with encoder.", e );
        }
    }
}
