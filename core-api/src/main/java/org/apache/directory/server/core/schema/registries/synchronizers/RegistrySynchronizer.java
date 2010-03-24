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
package org.apache.directory.server.core.schema.registries.synchronizers;


import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;


/**
 * Interface used to detect and react to changes performed on schema entities
 * to update registries so they're synchronized with entries on disk.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface RegistrySynchronizer
{
    /** A constant to tell the caller that the schema has been modified */
    static final boolean SCHEMA_MODIFIED = true;

    /** A constant to tell the caller that the schema has not been modified */
    static final boolean SCHEMA_UNCHANGED = false;
    
    
    /**
     * Adds a new SchemaObject to its registry
     *
     * @param entry The SchemObject to add
     * @throws Exception If the addition failed
     */
    void add( ServerEntry entry ) throws Exception;
    
    
    /**
     * Delete the schema object and update the registries
     *
     * @param entry The entry associated with the SchemaObject to delete
     * @param cascaded unused
     * @throws Exception If the deletion failed
     */
    void delete( ServerEntry entry, boolean cascaded ) throws Exception;
    
    
    /**
     * Rename a schemaObject. It is not supposed to have any child
     *
     * @param entry The entry to be renamed
     * @param newRdn The new entry name
     * @param cascaded unused
     * @throws Exception If the rename failed
     */
    void rename( ServerEntry entry, RDN newRdn, boolean cascaded ) throws Exception;
    

    /**
     * Applies a set of modification to an entry
     *
     * @param opContext The OperationContext, which contains the entry and the modifications to apply
     * @param targetEntry The modified entry
     * @param cascaded Unused
     * @return True if the modification has been done
     * @throws Exception If the modification failed
     */
    boolean modify( ModifyOperationContext opContext, ServerEntry targetEntry, boolean cascaded )
        throws Exception;
    
    void moveAndRename( DN oriChildName, DN newParentName, RDN newRn, boolean deleteOldRn, ServerEntry entry,
        boolean cascaded ) throws Exception;
    
    void move( DN oriChildName, DN newParentName, ServerEntry entry, boolean cascaded ) throws Exception;
}
