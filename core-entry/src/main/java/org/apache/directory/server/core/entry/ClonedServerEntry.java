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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * A ServerEntry refers to the original entry before being modified by 
 * EntryFilters or operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ClonedServerEntry implements ServerEntry
{
    /** The original entry as returned by the backend */
    private final ServerEntry originalEntry;
    
    /** The copied entry */
    private final ServerEntry clonedEntry;

    
    /**
     * Creates a new instance of ClonedServerEntry.
     * 
     * The original entry is cloned in order to protect its content.
     *
     * @param originalEntry The original entry
     */
    public ClonedServerEntry( ServerEntry originalEntry )
    {
        this.originalEntry = originalEntry;
        this.clonedEntry = ( ServerEntry ) originalEntry.clone();
    }
    
    
    /**
     * @return the originalEntry
     */
    public ServerEntry getOriginalEntry()
    {
        return originalEntry;
    }


    public void add( AttributeType attributeType, byte[]... values ) throws NamingException
    {
        clonedEntry.add( attributeType, values );
    }


    public void add( AttributeType attributeType, String... values ) throws NamingException
    {
        clonedEntry.add( attributeType, values );
    }


    public void add( AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        clonedEntry.add( attributeType, values );
    }


    public void add( String upId, AttributeType attributeType, byte[]... values ) throws NamingException
    {
        clonedEntry.add( attributeType, values );
    }


    public void add( String upId, AttributeType attributeType, String... values ) throws NamingException
    {
        clonedEntry.add( attributeType, values );
    }


    public void add( String upId, AttributeType attributeType, Value<?>... values ) throws NamingException
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


    public EntryAttribute put( AttributeType attributeType, byte[]... values ) throws NamingException
    {
        return clonedEntry.put( attributeType, values );
    }


    public EntryAttribute put( AttributeType attributeType, String... values ) throws NamingException
    {
        return clonedEntry.put( attributeType, values );
    }


    public EntryAttribute put( AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        return clonedEntry.put( attributeType, values );
    }


    public EntryAttribute put( String upId, AttributeType attributeType, byte[]... values ) throws NamingException
    {
        return clonedEntry.put( attributeType, values );
    }


    public EntryAttribute put( String upId, AttributeType attributeType, String... values ) throws NamingException
    {
        return clonedEntry.put( upId, attributeType, values );
    }


    public EntryAttribute put( String upId, AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        return clonedEntry.put( upId, attributeType, values );
    }


    public boolean remove( AttributeType attributeType, byte[]... values ) throws NamingException
    {
        return clonedEntry.remove( attributeType, values );
    }


    public boolean remove( AttributeType attributeType, String... values ) throws NamingException
    {
        return clonedEntry.remove( attributeType, values );
    }


    public boolean remove( AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        return clonedEntry.remove( attributeType, values );
    }


    public List<EntryAttribute> remove( EntryAttribute... attributes ) throws NamingException
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


    public void add( EntryAttribute... attributes ) throws NamingException
    {
        clonedEntry.add( attributes );
    }


    public void add( String upId, String... values ) throws NamingException
    {
        clonedEntry.add( upId, values );
    }


    public void add( String upId, byte[]... values ) throws NamingException
    {
        clonedEntry.add( upId, values );
    }


    public void add( String upId, Value<?>... values ) throws NamingException
    {
        clonedEntry.add( upId, values );
    }


    public void clear()
    {
        clonedEntry.clear();
    }


    public boolean contains( EntryAttribute... attributes ) throws NamingException
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


    public LdapDN getDn()
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


    public List<EntryAttribute> put( EntryAttribute... attributes ) throws NamingException
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


    public boolean remove( String upId, byte[]... values ) throws NamingException
    {
        return clonedEntry.remove( upId, values );
    }


    public boolean remove( String upId, String... values ) throws NamingException
    {
        return clonedEntry.remove( upId, values );
    }


    public boolean remove( String upId, Value<?>... values ) throws NamingException
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


    public void setDn( LdapDN dn )
    {
        clonedEntry.setDn( dn );
    }


    public int size()
    {
        return clonedEntry.size();
    }


    public ServerEntry clone()
    {
        return ( ServerEntry ) clonedEntry.clone();
    }
}
