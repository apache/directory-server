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
package org.apache.eve.schema;


import javax.naming.NamingException;

import org.apache.ldap.common.schema.AttributeType;


/**
 * An AttributeType registry's service interface.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface AttributeTypeRegistry
{
    /**
     * Registers a Comparator with this registry.
     * 
     * @param attributeType the attributeType to register
     * @throws javax.naming.NamingException if the AttributeType is already
     * registered or the registration operation is not supported
     */
    void register( AttributeType attributeType ) throws NamingException;
    
    /**
     * Looks up an attributeType by its unique Object Identifier.
     * 
     * @param oid the object identifier
     * @return the AttributeType instance for the oid
     * @throws javax.naming.NamingException if the AttributeType does not exist
     */
    AttributeType lookup( String oid ) throws NamingException;

    /**
     * Checks to see if an attributeType exists.
     * 
     * @param oid the object identifier
     * @return true if an attributeType definition exists for the oid, false
     * otherwise
     */
    boolean hasAttributeType( String oid );
}
