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
package org.apache.directory.server.core.entry;

import java.io.Serializable;

import javax.naming.directory.DirContext;

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;

/**
 * An internal implementation for a ModificationItem. The name has been
 * chosen so that it does not conflict with @see ModificationItem
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerModification implements Modification, Serializable
{
    public static final long serialVersionUID = 1L;
    
    /** The modification operation */
    private ModificationOperation operation;
    
    /** The attribute which contains the modification */
    private EntryAttribute<? extends Value<?>> attribute;
 
    
    /**
     *  @return the operation
     */
    public ModificationOperation getOperation()
    {
        return operation;
    }
    
    
    /**
     * Store the modification operation
     *
     * @param operation The DirContext value to assign
     */
    public void setOperation( int operation )
    {
        switch ( operation )
        {
            case DirContext.ADD_ATTRIBUTE :
                this.operation = ModificationOperation.ADD_ATTRIBUTE;

            case DirContext.REPLACE_ATTRIBUTE :
                this.operation = ModificationOperation.REPLACE_ATTRIBUTE;
            
            case DirContext.REMOVE_ATTRIBUTE :
                this.operation = ModificationOperation.REMOVE_ATTRIBUTE;
        }
    }

    
    /**
     * Store the modification operation
     *
     * @param operation The DirContext value to assign
     */
    public void setOperation( ModificationOperation operation )
    {
        this.operation = operation;
    }
        
    
    /**
     * @return the attribute containing the modifications
     */
    public EntryAttribute<? extends Value<?>> getAttribute()
    {
        return attribute;
    }
    
    
    /**
     * Set the attribute's modification
     *
     * @param attribute The modified attribute 
     */
    public void setAttribute( EntryAttribute<? extends Value<?>> attribute )
    {
        this.attribute = attribute;
    }
    
    
    /**
     * Compute the modification @see Object#hashCode
     */
    public int hashCode()
    {
        int h = 37;
        
        h += h*17 + operation.getValue();
        h += h*17 + attribute.hashCode();
        
        return h;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "Modification: " ).
            append( operation ).
            append( "\n" ).
            append( ", attribute : " ).
            append( attribute );
        
        return sb.toString();
    }
}
