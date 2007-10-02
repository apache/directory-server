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
package org.apache.directory.shared.ldap.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * An implementation of an Entry. Compared to the javax.naming.Attributes, 
 * we store the entry DN into this element, to avoid a DN parsing when
 * retrieving an entry from the backend.
 * 
 * This class is <b>not</b> thread safe ! Manipulating entry's attributes in 
 * two threads may lead to inconsistancies
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerEntryImpl implements ServerEntry
{
    /** The entry's DN */
    private LdapDN dn;
    
    /** 
     * An Hashed structure to store a link between attribute OID and
     * the ServerAttribute. It is used for increasing performances
     */
    private transient Map<OID, ServerAttribute> hashAttrs;
    
    /** The attribute list. */
    private List<ServerAttribute> attributes;
    
    /**
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    static final long serialVersionUID = 2L;

    
    /**
     * 
     * Creates a new instance of ServerEntryImpl.
     *
     */
    public ServerEntryImpl()
    {
        attributes = new ArrayList<ServerAttribute>();
        hashAttrs = new HashMap<OID, ServerAttribute>();
    }

    
    /**
     * 
     * Creates a new instance of ServerEntryImpl.
     *
     */
    public ServerEntryImpl( LdapDN dn)
    {
        attributes = new ArrayList<ServerAttribute>();
        hashAttrs = new HashMap<OID, ServerAttribute>();
        this.dn = dn;
    }

    
    /**
     * Removes all the attributes.
     */
    public void clear()
    {
        if ( attributes != null )
        {
            attributes.clear();
            hashAttrs.clear();
        }
    }


    /**
     * Returns a deep copy of this <code>Attributes</code> instance. The
     * attribute objects <b>are</b> cloned.
     * 
     * @return a deep copy of this <code>Attributes</code> instance
     */
    public ServerEntry clone()
    {
        try
        {
            ServerEntryImpl clone = (ServerEntryImpl)super.clone();
            
            clone.dn = (LdapDN)dn.clone();
            
            if ( attributes != null )
            {
                clone.attributes = new ArrayList<ServerAttribute>( attributes.size() );
                clone.hashAttrs = new HashMap<OID, ServerAttribute>( attributes.size() );
            
                for ( ServerAttribute attr:attributes )
                {
                    ServerAttribute clonedAttr = (ServerAttribute)attr.clone();
                    clone.attributes.add( attr );
                    clone.hashAttrs.put( attr.getOid(), clonedAttr );
                }
            }
            
            return clone;
        }
        catch ( CloneNotSupportedException cnse )
        {
            // I see no reason why it should ever happen...
            return null;
        }
    }


    /**
     * Places a non-null attribute in the attribute collection. If there is
     * already an attribute with the same OID as the new attribute, the old one
     * is removed from the collection and is returned by this method. If there
     * was no attribute with the same OID the return value is <code>null</code>.
     * 
     * This method clone the attribute.
     * 
     * @param attribute the attribute to be copied
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     */
    public ServerAttribute copy( ServerAttribute attr )
    {
        if ( attr == null )
        {
            return null;
        }
        
        return put( attr.clone() );
    }
    
    
    /**
     * Returns the attribute with the specified OID. The return value
     * is <code>null</code> if no match is found.
     * 
     * @param oid attribute OID
     * @return the attribute with the specified OID
     */
    public ServerAttribute get( OID oid )
    {
        assert( oid != null );
        
        return hashAttrs.get( oid );
    }


    /**
     * Returns an iterator containing the zero or more attributes in the
     * collection. The behaviour of the iterator is not specified if the
     * attribute collection is changed.
     * 
     * @return an iterator of all contained attributes
     */
    public Iterator<ServerAttribute> getAll()
    {
        return new ServerAttributeIterator();
    }

    
    /**
     * Get this entry's DN.
     *
     * @return The entry DN
     */
    public LdapDN getDn()
    {
        return dn;
    }
    
    
    /**
     * Set this entry's DN.
     * 
     * @param dn The LdapdN associated with this entry
     */
    public void setDn( LdapDN dn)
    {
        this.dn = dn;
    }
    

    /**
     * An inner class used to iterate through OIDs.
=    */
    private class OidIterator implements Iterator<OID>
    {
        /** The iterator */
        private Iterator<ServerAttribute> iterator;
        
        /** The current position */
        int index;
        
        /** The max number of elements */
        int max;
        
        /**
         * Creates a new instance of OidIterator.
         */
        private OidIterator()
        {
            iterator = attributes.iterator();
            
            index = 0;

            if ( ( attributes != null ) && ( attributes.size() != 0 ) )
            {
                max = 0;
            }
            else
            {
                max = attributes.size();
            }
        }
    
        /**
         * Not supported
         */
        public void remove()
        {
            throw new UnsupportedOperationException( "Remove is not supported" );
        }
        
        /**
         * Tells if there is a next element in the list
         */
        public boolean hasNext()
        {
            return index < max;
        }
        
        /**
         * Returns the next OID in the list
         */
        public OID next()
        {
            if ( index >= max )
            {
                return null;
            }
            
            index++;
            ServerAttribute current = iterator.next();
             
            return current.getOid();
        }
    }    

    
    /**
     * An inner class used to iterate through ServerAttributes.
=    */
    private class ServerAttributeIterator implements Iterator<ServerAttribute>
    {
        /** The iterator */
        private Iterator<ServerAttribute> iterator;
        
        /** The current position */
        int index;
        
        /** The max number of elements */
        int max;
        
        /**
         * Creates a new instance of ServerAttributeIterator.
         */
        private ServerAttributeIterator()
        {
            iterator = attributes.iterator();
            
            index = 0;

            if ( ( attributes != null ) && ( attributes.size() != 0 ) )
            {
                max = 0;
            }
            else
            {
                max = attributes.size();
            }
        }
    
        /**
         * Not supported
         */
        public void remove()
        {
            throw new UnsupportedOperationException( "Remove is not supported" );
        }
        
        /**
         * Tells if there is a next element in the list
         */
        public boolean hasNext()
        {
            return index < max;
        }
        
        /**
         * Returns the next OID in the list
         */
        public ServerAttribute next()
        {
            if ( index >= max )
            {
                return null;
            }
            
            index++;
            ServerAttribute current = iterator.next();
             
            return current;
        }
    }    

    
    /**
     * Returns an iterator containing zero or more OIDs of the
     * attributes in the collection. The behaviour of the iterator is not
     * specified if the attribute collection is changed.
     * 
     * @return an iterator of the OIDs of all contained attributes
     */
    public Iterator<OID> getOids()
    {
        return new OidIterator();
    }
    
    
    /**
     * Places a non-null attribute in the attribute collection. If there is
     * already an attribute with the same OID as the new attribute, the old one
     * is removed from the collection and is returned by this method. If there
     * was no attribute with the same OID the return value is <code>null</code>.
     * 
     * @param attribute the attribute to be put
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     */
    public ServerAttribute put( ServerAttribute attr )
    {
        // First, some defensive code
        if ( attr == null )
        {
            return null;
        }
        
        if( attr.getOid() == null )
        {
            return null;
        }
        
        // Get the previous attribute, if any
        ServerAttribute replacedAttribute = hashAttrs.put( attr.getOid(), attr );
        
        if ( replacedAttribute != null )
        {
            // Remove the previous attribute from the list
            attributes.remove( replacedAttribute );
        }
        
        // Store the new attribute at the end of the list 
        attributes.add( attr );
        
        return replacedAttribute;
    }
    
    
    /**
     * Places a new attribute with the supplied OID and value into the attribute
     * collection. If there is already an attribute with the same OID, the old
     * one is removed from the collection and is returned by this method. If
     * there was no attribute with the same OID the return value is
     * <code>null</code>.
     * 
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value of <code>obj</code> may be
     * <code>null</code>.
     * 
     * @param oid the OID of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException If the oid is null.
     */
    public ServerAttribute put( OID oid, Value<?> val ) throws NamingException
    {
        return put( new ServerAttributeImpl( oid, val ) );
    }
    

    /**
     * Places a new attribute with the supplied OID and value into the attribute
     * collection. If there is already an attribute with the same OID, the old
     * one is removed from the collection and is returned by this method. If
     * there was no attribute with the same OID the return value is
     * <code>null</code>.
     * 
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value of <code>obj</code> may be
     * <code>null</code>.
     * 
     * @param oid the OID of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException If the oid is null.
     */
    public ServerAttribute put( OID oid, String val ) throws NamingException
    {
        return put( new ServerAttributeImpl( oid, val ) );
    }
    
    

    /**
     * Places a new attribute with the supplied OID and value into the attribute
     * collection. If there is already an attribute with the same OID, the old
     * one is removed from the collection and is returned by this method. If
     * there was no attribute with the same OID the return value is
     * <code>null</code>.
     * 
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value of <code>obj</code> may be
     * <code>null</code>.
     * 
     * @param oid the OID of the new attribute to be put
     * @param val the value of the new attribute to be put
     * @return the old attribute with the same OID, if exists; otherwise
     *         <code>null</code>
     * @throws NamingException If the oid is null.
     */
    public ServerAttribute put( OID oid, byte[] val ) throws NamingException
    {
        if ( oid == null )
        {
            // We can't add a new ServerAttribute with no OID
            throw new NamingException( "Attributes with an empty ID or OID are not allowed" );
        }
        
        // Remove any previoulsy existing ServerAttribute with the
        // same OID
        ServerAttribute oldAttr = hashAttrs.get( oid );
        
        if ( oldAttr != null )
        {
            attributes.remove( oid );
            hashAttrs.remove( oid );
        }
        
        // Create a new ServerAttribute
        ServerAttribute attribute = new ServerAttributeImpl( oid, val );
        
        attributes.add( attribute );
        hashAttrs.put(  oid, attribute );
        
        return oldAttr;
    }
    
    /**
     * Removes the attribute with the specified OID. The removed attribute is
     * returned by this method. If there is no attribute with the specified OID,
     * the return value is <code>null</code>.
     * 
     * @param oid the OID of the attribute to be removed
     * @return the removed attribute, if exists; otherwise <code>null</code>
     */
     public ServerAttribute remove( OID oid )
     {
         // First, some defensive code
         if ( oid == null )
         {
             return null;
         }
         
         // Get the previous attribute, if any
         ServerAttribute attribute = hashAttrs.get( oid );
         
         if ( attribute != null )
         {
             hashAttrs.remove( oid );
             attributes.remove( attribute );
         }
         
         return attribute;
     }


     /**
      * Removes the attribute with the specified OID. The removed attribute is
      * returned by this method. If there is no attribute with the specified OID,
      * the return value is <code>null</code>.
      * 
      * @param attribute the attribute to be removed
      * @return the removed attribute, if exists; otherwise <code>null</code>
      */
      public ServerAttribute remove( ServerAttribute attribute )
      {
          // First, some defensive code
          if ( attribute == null )
          {
              return null;
          }
          
          OID oid = attribute.getOid();

          if ( oid == null )
          {
              return null;
          }
          
          // Get the previous attribute, if any
          ServerAttribute existingAttr = hashAttrs.get( oid );
          
          if ( existingAttr != null )
          {
              hashAttrs.remove( oid );
              attributes.remove( existingAttr );
          }
          
          return existingAttr;
      }


     /**
      * Returns the number of attributes.
      * 
      * @return the number of attributes
      */
     public int size()
     {
         if ( attributes == null )
         {
             return 0;
         }
         else
         {
             return attributes.size();
         }
     }
}
