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
package org.apache.directory.shared.ldap.message;


/**
 * Lockable Control implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class ControlImpl implements Control
{
    /** Unique object identifier for this control */
    private String oid;

    /** Control ASN.1 encoded parameters */
    private byte[] value;

    /** Flag for control criticality */
    private boolean isCritical;


    // ------------------------------------------------------------------------
    // Control Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Determines whether or not this control is critical for the correct
     * operation of a request or response message. The default for this value
     * should be false.
     * 
     * @return true if the control is critical false otherwise.
     */
    public boolean isCritical()
    {
        return this.isCritical;
    }


    /**
     * Sets the criticil flag which determines whether or not this control is
     * critical for the correct operation of a request or response message. The
     * default for this value should be false.
     * 
     * @param isCritical
     *            true if the control is critical false otherwise.
     */
    public void setCritical( boolean isCritical )
    {
        this.isCritical = isCritical;
    }


    /**
     * Gets the OID of the Control to identify the control type.
     * 
     * @return the OID of this Control.
     */
    public String getType()
    {
        return this.oid;
    }


    /**
     * Sets the OID of the Control to identify the control type.
     * 
     * @param oid
     *            the OID of this Control.
     */
    public void setType( String oid )
    {
        this.oid = oid;
    }


    /**
     * Gets the ASN.1 BER encoded value of the control which would have its own
     * custom ASN.1 defined structure based on the nature of the control.
     * 
     * @return ASN.1 BER encoded value as binary data.
     */
    public byte[] getValue()
    {
        return this.value;
    }


    /**
     * Sets the ASN.1 BER encoded value of the control which would have its own
     * custom ASN.1 defined structure based on the nature of the control.
     * 
     * @param value
     *            ASN.1 BER encoded value as binary data.
     */
    public void setValue( byte[] value )
    {
        this.value = value;
    }


    /**
     * Retrieves the object identifier assigned for the LDAP control.
     * 
     * @return The non-null object identifier string.
     */
    public String getID()
    {
        return this.oid;
    }
}
