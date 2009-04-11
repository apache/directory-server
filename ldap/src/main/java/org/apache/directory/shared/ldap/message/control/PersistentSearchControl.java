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
package org.apache.directory.shared.ldap.message.control;


import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;
import org.apache.directory.shared.ldap.codec.search.controls.pSearch.PSearchControlCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The control for a persistent search operation, as defined in 
 * http://www.ietf.org/proceedings/01mar/I-D/ldapext-psearch-03.txt.
 * 
 * The data structure is defined by the following ASN.1 description :
 * 
 * PersistentSearch ::= SEQUENCE {
 *     changeTypes INTEGER,
 *     changesOnly BOOLEAN,
 *     returnECs BOOLEAN
 * }
 *
 * The associated OID is : "2.16.840.1.113730.3.4.3" 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PersistentSearchControl extends InternalAbstractControl
{
    /** As this class is serializable, defined its serialVersionUID */ 
    private static final long serialVersionUID = -2356861450876343999L;

    /** The Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( PersistentSearchControl.class );

    /** This control OID */
    public static final String CONTROL_OID = "2.16.840.1.113730.3.4.3";

    /** A combinaison of all the possible changes. Resolves to 0x0F */
    public static final int ALL_CHANGES = 1 | 2 | 4 | 8;

    /**
     * If changesOnly is TRUE, the server MUST NOT return any existing entries
     * that match the search criteria. Entries are only returned when they are
     * changed (added, modified, deleted, or subject to a modifyDN operation).
     * By default this is set to true.
     */
    private boolean changesOnly = true;

    /**
     * If returnECs is TRUE, the server MUST return an Entry Change Notification
     * control with each entry returned as the result of changes. By default
     * this is set to false.
     */
    private boolean returnECs = false;

    /**
     * As changes are made to the server, the effected entries MUST be returned
     * to the client if they match the standard search criteria and if the
     * operation that caused the change is included in the changeTypes field.
     * The changeTypes field is the logical OR of one or more of these values:
     * <ul>
     * <li>add (1)</li> 
     * <li>delete (2)</li> 
     * <li>modify (4)</li>  
     * <li>modDN (8)</li> 
     * </ul>
     * <br>
     * By default this is set to 1 | 2 | 4 | 8 which is the int value 0x0F or 15.
     */
    private int changeTypes = ALL_CHANGES;


    public PersistentSearchControl()
    {
        super();
        setID( CONTROL_OID );
    }


    public void setChangesOnly( boolean changesOnly )
    {
        this.changesOnly = changesOnly;
    }


    public boolean isChangesOnly()
    {
        return changesOnly;
    }


    public void setReturnECs( boolean returnECs )
    {
        this.returnECs = returnECs;
    }


    public boolean isReturnECs()
    {
        return returnECs;
    }


    public void setChangeTypes( int changeTypes )
    {
        this.changeTypes = changeTypes;
    }


    public int getChangeTypes()
    {
        return changeTypes;
    }


    public boolean isNotificationEnabled( ChangeType changeType )
    {
        return ( changeType.getValue() & changeTypes ) > 0;
    }


    public void enableNotification( ChangeType changeType )
    {
        changeTypes |= changeType.getValue();
    }


    /**
     * @return The encoded byte[] for this persistentSearch control
     * 
     */
    public byte[] getEncodedValue()
    {
        PSearchControlCodec psearchCtlCodec = new PSearchControlCodec();
        psearchCtlCodec.setChangesOnly( changesOnly );
        psearchCtlCodec.setChangeTypes( changeTypes );
        psearchCtlCodec.setReturnECs( returnECs );

        try
        {
            return psearchCtlCodec.encode( null ).array();
        }
        catch ( EncoderException e )
        {
            LOG.error( "Failed to encode psearch control", e );
            throw new IllegalStateException( "Failed to encode control with encoder.", e );
        }
    }
}
