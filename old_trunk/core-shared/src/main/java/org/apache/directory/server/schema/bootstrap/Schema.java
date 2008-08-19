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
package org.apache.directory.server.schema.bootstrap;


/**
 * Base schema interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Schema
{
    /**
     * Checks whether or not this schema is enabled or disabled.
     * 
     * @return true if this schema is disabled, false otherwise
     */
    boolean isDisabled();
    
    /**
     * Gets the name of the owner of the schema objects within this
     * Schema.
     *
     * @return the identifier for the owner of this set's objects
     */
    String getOwner();


    /**
     * Gets the name of the logical schema the objects of this Schema
     * belong to: e.g. krb5-kdc may be the logical LDAP schema name.
     *
     * @return the name of the logical schema
     */
    String getSchemaName();


    /**
     * Gets the names of other schemas that the objects within this
     * Schema depends upon.
     *
     * @return the String names of schema dependencies
     */
    String[] getDependencies();
}
