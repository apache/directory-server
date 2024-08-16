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
package org.apache.directory.server.core.api.entry;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.server.i18n.I18n;


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
        this.clonedEntry = originalEntry.clone();
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


    @Override
    public Entry add( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        return clonedEntry.add( attributeType, values );
    }


    @Override
    public Entry add( AttributeType attributeType, String... values ) throws LdapException
    {
        return clonedEntry.add( attributeType, values );
    }


    @Override
    public Entry add( AttributeType attributeType, Value... values ) throws LdapException
    {
        return clonedEntry.add( attributeType, values );
    }


    @Override
    public Entry add( String upId, AttributeType attributeType, byte[]... values ) throws LdapException
    {
        return clonedEntry.add( attributeType, values );
    }


    @Override
    public Entry add( String upId, AttributeType attributeType, String... values ) throws LdapException
    {
        return clonedEntry.add( attributeType, values );
    }


    @Override
    public Entry add( String upId, AttributeType attributeType, Value... values ) throws LdapException
    {
        return clonedEntry.add( attributeType, values );
    }


    @Override
    public boolean contains( AttributeType attributeType, byte[]... values )
    {
        return clonedEntry.contains( attributeType, values );
    }


    @Override
    public boolean contains( AttributeType attributeType, String... values )
    {
        return clonedEntry.contains( attributeType, values );
    }


    @Override
    public boolean contains( AttributeType attributeType, Value... values )
    {
        return clonedEntry.contains( attributeType, values );
    }


    @Override
    public boolean containsAttribute( AttributeType attributeType )
    {
        return clonedEntry.containsAttribute( attributeType );
    }


    @Override
    public Attribute get( AttributeType attributeType )
    {
        return clonedEntry.get( attributeType );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Attribute> getAttributes()
    {
        return clonedEntry.getAttributes();
    }


    @Override
    public boolean hasObjectClass( Attribute... objectClasses )
    {
        return clonedEntry.hasObjectClass( objectClasses );
    }


    @Override
    public Attribute put( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        return clonedEntry.put( attributeType, values );
    }


    @Override
    public Attribute put( AttributeType attributeType, String... values ) throws LdapException
    {
        return clonedEntry.put( attributeType, values );
    }


    @Override
    public Attribute put( AttributeType attributeType, Value... values ) throws LdapException
    {
        return clonedEntry.put( attributeType, values );
    }


    @Override
    public Attribute put( String upId, AttributeType attributeType, byte[]... values ) throws LdapException
    {
        return clonedEntry.put( attributeType, values );
    }


    @Override
    public Attribute put( String upId, AttributeType attributeType, String... values ) throws LdapException
    {
        return clonedEntry.put( upId, attributeType, values );
    }


    @Override
    public Attribute put( String upId, AttributeType attributeType, Value... values ) throws LdapException
    {
        return clonedEntry.put( upId, attributeType, values );
    }


    @Override
    public boolean remove( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        return clonedEntry.remove( attributeType, values );
    }


    @Override
    public boolean remove( AttributeType attributeType, String... values ) throws LdapException
    {
        return clonedEntry.remove( attributeType, values );
    }


    @Override
    public boolean remove( AttributeType attributeType, Value... values ) throws LdapException
    {
        return clonedEntry.remove( attributeType, values );
    }


    @Override
    public List<Attribute> remove( Attribute... attributes ) throws LdapException
    {
        return clonedEntry.remove( attributes );
    }


    @Override
    public void removeAttributes( AttributeType... attributes )
    {
        clonedEntry.removeAttributes( attributes );
    }


    @Override
    public Entry add( Attribute... attributes ) throws LdapException
    {
        return clonedEntry.add( attributes );
    }


    @Override
    public Entry add( String upId, String... values ) throws LdapException
    {
        return clonedEntry.add( upId, values );
    }


    @Override
    public Entry add( String upId, byte[]... values ) throws LdapException
    {
        return clonedEntry.add( upId, values );
    }


    @Override
    public Entry add( String upId, Value... values ) throws LdapException
    {
        return clonedEntry.add( upId, values );
    }


    @Override
    public void clear()
    {
        clonedEntry.clear();
    }


    @Override
    public boolean contains( Attribute... attributes )
    {
        return clonedEntry.contains( attributes );
    }


    @Override
    public boolean contains( String upId, byte[]... values )
    {
        return clonedEntry.contains( upId, values );
    }


    @Override
    public boolean contains( String upId, String... values )
    {
        return clonedEntry.contains( upId, values );
    }


    @Override
    public boolean contains( String upId, Value... values )
    {
        return clonedEntry.contains( upId, values );
    }


    @Override
    public boolean containsAttribute( String... attributes )
    {
        return clonedEntry.containsAttribute( attributes );
    }


    @Override
    public Attribute get( String alias )
    {
        return clonedEntry.get( alias );
    }


    @Override
    public Dn getDn()
    {
        return clonedEntry.getDn();
    }


    @Override
    public boolean hasObjectClass( String... objectClasses )
    {
        return clonedEntry.hasObjectClass( objectClasses );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSchemaAware()
    {
        return clonedEntry.isSchemaAware();
    }


    @Override
    public Iterator<Attribute> iterator()
    {
        return clonedEntry.iterator();
    }


    @Override
    public List<Attribute> put( Attribute... attributes ) throws LdapException
    {
        return clonedEntry.put( attributes );
    }


    @Override
    public Attribute put( String upId, byte[]... values )
    {
        return clonedEntry.put( upId, values );
    }


    @Override
    public Attribute put( String upId, String... values )
    {
        return clonedEntry.put( upId, values );
    }


    @Override
    public Attribute put( String upId, Value... values )
    {
        return clonedEntry.put( upId, values );
    }


    @Override
    public boolean remove( String upId, byte[]... values ) throws LdapException
    {
        return clonedEntry.remove( upId, values );
    }


    @Override
    public boolean remove( String upId, String... values ) throws LdapException
    {
        return clonedEntry.remove( upId, values );
    }


    @Override
    public boolean remove( String upId, Value... values ) throws LdapException
    {
        return clonedEntry.remove( upId, values );
    }


    @Override
    public void removeAttributes( String... attributes )
    {
        clonedEntry.removeAttributes( attributes );
    }


    @Override
    public void setDn( Dn dn )
    {
        clonedEntry.setDn( dn );
    }


    @Override
    public void setDn( String dn ) throws LdapInvalidDnException
    {
        clonedEntry.setDn( dn );
    }


    @Override
    public int size()
    {
        return clonedEntry.size();
    }


    public Entry toClientEntry() throws LdapException
    {
        // Copy the Dn
        Entry clientEntry = new DefaultEntry( clonedEntry.getDn() );

        // Convert each attribute
        for ( Attribute clonedEntry : this )
        {
            Attribute clientAttribute = clonedEntry.clone();
            clientEntry.add( clientAttribute );
        }

        return clientEntry;
    }


    /**
     * @see java.io.Externalizable#readExternal(ObjectInput)
     *
     * We can't use this method for a ServerEntry
     */
    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        throw new IllegalStateException( I18n.err( I18n.ERR_02000_CANNOT_USE_SERIALIZATION_FOR_SERVER_ATTRIBUTE ) );
    }


    /**
     * @see java.io.Externalizable#writeExternal(ObjectOutput)
     *
     * We can't use this method for a ServerEntry
     */
    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        throw new IllegalStateException( I18n.err( I18n.ERR_02001_CANNOT_USE_SERIALIZATION_FOR_SERVER_ENTRY ) );
    }


    @Override
    public Entry clone()
    {
        return clonedEntry.clone();
    }


    @Override
    public Entry shallowClone()
    {
        return clonedEntry.shallowClone();
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
     * {@inheritDoc}
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
            other = ( ( ClonedServerEntry ) obj ).getClonedEntry();
        }
        else if ( obj instanceof Entry )
        {
            other = ( Entry ) obj;
        }
        else
        {
            return false;
        }
        if ( clonedEntry == null )
        {
            return other == null;
        }
        else
        {
            return clonedEntry.equals( other );
        }
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return toString( "" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString( String tabs )
    {
        return clonedEntry.toString( tabs );
    }
}
