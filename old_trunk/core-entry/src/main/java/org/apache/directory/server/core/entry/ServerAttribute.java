/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.entry;


import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import org.apache.directory.shared.ldap.entry.client.ClientAttribute;
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * The server specific interface extending the EntryAttribute interface. It adds
 * three more methods which are Server side.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ServerAttribute extends ClientAttribute
{
    /**
     * Get the attribute type associated with this ServerAttribute.
     *
     * @return the attributeType associated with this entry attribute
     */
    AttributeType getAttributeType();

    
    /**
     * <p>
     * Set the attribute type associated with this ServerAttribute.
     * </p>
     * <p>
     * The current attributeType will be replaced. It is the responsibility of
     * the caller to insure that the existing values are compatible with the new
     * AttributeType
     * </p>
     *
     * @param attributeType the attributeType associated with this entry attribute
     */
    void setAttributeType( AttributeType attributeType );

    
    /**
     * <p>
     * Check if the current attribute type is of the expected attributeType
     * </p>
     * <p>
     * This method won't tell if the current attribute is a descendant of 
     * the attributeType. For instance, the "CN" serverAttribute will return
     * false if we ask if it's an instance of "Name". 
     * </p> 
     *
     * @param attributeId The AttributeType ID to check
     * @return True if the current attribute is of the expected attributeType
     * @throws InvalidAttributeValueException If there is no AttributeType
     */
    boolean instanceOf( String attributeId ) throws InvalidAttributeValueException;


    /**
     * <p>
     * Set the user provided ID. If we have none, the upId is assigned
     * the attributetype's name. If it does not have any name, we will
     * use the OID.
     * </p>
     * <p>
     * If we have an upId and an AttributeType, they must be compatible. :
     *  - if the upId is an OID, it must be the AttributeType's OID
     *  - otherwise, its normalized form must be equals to ones of
     *  the attributeType's names.
     * </p>
     * <p>
     * In any case, the ATtributeType will be changed. The caller is responsible for
     * the present values to be compatoble with the new AttributeType.
     * </p>
     * 
     * @param upId The attribute ID
     * @param attributeType The associated attributeType
     */
    void setUpId( String upId, AttributeType attributeType );
    
    
    /**
     * <p>
     * Checks to see if this attribute is valid along with the values it contains.
     * </p>
     * <p>
     * An attribute is valid if :
     * <li>All of its values are valid with respect to the attributeType's syntax checker</li>
     * <li>If the attributeType is SINGLE-VALUE, then no more than a value should be present</li>
     *</p>
     * @return true if the attribute and it's values are valid, false otherwise
     * @throws NamingException if there is a failure to check syntaxes of values
     */
    boolean isValid() throws NamingException;
}
