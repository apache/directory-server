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

import org.apache.directory.shared.ldap.message.InternalControl;


/**
 * Control implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class InternalAbstractControl implements InternalControl
{
    /** Unique object identifier for this control */
    private String oid;

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
     * Sets the critical flag which determines whether or not this control is
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
     * Sets the OID of the Control to identify the control type.
     * 
     * @param oid
     *            the OID of this Control.
     */
    public void setID( String oid )
    {
        this.oid = oid;
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
