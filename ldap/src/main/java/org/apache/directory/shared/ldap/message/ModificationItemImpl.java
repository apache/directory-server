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

import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.shared.ldap.util.AttributeUtils;

/**
 *
 * A specific version of this class, which do a transformation of a
 * BasicAttribute to a AttributeImpl when created.
 * 
 * This is necessary because BasicAttribute clone method do not do a
 * deep clone, which is _bad_. AttributeImpl do a deep copy when
 * cloning, which is _good_.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
public class ModificationItemImpl extends ModificationItem
{
    private static final long serialVersionUID = 1L;

    /**
     * Create a modificationItemImpl
     * @param modificationOp The modification operation : on of :
      *  - DirContext.ADD_ATTRIBUTE        
      *  - DirContext.REPLACE_ATTRIBUTE
      *  - DirContext.REMOVE_ATTRIBUTE
     * @param attribte The attribute to add, modify or remove
     */
    public ModificationItemImpl( int modificationOp, Attribute attribute ) 
    {
        super( modificationOp, AttributeUtils.toAttributeImpl( attribute ) );
    }
    
    /**
     * Create a modificationItemImpl from a modificationItem
     * @param modificationOp The modification operation : on of :
     *  - DirContext.ADD_ATTRIBUTE        
     *  - DirContext.REPLACE_ATTRIBUTE
     *  - DirContext.REMOVE_ATTRIBUTE
     * @param attribte The attribute to add, modify or remove
     */
    public ModificationItemImpl( ModificationItem modification ) 
    {
        super( modification.getModificationOp(), 
            AttributeUtils.toAttributeImpl( modification.getAttribute() ) );
    }
    
    /**
     * Create a modificationItemImpl from a modificationItem
     * @param modificationOp The modification operation : on of :
      *  - DirContext.ADD_ATTRIBUTE        
      *  - DirContext.REPLACE_ATTRIBUTE
      *  - DirContext.REMOVE_ATTRIBUTE
     * @param attribte The attribute to add, modify or remove
     */
    public ModificationItemImpl( ModificationItemImpl modification ) 
    {
        super( modification.getModificationOp(), 
            AttributeUtils.toAttributeImpl( modification.getAttribute() ) );
    }
    
    /**
     * @return The modification operation
     */
    public int getModificationOp() 
    {
        return super.getModificationOp();
    }
    
    /**
     * Retrieves the attribute associated with this modification item.
     * 
     * @return The non-null attribute to use for the modification.
     */
    public Attribute getAttribute() 
    {
        return super.getAttribute();
    }
   
    /**
     * @see Object#clone()
     */
    public Object clone() throws CloneNotSupportedException
    {
        return new ModificationItemImpl( getModificationOp(), (Attribute)getAttribute().clone() ); 
    }
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        
        sb.append( "ModificationItem : \n" );
        
        switch ( getModificationOp() )
        {
            case DirContext.ADD_ATTRIBUTE :
                sb.append( "    Add operation, ");
                break;
                
            case DirContext.REPLACE_ATTRIBUTE :
                sb.append( "    Replace operation, ");
                break;
                
            case DirContext.REMOVE_ATTRIBUTE :
                sb.append( "    Remove operation, ");
                break;
                
            default :
                sb.append( "    Unknown operation, ");
                break;
        }

        sb.append( AttributeUtils.toString( "    ", getAttribute() ) );
        
        return sb.toString();
    }
}
