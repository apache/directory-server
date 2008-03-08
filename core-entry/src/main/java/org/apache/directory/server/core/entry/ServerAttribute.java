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


import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.entry.client.ClientAttribute;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;


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
     * Gets the attribute type associated with this ServerAttribute.
     *
     * @return the attributeType associated with this entry attribute
     */
    AttributeType getType();

    
    /**
     * Set the user provided ID. If we have none, the upId is assigned
     * the attributetype's name. If it does not have any name, we will
     * use the OID.
     * <p>
     * If we have an upId and an AttributeType, they must be compatible. :
     *  - if the upId is an OID, it must be the AttributeType's OID
     *  - otherwise, its normalized form must be equals to ones of
     *  the attributeType's names.
     *
     * @param upId The attribute ID
     * @param attributeType The associated attributeType
     */
    public void setUpId( String upId, AttributeType attributeType );

    
    /**
     * Check if the current attribute type is of the expected attributeType 
     *
     * @param attributeId The AttributeType ID to check
     * @return True if the current attribute is of the expected attributeType
     * @throws InvalidAttributeValueException If there is no AttributeType
     */
    boolean isA( String attributeId ) throws InvalidAttributeValueException;
}
