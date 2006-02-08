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
package org.apache.directory.shared.ldap.message;


/**
 * Protocol request and response altering control interface. Any number of
 * controls may be associated with a protocol message.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Control extends javax.naming.ldap.Control
{
    /**
     * Gets the OID of the Control to identify the control type.
     * 
     * @return the OID of this Control.
     */
    String getType();


    /**
     * Sets the OID of the Control to identify the control type.
     * 
     * @param oid
     *            the OID of this Control.
     */
    void setType( String oid );


    /**
     * Gets the ASN.1 BER encoded value of the control which would have its own
     * custom ASN.1 defined structure based on the nature of the control.
     * 
     * @return ASN.1 BER encoded value as binary data.
     */
    byte[] getValue();


    /**
     * Sets the ASN.1 BER encoded value of the control which would have its own
     * custom ASN.1 defined structure based on the nature of the control.
     * 
     * @param value
     *            ASN.1 BER encoded value as binary data.
     */
    void setValue( byte[] value );


    /**
     * Determines whether or not this control is critical for the correct
     * operation of a request or response message. The default for this value
     * should be false.
     * 
     * @return true if the control is critical false otherwise.
     */
    boolean isCritical();


    /**
     * Sets the criticil flag which determines whether or not this control is
     * critical for the correct operation of a request or response message. The
     * default for this value should be false.
     * 
     * @param isCritical
     *            true if the control is critical false otherwise.
     */
    void setCritical( boolean isCritical );
}
