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

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlCodec;
import org.apache.directory.shared.ldap.message.control.InternalAbstractControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implement the SyncInfoValue interface, and represent the forth
 * choice : syncIdSet.
 * The structure for this control is :
 * 
 * syncInfoValue ::= CHOICE {
 *     ...
 *     syncIdSet      [3] SEQUENCE {
 *         cookie         syncCookie OPTIONAL,
 *         refreshDeletes BOOLEAN DEFAULT FALSE,
 *         syncUUIDs      SET OF syncUUID
 *     }
 * }
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc4533.html">RFC 4533</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: $
 *
 */
public class SyncInfoValueSyncIdSetControl extends InternalAbstractControl implements SyncInfoValueControl 
{
    /** As this class is serializable, defined its serialVersionUID */ 
    private static final long serialVersionUID = 1L;

    /** The Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SyncInfoValueSyncIdSetControl.class );

    /** The cookie */
    private byte[] cookie;
    
    
    /** The refreshDeletesflag, default to false */
    private boolean refreshDeletes= true;
    
    /** The list of UUIDs */
    private List<byte[]> syncUUIDs = new ArrayList<byte[]>();
    
    
    /**
     * {@inheritDoc}
     */
    public byte[] getCookie()
    {
        return cookie;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void setCookie( byte[] cookie )
    {
        this.cookie = cookie;
    }


    /**
     * @return the refreshDeletes
     */
    public boolean isRefreshDeletes()
    {
        return refreshDeletes;
    }


    /**
     * @param refreshDone the refreshDeletes to set
     */
    public void setRefreshDeletes( boolean refreshDeletes )
    {
        this.refreshDeletes = refreshDeletes;
    }
    
    
    /**
     * @return the syncUUIDs
     */
    public List<byte[]> getSyncUUIDs()
    {
        return syncUUIDs;
    }


    /**
     * @param syncUUIDs the syncUUIDs to set
     */
    public void addSyncUUID( byte[] syncUUID )
    {
        syncUUIDs.add( syncUUID );
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
        SyncInfoValueControlCodec syncInfoValueCtlCodec = 
            new SyncInfoValueControlCodec( SynchronizationInfoEnum.SYNC_ID_SET );
        syncInfoValueCtlCodec.setCookie( cookie );

        try
        {
            return syncInfoValueCtlCodec.encode( null ).array();
        }
        catch ( EncoderException e )
        {
            LOG.error( "Failed to encode syncInfoValue control", e );
            throw new IllegalStateException( "Failed to encode control with encoder.", e );
        }
    }
}
