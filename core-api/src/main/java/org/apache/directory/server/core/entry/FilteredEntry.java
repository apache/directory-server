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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.UsageEnum;


/**
 * A special wrapper on the Entry object to be used for filtering the attributes according to the
 * specified search filter.
 * 
 *  This class avoids cloning of the attributes until the actual get() method is called.
 *  A special XXXRef() method is also provided to avoid cloning, but this gives the actual reference
 *  of the object that is present in wrapped original entry. But if the 'typesOnly' flag is set to
 *  'true' then the XXXRef() methods will return a cloned object instead of the actual reference.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class FilteredEntry implements Entry
{
    /** The original entry as returned by the backend */
    private Entry originalEntry;

    private static final UnsupportedOperationException UNSUPPORTED_OP_EX = new UnsupportedOperationException(
        "this operation is not supported" );

    private boolean typesOnly = false;

    private Set<AttributeType> attributeTypes = Collections.unmodifiableSet( Collections.EMPTY_SET );


    public FilteredEntry( Entry entry, Set<AttributeType> attributeTypes )
    {
        this.originalEntry = entry;
        if ( attributeTypes != null )
        {
            this.attributeTypes = Collections.unmodifiableSet( attributeTypes );
        }
    }


    public FilteredEntry( Entry entry, Set<AttributeType> attributeTypes, boolean typesOnly )
    {
        this( entry, attributeTypes );

        this.typesOnly = typesOnly;
    }


    public static FilteredEntry createFilteredEntry( Entry entry, UsageEnum atUsage )
    {
        return new FilteredEntry( entry, getRequestedAttributeTypes( entry, atUsage ) );
    }


    public static FilteredEntry createFilteredEntry( Entry entry )
    {
        return new FilteredEntry( entry, entry.getAttributeTypes() );
    }


    public static FilteredEntry createFilteredEntry( Entry entry, UsageEnum atUsage, boolean typesOnly )
    {
        return new FilteredEntry( entry, getRequestedAttributeTypes( entry, atUsage ), typesOnly );
    }


    private static Set<AttributeType> getRequestedAttributeTypes( Entry entry, UsageEnum atUsage )
    {
        Set<AttributeType> atSet = new HashSet<AttributeType>();
        for ( EntryAttribute entryAt : entry )
        {
            AttributeType at = entryAt.getAttributeType();
            if ( at.getUsage() == atUsage )
            {
                atSet.add( at );
            }
        }

        return atSet;
    }


    public DN getDn()
    {
        return ( DN ) originalEntry.getDn().clone();
    }


    public DN getDnRef()
    {
        return originalEntry.getDn();
    }


    public boolean hasObjectClass( String objectClass )
    {
        if ( typesOnly || attributeTypes.isEmpty() )
        {
            return false;
        }

        return originalEntry.hasObjectClass( objectClass );
    }


    public boolean hasObjectClass( EntryAttribute objectClass )
    {
        if ( typesOnly || attributeTypes.isEmpty() )
        {
            return false;
        }

        return originalEntry.hasObjectClass( objectClass );
    }


    public EntryAttribute get( String alias )
    {
        EntryAttribute at = getRef( alias );

        if ( at != null )
        {
            if ( !typesOnly )
            {
                at = at.clone();
            }

            return at;
        }

        return null;
    }


    public EntryAttribute getRef( String alias )
    {
        EntryAttribute at = originalEntry.get( alias );

        if ( at == null )
        {
            return null;
        }

        if ( attributeTypes.contains( at.getAttributeType() ) )
        {
            if ( typesOnly )
            {
                at = at.clone();
                at.clear();
            }

            return at;
        }

        return null;
    }


    public EntryAttribute get( AttributeType attributeType )
    {
        EntryAttribute at = getRef( attributeType );

        if ( at != null )
        {
            return at.clone();
        }

        return null;
    }


    public EntryAttribute getRef( AttributeType attributeType )
    {
        if ( attributeTypes.contains( attributeType ) )
        {
            EntryAttribute at = originalEntry.get( attributeType );

            if ( typesOnly )
            {
                at = at.clone();
                at.clear();
            }

            return at;
        }

        return null;
    }


    public Set<AttributeType> getAttributeTypes()
    {
        return attributeTypes;
    }


    public boolean isValid()
    {
        return originalEntry.isValid();
    }


    public boolean isValid( String objectClass )
    {
        return originalEntry.isValid( objectClass );
    }


    public boolean isValid( EntryAttribute objectClass )
    {
        return originalEntry.isValid( objectClass );
    }


    public Iterator<EntryAttribute> iterator()
    {
        Set<EntryAttribute> entryAtSet = new HashSet<EntryAttribute>();

        for ( AttributeType at : attributeTypes )
        {
            EntryAttribute entryAt = originalEntry.get( at ).clone();

            if ( typesOnly )
            {
                entryAt.clear();
            }

            entryAtSet.add( entryAt );
        }

        return entryAtSet.iterator();
    }


    public Iterator<EntryAttribute> iteratorRef()
    {
        Set<EntryAttribute> entryAtSet = new HashSet<EntryAttribute>();

        for ( AttributeType at : attributeTypes )
        {
            EntryAttribute entryAt = originalEntry.get( at );
            if ( typesOnly )
            {
                entryAt = entryAt.clone();
                entryAt.clear();
            }

            entryAtSet.add( entryAt );
        }

        return entryAtSet.iterator();
    }


    public boolean contains( String upId ) throws LdapException
    {
        EntryAttribute at = getRef( upId );

        if ( at != null )
        {
            return true;
        }

        return false;
    }


    public boolean contains( AttributeType attributeType, byte[]... values )
    {
        if ( attributeTypes.contains( attributeType ) )
        {
            return originalEntry.contains( attributeType, values );
        }

        return false;
    }


    public boolean contains( AttributeType attributeType, String... values )
    {
        if ( attributeTypes.contains( attributeType ) )
        {
            return originalEntry.contains( attributeType, values );
        }

        return false;
    }


    public boolean contains( AttributeType attributeType, Value<?>... values )
    {
        if ( attributeTypes.contains( attributeType ) )
        {
            return originalEntry.contains( attributeType, values );
        }

        return false;
    }


    public boolean containsAttribute( AttributeType attributeType )
    {
        return attributeTypes.contains( attributeType );
    }


    public boolean contains( EntryAttribute... attributes ) throws LdapException
    {
        for ( EntryAttribute at : attributes )
        {
            if ( !attributeTypes.contains( at.getAttributeType() ) )
            {
                return false;
            }
        }

        return true;
    }


    public boolean contains( String upId, byte[]... values )
    {
        EntryAttribute at = getRef( upId );
        if ( at != null )
        {
            return originalEntry.contains( at.getAttributeType(), values );
        }

        return false;
    }


    public boolean contains( String upId, String... values )
    {
        EntryAttribute at = getRef( upId );
        if ( at != null )
        {
            return originalEntry.contains( at.getAttributeType(), values );
        }

        return false;
    }


    public boolean contains( String upId, Value<?>... values )
    {
        EntryAttribute at = getRef( upId );
        if ( at != null )
        {
            return originalEntry.contains( at.getAttributeType(), values );
        }

        return false;
    }


    public boolean containsAttribute( String... attributes )
    {
        for ( String s : attributes )
        {
            if ( getRef( s ) == null )
            {
                return false;
            }
        }

        return true;
    }


    public int size()
    {
        return attributeTypes.size();
    }


    public boolean isTypesOnly()
    {
        return typesOnly;
    }


    // ----------------- unsupported operations ------------------------

    public void writeExternal( ObjectOutput out ) throws IOException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void clear()
    {
        throw UNSUPPORTED_OP_EX;
    }


    public Entry clone()
    {
        throw UNSUPPORTED_OP_EX;
    }


    public List<EntryAttribute> set( String... upIds )
    {
        throw UNSUPPORTED_OP_EX;
    }


    public List<EntryAttribute> set( AttributeType... attributeTypes )
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void setDn( DN dn )
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void add( EntryAttribute... attributes ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void add( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void add( AttributeType attributeType, String... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void add( AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void add( String upId, AttributeType attributeType, byte[]... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void add( String upId, AttributeType attributeType, String... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void add( String upId, AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void add( String upId, String... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void add( String upId, byte[]... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public void add( String upId, Value<?>... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public List<EntryAttribute> put( EntryAttribute... attributes ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public EntryAttribute put( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public EntryAttribute put( AttributeType attributeType, String... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public EntryAttribute put( AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public EntryAttribute put( String upId, AttributeType attributeType, byte[]... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public EntryAttribute put( String upId, AttributeType attributeType, String... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public EntryAttribute put( String upId, AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public EntryAttribute put( String upId, byte[]... values )
    {
        throw UNSUPPORTED_OP_EX;
    }


    public EntryAttribute put( String upId, String... values )
    {
        throw UNSUPPORTED_OP_EX;
    }


    public EntryAttribute put( String upId, Value<?>... values )
    {
        throw UNSUPPORTED_OP_EX;
    }


    public boolean remove( AttributeType attributeType, byte[]... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public boolean remove( AttributeType attributeType, String... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public boolean remove( AttributeType attributeType, Value<?>... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public List<EntryAttribute> remove( EntryAttribute... attributes ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public List<EntryAttribute> removeAttributes( AttributeType... attributes )
    {
        throw UNSUPPORTED_OP_EX;
    }


    public boolean remove( String upId, byte[]... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public boolean remove( String upId, String... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public boolean remove( String upId, Value<?>... values ) throws LdapException
    {
        throw UNSUPPORTED_OP_EX;
    }


    public List<EntryAttribute> removeAttributes( String... attributes )
    {
        throw UNSUPPORTED_OP_EX;
    }
}
