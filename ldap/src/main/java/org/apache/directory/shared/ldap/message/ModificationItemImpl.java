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
    
    /** A flag set when the server has created this item */
    private boolean serverModified;

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
        
        serverModified = false;
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
        
        serverModified = false;
    }
    
    /**
     * Create a modificationItemImpl from another one.
     * @param modification item to be copied.
     */
    public ModificationItemImpl( ModificationItemImpl modification ) 
    {
        super( modification.getModificationOp(), 
            AttributeUtils.toAttributeImpl( modification.getAttribute() ) );
        
        serverModified = false;
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
     * 
     * Tells if this modification has been created by the server or not
     *
     * @return <code>true</code> if the server has created this modifictionItem
     */
    public boolean isServerModified()
    {
        return serverModified;
    }

    
    /**
     * Set the serverModified item
     *
     * @param serverModified 
     */
    public void setServerModified()
    {
        serverModified = true;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        
        sb.append( "ModificationItem" );
        
        if ( serverModified )
        {
            sb.append( "[op]" );
        }
        
        sb.append( " : \n" );
        
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
    
    
    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        int hash = 37;
        
        hash += hash*17 + getModificationOp();
        hash += hash*17 + getAttribute().hashCode();
        return hash;
    }
    
    
    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object o )
    {
        // Basic equals checks
        if ( this == o )
        {
            return true;
        }
        
        if ( o == null )
        {
            return false;
        }
        
        if ( ! (o instanceof ModificationItemImpl ) )
        {
            return false;
        }
        
        ModificationItemImpl mod = (ModificationItemImpl)o;
        
        // Now, compares the modification content
        // First, the modification type
        if ( this.getModificationOp() != mod.getModificationOp() )
        {
            return false;
        }
        
        // then the attribute
        return this.getAttribute().equals( mod.getAttribute() );
    }
}
