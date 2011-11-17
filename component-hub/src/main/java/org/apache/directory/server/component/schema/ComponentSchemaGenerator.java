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
package org.apache.directory.server.component.schema;


import org.apache.felix.ipojo.Factory;


/**
 * Interface for classes generating schemas for component types.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ComponentSchemaGenerator
{
    /**
     * Generates a schema for representing all of factory's configurables.
     * Returned schema is in right order to add it to LDAP without any sorting.
     * 
     * If factory does not need a custom schema, it does not generate a schema for
     * it, just returns the name of stock schema instead.
     *
     * @param factory Factory reference to generate schema for.
     * @return Schema in the form of LdifEntry list.
     */
    public ADSComponentSchema generateOrGetSchemaElements( Factory factory );
}
