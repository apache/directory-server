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
package org.apache.directory.server.core.entry;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * A ServerEntry refers to the original entry before being modified by 
 * EntryFilters or operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ClonedServerEntry implements Entry
{
    /** The original entry as returned by the backend */
    protected Entry originalEntry;
    
    /** The copied entry */
    protected Entry clonedEntry;

    /**
     * Creates a new instance of ClonedServerEntry.
     */
    public ClonedServerEntry()
    {
    }

    
    /**
     * Creates a new instance of ClonedServerEntry.
     * 
     * The original entry is cloned in order to protect its content.
     *
     * @param originalEntry The original entry
     */
    public ClonedServerEntry( Entry originalEntry )
    {
        this.originalEntry = originalEntry;
        this.clonedEntry = ( Entry ) originalEntry.clone();
    }
    
    
    /**
     * @return the originalEntry
     */
    public Entry getOriginalEntry()
    {
        return originalEntry;
    }


    /**
     * @return the cloned Entry
     */
    public Entry getClonedEntry()
    {
        return clonedEntry;
    }


    public void add( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        clonedEntry.add( attributeType, values );
    }


    public void add( AttributeType attributeType, String... values ) throws LdapException
    {
        clonedEntry.add( attributeType, values );
    }


    public void add( AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        clonedEntry.add( attributeType, values );
    }


    public void add( String upId, AttributeType attributeType, byte[]... values ) throws LdapException
    {
        clonedEntry.add( attributeType, values );
    }


    public void add( String upId, AttributeType attributeType, String... values ) throws LdapException
    {
        clonedEntry.add( attributeType, values );
    }


    public void add( String upId, AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        clonedEntry.add( attributeType, values );
    }


    public boolean contains( AttributeType attributeType, byte[]... values )
    {
        return clonedEntry.contains( attributeType, values );
    }


    public boolean contains( AttributeType attributeType, String... values )
    {
        return clonedEntry.contains( attributeType, values );
    }


    public boolean contains( AttributeType attributeType, Value<?>... values )
    {
        return clonedEntry.contains( attributeType, values );
    }


    public boolean containsAttribute( AttributeType attributeType )
    {
        return clonedEntry.containsAttribute( attributeType );
    }


    public EntryAttribute get( AttributeType attributeType )
    {
        return clonedEntry.get( attributeType );
    }


    public Set<AttributeType> getAttributeTypes()
    {
        return clonedEntry.getAttributeTypes();
    }


    public boolean hasObjectClass( EntryAttribute objectClass )
    {
        return clonedEntry.hasObjectClass( objectClass );
    }


    public boolean isValid()
    {
        return clonedEntry.isValid();
    }


    public boolean isValid( String objectClass )
    {
        return clonedEntry.isValid( objectClass );
    }


    public boolean isValid( EntryAttribute objectClass )
    {
        return clonedEntry.isValid( objectClass );
    }


    public EntryAttribute put( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        return clonedEntry.put( attributeType, values );
    }


    public EntryAttribute put( AttributeType attributeType, String... values ) throws LdapException
    {
        return clonedEntry.put( attributeType, values );
    }


    public EntryAttribute put( AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        return clonedEntry.put( attributeType, values );
    }


    public EntryAttribute put( String upId, AttributeType attributeType, byte[]... values ) throws LdapException
    {
        return clonedEntry.put( attributeType, values );
    }


    public EntryAttribute put( String upId, AttributeType attributeType, String... values ) throws LdapException
    {
        return clonedEntry.put( upId, attributeType, values );
    }


    public EntryAttribute put( String upId, AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        return clonedEntry.put( upId, attributeType, values );
    }


    public boolean remove( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        return clonedEntry.remove( attributeType, values );
    }


    public boolean remove( AttributeType attributeType, String... values ) throws LdapException
    {
        return clonedEntry.remove( attributeType, values );
    }


    public boolean remove( AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        return clonedEntry.remove( attributeType, values );
    }


    public List<EntryAttribute> remove( EntryAttribute... attributes ) throws LdapException
    {
        return clonedEntry.remove( attributes );
    }


    public List<EntryAttribute> removeAttributes( AttributeType... attributes )
    {
        return clonedEntry.removeAttributes( attributes );
    }


    public List<EntryAttribute> set( AttributeType... attributeTypes )
    {
        return clonedEntry.set( attributeTypes );
    }


    public void add( EntryAttribute... attributes ) throws LdapException
    {
        clonedEntry.add( attributes );
    }


    public void add( String upId, String... values ) throws LdapException
    {
        clonedEntry.add( upId, values );
    }


    public void add( String upId, byte[]... values ) throws LdapException
    {
        clonedEntry.add( upId, values );
    }


    public void add( String upId, Value<?>... values ) throws LdapException
    {
        clonedEntry.add( upId, values );
    }


    public void clear()
    {
        clonedEntry.clear();
    }


    public boolean contains( EntryAttribute... attributes ) throws LdapException
    {
        return clonedEntry.contains( attributes );
    }


    public boolean contains( String upId, byte[]... values )
    {
        return clonedEntry.contains( upId, values );
    }


    public boolean contains( String upId, String... values )
    {
        return clonedEntry.contains( upId, values );
    }


    public boolean contains( String upId, Value<?>... values )
    {
        return clonedEntry.contains( upId, values );
    }


    public boolean containsAttribute( String... attributes )
    {
        return clonedEntry.containsAttribute( attributes );
    }


    public EntryAttribute get( String alias )
    {
        return clonedEntry.get( alias );
    }


    public Dn getDn()
    {
        return clonedEntry.getDn();
    }


    public boolean hasObjectClass( String objectClass )
    {
        return clonedEntry.hasObjectClass( objectClass );
    }


    public Iterator<EntryAttribute> iterator()
    {
        return clonedEntry.iterator();
    }


    public List<EntryAttribute> put( EntryAttribute... attributes ) throws LdapException
    {
        return clonedEntry.put( attributes );
    }


    public EntryAttribute put( String upId, byte[]... values )
    {
        return clonedEntry.put( upId, values );
    }


    public EntryAttribute put( String upId, String... values )
    {
        return clonedEntry.put( upId, values );
    }


    public EntryAttribute put( String upId, Value<?>... values )
    {
        return clonedEntry.put( upId, values );
    }


    public boolean remove( String upId, byte[]... values ) throws LdapException
    {
        return clonedEntry.remove( upId, values );
    }


    public boolean remove( String upId, String... values ) throws LdapException
    {
        return clonedEntry.remove( upId, values );
    }


    public boolean remove( String upId, Value<?>... values ) throws LdapException
    {
        return clonedEntry.remove( upId, values );
    }


    public List<EntryAttribute> removeAttributes( String... attributes )
    {
        return clonedEntry.removeAttributes( attributes );
    }


    public List<EntryAttribute> set( String... upIds )
    {
        return clonedEntry.set( upIds );
    }


    public void setDn( Dn dn )
    {
        clonedEntry.setDn( dn );
    }


    public int size()
    {
        return clonedEntry.size();
    }


    public Entry toClientEntry() throws LdapException
    {
        // Copy the Dn
        Entry clientEntry = new DefaultEntry( clonedEntry.getDn() );
        
        // Convert each attribute 
        for ( EntryAttribute clonedEntry:this )
        {
            EntryAttribute clientAttribute = clonedEntry.clone();
            clientEntry.add( clientAttribute );
        }
        
        return clientEntry;
    }
    
    
    /**
     * @see java.io.Externalizable#readExternal(ObjectInput)
     * 
     * We can't use this method for a ServerEntry
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        throw new IllegalStateException( I18n.err( I18n.ERR_455 ) );
    }
    
    
    /**
     * @see java.io.Externalizable#writeExternal(ObjectOutput)
     * 
     * We can't use this method for a ServerEntry
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        throw new IllegalStateException( I18n.err( I18n.ERR_456 ) );
    }
    
    
    public Entry clone()
    {
        return ( Entry ) clonedEntry.clone();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return 703;
    }

    /**
     * @see Object#equals(Object);
     */
    @Override
    public boolean equals( Object obj )
    {
        // Short circuit
        if ( this == obj )
        {
            return true;
        }
        
        Entry other;
        
        if ( obj instanceof ClonedServerEntry )
        {
            other = ((ClonedServerEntry)obj).getClonedEntry();
        }
        else if ( obj instanceof Entry )
        {
            other = (Entry)obj;
        }
        else 
        {
            return false;
        }
        if ( clonedEntry == null)
        {
            return other == null;
        }
        else
        {
            return clonedEntry.equals( other );
        }
    }
    
    
    public String toString()
    {
        return clonedEntry.toString();
    }

    
    public boolean contains( String upId ) throws LdapException
    {
        return clonedEntry.contains( upId );
    }
}
