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


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.AbstractEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.LdapDNSerializer;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A default implementation of a ServerEntry which should suite most
 * use cases.
 * 
 * This class is final, it should not be extended.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class DefaultServerEntry extends AbstractEntry<AttributeType> implements ServerEntry
{
    /** Used for serialization */
    private static final long serialVersionUID = 2L;
    
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultServerEntry.class );

    /** The AttributeType registries */
    private final transient AttributeTypeRegistry atRegistry;
    
    /** A speedup to get the ObjectClass attribute */
    private static transient AttributeType OBJECT_CLASS_AT;
    
    /** A mutex to manage synchronization*/
    private static transient Object MUTEX = new Object();


    //-------------------------------------------------------------------------
    // Helper methods
    //-------------------------------------------------------------------------
    /**
     * Returns the attributeType from an Attribute ID.
     */
    private AttributeType getAttributeType( String upId ) throws NamingException
    {
        if ( StringTools.isEmpty( StringTools.trim( upId ) ) )
        {
            String message = "The ID should not be null or empty";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        return atRegistry.lookup( upId );
    }

    
    /**
     * Get the UpId if it was null.
     */
    public static String getUpId( String upId, AttributeType attributeType )
    {
        String normUpId = StringTools.trim( upId );

        if ( ( attributeType == null ) )
        {
            if ( StringTools.isEmpty( normUpId ) )
            {
                String message = "Cannot add an attribute without an ID";
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
        else if ( StringTools.isEmpty( normUpId ) )
        {
            upId = attributeType.getName();
            
            if ( StringTools.isEmpty( upId ) )
            {
                upId = attributeType.getOid();
            }
        }
        
        return upId;
    }

    
    /**
     * This method is used to initialize the OBJECT_CLASS_AT attributeType.
     * 
     * We want to do it only once, so it's a synchronized method. Note that
     * the alternative would be to call the lookup() every time, but this won't
     * be very efficient, as it will get the AT from a map, which is also
     * synchronized, so here, we have a very minimal cost.
     * 
     * We can't do it once as a static part in the body of this class, because
     * the access to the registries is mandatory to get back the AttributeType.
     */
    private void initObjectClassAT( Registries registries )
    {
        try
        {
            if ( OBJECT_CLASS_AT == null )
            {
                synchronized ( MUTEX )
                {
                    OBJECT_CLASS_AT = atRegistry.lookup( SchemaConstants.OBJECT_CLASS_AT );
                }
            }
        }
        catch ( NamingException ne )
        {
            // do nothing...
        }
    }

    
    /**
     * Add a new ServerAttribute, with its upId. If the upId is null,
     * default to the AttributeType name.
     * 
     * Updates the serverAttributeMap.
     */
    private void createAttribute( String upId, AttributeType attributeType, byte[]... values ) 
    {
        ServerAttribute attribute = new DefaultServerAttribute( attributeType, values );
        attribute.setUpId( upId, attributeType );
        attributes.put( attributeType, attribute );
    }
    
    
    /**
     * Add a new ServerAttribute, with its upId. If the upId is null,
     * default to the AttributeType name.
     * 
     * Updates the serverAttributeMap.
     */
    private void createAttribute( String upId, AttributeType attributeType, String... values ) 
    {
        ServerAttribute attribute = new DefaultServerAttribute( attributeType, values );
        attribute.setUpId( upId, attributeType );
        attributes.put( attributeType, attribute );
    }
    
    
    /**
     * Add a new ServerAttribute, with its upId. If the upId is null,
     * default to the AttributeType name.
     * 
     * Updates the serverAttributeMap.
     */
    private void createAttribute( String upId, AttributeType attributeType, Value<?>... values ) 
    {
        ServerAttribute attribute = new DefaultServerAttribute( attributeType, values );
        attribute.setUpId( upId, attributeType );
        attributes.put( attributeType, attribute );
    }
    
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------
    /**
     * <p>
     * Creates a new instance of DefaultServerEntry.
     * </p> 
     * <p>
     * This entry <b>must</b> be initialized before being used !
     * </p>
     */
    /* no protection ! */ DefaultServerEntry()
    {
        atRegistry = null;
        dn = LdapDN.EMPTY_LDAPDN;
    }


    /**
     * <p>
     * Creates a new instance of DefaultServerEntry, with registries. 
     * </p>
     * <p>
     * No attributes will be created.
     * </p> 
     * 
     * @param registries The reference to the global registries
     */
    public DefaultServerEntry( Registries registries )
    {
        atRegistry = registries.getAttributeTypeRegistry();
        dn = LdapDN.EMPTY_LDAPDN;

        // Initialize the ObjectClass object
        initObjectClassAT( registries );
    }


    /**
     * <p>
     * Creates a new instance of DefaultServerEntry, copying 
     * another entry, which can be a ClientEntry. 
     * </p>
     * <p>
     * No attributes will be created.
     * </p> 
     * 
     * @param registries The reference to the global registries
     * @param entry the entry to copy
     */
    public DefaultServerEntry( Registries registries, Entry entry )
    {
        atRegistry = registries.getAttributeTypeRegistry();

        // Initialize the ObjectClass object
        initObjectClassAT( registries );

        // We will clone the existing entry, because it may be normalized
        if ( entry.getDn() != null )
        {
            dn = (LdapDN)entry.getDn().clone();
        }
        else
        {
            dn = LdapDN.EMPTY_LDAPDN;
        }
        
        if ( !dn.isNormalized( ) )
        {
            try
            {
                // The dn must be normalized
                dn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            }
            catch ( NamingException ne )
            {
                LOG.warn( "The DN '" + entry.getDn() + "' cannot be normalized" );
            }
        }
        
        // Init the attributes map
        attributes = new HashMap<AttributeType, EntryAttribute>( entry.size() );
        
        // and copy all the attributes
        for ( EntryAttribute attribute:entry )
        {
            try
            {
                // First get the AttributeType
                AttributeType attributeType = null;

                if ( attribute instanceof ServerAttribute )
                {
                    attributeType = ((ServerAttribute)attribute).getAttributeType();
                }
                else
                {
                    attributeType = registries.getAttributeTypeRegistry().lookup( attribute.getId() );
                }
                
                // Create a new ServerAttribute.
                EntryAttribute serverAttribute = new DefaultServerAttribute( attributeType, attribute );
                
                // And store it
                add( serverAttribute );
            }
            catch ( NamingException ne )
            {
                // Just log a warning
                LOG.warn( "The attribute '" + attribute.getId() + "' cannot be stored" );
            }
        }
    }


    /**
     * <p>
     * Creates a new instance of DefaultServerEntry, with a 
     * DN and registries. 
     * </p>
     * <p>
     * No attributes will be created.
     * </p> 
     * 
     * @param registries The reference to the global registries
     * @param dn The DN for this serverEntry. Can be null.
     */
    public DefaultServerEntry( Registries registries, LdapDN dn )
    {
        if ( dn == null )
        {
            dn = LdapDN.EMPTY_LDAPDN;
        }
        else
        {
            this.dn = dn;
        }
        
        atRegistry = registries.getAttributeTypeRegistry();

        // Initialize the ObjectClass object
        initObjectClassAT( registries );
    }


    /**
     * <p>
     * Creates a new instance of DefaultServerEntry, with a 
     * DN, registries and a list of attributeTypes. 
     * </p>
     * <p>
     * The newly created entry is fed with the list of attributeTypes. No
     * values are associated with those attributeTypes.
     * </p>
     * <p>
     * If any of the AttributeType does not exist, they it's simply discarded.
     * </p>
     * 
     * @param registries The reference to the global registries
     * @param dn The DN for this serverEntry. Can be null.
     * @param attributeTypes The list of attributes to create, without value.
     */
    public DefaultServerEntry( Registries registries, LdapDN dn, AttributeType... attributeTypes )
    {
        if ( dn == null )
        {
            dn = LdapDN.EMPTY_LDAPDN;
        }
        else
        {
            this.dn = dn;
        }

        atRegistry = registries.getAttributeTypeRegistry();

        // Initialize the ObjectClass object
        initObjectClassAT( registries );

        // Add the attributeTypes
        set( attributeTypes );
    }

    
    /**
     * <p>
     * Creates a new instance of DefaultServerEntry, with a 
     * DN, registries and an attributeType with the user provided ID. 
     * </p>
     * <p>
     * The newly created entry is fed with the given attributeType. No
     * values are associated with this attributeType.
     * </p>
     * <p>
     * If the AttributeType does not exist, they it's simply discarded.
     * </p>
     * <p>
     * We also check that the normalized upID equals the AttributeType ID
     * </p>
     * 
     * @param registries The reference to the global registries
     * @param dn The DN for this serverEntry. Can be null.
     * @param attributeType The attribute to create, without value.
     * @param upId The User Provided ID fro this AttributeType
     */
    public DefaultServerEntry( Registries registries, LdapDN dn, AttributeType attributeType, String upId )
    {
        if ( dn == null )
        {
            dn = LdapDN.EMPTY_LDAPDN;
        }
        else
        {
            this.dn = dn;
        }
        
        atRegistry = registries.getAttributeTypeRegistry();
        // Initialize the ObjectClass object

        // Initialize the ObjectClass object
        initObjectClassAT( registries );

        try
        {
            put( upId, attributeType, (String)null );
        }
        catch ( NamingException ne )
        {
            // Just discard the AttributeType
            LOG.error( "We have had an error while adding the '{}' AttributeType : {}", upId, ne.getMessage() );
        }
    }
    
    
    /**
     * Creates a new instance of DefaultServerEntry, with a 
     * DN, registries and a list of IDs. 
     * <p>
     * No attributes will be created except the ObjectClass attribute,
     * which will contains "top". 
     * <p>
     * If any of the AttributeType does not exist, they are simply discarded.
     * 
     * @param registries The reference to the global registries
     * @param dn The DN for this serverEntry. Can be null.
     * @param upIds The list of attributes to create.
     */
    public DefaultServerEntry( Registries registries, LdapDN dn, String... upIds )
    {
        if ( dn == null )
        {
            dn = LdapDN.EMPTY_LDAPDN;
        }
        else
        {
            this.dn = dn;
        }
        
        atRegistry = registries.getAttributeTypeRegistry();

        initObjectClassAT( registries );

        set( upIds );
    }

    
    /**
     * Creates a new instance of DefaultServerEntry, with a 
     * DN, registries and a list of ServerAttributes. 
     * <p>
     * No attributes will be created except the ObjectClass attribute,
     * which will contains "top". 
     * <p>
     * If any of the AttributeType does not exist, they are simply discarded.
     * 
     * @param registries The reference to the global registries
     * @param dn The DN for this serverEntry. Can be null
     * @param attributes The list of attributes to create
     */
    public DefaultServerEntry( Registries registries, LdapDN dn, ServerAttribute... attributes )
    {
        if ( dn == null )
        {
            dn = LdapDN.EMPTY_LDAPDN;
        }
        else
        {
            this.dn = dn;
        }
        
        atRegistry = registries.getAttributeTypeRegistry();

        initObjectClassAT( registries );

        for ( ServerAttribute attribute:attributes )
        {
            // Store a new ServerAttribute
            try
            {
                put( attribute );
            }
            catch ( NamingException ne )
            {
                LOG.warn( "The ServerAttribute '{}' does not exist. It has been discarded", attribute );
            }
        }
    }

    
    //-------------------------------------------------------------------------
    // API
    //-------------------------------------------------------------------------
    /**
     * <p>
     * Add an attribute (represented by its AttributeType and some binary values) into an 
     * entry.
     * </p>
     * <p> 
     * If we already have an attribute with the same values, nothing is done 
     * (duplicated values are not allowed)
     * </p>
     * <p>
     * If the value cannot be added, or if the AttributeType is null or invalid, 
     * a NamingException is thrown.
     * </p>
     *
     * @param attributeType The attribute Type.
     * @param values The list of binary values to inject. It can be empty.
     * @throws NamingException If the attribute does not exist
     */
    public void add( AttributeType attributeType, byte[]... values ) throws NamingException
    {
        if ( attributeType == null )
        {
            String message = "The attributeType should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        // ObjectClass with binary values are not allowed
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            String message = "Only String values supported for objectClass attribute";
            LOG.error(  message  );
            throw new UnsupportedOperationException( message );
        }

        EntryAttribute attribute = attributes.get( attributeType );
        
        if ( attribute != null )
        {
            // This Attribute already exist, we add the values 
            // into it
            attribute.add( values );
        }
        else
        {
            // We have to create a new Attribute and set the values.
            // The upId, which is set to null, will be setup by the 
            // createAttribute method
            createAttribute( null, attributeType, values );
        }
    }


    /**
     * <p>
     * Add an attribute (represented by its AttributeType and some String values) into an 
     * entry.
     * </p>
     * <p> 
     * If we already have an attribute with public the same value, nothing is done 
     * (duplicated values are not allowed)
     * </p>
     * <p>public 
     * If the value cannot be added, or if the AttributeType is null or invalid, 
     * a NamingException is thrown.
     * </p>
     *public 
     * @param attributeType The attribute Type
     * @param values The list of binary values to inject. It can be empty
     * @throws NamingException If the attribute does not exist
     */
    public void add( AttributeType attributeType, String... values ) throws NamingException
    {    
        if ( attributeType == null )
        {
            String message = "The attributeType should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        EntryAttribute attribute = attributes.get( attributeType );
        
        if ( attribute != null )
        {
            // This Attribute already exist, we add the values 
            // into it
            attribute.add( values );
        }
        else
        {
            // We have to create a new Attribute and set the values.
            // The upId, which is set to null, will be setup by the 
            // createAttribute method
            createAttribute( null, attributeType, values );
        }
    }

    
    /**
     * <p>
     * Add an attribute (represented by its AttributeType and some values) into an 
     * entry.
     * </p>
     * <p> 
     * If we already have an attribute with the same value, nothing is done.
     * (duplicated values are not allowed)
     * </p>
     * <p>
     * If the value cannot be added, or if the AttributeType is null or invalid, 
     * a NamingException is thrown.
     * </p>
     *
     * @param attributeType The attribute Type
     * @param values The list of binary values to inject. It can be empty
     * @throws NamingException If the attribute does not exist
     */
    public void add( AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        if ( attributeType == null )
        {
            String message = "The attributeType should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        EntryAttribute attribute = attributes.get( attributeType );
    
        if ( attribute != null )
        {
            // This Attribute already exist, we add the values 
            // into it
            attribute.add( values );
        }
        else
        {
            // We have to create a new Attribute and set the values.
            // The upId, which is set to null, will be setup by the 
            // createAttribute method
            createAttribute( null, attributeType, values );
        }
    }


    /**
     * Add some EntryAttributes to the current Entry.
     *
     * @param attributes The attributes to add
     * @throws NamingException If we can't add any of the attributes
     */
    public void add( EntryAttribute... attributes ) throws NamingException
    {
        for ( EntryAttribute attribute:attributes )
        {
            ServerAttribute serverAttribute = (ServerAttribute)attribute;
            AttributeType attributeType = serverAttribute.getAttributeType();
            
            if ( this.attributes.containsKey( attributeType ) )
            {
                // We already have an attribute with the same AttributeType
                // Just add the new values into it.
                EntryAttribute oldAttribute = this.attributes.get( attributeType );
                
                for ( Value<?> value:serverAttribute )
                {
                    oldAttribute.add( value );
                }
                
                // And update the upId
                oldAttribute.setUpId( serverAttribute.getUpId() );
            }
            else
            {
                // The attributeType does not exist, add it
                this.attributes.put( attributeType, attribute );
            }
        }
    }


    /**
     * <p>
     * Add an attribute (represented by its AttributeType and some binary values) into an 
     * entry. Set the User Provider ID at the same time
     * </p>
     * <p> 
     * If we already have an attribute with the same values, nothing is done 
     * (duplicated values are not allowed)
     * </p>
     * <p>
     * If the value cannot be added, or if the AttributeType is null or invalid, 
     * a NamingException is thrown.
     * </p>
     *
     * @param upId The user provided ID for the added AttributeType
     * @param attributeType The attribute Type.
     * @param values The list of binary values to add. It can be empty.
     * @throws NamingException If the attribute does not exist
     */
    public void add( String upId, AttributeType attributeType, byte[]... values ) throws NamingException
    {
        // ObjectClass with binary values are not allowed
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            String message = "Only String values supported for objectClass attribute";
            LOG.error(  message  );
            throw new UnsupportedOperationException( message );
        }

        ServerAttribute attribute = (ServerAttribute)attributes.get( attributeType );
        
        upId = getUpId( upId, attributeType );
        
        if ( attribute != null )
        {
            // This Attribute already exist, we add the values 
            // into it
            attribute.add( values );
            attribute.setUpId( upId, attributeType );
        }
        else
        {
            // We have to create a new Attribute and set the values
            // and the upId
            createAttribute( upId, attributeType, values );
        }
    }


    /**
     * <p>
     * Add an attribute (represented by its AttributeType and some values) into an 
     * entry. Set the User Provider ID at the same time
     * </p>
     * <p> 
     * If we already have an attribute with the same values, nothing is done 
     * (duplicated values are not allowed)
     * </p>
     * <p>
     * If the value cannot be added, or if the AttributeType is null or invalid, 
     * a NamingException is thrown.
     * </p>
     *
     * @param upId The user provided ID for the added AttributeType
     * @param attributeType The attribute Type.
     * @param values The list of values to add. It can be empty.
     * @throws NamingException If the attribute does not exist
     */
    public void add( String upId, AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        if ( attributeType == null )
        {
            String message = "The attributeType should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        upId = getUpId( upId, attributeType );
        
        ServerAttribute attribute = (ServerAttribute)attributes.get( attributeType );
    
        if ( attribute != null )
        {
            // This Attribute already exist, we add the values 
            // into it
            attribute.add( values );
            attribute.setUpId( upId, attributeType );
        }
        else
        {
            createAttribute( upId, attributeType, values );
        }
    }

    
    /**
     * Adds a new attribute with some String values into an entry, setting
     * the User Provided ID in the same time.
     *
     * @param upId The User provided ID
     * @param attributeType The associated AttributeType
     * @param values The String values to store into the new Attribute
     * @throws NamingException 
     */
    public void add( String upId, AttributeType attributeType, String... values ) throws NamingException
    {
        if ( attributeType == null )
        {
            String message = "The attributeType should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        upId = getUpId( upId, attributeType );

        ServerAttribute attribute = (ServerAttribute)attributes.get( attributeType );
        
        if ( attribute != null )
        {
            // This Attribute already exist, we add the values 
            // into it
            attribute.add( values );
            attribute.setUpId( upId, attributeType );
        }
        else
        {
            // We have to create a new Attribute and set the values
            // and the upId
            createAttribute( upId, attributeType, values );
        }
    }


    /**                 
     * Add an attribute (represented by its ID and binary values) into an entry. 
     *
     * @param upId The attribute ID
     * @param values The list of binary values to inject. It can be empty
     * @throws NamingException If the attribute does not exist
     */
    public void add( String upId, byte[]... values ) throws NamingException
    {
        add( upId, getAttributeType( upId ), values );
    }


    /**
     * Add an attribute (represented by its ID and string values) into an entry. 
     *
     * @param upId The attribute ID
     * @param values The list of string values to inject. It can be empty
     * @throws NamingException If the attribute does not exist
     */
    public void add( String upId, String... values ) throws NamingException
    {
        add( upId, getAttributeType( upId ), values );
    }


    /**
     * Add an attribute (represented by its ID and Value values) into an entry. 
     *
     * @param upId The attribute ID
     * @param values The list of Value values to inject. It can be empty
     * @throws NamingException If the attribute does not exist
     */
    public void add( String upId, Value<?>... values ) throws NamingException
    {
        add( upId, getAttributeType( upId ), values );
    }


    /**
     * Checks if an entry contains an attribute with some given binary values.
     *
     * @param attributeType The Attribute we are looking for.
     * @param values The searched values.
     * @return <code>true</code> if all the values are found within the attribute,
     * <code>false</code> otherwise, or if the attributes does not exist.
     */
    public boolean contains( AttributeType attributeType, byte[]... values )
    {
        if ( attributeType == null )
        {
            return false;
        }
        
        EntryAttribute attribute = attributes.get( attributeType );
        
        if ( attribute != null )
        {
            return attribute.contains( values );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with some given String values.
     *
     * @param attributeType The Attribute we are looking for.
     * @param values The searched values.
     * @return <code>true</code> if all the values are found within the attribute,
     * <code>false</code> otherwise, or if the attributes does not exist.
     */
    public boolean contains( AttributeType attributeType, String... values )
    {
        if ( attributeType == null )
        {
            return false;
        }

        EntryAttribute attribute = attributes.get( attributeType );
        
        if ( attribute != null )
        {
            return attribute.contains( values );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with some given binary values.
     *
     * @param attributeType The Attribute we are looking for.
     * @param values The searched values.
     * @return <code>true</code> if all the values are found within the attribute,
     * <code>false</code> otherwise, or if the attributes does not exist.
     */
    public boolean contains( AttributeType attributeType, Value<?>... values )
    {
        if ( attributeType == null )
        {
            return false;
        }
        
        EntryAttribute attribute = attributes.get( attributeType );
        
        if ( attribute != null )
        {
            return attribute.contains( values );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * <p>
     * Checks if an entry contains a list of attributes.
     * </p>
     * <p>
     * If the list is null or empty, this method will return <code>true</code>
     * if the entry has no attribute, <code>false</code> otherwise.
     * </p>
     *
     * @param attributes The Attributes to look for
     * @return <code>true</code> if all the attributes are found within 
     * the entry, <code>false</code> if at least one of them is not present.
     * @throws NamingException If the attribute does not exist
     */
    public boolean contains( EntryAttribute... attributes ) throws NamingException
    {
        for ( EntryAttribute entryAttribute:attributes )
        {
            if ( entryAttribute == null )
            {
                return this.attributes.size() == 0;
            }
            
            if ( !this.attributes.containsKey( ((ServerAttribute)entryAttribute).getAttributeType() ) )
            {
                return false;
            }
        }
        
        return true;
    }


    /**
     * Checks if an entry contains an attribute with some binary values.
     *
     * @param id The Attribute we are looking for.
     * @param values The searched values.
     * @return <code>true</code> if all the values are found within the attribute,
     * false if at least one value is not present or if the ID is not valid. 
     */
    public boolean contains( String id, byte[]... values )
    {
        if ( id == null )
        {
            return false;
        }
        
        try
        {
            AttributeType attributeType = atRegistry.lookup( id );
            
            if ( attributeType == null )
            {
                return false;
            }
            
            EntryAttribute attribute = attributes.get( attributeType );
            
            if ( attribute != null )
            {
                return attribute.contains( values );
            }
            else
            {
                return false;
            }
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with some String values.
     *
     * @param id The Attribute we are looking for.
     * @param values The searched values.
     * @return <code>true</code> if all the values are found within the attribute,
     * false if at least one value is not present or if the ID is not valid. 
     */
    public boolean contains( String id, String... values )
    {
        if ( id == null )
        {
            return false;
        }
        
        try
        {
            AttributeType attributeType = atRegistry.lookup( id );
            
            if ( attributeType == null )
            {
                return false;
            }
            
            EntryAttribute attribute = attributes.get( attributeType );
            
            if ( attribute != null )
            {
                return attribute.contains( values );
            }
            else
            {
                return false;
            }
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with some values.
     *
     * @param id The Attribute we are looking for.
     * @param values The searched values.
     * @return <code>true</code> if all the values are found within the attribute,
     * false if at least one value is not present or if the ID is not valid. 
     */
    public boolean contains( String id, Value<?>... values )
    {
        if ( id == null )
        {
            return false;
        }
        
        try
        {
            AttributeType attributeType = atRegistry.lookup( id );
            
            if ( attributeType == null )
            {
                return false;
            }

            EntryAttribute attribute = attributes.get( attributeType );
            
            if ( attribute != null )
            {
                return attribute.contains( values );
            }
            else
            {
                return false;
            }
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains a specific AttributeType.
     *
     * @param attributeType The AttributeType to look for.
     * @return <code>true</code> if the attribute is found within the entry.
     */
    public boolean containsAttribute( AttributeType attributeType )
    {
        return attributes.containsKey( attributeType );
    }

    
    /**
     * Checks if an entry contains some specific attributes.
     *
     * @param attributes The Attributes to look for.
     * @return <code>true</code> if the attributes are all found within the entry.
     */
    public boolean containsAttribute( String... attributes )
    {
        for ( String attribute:attributes )
        {
            try
            {
                if ( !this.attributes.containsKey( getAttributeType( attribute ) ) )
                {
                    return false;
                }
            }
            catch ( NamingException ne )
            {
                return false;
            }
        }
        
        return true;
    }

    
    /**
     * <p>
     * Returns the attribute with the specified AttributeType. The return value
     * is <code>null</code> if no match is found.  
     * </p>
     *
     * @param attributeType The attributeType we are looking for.
     * @return the attribute associated with the AttributeType.
     */
    /**
     * Returns the attribute associated with an AttributeType
     * 
     * @param attributeType the AttributeType we are looking for
     * @return the associated attribute
     */
    public EntryAttribute get( AttributeType attributeType )
    {
        return attributes.get( attributeType );
    }


    /**
     * <p>
     * Returns the attribute with the specified alias. The return value
     * is <code>null</code> if no match is found.  
     * </p>
     * <p>An Attribute with an id different from the supplied alias may 
     * be returned: for example a call with 'cn' may in some implementations 
     * return an Attribute whose getId() field returns 'commonName'.
     * </p>
     * <p>
     * If the attributeType is not found, returns null.
     * </p>
     *
     * @param alias an aliased name of the attribute identifier
     * @return the attribute associated with the alias
     */
    public EntryAttribute get( String alias )
    {
        try
        {
            return get( atRegistry.lookup( alias ) );
        }
        catch ( NamingException ne )
        {
            String message = ne.getMessage();
            LOG.error( message );
            return null;
        }
    }


    /**
     * Gets all the attributes type
     *
     * @return The combined set of all the attributes, including ObjectClass.
     */
    public Set<AttributeType> getAttributeTypes()
    {
        return attributes.keySet();
    }
    
    
    /**
     * Tells if an entry has a specific ObjectClass value
     * 
     * @param objectClass The ObjectClass ID we want to check
     * @return <code>true</code> if the ObjectClass value is present 
     * in the ObjectClass attribute
     */
    public boolean hasObjectClass( String objectClass )
    {
        return contains( OBJECT_CLASS_AT, objectClass );
    }

    
    /**
     * Tells if an entry has a specific ObjectClass Attribute
     * 
     * @param objectClass The ObjectClass we want to check
     * @return <code>true</code> if the ObjectClass value is present 
     * in the ObjectClass attribute
     */
    public boolean hasObjectClass( EntryAttribute objectClass )
    {
        if ( objectClass == null )
        {
            return false;
        }
        
        // We have to check that we are checking the ObjectClass attributeType
        if ( !((ServerAttribute)objectClass).getAttributeType().equals( OBJECT_CLASS_AT ) )
        {
            return false;
        }
        
        EntryAttribute attribute = attributes.get( OBJECT_CLASS_AT );
        
        if ( attribute == null )
        {
            // The entry does not have an ObjectClass attribute
            return false;
        }
        
        for ( Value<?> value:objectClass )
        {
            // Loop on all the values, and check if they are present
            if ( !attribute.contains( value.getString() ) )
            {
                return false;
            }
        }
        
        return true;
    }

    
    /**
     * Fail fast check performed to determine entry consistency according to schema
     * characteristics.
     *
     * @return true if the entry, it's attributes and their values are consistent
     * with the schema
     */
    public boolean isValid()
    {
        // @TODO Implement me !
        throw new NotImplementedException();
    }


    /**
     * Check performed to determine entry consistency according to the schema
     * requirements of a particular objectClass.  The entry must be of that objectClass
     * to return true: meaning if the entry's objectClass attribute does not contain
     * the objectClass argument, then false should be returned.
     *
     * @param objectClass the objectClass to use while checking for validity
     * @return true if the entry, it's attributes and their values are consistent
     * with the objectClass
     */
    public boolean isValid( EntryAttribute objectClass )
    {
        // @TODO Implement me !
        throw new NotImplementedException();
    }


    /**
     * Check performed to determine entry consistency according to the schema
     * requirements of a particular objectClass.  The entry must be of that objectClass
     * to return true: meaning if the entry's objectClass attribute does not contain
     * the objectClass argument, then false should be returned.
     *
     * @param objectClass the objectClass to use while checking for validity
     * @return true if the entry, it's attributes and their values are consistent
     * with the objectClass
     */
    public boolean isValid( String objectClass )
    {
        // @TODO Implement me !
        throw new NotImplementedException();
    }


    /**
     * <p>
     * Places a new attribute with the supplied AttributeType and binary values 
     * into the attribute collection. 
     * </p>
     * <p>
     * If there is already an attribute with the same AttributeType, the old
     * one is removed from the collection and is returned by this method. 
     * </p>
     * <p>
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value may be <code>null</code>.
     *
     * @param attributeType the type of the new attribute to be put
     * @param values the binary values of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     * <code>null</code>
     * @throws NamingException if there are failures
     */
    public EntryAttribute put( AttributeType attributeType, byte[]... values ) throws NamingException
    {
        return put( null, attributeType, values );
    }


    /**
     * <p>
     * Places a new attribute with the supplied AttributeType and String values 
     * into the attribute collection. 
     * </p>
     * <p>
     * If there is already an attribute with the same AttributeType, the old
     * one is removed from the collection and is returned by this method. 
     * </p>
     * <p>
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value may be <code>null</code>.
     *
     * @param attributeType the type of the new attribute to be put
     * @param values the String values of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     * <code>null</code>
     * @throws NamingException if there are failures
     */
    public EntryAttribute put( AttributeType attributeType, String... values ) throws NamingException
    {
        return put( null, attributeType, values );
    }

    
    /**
     * <p>
     * Places a new attribute with the supplied AttributeType and some values 
     * into the attribute collection. 
     * </p>
     * <p>
     * If there is already an attribute with the same AttributeType, the old
     * one is removed from the collection and is returned by this method. 
     * </p>
     * <p>
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value may be <code>null</code>.
     *
     * @param attributeType the type of the new attribute to be put
     * @param values the values of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     * <code>null</code>
     * @throws NamingException if there are failures
     */
    public EntryAttribute put( AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        return put( null, attributeType, values );
    }


    /**
     * <p>
     * Places attributes in the attribute collection. 
     * </p>
     * <p>If there is already an attribute with the same ID as any of the 
     * new attributes, the old ones are removed from the collection and 
     * are returned by this method. If there was no attribute with the 
     * same ID the return value is <code>null</code>.
     *</p>
     *
     * @param attributes the attributes to be put
     * @return the old attributes with the same OID, if exist; otherwise
     *         <code>null</code>
     * @exception NamingException if the operation fails
     */
    public List<EntryAttribute> put( EntryAttribute... attributes ) throws NamingException
    {
        List<EntryAttribute> previous = new ArrayList<EntryAttribute>();
        
        for ( EntryAttribute serverAttribute:attributes )
        {
            if ( serverAttribute == null )
            {
                String message = "The ServerAttribute list should not contain null elements";
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
            
            EntryAttribute removed = this.attributes.put( ((ServerAttribute)serverAttribute).getAttributeType(), serverAttribute );
            
            if ( removed != null )
            {
                previous.add( removed );
            }
        }
        
        return previous;
    }


    /**
     * <p>
     * Places a new attribute with the supplied AttributeType and some binary values 
     * into the attribute collection. 
     * </p>
     * <p>
     * The given User provided ID will be used for this new AttributeEntry.
     * </p>
     * <p>
     * If there is already an attribute with the same AttributeType, the old
     * one is removed from the collection and is returned by this method. 
     * </p>
     * <p>
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value may be <code>null</code>.
     *
     * @param upId The User Provided ID to be stored into the AttributeEntry
     * @param attributeType the type of the new attribute to be put
     * @param values the binary values of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     * <code>null</code>
     * @throws NamingException if there are failures.
     */
    public EntryAttribute put( String upId, AttributeType attributeType, byte[]... values ) throws NamingException
    {
        if ( attributeType == null )
        {
            String message = "The attributeType should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        if ( !StringTools.isEmpty( upId ) )
        {
            AttributeType tempAT = getAttributeType( upId );
        
            if ( !tempAT.equals( attributeType ) )
            {
                String message = "The '" + upId + "' id is not compatible with the '" + attributeType + "' attribute type";
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
        else
        {
            upId = getUpId( upId, attributeType );
        }
        
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            String message = "Only String values supported for objectClass attribute";
            LOG.error( message );
            throw new UnsupportedOperationException( message );
        }

        EntryAttribute attribute = new DefaultServerAttribute( upId, attributeType, values );
        return attributes.put( attributeType, attribute );
    }


    /**
     * <p>
     * Places a new attribute with the supplied AttributeType and some String values 
     * into the attribute collection. 
     * </p>
     * <p>
     * The given User provided ID will be used for this new AttributeEntry.
     * </p>
     * <p>
     * If there is already an attribute with the same AttributeType, the old
     * one is removed from the collection and is returned by this method. 
     * </p>
     * <p>
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value may be <code>null</code>.
     *
     * @param upId The User Provided ID to be stored into the AttributeEntry
     * @param attributeType the type of the new attribute to be put
     * @param values the String values of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     * <code>null</code>
     * @throws NamingException if there are failures.
     */
    public EntryAttribute put( String upId, AttributeType attributeType, String... values ) throws NamingException
    {
        if ( attributeType == null )
        {
            try
            {
                attributeType = getAttributeType( upId );
            }
            catch ( IllegalArgumentException iae )
            {
                String message = "The attributeType should not be null";
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
        else
        {
            if ( !StringTools.isEmpty( upId ) )
            {
                AttributeType tempAT = getAttributeType( upId );
            
                if ( !tempAT.equals( attributeType ) )
                {
                    String message = "The '" + upId + "' id is not compatible with the '" + attributeType + "' attribute type";
                    LOG.error( message );
                    throw new IllegalArgumentException( message );
                }
            }
            else
            {
                upId = getUpId( upId, attributeType );
            }
        }
        
        EntryAttribute attribute = new DefaultServerAttribute( upId, attributeType, values );
        return attributes.put( attributeType, attribute );
    }


    /**
     * <p>
     * Places a new attribute with the supplied AttributeType and some values 
     * into the attribute collection. 
     * </p>
     * <p>
     * The given User provided ID will be used for this new AttributeEntry.
     * </p>
     * <p>
     * If there is already an attribute with the same AttributeType, the old
     * one is removed from the collection and is returned by this method. 
     * </p>
     * <p>
     * This method provides a mechanism to put an attribute with a
     * <code>null</code> value: the value may be <code>null</code>.
     *
     * @param upId The User Provided ID to be stored into the AttributeEntry
     * @param attributeType the type of the new attribute to be put
     * @param values the values of the new attribute to be put
     * @return the old attribute with the same identifier, if exists; otherwise
     * <code>null</code>
     * @throws NamingException if there are failures.
     */
    public EntryAttribute put( String upId, AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        if ( attributeType == null )
        {
            String message = "The attributeType should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        if ( !StringTools.isEmpty( upId ) )
        {
            AttributeType tempAT = getAttributeType( upId );
        
            if ( !tempAT.equals( attributeType ) )
            {
                String message = "The '" + upId + "' id is not compatible with the '" + attributeType + "' attribute type";
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
        else
        {
            upId = getUpId( upId, attributeType );
        }
        
        EntryAttribute attribute = new DefaultServerAttribute( upId, attributeType, values );
        return attributes.put( attributeType, attribute );
    }


    /**
     * <p>
     * Put an attribute (represented by its ID and some binary values) into an entry. 
     * </p>
     * <p> 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     * </p>
     * <p>
     * If the upId is not the ID of an existing AttributeType, an IllegalArgumentException is thrown.
     * </p>
     *
     * @param upId The attribute ID
     * @param values The list of binary values to put. It can be empty.
     * @return The replaced attribute
     */
    public EntryAttribute put( String upId, byte[]... values )
    {
        try
        {
            return put( upId, getAttributeType( upId ), values );
        }
        catch ( NamingException ne )
        {
            String message = "Error while adding values into the '" + upId + "' attribute. Error : " + 
                ne.getMessage();
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
    }


    /**
     * <p>
     * Put an attribute (represented by its ID and some String values) into an entry. 
     * </p>
     * <p> 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     * </p>
     * <p>
     * If the upId is not the ID of an existing AttributeType, an IllegalArgumentException is thrown.
     * </p>
     *
     * @param upId The attribute ID
     * @param values The list of String values to put. It can be empty.
     * @return The replaced attribute
     */
    public EntryAttribute put( String upId, String... values )
    {            

        try
        {
            return put( upId, getAttributeType( upId ), values );
        }
        catch ( NamingException ne )
        {
            String message = "Error while adding values into the '" + upId + "' attribute. Error : " + 
                ne.getMessage();
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
    }


    /**
     * <p>
     * Put an attribute (represented by its ID and some values) into an entry. 
     * </p>
     * <p> 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     * </p>
     * <p>
     * If the upId is not the ID of an existing AttributeType, an IllegalArgumentException is thrown.
     * </p>
     *
     * @param upId The attribute ID
     * @param values The list of values to put. It can be empty.
     * @return The replaced attribute
     */
    public EntryAttribute put( String upId, Value<?>... values )
    {
        try
        {
            return put( upId, getAttributeType( upId ), values );
        }
        catch ( NamingException ne )
        {
            String message = "Error while adding values into the '" + upId + "' attribute. Error : " + 
                ne.getMessage();
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
    }


    /**
     * <p>
     * Removes the specified binary values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns 
     * <code>false</code>
     * </p> 
     *
     * @param attributeType The attribute type  
     * @param values the values to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist. 
     */
    public boolean remove( AttributeType attributeType, byte[]... values ) throws NamingException
    {
        try
        {
            EntryAttribute attribute = attributes.get( attributeType );
            
            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }
            
            int nbOldValues = attribute.size();
            
            // Remove the values
            attribute.remove( values );
            
            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( attributeType );
                
                return true;
            }
            
            if ( nbOldValues != attribute.size() )
            {
                // At least one value have been removed, return true.
                return true;
            }
            else
            {
                // No values have been removed, return false.
                return false;
            }
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( "The removal of values for the missing '{}' attribute is not possible", attributeType );
            return false;
        }
    }
    
    
    /**
     * <p>
     * Removes the specified String values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns 
     * <code>false</code>
     * </p> 
     *
     * @param attributeType The attribute type  
     * @param values the values to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist. 
     */
    public boolean remove( AttributeType attributeType, String... values ) throws NamingException
    {
        try
        {
            EntryAttribute attribute = attributes.get( attributeType );
            
            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }
            
            int nbOldValues = attribute.size();
            
            // Remove the values
            attribute.remove( values );
            
            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( attributeType );
                
                return true;
            }
            
            if ( nbOldValues != attribute.size() )
            {
                // At least one value have been removed, return true.
                return true;
            }
            else
            {
                // No values have been removed, return false.
                return false;
            }
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( "The removal of values for the missing '{}' attribute is not possible", attributeType );
            return false;
        }
    }
    
    
    /**
     * <p>
     * Removes the specified values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns 
     * <code>false</code>
     * </p> 
     *
     * @param attributeType The attribute type  
     * @param values the values to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist. 
     */
    public boolean remove( AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        try
        {
            EntryAttribute attribute = attributes.get( attributeType );
            
            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }
            
            int nbOldValues = attribute.size();
            
            // Remove the values
            attribute.remove( values );
            
            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( attributeType );
                
                return true;
            }
            
            if ( nbOldValues != attribute.size() )
            {
                // At least one value have been removed, return true.
                return true;
            }
            else
            {
                // No values have been removed, return false.
                return false;
            }
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( "The removal of values for the missing '{}' attribute is not possible", attributeType );
            return false;
        }
    }
    
    
    public List<EntryAttribute> remove( EntryAttribute... attributes ) throws NamingException
    {
        List<EntryAttribute> removedAttributes = new ArrayList<EntryAttribute>();
        
        for ( EntryAttribute serverAttribute:attributes )
        {
            if ( this.attributes.containsKey( ((ServerAttribute)serverAttribute).getAttributeType() ) )
            {
                this.attributes.remove( ((ServerAttribute)serverAttribute).getAttributeType() );
                removedAttributes.add( serverAttribute );
            }
        }
        
        return removedAttributes;
    }


    /**
     * <p>
     * Removes the specified binary values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns 
     * <code>false</code>
     * </p> 
     *
     * @param upId The attribute ID 
     * @param values the values to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist. 
     */
    public boolean remove( String upId, byte[]... values ) throws NamingException
    {
        try
        {
            AttributeType attributeType = getAttributeType( upId );

            return remove( attributeType, values );
        }
        catch ( NamingException ne )
        {
            LOG.error( "The removal of values for the missing '{}' attribute is not possible", upId );
            return false;
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( "The removal of values for the bad '{}' attribute is not possible", upId );
            return false;
        }
    }
    
    
    /**
     * <p>
     * Removes the specified String values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns 
     * <code>false</code>
     * </p> 
     *
     * @param upId The attribute ID 
     * @param values the values to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist. 
     */
    public boolean remove( String upId, String... values ) throws NamingException
    {
        try
        {
            AttributeType attributeType = getAttributeType( upId );

            return remove( attributeType, values );
        }
        catch ( NamingException ne )
        {
            LOG.error( "The removal of values for the missing '{}' attribute is not possible", upId );
            return false;
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( "The removal of values for the bad '{}' attribute is not possible", upId );
            return false;
        }
    }
    
    
    /**
     * <p>
     * Removes the specified Value values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns 
     * <code>false</code>
     * </p> 
     *
     * @param upId The attribute ID 
     * @param values the values to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist. 
     */
    public boolean remove( String upId, Value<?>... values ) throws NamingException
    {
        try
        {
            AttributeType attributeType = getAttributeType( upId );

            return remove( attributeType, values );
        }
        catch ( NamingException ne )
        {
            LOG.error( "The removal of values for the missing '{}' attribute is not possible", upId );
            return false;
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( "The removal of values for the bad '{}' attribute is not possible", upId );
            return false;
        }
    }
    
    
    /**
     * <p>
     * Removes the attribute with the specified AttributeTypes. 
     * </p>
     * <p>
     * The removed attribute are returned by this method. 
     * </p>
     * <p>
     * If there is no attribute with the specified AttributeTypes,
     * the return value is <code>null</code>.
     * </p>
     *
     * @param attributes the AttributeTypes to be removed
     * @return the removed attributes, if any, as a list; otherwise <code>null</code>
     */
    public List<EntryAttribute> removeAttributes( AttributeType... attributes )
    {
        if ( attributes.length == 0 )
        {
            return null;
        }
        
        List<EntryAttribute> removed = new ArrayList<EntryAttribute>( attributes.length );
        
        for ( AttributeType attributeType:attributes )
        {
            EntryAttribute attr = this.attributes.remove( attributeType );
            
            if ( attr != null )
            {
                removed.add( attr );
            }
        }
        
        if ( removed.size() == 0 )
        {
            return null;
        }
        else
        {
            return removed;
        }
    }
    
    
    /**
     * <p>
     * Removes the attribute with the specified alias. 
     * </p>
     * <p>
     * The removed attribute are returned by this method. 
     * </p>
     * <p>
     * If there is no attribute with the specified alias,
     * the return value is <code>null</code>.
     * </p>
     *
     * @param attributes an aliased name of the attribute to be removed
     * @return the removed attributes, if any, as a list; otherwise <code>null</code>
     */
    public List<EntryAttribute> removeAttributes( String... attributes )
    {
        if ( attributes.length == 0 )
        {
            return null;
        }
        
        List<EntryAttribute> removed = new ArrayList<EntryAttribute>( attributes.length );
        
        for ( String attribute:attributes )
        {
            AttributeType attributeType = null;
            
            try
            {
                attributeType = atRegistry.lookup( attribute );
            }
            catch ( NamingException ne )
            {
                String message = "The attribute '" + attribute + "' does not exist in the entry";
                LOG.warn( message );
                continue;
            }
    
            EntryAttribute attr = this.attributes.remove( attributeType );
            
            if ( attr != null )
            {
                removed.add( attr );
            }
        }
        
        if ( removed.size() == 0 )
        {
            return null;
        }
        else
        {
            return removed;
        }
    }


    /**
     * <p>
     * Put some new attributes using the attributeTypes. 
     * No value is inserted. 
     * </p>
     * <p>
     * If an existing Attribute is found, it will be replaced by an
     * empty attribute, and returned to the caller.
     * </p>
     * 
     * @param attributeTypes The AttributeTypes to add.
     * @return A list of replaced Attributes, of <code>null</code> if no attribute are removed.
     */
    public List<EntryAttribute> set( AttributeType... attributeTypes )
    {
        List<EntryAttribute> removed = new ArrayList<EntryAttribute>();
        
        // Now, loop on all the attributeType to add
        for ( AttributeType attributeType:attributeTypes )
        {
            if ( attributeType == null )
            {
                String message = "The AttributeType list should not contain null values";
                LOG.error( message );
                continue;
            }
            
            EntryAttribute attribute = attributes.put( attributeType, new DefaultServerAttribute( attributeType ) );

            if ( attribute != null )
            {
                removed.add( attribute );
            }
        }
        
        if ( removed.size() == 0 )
        {
            return null;
        }
        else
        {
            return removed;
        }
    }

    
    /**
     * <p>
     * Put some new EntryAttribute using the User Provided ID. 
     * No value is inserted. 
     * </p>
     * <p>
     * If an existing Attribute is found, it will be replaced by an
     * empty attribute, and returned to the caller.
     * </p>
     * 
     * @param upIds The user provided IDs of the AttributeTypes to add.
     * @return A list of replaced Attributes.
     */
    public List<EntryAttribute> set( String... upIds )
    {
        List<EntryAttribute> removed = new ArrayList<EntryAttribute>();
        
        for ( String upId:upIds )
        {
            // Search for the corresponding AttributeType, based on the upID 
            AttributeType attributeType = null;
            
            try
            {
                attributeType = getAttributeType( upId );
            }
            catch ( NamingException ne )
            {
                LOG.warn( "Trying to add a bad attribute type '{}', error : ", upId, ne.getMessage() );
                continue;
            }
            catch ( IllegalArgumentException iae )
            {
                LOG.warn( "Trying to add a bad attribute type '{}', error : ", upId, iae.getMessage() );
                continue;
            }
            
            EntryAttribute attribute = attributes.put( attributeType, 
                new DefaultServerAttribute( upId, attributeType ));
            
            if ( attribute != null )
            {
                removed.add( attribute );
            }
        }
        
        if ( removed.size() == 0 )
        {
            return null;
        }
        else
        {
            return removed;
        }
    }


    /**
     * Convert the ServerEntry to a ClientEntry
     *
     * @return An instance of ClientEntry
     */
    public Entry toClientEntry() throws NamingException
    {
        // Copy the DN
        Entry clientEntry = new DefaultClientEntry( dn );
        
        // Convert each attribute 
        for ( EntryAttribute serverAttribute:this )
        {
            EntryAttribute clientAttribute = ((ServerAttribute)serverAttribute).toClientAttribute();
            clientEntry.add( clientAttribute );
        }
        
        return clientEntry;
    }
    
    
    //-------------------------------------------------------------------------
    // Object methods
    //-------------------------------------------------------------------------
    /**
     * Clone an entry. All the element are duplicated, so a modification on
     * the original object won't affect the cloned object, as a modification
     * on the cloned object has no impact on the original object
     */
    public Entry clone()
    {
        // First, clone the structure
        DefaultServerEntry clone = (DefaultServerEntry)super.clone();
        
        // A serverEntry has a DN, an ObjectClass attribute
        // and many attributes.
        // Clone the DN  first.
        if ( dn != null )
        {
            clone.dn = (LdapDN)dn.clone();
        }
        
        // clone the ServerAttribute Map
        clone.attributes = (Map<AttributeType, EntryAttribute>)(((HashMap<AttributeType, EntryAttribute>)attributes).clone());
        
        // now clone all the servrAttributes
        clone.attributes.clear();
        
        for ( AttributeType key:attributes.keySet() )
        {
            EntryAttribute value = (ServerAttribute)attributes.get( key ).clone();
            clone.attributes.put( key, value );
        }
        
        // We are done !
        return clone;
    }
    

    /**
     * @see java.io.Externalizable#writeExternal(ObjectOutput)
     * 
     * We can't use this method for a ServerEntry, as we have to feed the entry
     * with an registries reference
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        throw new IllegalStateException( "Cannot use standard serialization for a ServerEntry" );
    }
    
    
    /**
     * Serialize a server entry.
     * 
     * The structure is the following :
     * 
     * <b>[DN]</b> : The entry DN. can be empty
     * <b>[numberAttr]</b> : the bumber of attributes. Can be 0 
     * <b>[attribute's oid]*</b> : The attribute's OID to get back 
     * the attributeType on deserialization
     * <b>[Attribute]*</b> The attribute
     * 
     * @param out the buffer in which the data will be serialized
     * @throws IOException if the serialization failed
     */
    public void serialize( ObjectOutput out ) throws IOException
    {
        // First, the DN
        // Write the DN
        LdapDNSerializer.serialize( dn, out );
        
        // Then the attributes.
        out.writeInt( attributes.size() );
        
        // Iterate through the keys. We store the Attribute
        // here, to be able to restore it in the readExternal :
        // we need access to the registries, which are not available
        // in the ServerAttribute class.
        for ( AttributeType attributeType:attributes.keySet() )
        {
            // Write the oid to be able to restore the AttributeType when deserializing
            // the attribute
            String oid = attributeType.getOid();
            
            out.writeUTF( oid );
            
            // Get the attribute
            DefaultServerAttribute attribute = (DefaultServerAttribute)attributes.get( attributeType );

            // Write the attribute
            attribute.serialize( out );
        }
    }

    
    /**
     * @see java.io.Externalizable#readExternal(ObjectInput)
     * 
     * We can't use this method for a ServerEntry, as we have to feed the entry
     * with an registries reference
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        throw new IllegalStateException( "Cannot use standard serialization for a ServerAttribute" );
    }
    
    
    /**
     * Deserialize a server entry. 
     * 
     * @param in The buffer containing the serialized serverEntry
     * @throws IOException if there was a problem when deserializing
     * @throws ClassNotFoundException if we can't deserialize an expected object
     */
    public void deserialize( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the DN
        dn = LdapDNSerializer.deserialize( in );
        
        // Read the number of attributes
        int nbAttributes = in.readInt();
        
        // Read the attributes
        for ( int i = 0; i < nbAttributes; i++ )
        {
            // Read the attribute's OID
            String oid = in.readUTF();
            
            try
            {
                AttributeType attributeType = atRegistry.lookup( oid );
                
                // Create the attribute we will read
                DefaultServerAttribute attribute = new DefaultServerAttribute( attributeType );
                
                // Read the attribute
                attribute.deserialize( in );
                
                attributes.put( attributeType, attribute );
            }
            catch ( NamingException ne )
            {
                // We weren't able to find the OID. The attribute will not be added
                LOG.warn( "Cannot read the attribute as it's OID ('" + oid + "') does not exist" );
                
            }
        }
    }
    
    
    /**
    * Gets the hashCode of this ServerEntry.
    *
    * @see java.lang.Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int result = 37;
        
        result = result*17 + dn.hashCode();
        
        for ( EntryAttribute attribute:attributes.values() )
        {
            result = result*17 + attribute.hashCode();
        }

        return result;
    }

    
    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object o )
    {
        // Short circuit
        if ( this == o )
        {
            return true;
        }
        
        if ( ! ( o instanceof DefaultServerEntry ) )
        {
            return false;
        }
        
        ServerEntry other = (DefaultServerEntry)o;
        
        if ( dn == null )
        {
            if ( other.getDn() != null )
            {
                return false;
            }
        }
        else
        {
            if ( !dn.equals( other.getDn() ) )
            {
                return false;
            }
        }
        
        if ( size() != other.size() )
        {
            return false;
        }
        
        for ( EntryAttribute attribute:other )
        {
            EntryAttribute attr = attributes.get( ((ServerAttribute)attribute).getAttributeType() );
            
            if ( attr == null )
            {
                return false;
            }
            
            if ( !attribute.equals( attr ) )
            {
                return false;
            }
        }
        
        return true;
    }
        
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "ServerEntry\n" );
        sb.append( "    dn" );
        
        if ( dn.isNormalized() )
        {
            sb.append( "[n]: " );
            sb.append( dn.getUpName() );
        }
        else
        {
            sb.append( "[]: " );
            sb.append( dn );
        }
        
        sb.append( '\n' );
        
        // First dump the ObjectClass attribute
        if ( containsAttribute( OBJECT_CLASS_AT ) )
        {
            EntryAttribute objectClass = get( OBJECT_CLASS_AT );
            
            sb.append( objectClass );
        }
        
        if ( attributes.size() != 0 )
        {
            for ( EntryAttribute attribute:attributes.values() )
            {
                if ( !((ServerAttribute)attribute).getAttributeType().equals( OBJECT_CLASS_AT ) )
                {
                    sb.append( attribute );
                }
            }
        }
        
        return sb.toString();
    }
}
