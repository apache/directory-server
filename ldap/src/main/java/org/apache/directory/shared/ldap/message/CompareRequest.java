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

import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Compare request protocol message that tests an entry to see if it abides by
 * an attribute value assertion.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public interface CompareRequest extends SingleReplyRequest, AbandonableRequest
{
    /** Compare request message type enum code */
    MessageTypeEnum TYPE = MessageTypeEnum.COMPAREREQUEST;

    /** Compare response message type enum code */
    MessageTypeEnum RESP_TYPE = CompareResponse.TYPE;


    /**
     * Gets the distinguished name of the entry to be compared using the
     * attribute value assertion.
     * 
     * @return the DN of the compared entry.
     */
    LdapDN getName();


    /**
     * Sets the distinguished name of the entry to be compared using the
     * attribute value assertion.
     * 
     * @param a_name
     *            the DN of the compared entry.
     */
    void setName( LdapDN name );


    /**
     * Gets the attribute value to use in making the comparison.
     * 
     * @return the attribute value to used in comparison.
     */
    byte[] getAssertionValue();


    /**
     * Sets the attribute value to use in the comparison.
     * 
     * @param a_value
     *            the attribute value used in comparison.
     */
    void setAssertionValue( String a_value );


    /**
     * Sets the attribute value to use in the comparison.
     * 
     * @param a_value
     *            the attribute value used in comparison.
     */
    void setAssertionValue( byte[] value );


    /**
     * Gets the attribute id use in making the comparison.
     * 
     * @return the attribute id used in comparison.
     */
    String getAttributeId();


    /**
     * Sets the attribute id used in the comparison.
     * 
     * @param a_attrId
     *            the attribute id used in comparison.
     */
    void setAttributeId( String a_attrId );
}
