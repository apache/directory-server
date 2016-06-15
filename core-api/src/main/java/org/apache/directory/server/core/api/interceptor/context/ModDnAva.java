/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.api.interceptor.context;

import org.apache.directory.api.ldap.model.name.Ava;

/**
 * A class used to hold the RDN changed during a Rename or a MoveAndRename operation.
 * We store and AVA that is part of the oldRDN or the new RDN in an instance of this class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModDnAva
{
    /** The type of modification */
    public enum ModDnType
    {
        ADD,            // for added AttributeTypes 
        UPDATE_ADD,     // for attributeTypes present in both Rdn but with a different value 
        UPDATE_DELETE,  // for attributeTypes present in 
        DELETE          // for deleted attributeTypes
    }
    
    /** The type of modification applied on this AVA */
    private ModDnType type;
    
    /** The added or removed Ava */
    private Ava ava;

    /**
     * Creates a new instance of a ModDnAva
     * @param type The type of modification
     * @param ava The AVA to store
     */
    public ModDnAva( ModDnType type, Ava ava )
    {
        this.type = type;
        this.ava = ava;
    }
    
    
    /**
     * @return the ava
     */
    public Ava getAva()
    {
        return ava;
    }
    
    
    /**
     * @return the modification type
     */
    public ModDnType getType()
    {
        return type;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( '<' ).append( type ).append( ',' ).append( ava ).append( '>' );
        
        return sb.toString();
    }
}
