/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.entry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.SchemaUtils;
import org.apache.directory.shared.util.EmptyEnumeration;
import org.apache.directory.shared.util.Strings;

/**
 * A helper class used to manipulate Entries, Attributes and Values.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ServerEntryUtils
{
    /**
     * Convert a ServerAttribute into a BasicAttribute. The Dn is lost
     * during this conversion, as the Attributes object does not store
     * this element.
     *
     * @return An instance of a AttributesImpl() object
     */
    public static Attribute toBasicAttribute( EntryAttribute entryAttribute )
    {
        AttributeType attributeType = entryAttribute.getAttributeType();
        
        Attribute attribute = new BasicAttribute( attributeType.getName() );
        
        for ( Value<?> value: entryAttribute )
        {
            attribute.add( value.getValue() );
        }
        
        return attribute;
    }
    
    
    /**
     * Convert a ServerEntry into a BasicAttributes. The Dn is lost
     * during this conversion, as the Attributes object does not store
     * this element.
     *
     * @return An instance of a AttributesImpl() object
     */
    public static Attributes toBasicAttributes( Entry entry )
    {
        if ( entry == null )
        {
            return null;
        }
        
        Attributes attributes = new BasicAttributes( true );

        for ( AttributeType attributeType:entry.getAttributeTypes() )
        {
            EntryAttribute attr = entry.get( attributeType );
            
            // Deal with a special case : an entry without any ObjectClass
            if ( attributeType.getOid().equals( SchemaConstants.OBJECT_CLASS_AT_OID ) && attr.size() == 0 )
            {
                // We don't have any objectClass, just dismiss this element
                continue;
            }
            
            attributes.put( toBasicAttribute( attr ) );
        }
        
        return attributes;
    }


    /**
     * Convert a BasicAttribute or a AttributeImpl to a ServerAtribute
     *
     * @param attribute the BasicAttributes or AttributesImpl instance to convert
     * @param attributeType
     * @return An instance of a ServerEntry object
     * 
     * @throws InvalidAttributeIdentifierException If we had an incorrect attribute
     */
    public static EntryAttribute toServerAttribute( Attribute attribute, AttributeType attributeType ) throws LdapException
    {
        if ( attribute == null )
        {
            return null;
        }
        
        try 
        {
            EntryAttribute serverAttribute = new DefaultEntryAttribute( attributeType );
            
            for ( NamingEnumeration<?> values = attribute.getAll(); values.hasMoreElements(); )
            {
                Object value = values.nextElement();
                int nbAdded = 0;
                
                if ( value == null )
                {
                    continue;
                }
                
                if ( serverAttribute.isHR() )
                {
                    if ( value instanceof String )
                    {
                        nbAdded = serverAttribute.add( (String)value );
                    }
                    else if ( value instanceof byte[] )
                    {
                        nbAdded = serverAttribute.add( Strings.utf8ToString((byte[]) value) );
                    }
                    else
                    {
                        throw new LdapInvalidAttributeTypeException();
                    }
                }
                else
                {
                    if ( value instanceof String )
                    {
                        nbAdded = serverAttribute.add( Strings.getBytesUtf8((String) value) );
                    }
                    else if ( value instanceof byte[] )
                    {
                        nbAdded = serverAttribute.add( (byte[])value );
                    }
                    else
                    {
                        throw new LdapInvalidAttributeTypeException();
                    }
                }
                
                if ( nbAdded == 0 )
                {
                    throw new LdapInvalidAttributeTypeException();
                }
            }
            
            return serverAttribute;
        }
        catch ( NamingException ne )
        {
            throw new LdapInvalidAttributeTypeException();
        }
    }


    /**
     * Convert a BasicAttributes or a AttributesImpl to a ServerEntry
     *
     * @param attributes the BasicAttributes or AttributesImpl instance to convert
     * @param registries The registries, needed ro build a ServerEntry
     * @param dn The Dn which is needed by the ServerEntry
     * @return An instance of a ServerEntry object
     * 
     * @throws LdapInvalidAttributeTypeException If we get an invalid attribute
     */
    public static Entry toServerEntry( Attributes attributes, Dn dn, SchemaManager schemaManager )
            throws LdapInvalidAttributeTypeException
    {
        if ( attributes instanceof BasicAttributes )
        {
            try 
            {
                Entry entry = new DefaultEntry( schemaManager, dn );
    
                for ( NamingEnumeration<? extends Attribute> attrs = attributes.getAll(); attrs.hasMoreElements(); )
                {
                    Attribute attr = attrs.nextElement();

                    String attributeId = attr.getID();
                    String id = SchemaUtils.stripOptions( attributeId );
                    Set<String> options = SchemaUtils.getOptions( attributeId );
                    // TODO : handle options.
                    AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( id );
                    EntryAttribute serverAttribute = ServerEntryUtils.toServerAttribute( attr, attributeType );
                    
                    if ( serverAttribute != null )
                    {
                        entry.put( serverAttribute );
                    }
                }
                
                return entry;
            }
            catch ( LdapException ne )
            {
                throw new LdapInvalidAttributeTypeException( ne.getLocalizedMessage() );
            }
        }
        else
        {
            return null;
        }
    }


    /**
     * Gets the target entry as it would look after a modification operation 
     * was performed on it.
     * 
     * @param mod the modification
     * @param entry the source entry that is modified
     * @return the resultant entry after the modification has taken place
     * @throws LdapException if there are problems accessing attributes
     */
    public static Entry getTargetEntry( Modification mod, Entry entry, SchemaManager schemaManager ) throws LdapException
    {
        Entry targetEntry = ( Entry ) entry.clone();
        ModificationOperation modOp = mod.getOperation();
        String id = mod.getAttribute().getUpId();
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( id );
        
        switch ( modOp )
        {
            case REPLACE_ATTRIBUTE :
                targetEntry.put( mod.getAttribute() );
                break;
                
            case REMOVE_ATTRIBUTE :
                EntryAttribute toBeRemoved = mod.getAttribute();

                if ( toBeRemoved.size() == 0 )
                {
                    targetEntry.removeAttributes( id );
                }
                else
                {
                    EntryAttribute existing = targetEntry.get( id );

                    if ( existing != null )
                    {
                        for ( Value<?> value:toBeRemoved )
                        {
                            existing.remove( value );
                        }
                    }
                }
                break;
                
            case ADD_ATTRIBUTE :
                EntryAttribute combined = new DefaultEntryAttribute( id, attributeType );
                EntryAttribute toBeAdded = mod.getAttribute();
                EntryAttribute existing = entry.get( id );

                if ( existing != null )
                {
                    for ( Value<?> value:existing )
                    {
                        combined.add( value );
                    }
                }

                for ( Value<?> value:toBeAdded )
                {
                    combined.add( value );
                }
                
                targetEntry.put( combined );
                break;
                
            default:
                throw new IllegalStateException( I18n.err( I18n.ERR_464, modOp ) );
        }

        return targetEntry;
    }


    /**
     * Creates a new attribute which contains the values representing the union
     * of two attributes. If one attribute is null then the resultant attribute
     * returned is a copy of the non-null attribute. If both are null then we
     * cannot determine the attribute ID and an {@link IllegalArgumentException}
     * is raised.
     * 
     * @param attr0 the first attribute
     * @param attr1 the second attribute
     * @return a new attribute with the union of values from both attribute
     *         arguments
     * @throws LdapException if there are problems accessing attribute values
     */
    public static EntryAttribute getUnion( EntryAttribute attr0, EntryAttribute attr1 ) throws LdapException
    {
        if ( attr0 == null && attr1 == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_465 ) );
        }
        else if ( attr0 == null )
        {
            return attr1.clone();
        }
        else if ( attr1 == null )
        {
            return attr0.clone();
        }
        else if ( !attr0.getAttributeType().equals( attr1.getAttributeType() ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_466 ) );
        }

        EntryAttribute attr = attr0.clone();

        for ( Value<?> value:attr1 )
        {
            attr.add( value );
        }

        return attr;
    }
    
    
    /**
     * Convert a ModificationItem to an instance of a ServerModification object
     *
     * @param modificationImpl the modification instance to convert
     * @param attributeType the associated attributeType
     * @return a instance of a ServerModification object
     */
    private static Modification toServerModification( ModificationItem modificationImpl, AttributeType attributeType ) throws LdapException
    {
        ModificationOperation operation;
        
        switch ( modificationImpl.getModificationOp() )
        {
            case DirContext.REMOVE_ATTRIBUTE :
                operation = ModificationOperation.REMOVE_ATTRIBUTE;
                break;
                
            case DirContext.REPLACE_ATTRIBUTE :
                operation = ModificationOperation.REPLACE_ATTRIBUTE;
                break;

            case DirContext.ADD_ATTRIBUTE :
            default :
                operation = ModificationOperation.ADD_ATTRIBUTE;
                break;
                
        }
        
        Modification modification = new DefaultModification( 
            operation,
            ServerEntryUtils.toServerAttribute( modificationImpl.getAttribute(), attributeType ) ); 
        
        return modification;
        
    }

    
    /**
     * 
     * Convert a list of ModificationItemImpl to a list of 
     *
     * @param modificationImpls
     * @param atRegistry
     * @return
     * @throws LdapException
     */
    public static List<Modification> convertToServerModification( List<ModificationItem> modificationItems, 
        SchemaManager schemaManager ) throws LdapException
    {
        if ( modificationItems != null )
        {
            List<Modification> modifications = new ArrayList<Modification>( modificationItems.size() );

            for ( ModificationItem modificationItem: modificationItems )
            {
                AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( modificationItem.getAttribute().getID() );
                modifications.add( toServerModification( modificationItem, attributeType ) );
            }
        
            return modifications;
        }
        else
        {
            return null;
        }
    }
    
    
    /**
     * Convert a Modification to an instance of a ServerModification object.
     *
     * @param modificationImpl the modification instance to convert
     * @param attributeType the associated attributeType
     * @return a instance of a ServerModification object
     */
    private static Modification toServerModification( Modification modification, AttributeType attributeType )
        throws LdapException
    {
        Modification serverModification = new DefaultModification( 
            modification.getOperation(),
            new DefaultEntryAttribute( attributeType, modification.getAttribute() ) ); 
        
        return serverModification;
        
    }

    
    public static List<Modification> toServerModification( Modification[] modifications, 
        SchemaManager schemaManager ) throws LdapException
    {
        if ( modifications != null )
        {
            List<Modification> modificationsList = new ArrayList<Modification>();
    
            for ( Modification modification: modifications )
            {
                String attributeId = modification.getAttribute().getUpId();
                String id = stripOptions( attributeId );
                modification.getAttribute().setUpId( id );
                Set<String> options = getOptions( attributeId );

                // -------------------------------------------------------------------
                // DIRSERVER-646 Fix: Replacing an unknown attribute with no values 
                // (deletion) causes an error
                // -------------------------------------------------------------------
                if ( ! schemaManager.getAttributeTypeRegistry().contains( id ) 
                     && modification.getAttribute().size() == 0 
                     && modification.getOperation() == ModificationOperation.REPLACE_ATTRIBUTE )
                {
                    // The attributeType does not exist in the schema.
                    // It's an error
                    String message = I18n.err( I18n.ERR_467, id );
                    throw new LdapInvalidAttributeTypeException( message );
                }
                else
                {
                    // TODO : handle options
                    AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( id );
                    modificationsList.add( toServerModification( modification, attributeType ) );
                }
            }
        
            return modificationsList;
        }
        else
        {
            return null;
        }
    }


    public static List<Modification> toServerModification( ModificationItem[] modifications, 
        SchemaManager schemaManager ) throws LdapException
    {
        if ( modifications != null )
        {
            List<Modification> modificationsList = new ArrayList<Modification>();
    
            for ( ModificationItem modification: modifications )
            {
                String attributeId = modification.getAttribute().getID();
                String id = stripOptions( attributeId );
                Set<String> options = getOptions( attributeId );

                // -------------------------------------------------------------------
                // DIRSERVER-646 Fix: Replacing an unknown attribute with no values 
                // (deletion) causes an error
                // -------------------------------------------------------------------
                
                // TODO - after removing JNDI we need to make the server handle 
                // this in the codec
                
                if ( ! schemaManager.getAttributeTypeRegistry().contains( id ) 
                     && modification.getAttribute().size() == 0 
                     && modification.getModificationOp() == DirContext.REPLACE_ATTRIBUTE )
                {
                    continue;
                }

                // -------------------------------------------------------------------
                // END DIRSERVER-646 Fix
                // -------------------------------------------------------------------
                
                
                // TODO : handle options
                AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( id );
                modificationsList.add( toServerModification( (ModificationItem)modification, attributeType ) );
            }
        
            return modificationsList;
        }
        else
        {
            return null;
        }
    }


    /**
     * Utility method to extract a modification item from an array of modifications.
     * 
     * @param mods the array of ModificationItems to extract the Attribute from.
     * @param type the attributeType spec of the Attribute to extract
     * @return the modification item on the attributeType specified
     */
    public static final Modification getModificationItem( List<Modification> mods, AttributeType type )
    {
        for ( Modification modification:mods )
        {
            EntryAttribute attribute = modification.getAttribute();
            
            if ( attribute.getAttributeType() == type )
            {
                return modification;
            }
        }
        
        return null;
    }
    
    
    /**
     * Utility method to extract an attribute from a list of modifications.
     * 
     * @param mods the list of ModificationItems to extract the Attribute from.
     * @param type the attributeType spec of the Attribute to extract
     * @return the extract Attribute or null if no such attribute exists
     */
    public static EntryAttribute getAttribute( List<Modification> mods, AttributeType type )
    {
        Modification mod = getModificationItem( mods, type );
        
        if ( mod != null )
        {
            return mod.getAttribute();
        }
        
        return null;
    }
    
    
    /**
     * Encapsulate a ServerSearchResult enumeration into a SearchResult enumeration
     * @param result The ServerSearchResult enumeration
     * @return A SearchResultEnumeration
     */
    public static NamingEnumeration<SearchResult> toSearchResultEnum( final NamingEnumeration<ServerSearchResult> result )
    {
        if ( result instanceof EmptyEnumeration<?> )
        {
            return new EmptyEnumeration<SearchResult>();
        }
        
        return new NamingEnumeration<SearchResult> ()
        {
            public void close() throws NamingException
            {
                result.close();
            }


            /**
             * @see javax.naming.NamingEnumeration#hasMore()
             */
            public boolean hasMore() throws NamingException
            {
                return result.hasMore();
            }


            /**
             * @see javax.naming.NamingEnumeration#next()
             */
            public SearchResult next() throws NamingException
            {
                ServerSearchResult rec = result.next();
                
                SearchResult searchResult = new SearchResult( 
                        rec.getDn().getName(), 
                        rec.getObject(), 
                        toBasicAttributes( rec.getServerEntry() ), 
                        rec.isRelative() );
                
                return searchResult;
            }
            
            
            /**
             * @see java.util.Enumeration#hasMoreElements()
             */
            public boolean hasMoreElements()
            {
                return result.hasMoreElements();
            }


            /**
             * @see java.util.Enumeration#nextElement()
             */
            public SearchResult nextElement()
            {
                try
                {
                    ServerSearchResult rec = result.next();
    
                    SearchResult searchResult = new SearchResult( 
                            rec.getDn().getName(), 
                            rec.getObject(), 
                            toBasicAttributes( rec.getServerEntry() ), 
                            rec.isRelative() );
                    
                    return searchResult;
                }
                catch ( NamingException ne )
                {
                    NoSuchElementException nsee = 
                        new NoSuchElementException( I18n.err( I18n.ERR_468 ) );
                    nsee.initCause( ne );
                    throw nsee;
                }
            }
        };
    }
    
    
    /**
     * Remove the options from the attributeType, and returns the ID.
     * 
     * RFC 4512 :
     * attributedescription = attributetype options
     * attributetype = oid
     * options = *( SEMI option )
     * option = 1*keychar
     */
    private static String stripOptions( String attributeId )
    {
        int optionsPos = attributeId.indexOf( ";" ); 
        
        if ( optionsPos != -1 )
        {
            return attributeId.substring( 0, optionsPos );
        }
        else
        {
            return attributeId;
        }
    }
    

    /**
     * Get the options from the attributeType.
     * 
     * For instance, given :
     * jpegphoto;binary;lang=jp
     * 
     * your get back a set containing { "binary", "lang=jp" }
     */
    private static Set<String> getOptions( String attributeId )
    {
        int optionsPos = attributeId.indexOf( ";" ); 

        if ( optionsPos != -1 )
        {
            Set<String> options = new HashSet<String>();
            
            String[] res = attributeId.substring( optionsPos + 1 ).split( ";" );
            
            for ( String option:res )
            {
                if ( !Strings.isEmpty(option) )
                {
                    options.add( option );
                }
            }
            
            return options;
        }
        else
        {
            return null;
        }
    }
}
