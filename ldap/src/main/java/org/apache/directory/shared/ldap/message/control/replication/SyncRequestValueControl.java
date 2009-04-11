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
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.SyncRequestValueControlCodec;
import org.apache.directory.shared.ldap.message.control.InternalAbstractControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the Sunc Request Control, as described by RFC 4533.
 * The structure for this control is :
 * 
 * syncRequestValue ::= SEQUENCE {
 *     mode ENUMERATED {
 *         -- 0 unused
 *         refreshOnly       (1),
 *         -- 2 reserved
 *         refreshAndPersist (3)
 *     },
 *     cookie     syncCookie OPTIONAL,
 *     reloadHint BOOLEAN DEFAULT FALSE
 * }
 * 
 * This control OID is 1.3.6.1.4.1.4203.1.9.1.1
 * @see <a href="http://www.faqs.org/rfcs/rfc4533.html">RFC 4533</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: $
 *
 */
public class SyncRequestValueControl extends InternalAbstractControl
{
    /** As this class is serializable, defined its serialVersionUID */ 
    private static final long serialVersionUID = 1L;

    /** The Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SyncRequestValueControl.class );

    /** This control OID */
    public static final String CONTROL_OID = "1.3.6.1.4.1.4203.1.9.1.1";

    /** The synchronization type */
    private SynchronizationModeEnum mode;
    
    /** The cookie */
    private byte[] cookie;
    
    /** The reloadHint flag */
    private boolean reloadHint;

    
    /**
     * @return the mode
     */
    public SynchronizationModeEnum getMode()
    {
        return mode;
    }

    
    /**
     * @param syncMode the mode to set
     */
    public void setMode( SynchronizationModeEnum mode )
    {
        this.mode = mode;
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
     * @return the reloadHint
     */
    public boolean isReloadHint()
    {
        return reloadHint;
    }

    
    /**
     * @param reloadHint the reloadHint to set
     */
    public void setReloadHint( boolean reloadHint )
    {
        this.reloadHint = reloadHint;
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
        SyncRequestValueControlCodec syncRequestValueCtlCodec = new SyncRequestValueControlCodec();
        syncRequestValueCtlCodec.setMode( mode );
        syncRequestValueCtlCodec.setCookie( cookie );
        syncRequestValueCtlCodec.setReloadHint( reloadHint );

        try
        {
            return syncRequestValueCtlCodec.encode( null ).array();
        }
        catch ( EncoderException e )
        {
            LOG.error( "Failed to encode syncRequestValue control", e );
            throw new IllegalStateException( "Failed to encode control with encoder.", e );
        }
    }
}
