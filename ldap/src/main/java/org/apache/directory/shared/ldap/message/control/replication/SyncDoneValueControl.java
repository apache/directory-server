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
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControlCodec;
import org.apache.directory.shared.ldap.message.control.InternalAbstractControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * An implementation of syncDoneValue Contol as described in rfc 4533.
 * The structure of this control is:
 * 
 *  syncDoneValue ::= SEQUENCE 
 *  {
 *       cookie          syncCookie OPTIONAL,
 *       refreshDeletes  BOOLEAN DEFAULT FALSE
 *  }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyncDoneValueControl extends InternalAbstractControl
{
    /** As this class is serializable, defined its serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SyncRequestValueControl.class );

    /** This control OID */
    public static final String CONTROL_OID = "1.3.6.1.4.1.4203.1.9.1.3";

    /** The cookie */
    private byte[] cookie;

    /** the refreshDeletes flag */
    private boolean refreshDeletes;


    /**
     * @return the cookie
     */
    public byte[] getCookie()
    {
        return cookie;
    }


    /**
     * @param cookie cookie to be set
     */
    public void setCookie( byte[] cookie )
    {
        this.cookie = cookie;
    }


    /**
     * @return true, if refreshDeletes flag is set, false otherwise
     */
    public boolean isRefreshDeletes()
    {
        return refreshDeletes;
    }


    /**
     * @param refreshDeletes set the refreshDeletes flag 
     */
    public void setRefreshDeletes( boolean refreshDeletes )
    {
        this.refreshDeletes = refreshDeletes;
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
        SyncDoneValueControlCodec codec = new SyncDoneValueControlCodec();
        codec.setCookie( cookie );
        codec.setRefreshDeletes( refreshDeletes );

        try
        {
            return codec.encode( null ).array();
        }
        catch ( EncoderException e )
        {
            LOG.error( "Failed to encode syncDoneValue control", e );
            throw new IllegalStateException( "Failed to encode control with encoder.", e );
        }
    }

}
