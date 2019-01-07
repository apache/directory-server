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
package org.apache.directory.server.core.api.entry;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.SchemaUtils;
import org.apache.directory.api.util.EmptyEnumeration;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.interceptor.context.FilteringOperationContext;
import org.apache.directory.server.i18n.I18n;


/**
 * A helper class used to manipulate Entries, Attributes and Values.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ServerEntryUtils
{
    private ServerEntryUtils()
    {
    }


    /**
     * Convert a ServerAttribute into a BasicAttribute. The Dn is lost
     * during this conversion, as the Attributes object does not store
     * this element.
     *
     * @param entryAttribute The Server entry to convert
     * @return An instance of a AttributesImpl() object
     */
    public static javax.naming.directory.Attribute toBasicAttribute( Attribute entryAttribute )
    {
        AttributeType attributeType = entryAttribute.getAttributeType();

        javax.naming.directory.Attribute attribute = new BasicAttribute( attributeType.getName() );

        for ( Value value : entryAttribute )
        {
            if ( attributeType.isHR() )
            {
                attribute.add( value.getString() );
            }
            else
            {
                attribute.add( value.getBytes() );
            }
        }

        return attribute;
    }


    /**
     * Convert a ServerEntry into a BasicAttributes. The Dn is lost
     * during this conversion, as the Attributes object does not store
     * this element.
     *
     * @param entry The entry to convert
     * @return An instance of a AttributesImpl() object
     */
    public static Attributes toBasicAttributes( Entry entry )
    {
        if ( entry == null )
        {
            return null;
        }

        Attributes attributes = new BasicAttributes( true );

        for ( Attribute attribute : entry.getAttributes() )
        {
            AttributeType attributeType = attribute.getAttributeType();
            Attribute attr = entry.get( attributeType );

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
     * @param attributeType The AttributeType to use
     * @return An instance of a ServerEntry object
     * 
     * @throws LdapException If we had an incorrect attribute
     */
    public static Attribute toServerAttribute( javax.naming.directory.Attribute attribute, AttributeType attributeType )
        throws LdapException
    {
        if ( attribute == null )
        {
            return null;
        }

        try
        {
            Attribute serverAttribute = new DefaultAttribute( attributeType );

            for ( NamingEnumeration<?> values = attribute.getAll(); values.hasMoreElements(); )
            {
                Object value = values.nextElement();
                int nbAdded = 0;

                if ( value == null )
                {
                    continue;
                }

                if ( serverAttribute.isHumanReadable() )
                {
                    if ( value instanceof String )
                    {
                        nbAdded = serverAttribute.add( ( String ) value );
                    }
                    else if ( value instanceof byte[] )
                    {
                        nbAdded = serverAttribute.add( Strings.utf8ToString( ( byte[] ) value ) );
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
                        nbAdded = serverAttribute.add( Strings.getBytesUtf8( ( String ) value ) );
                    }
                    else if ( value instanceof byte[] )
                    {
                        nbAdded = serverAttribute.add( ( byte[] ) value );
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
     * @param dn The Dn which is needed by the ServerEntry
     * @param schemaManager The SchemaManager instance
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

                for ( NamingEnumeration<? extends javax.naming.directory.Attribute> attrs = attributes.getAll(); attrs
                    .hasMoreElements(); )
                {
                    javax.naming.directory.Attribute attr = attrs.nextElement();

                    String attributeId = attr.getID();
                    String id = SchemaUtils.stripOptions( attributeId );
                    Set<String> options = SchemaUtils.getOptions( attributeId );
                    // TODO : handle options.
                    AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( id );
                    Attribute serverAttribute = ServerEntryUtils.toServerAttribute( attr, attributeType );

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
     * @param schemaManager The SchemaManager instance
     * @return the resultant entry after the modification has taken place
     * @throws LdapException if there are problems accessing attributes
     */
    public static Entry getTargetEntry( Modification mod, Entry entry, SchemaManager schemaManager )
        throws LdapException
    {
        Entry targetEntry = entry.clone();
        ModificationOperation modOp = mod.getOperation();
        String id = mod.getAttribute().getUpId();
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( id );

        switch ( modOp )
        {
            case REPLACE_ATTRIBUTE:
                targetEntry.put( mod.getAttribute() );
                break;

            case REMOVE_ATTRIBUTE:
                Attribute toBeRemoved = mod.getAttribute();

                if ( toBeRemoved.size() == 0 )
                {
                    targetEntry.removeAttributes( id );
                }
                else
                {
                    Attribute existing = targetEntry.get( id );

                    if ( existing != null )
                    {
                        for ( Value value : toBeRemoved )
                        {
                            existing.remove( value );
                        }
                    }
                }
                break;

            case ADD_ATTRIBUTE:
                Attribute combined = new DefaultAttribute( id, attributeType );
                Attribute toBeAdded = mod.getAttribute();
                Attribute existing = entry.get( id );

                if ( existing != null )
                {
                    for ( Value value : existing )
                    {
                        combined.add( value );
                    }
                }

                for ( Value value : toBeAdded )
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
    public static Attribute getUnion( Attribute attr0, Attribute attr1 ) throws LdapException
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

        Attribute attr = attr0.clone();

        for ( Value value : attr1 )
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
    private static Modification toServerModification( ModificationItem modificationImpl, AttributeType attributeType )
        throws LdapException
    {
        ModificationOperation operation;

        switch ( modificationImpl.getModificationOp() )
        {
            case DirContext.REMOVE_ATTRIBUTE:
                operation = ModificationOperation.REMOVE_ATTRIBUTE;
                break;

            case DirContext.REPLACE_ATTRIBUTE:
                operation = ModificationOperation.REPLACE_ATTRIBUTE;
                break;

            case DirContext.ADD_ATTRIBUTE:
            default:
                operation = ModificationOperation.ADD_ATTRIBUTE;
                break;

        }

        return new DefaultModification( operation,
            ServerEntryUtils.toServerAttribute( modificationImpl.getAttribute(), attributeType ) );
    }


    /**
     * 
     * Convert a list of ModificationItemImpl to a list of LDAP API Modifications
     *
     * @param modificationItems The modificationItems to convert
     * @param schemaManager The SchemaManager instance
     * @return A list of converted Modification
     * @throws LdapException If the conversion failed
     */
    public static List<Modification> convertToServerModification( List<ModificationItem> modificationItems,
        SchemaManager schemaManager ) throws LdapException
    {
        if ( modificationItems != null )
        {
            List<Modification> modifications = new ArrayList<>( modificationItems.size() );

            for ( ModificationItem modificationItem : modificationItems )
            {
                AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( modificationItem
                    .getAttribute().getID() );
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
        return new DefaultModification(
            modification.getOperation(),
            new DefaultAttribute( attributeType, modification.getAttribute() ) );
    }


    /**
     * Convert a JNDI set of Modifications to LDAP API Modifications
     * 
     * @param modifications The modifications to convert
     * @param schemaManager The SchemaManager instance
     * @return The list of converted Modifications
     * @throws LdapException If the conversion failed
     */
    public static List<Modification> toServerModification( Modification[] modifications,
        SchemaManager schemaManager ) throws LdapException
    {
        if ( modifications != null )
        {
            List<Modification> modificationsList = new ArrayList<>();

            for ( Modification modification : modifications )
            {
                String attributeId = modification.getAttribute().getUpId();
                String id = stripOptions( attributeId );
                modification.getAttribute().setUpId( id );
                Set<String> options = getOptions( attributeId );

                // -------------------------------------------------------------------
                // DIRSERVER-646 Fix: Replacing an unknown attribute with no values 
                // (deletion) causes an error
                // -------------------------------------------------------------------
                if ( !schemaManager.getAttributeTypeRegistry().contains( id )
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


    /**
     * Convert a JNDI set of ModificationItems to LDAP API Modifications
     * 
     * @param modifications The modificationItems to convert
     * @param schemaManager The SchemaManager instance
     * @return The list of converted ModificationItems
     * @throws LdapException If the conversion failed
     */
    public static List<Modification> toServerModification( ModificationItem[] modifications,
        SchemaManager schemaManager ) throws LdapException
    {
        if ( modifications != null )
        {
            List<Modification> modificationsList = new ArrayList<>();

            for ( ModificationItem modification : modifications )
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

                if ( !schemaManager.getAttributeTypeRegistry().contains( id )
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
                modificationsList.add( toServerModification( modification, attributeType ) );
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
    public static Modification getModificationItem( List<Modification> mods, AttributeType type )
    {
        for ( Modification modification : mods )
        {
            Attribute attribute = modification.getAttribute();

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
    public static Attribute getAttribute( List<Modification> mods, AttributeType type )
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
            return new EmptyEnumeration<>();
        }

        return new NamingEnumeration<SearchResult>()
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

                return new SearchResult(
                    rec.getDn().getName(),
                    rec.getObject(),
                    toBasicAttributes( rec.getServerEntry() ),
                    rec.isRelative() );
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

                    return new SearchResult(
                        rec.getDn().getName(),
                        rec.getObject(),
                        toBasicAttributes( rec.getServerEntry() ),
                        rec.isRelative() );
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
        int optionsPos = attributeId.indexOf( ';' );

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
        int optionsPos = attributeId.indexOf( ';' );

        if ( optionsPos != -1 )
        {
            Set<String> options = new HashSet<>();

            String[] res = attributeId.substring( optionsPos + 1 ).split( ";" );

            for ( String option : res )
            {
                if ( !Strings.isEmpty( option ) )
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


    /**
     * Filters an entry accordingly to the requested Attribute list.
     * 
     * @param schemaManager The SchemaManager instance
     * @param operationContext The SearchingOperationContext
     * @param entry The entry to filter
     * @throws LdapException If the filtering fails
     */
    public static void filterContents( SchemaManager schemaManager, FilteringOperationContext operationContext,
        Entry entry ) throws LdapException
    {
        boolean typesOnly = operationContext.isTypesOnly();

        boolean returnAll = ( operationContext.isAllOperationalAttributes() && operationContext.isAllUserAttributes() )
            && ( !typesOnly );

        if ( returnAll )
        {
            return;
        }

        // for special handling of entryDN attribute, see DIRSERVER-1902
        Entry originalEntry = ( ( ClonedServerEntry ) entry ).getOriginalEntry();

        AttributeType entryDnType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_DN_AT_OID );
        AttributeType refType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.REF_AT_OID );

        // First, remove all the attributes if we have the NoAttribute flag set to true
        if ( operationContext.isNoAttributes() )
        {
            for ( Attribute attribute : originalEntry )
            {
                AttributeType attributeType = attribute.getAttributeType();

                // Bypass the ref attribute, unless the ManageDSAIT control is present
                if ( operationContext.isReferralThrown() && attributeType.equals( refType ) )
                {
                    continue;
                }

                entry.remove( entry.get( attributeType ) );
            }

            entry.removeAttributes( entryDnType );

            return;
        }

        // If the user has requested all the User attributes ('*') only, we filter the entry's attribute to keep only
        // the USER attributes, plus the Operational attributes in the returning list 
        if ( operationContext.isAllUserAttributes() )
        {
            for ( Attribute attribute : originalEntry )
            {
                AttributeType attributeType = attribute.getAttributeType();

                // Bypass the ref attribute, unless the ManageDSAIT control is present
                if ( operationContext.isReferralThrown() && attributeType.equals( refType ) )
                {
                    continue;
                }

                if ( attributeType.isOperational() )
                {
                    if ( !operationContext.contains( schemaManager, attributeType ) )
                    {
                        entry.removeAttributes( attributeType );
                    }
                    else if ( typesOnly )
                    {
                        entry.get( attributeType ).clear();
                    }
                }
                else if ( typesOnly )
                {
                    entry.get( attributeType ).clear();
                }
            }

            // DIRSERVER-1953
            if ( !operationContext.contains( schemaManager, entryDnType ) )
            {
                entry.removeAttributes( entryDnType );
            }

            return;
        }

        // If the user has requested all the Operational attributes ('+') only, we filter the entry's attribute to keep only
        // the OPERATIONAL attributes, plus the User attributes in the returning list 
        if ( operationContext.isAllOperationalAttributes() )
        {
            for ( Attribute attribute : originalEntry )
            {
                AttributeType attributeType = attribute.getAttributeType();

                if ( attributeType.isUser() )
                {
                    if ( !operationContext.contains( schemaManager, attributeType ) )
                    {
                        entry.removeAttributes( attributeType );
                    }
                    else if ( typesOnly )
                    {
                        entry.get( attributeType ).clear();
                    }
                }
                else if ( typesOnly )
                {
                    entry.get( attributeType ).clear();
                }
            }

            if ( !operationContext.contains( schemaManager, entryDnType ) )
            {
                entry.removeAttributes( entryDnType );
            }
            else if ( typesOnly )
            {
                entry.get( entryDnType ).clear();
            }

            return;
        }

        // Last, not least, check if the attributes are in the returning list
        if ( operationContext.getReturningAttributes() != null )
        {
            for ( Attribute attribute : originalEntry )
            {
                AttributeType attributeType = attribute.getAttributeType();

                // Bypass the ref attribute, unless the ManageDSAIT control is present
                if ( operationContext.isReferralThrown() && attributeType.equals( refType ) )
                {
                    continue;
                }

                if ( !operationContext.contains( schemaManager, attributeType ) )
                {
                    entry.removeAttributes( attributeType );
                    continue;
                }

                boolean isNotRequested = true;

                for ( AttributeTypeOptions attrOptions : operationContext.getReturningAttributes() )
                {
                    if ( attrOptions.getAttributeType().equals( attributeType )
                        || attrOptions.getAttributeType().isAncestorOf( attributeType ) )
                    {
                        isNotRequested = false;
                        break;
                    }
                }

                if ( isNotRequested )
                {
                    entry.removeAttributes( attributeType );
                }
                else if ( typesOnly )
                {
                    entry.get( attributeType ).clear();
                }
            }

            if ( !operationContext.contains( schemaManager, entryDnType ) )
            {
                entry.removeAttributes( entryDnType );
            }
            else if ( typesOnly )
            {
                entry.get( entryDnType ).clear();
            }
        }
    }
}
