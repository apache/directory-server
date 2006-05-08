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

package org.apache.directory.shared.ldap.ldif;


import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.Control;


/**
 * A entry to be populated by an ldif parser.
 * 
 * We will have different kind of entries :
 *  - added entries
 *  - deleted entries
 *  - modified entries
 *  - RDN modified entries
 *  - DN modified entries
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Entry
{
    /** the version of the ldif */
    private int version;

    /** the change type */
    private int changeType;

    /** the modification item list */
    private List modificationList;
    private Map modificationItems;

    /** the dn of the ldif entry */
    private String dn;
    
    /** The new superior */
    private String newSuperior;
    
    /** The new rdn */
    private String newRdn;
    
    /** The delete old rdn flag */
    private boolean deleteOldRdn;

    /** attributes of the entry */
    private BasicAttributes attributeList;
    
    /** The possible change types */
    public final static int ADD = 0;
    public final static int MODIFY = 1;
    public final static int MODDN = 2;
    public final static int MODRDN = 3;
    public final static int DELETE = 4;
    
    /** The control */
    private Control control;
    
    /**
     * Creates a new Entry object.
     */
    public Entry()
    {
    	changeType = ADD; // Default LDIF content
    	modificationList = new LinkedList();
    	modificationItems = new HashMap();
        dn = null;
        attributeList = new BasicAttributes( true );
        version = 1; // default version in ldif
        control = null;
    }

    /**
     * Sets the version of this ldif
     * 
     * @param version The version of this ldif
     */
    public void setVersion( int version )
    {
        this.version = version;
    }

    /**
     * Set the Distinguished Name 
     * 
     * @param dn The Distinguished Name
     */
    public void setDn( String dn )
    {
        this.dn = dn;
    }

    /**
     * Set the modification type
     * 
     * @param changeType The change type
     * 
     */
    public void setChangeType( int changeType )
    {
        this.changeType = changeType;
    }

    /**
     * Set the change type
     * 
     * @param changeType The change type
     */
    public void setChangeType( String changeType )
    {
    	if ( "add".equals( changeType ) )
    	{
            this.changeType = ADD;
    	} 
    	else if ( "modify".equals( changeType ) )
    	{
            this.changeType = MODIFY;
    	}
    	else if ( "moddn".equals( changeType ) )
    	{
            this.changeType = MODDN;
    	}
    	else if ( "modrdn".equals( changeType ) )
    	{
            this.changeType = MODRDN;
    	}
    	else if ( "delete".equals( changeType ) )
    	{
            this.changeType = DELETE;
    	} 
    }

    /**
     * Add a modification item (used by modify operations)
     * 
     * @param ModificationItem The modification to be added
     */
    public void addModificationItem( ModificationItem modification )
    {
    	if ( changeType == MODIFY )
    	{
    		modificationList.add( modification );
    		modificationItems.put( modification.getAttribute().getID(), modification );
    	}
    }

    /**
     * Add a modification item (used by modify operations)
     * 
     * @param modOp The operation. One of : 
     * 	DirContext.ADD_ATTRIBUTE
     *  DirContext.REMOVE_ATTRIBUTE
     *  DirContext.REPLACE_ATTRIBUTE
     *  
     * @param attr The attribute to be added
     */
    public void addModificationItem( int modOp, Attribute attr ) throws NamingException
    {
    	if ( changeType == MODIFY )
    	{
    		if ( modificationItems.containsKey( attr.getID() ) )
    		{
    			ModificationItem item = (ModificationItem)modificationItems.get( attr.getID() );
    			Attribute attribute = item.getAttribute();
    			
    			Enumeration attrs = attr.getAll();
    			
    			while ( attrs.hasMoreElements() )
    			{
    				attribute.add( attrs.nextElement() );
    			}
    		}
    		else
    		{
		    	ModificationItem item = new ModificationItem( modOp, attr );
		    	modificationList.add( item );
	    		modificationItems.put( attr.getID(), item );
    		}
    	}
    }

    /**
     * Add a modification item
     * 
     * @param modOp The operation. One of : 
     * 	DirContext.ADD_ATTRIBUTE
     *  DirContext.REMOVE_ATTRIBUTE
     *  DirContext.REPLACE_ATTRIBUTE
     *            
     * @param id The attribute's ID
     * @param value The attribute's value
     */
    public void addModificationItem( int modOp, String id, Object value )
    {
    	if ( changeType == MODIFY )
    	{
	        BasicAttribute attr = new BasicAttribute( id, value );
	        
    		if ( modificationItems.containsKey( id ) )
    		{
    			ModificationItem item = (ModificationItem)modificationItems.get( id );
    			Attribute attribute = item.getAttribute();
    			
   				attribute.add( value );
    		}
    		else
    		{
		        ModificationItem item = new ModificationItem( modOp, attr );
		        modificationList.add( item );
		        modificationItems.put( id, item );
    		}
    	}    
    }

    /**
     * Add an attribute to the entry
     * 
     * @param attr The attribute to be added
     */
    public void addAttribute( Attribute attr )
    {
        attributeList.put( attr );
    }

    /**
     * Add an attribute to the entry
     * 
     * @param id The attribute ID
     *            
     * @param value The attribute value
     *            
     */
    public void addAttribute( String id, Object value )
    {
		Attribute attr = get( id );
		
		if ( attr != null )
		{
			attr.add( value );
		}
		else
		{
			attributeList.put( id, value );
		}
    }

    /**
     * Add an attribute value to an existing attribute
     * 
     * @param id The attribute ID
     *            
     * @param value The attribute value
     *            
     */
    public void putAttribute( String id, Object value )
    {
        Attribute attribute = attributeList.get( id );
        
        if ( attribute != null )
        {
        	attribute.add( value );
        }
        else
        {
        	attributeList.put( id, value );
        }
    }

    /**
     * Get the change type
     * 
     * @return The change type. One of : 
     * 	ADD = 0;
     *  MODIFY = 1;
     *  MODDN = 2;
     *  MODRDN = 3;
     *  DELETE = 4;
     */
    public int getChangeType()
    {
        return changeType;
    }

    /**
     * @return The list of modification items
     */
    public List getModificationItems()
    {
        return modificationList;
    }

    /**
     * @return The entry Distinguished name
     */
    public String getDn()
    {
        return dn;
    }

    /**
     * @return The ldif file version
     */
    public int getVersion()
    {
        return this.version;
    }
    
    /**
     * @return The number of entry modifications
     */
    public int size()
    {
    	return modificationList.size();
    }
    
    /**
     * Returns a attribute given it's id
     * 
     * @param attributeId The attribute Id
     * @return The attribute if it exists
     */
    public Attribute get( String attributeId )
    {
    	if ( "dn".equalsIgnoreCase( attributeId ) )
    	{
    		return new BasicAttribute( "dn", dn );
    	}
    	
  		return attributeList.get( attributeId );
    }

    /**
     * @return True, if the old RDN should be deleted.
     */
	public boolean isDeleteOldRdn() 
	{
		return deleteOldRdn;
	}

	/**
	 * Set the flage deleteOldRdn
	 * @param deleteOldRdn True if the old RDN should be deleted
	 */
	public void setDeleteOldRdn( boolean deleteOldRdn ) 
	{
		this.deleteOldRdn = deleteOldRdn;
	}

	/**
	 * @return The new RDN
	 */
	public String getNewRdn() 
	{
		return newRdn;
	}

	/**
	 * Set the new RDN
	 * @param newRdn The new RDN
	 */
	public void setNewRdn( String newRdn ) 
	{
		this.newRdn = newRdn;
	}

	/**
	 * @return The new superior
	 */
	public String getNewSuperior() 
	{
		return newSuperior;
	}

	/**
	 * Set the new superior
	 * @param newSuperior The new Superior
	 */
	public void setNewSuperior( String newSuperior ) 
	{
		this.newSuperior = newSuperior;
	}
	
	/**
	 * @return True if the entry is an ADD entry
	 */
	public boolean isChangeAdd()
	{
		return changeType == ADD;
	}

	/**
	 * @return True if the entry is a DELETE entry
	 */
	public boolean isChangeDelete()
	{
		return changeType == DELETE;
	}

	/**
	 * @return True if the entry is a MODDN entry
	 */
	public boolean isChangeModDn()
	{
		return changeType == MODDN;
	}

	/**
	 * @return True if the entry is a MODRDN entry
	 */
	public boolean isChangeModRdn()
	{
		return changeType == MODRDN;
	}

	/**
	 * @return True if the entry is a MODIFY entry
	 */
	public boolean isChangeModify()
	{
		return changeType == MODIFY;
	}

	/**
	 * @return The associated control, if any
	 */
	public Control getControl() 
	{
		return control;
	}

	/**
	 * Add a control to the entry 
	 * @param control The control
	 */
	public void setControl( Control control ) 
	{
		this.control = control;
	}
}
