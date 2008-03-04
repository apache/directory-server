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


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;


/**
 * A default implementation of a ServerEntry which should suite most
 * use cases.
 * 
 * This class is final, it should not be extended.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class DefaultServerEntry implements ServerEntry, Externalizable
{
    /** Used for serialization */
    public static final long serialVersionUID = 2L;
    
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultServerEntry.class );

    /** A map containing all the attributes for this entry */
    private Map<AttributeType, ServerAttribute> serverAttributeMap = new HashMap<AttributeType, ServerAttribute>();
    
    /** The objectClass container */
    private ObjectClassAttribute objectClassAttribute;
    
    /** The global registries */
    private final transient Registries registries;
    
    /** A speedup to get the ObjectClass attribute */
    private static transient AttributeType OBJECT_CLASS_AT;
    
    /** The DN for this entry */
    private LdapDN dn;
    
    /** A mutex to manage synchronization*/
    private transient static Object MUTEX = new Object();


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
                    OBJECT_CLASS_AT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
                }
            }
            
            setObjectClassAttribute( new ObjectClassAttribute( registries, SchemaConstants.OBJECT_CLASS_AT ) );
        }
        catch ( NamingException ne )
        {
            // do nothing...
        }
    }

    
    /**
     * Creates a new instance of DefaultServerEntry. This is a private constructor
     * which will only be used by the desrialization method 
     * <p>
     * This entry <b>must</b> be initialized before being used !
     */
    public DefaultServerEntry()
    {
        registries = null;
    }


    /**
     * Creates a new instance of DefaultServerEntry, with a 
     * DN and registries. 
     * <p>
     * No attributes will be created except the ObjectClass attribute,
     * which will contains "top". 
     * 
     * @param registries The reference to the global registries
     * @param dn The DN for this serverEntry. Can be null.
     */
    public DefaultServerEntry( Registries registries, LdapDN dn )
    {
        this.dn = dn;
        this.registries = registries;

        initObjectClassAT( registries );
    }


    /**
     * Creates a new instance of DefaultServerEntry, with a 
     * DN, registries and a list of attributeTypes. 
     * <p>
     * No attributes will be created except the ObjectClass attribute,
     * which will contains "top". 
     * <p>
     * If any of the AttributeType does not exist, they are simply discarded.
     * 
     * @param registries The reference to the global registries
     * @param dn The DN for this serverEntry. Can be null.
     * @param attributeTypes The list of attributes to create, without value.
     */
    public DefaultServerEntry( Registries registries, LdapDN dn, AttributeType... attributeTypes )
    {
        this.dn = dn;
        this.registries = registries;

        initObjectClassAT( registries );

        for ( AttributeType attributeType:attributeTypes )
        {
            if ( attributeType.equals(  OBJECT_CLASS_AT ) )
            {
                // The ObjectClass AttributeType has already been added
                continue;
            }
            
            // Add a new AttributeType without value
            set( attributeType );
        }
    }

    
    /**
     * Creates a new instance of DefaultServerEntry, with a 
     * DN, registries and an attributeType with the user provided ID. 
     * <p>
     * No attributes will be created except the ObjectClass attribute,
     * which will contains "top". 
     * <p>
     * If the AttributeType does not exist, then an empty Entry is created.
     * <p>
     * We also check that the normalized upID equals the AttributeType ID
     * 
     * @param registries The reference to the global registries
     * @param dn The DN for this serverEntry. Can be null.
     * @param attributeType The attribute to create, without value.
     * @param upId The User Provided ID fro this AttributeType
     */
    public DefaultServerEntry( Registries registries, LdapDN dn, AttributeType attributeType, String upId )
    {
        this.dn = dn;
        this.registries = registries;

        initObjectClassAT( registries );

        if ( attributeType.equals(  OBJECT_CLASS_AT ) )
        {
            // If the AttributeType is the ObjectClass AttributeType, then
            // we don't add it to the entry, as it has already been added
            // before. But we have to store the upId.
            objectClassAttribute.setUpId( upId, OBJECT_CLASS_AT );
        }
        else
        {
            try
            {
                put( upId, attributeType, (String)null );
            }
            catch ( NamingException ne )
            {
                // What do we do ???
                LOG.error( "We have had an error while adding the '{}' AttributeType : {}", upId, ne.getMessage() );
            }
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
        this.dn = dn;
        this.registries = registries;

        initObjectClassAT( registries );

        for ( String upId:upIds )
        {
            try
            {
                AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( upId );
                
                if ( attributeType.equals(  OBJECT_CLASS_AT ) )
                {
                    // The ObjectClass AttributeType has already been added
                    continue;
                }
                
                // Add a new AttributeType without value
                set( upId );
            }
            catch ( NamingException ne )
            {
                // Just log an error...
                LOG.error( "The '{}' AttributeType does not exist", upId );
            }
        }
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
        this.dn = dn;
        this.registries = registries;

        initObjectClassAT( registries );

        for ( ServerAttribute attribute:attributes )
        {
            if ( attribute.getType().equals(  OBJECT_CLASS_AT ) )
            {
                // Treat the ObjectClass in a specific way
                setObjectClassAttribute( attribute );
                continue;
            }
            
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
    // Helper methods
    //-------------------------------------------------------------------------
    
    /**
     * Returns the attributeType from an Attribute ID.
     */
    private AttributeType getAttributeType( String upId ) throws NamingException
    {
        if ( StringTools.isEmpty( StringTools.trim( upId ) ) )
        {
            String message = "The ID should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        return registries.getAttributeTypeRegistry().lookup( upId );
    }

    
    /**
     * Get the UpId if it was null.
     */
    public static String getUpId( String upId, AttributeType attributeType ) throws NamingException
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
     * Get the attributeType from the UpId if null.
     */
    private AttributeType getAttributeType( String upId, AttributeType attributeType ) throws NamingException
    {
        if ( ( attributeType == null ) )
        {
            String normUpId = StringTools.trim( upId );
            
            if ( StringTools.isEmpty( normUpId ) )
            {
                String message = "Cannot add an attribute without an ID";
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
            else
            {
                attributeType = registries.getAttributeTypeRegistry().lookup( upId );
            }
        }
        
        return attributeType;
    }

    
    /**
     * Stores the ObjectClassAttribute into its container.
     *
     * @param objectClassAttribute The instance of ObjectClassAttribute
     * @return The previously stored ObjectClassAttribute, if any
     */
    private void setObjectClassAttribute( ObjectClassAttribute objectClassAttribute )
    {
        assert objectClassAttribute != null : "The ObjectClass Attribute should not be null";
        
        this.objectClassAttribute = objectClassAttribute;
    }


    /**
     * Stores the ObjectClassAttribute into its container.
     *
     * @param objectClassAttribute The instance of ObjectClassAttribute
     * @return The previously stored ObjectClassAttribute, if any
     */
    private ServerAttribute setObjectClassAttribute( ServerAttribute serverAttribute )
    {
        assert serverAttribute != null : "The ObjectClass Attribute should not be null";
        
        this.objectClassAttribute = new ObjectClassAttribute( registries );
        ServerAttribute previous = serverAttributeMap.put( OBJECT_CLASS_AT, objectClassAttribute );
        
        return previous;
    }


    private ServerAttribute removeObjectClassAttribute( ServerAttribute objectClassAttribute ) throws NamingException
    {
        this.objectClassAttribute = (ObjectClassAttribute)objectClassAttribute;

        return serverAttributeMap.remove( OBJECT_CLASS_AT );
    }


    public boolean addObjectClass( ObjectClass objectClass, String alias ) throws NamingException
    {
        boolean exists = objectClassAttribute.addObjectClass( objectClass, alias );
        
        if ( !exists )
        {
            // The ObjectClass is a new one : update the vaules
        }

        ServerAttribute existing = serverAttributeMap.get( OBJECT_CLASS_AT );
        
        existing.add( alias );
        
        return exists;
    }


    public boolean addObjectClass( ObjectClass objectClass ) throws NamingException
    {
        return addObjectClass( objectClass, objectClass.getName() );
    }


    public void addObjectClass( ObjectClassAttribute objectClassAttribute ) throws NamingException
    {
        this.objectClassAttribute = objectClassAttribute;
        
        ServerAttribute currentOc = serverAttributeMap.get( objectClassAttribute.getType() );
        
        if ( currentOc != null )
        {
            for ( Value<?> value:objectClassAttribute )
            {
                currentOc.add( value );
            }
        }
        else
        {
            serverAttributeMap.put( OBJECT_CLASS_AT, objectClassAttribute );
        }
    }


    public boolean hasObjectClass( ObjectClass objectClass )
    {
        return objectClassAttribute.hasObjectClass( objectClass );
    }


    public boolean hasObjectClass( String objectClass )
    {
        try
        {
            ObjectClass oc = registries.getObjectClassRegistry().lookup( objectClass );
            return objectClassAttribute.hasObjectClass( oc );
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }


    public Set<ObjectClass> getAbstractObjectClasses()
    {
        return objectClassAttribute.getAbstractObjectClasses();
    }


    public ObjectClass getStructuralObjectClass()
    {
        return objectClassAttribute.getStructuralObjectClass();
    }


    public Set<ObjectClass> getStructuralObjectClasses()
    {
        return objectClassAttribute.getStructuralObjectClasses();
    }


    public Set<ObjectClass> getAuxiliaryObjectClasses()
    {
        return objectClassAttribute.getAuxiliaryObjectClasses();
    }


    public Set<ObjectClass> getAllObjectClasses()
    {
        return objectClassAttribute.getAllObjectClasses();
    }


    public Set<AttributeType> getMustList()
    {
        return objectClassAttribute.getMustList();
    }


    public Set<AttributeType> getMayList()
    {
        return objectClassAttribute.getMayList();
    }


    public boolean isValid()
    {
        throw new NotImplementedException();
    }


    public boolean isValid( ObjectClass objectClass )
    {
        throw new NotImplementedException();
    }


    /**
     * Returns the attribute associated with an AttributeType
     * 
     * @param the AttributeType we are looking for
     * @return the associated attribute
     */
    public ServerAttribute get( AttributeType attributeType )
    {
        return serverAttributeMap.get( attributeType );
    }


    /**
     * Returns the attribute associated with a String
     * 
     * @param the Attribute ID we are looking for
     * @return the associated attribute
     */
    public ServerAttribute get( String attributeType ) throws NamingException
    {
        return get( registries.getAttributeTypeRegistry().lookup( attributeType ) );
    }


    /**
     * Put an attribute (represented by its ID and values) into an entry. 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     *
     * @param upId The attribute ID
     * @param values The list of values to inject. It can be empty
     * @return The replaced attribute
     * @throws NamingException If the attribute does not exist
     */
    public ServerAttribute put( String upId, String... values ) throws NamingException
    {
        return put( upId, getAttributeType( upId ), values );
    }


    /**
     * Put an attribute (represented by its ID and values) into an entry. 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     *
     * @param upId The attribute ID
     * @param values The list of values to inject. It can be empty.
     * @return The replaced attribute
     * @throws NamingException If the attribute does not exist
     */
    public ServerAttribute put( String upId, byte[]... values ) throws NamingException
    {
        return put( upId, getAttributeType( upId ), values );
    }


    /**
     * Set some new empty AttributeTypes into the serverEntry.
     * <p>
     * If there is already a ServerAttribute with the same AttributeType, 
     * it will be removed from the entry and returned back to the caller.
     * 
     * @param attributeTypse the new ServerAttribute attributeType to be added
     * @return The list of existing ServerAttribute, if any with the same AttributeType
     */
    public List<ServerAttribute> set( AttributeType... attributeTypes )
    {
        if ( attributeTypes == null )
        {
            String message = "The AttributeType list should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        List<ServerAttribute> returnedServerAttributes = null;
        
        // Now, loop on all the attributeType to add
        for ( AttributeType attributeType:attributeTypes )
        {
            if ( attributeType == null )
            {
                String message = "The AttributeType list should not contain null values";
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
            
            // The ObjectClass AT is special
            if ( attributeType.equals( OBJECT_CLASS_AT ) )
            {
                // Just do nothing but clear the ObjectClass values
                objectClassAttribute.clear();
                ServerAttribute newAttribute = new ObjectClassAttribute( registries );
                serverAttributeMap.put( attributeType, newAttribute );
            }
            else
            {
                if ( returnedServerAttributes == null )
                {
                    returnedServerAttributes = new ArrayList<ServerAttribute>();
                }

                if ( serverAttributeMap.containsKey( attributeType ) )
                {
                    // Add the removed serverAttribute to the list
                    returnedServerAttributes.add( serverAttributeMap.remove( attributeType ) );
                }

                ServerAttribute newAttribute = new DefaultServerAttribute( attributeType );
                serverAttributeMap.put( attributeType, newAttribute );
            }
        }
        
        return returnedServerAttributes;
    }

    
    /**
     * Put some new empty ServerAttribute into the serverEntry. 
     * <p>
     * If there is already a ServerAttribute with the same AttributeType, 
     * it will be removed from the entry and returned back to the caller.
     * <p>
     * The added ServerAttributes are supposed to be valid.
     * 
     * @param serverAttributes the new ServerAttributes to put into the serverEntry
     * @return An existing ServerAttribute, if any of the added serverAttribute 
     * already exists
     */
    public List<ServerAttribute> put( ServerAttribute... serverAttributes ) throws NamingException
    {
        List<ServerAttribute> previous = new ArrayList<ServerAttribute>();
        
        for ( ServerAttribute serverAttribute:serverAttributes )
        {
            if ( serverAttribute == null )
            {
                String message = "The ServerAttribute list should not contain null elements";
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
            
            if ( serverAttribute.getType().equals( OBJECT_CLASS_AT ) )
            {
                ServerAttribute removed = null;
                
                // The objectClass attributeType is special 
                if ( serverAttribute instanceof ObjectClassAttribute )
                {
                    removed = this.objectClassAttribute;

                    this.objectClassAttribute = (ObjectClassAttribute)serverAttribute;
                    serverAttributeMap.put( OBJECT_CLASS_AT, serverAttribute );
                    previous.add( removed );
                }
                else
                {
                    // Here, the attributeType is ObjectClass, but the Attribute itself is 
                    // not a instance of the ObjectClassAttribute. We will store all of
                    // its values into a new instance of ObjectClassAttribute. 
                    if ( serverAttribute.getType().getSyntax().isHumanReadable() )
                    {
                        ObjectClassAttribute objectClassAttribute = new ObjectClassAttribute( registries, serverAttribute );
                        this.objectClassAttribute = objectClassAttribute;
                        removed = serverAttributeMap.put( OBJECT_CLASS_AT, objectClassAttribute );
                    }
                    else
                    {
                        // The attribute must be a String
                        String message = "Only String values supported for objectClass attribute";
                        LOG.error(  message  );
                        throw new UnsupportedOperationException( message );
                    }

                    previous.add( removed );
                }
            }
            else
            {
                ServerAttribute removed = serverAttributeMap.put( serverAttribute.getType(), serverAttribute );
                
                if ( removed != null )
                {
                    previous.add( removed );
                }
            }
        }
        
        return previous;
    }


    /**
     * Put some new ServerAttribute using the User Provided ID to select
     * the AttributeType. No value is inserted.
     * 
     * @param upIds The user provided IDs of the AttributeTypes to add.
     * @return A list of existing ServerAttribute, if any with the same 
     * AttributeType
     * 
     * @throws NamingException If one of the user provided ID is not an 
     * attributeType's name
     */
    public List<ServerAttribute> set( String... upIds ) throws NamingException
    {
        List<ServerAttribute> existings = null;
        
        for ( String upId:upIds )
        {
            // Search for the corresponding AttributeType, based on the upID 
            AttributeType attributeType = getAttributeType( upId );
            
            ServerAttribute existing = serverAttributeMap.put( attributeType, 
                new DefaultServerAttribute( upId, attributeType ));
            
            if ( existing != null )
            {
                if ( existings == null )
                {
                    existings = new ArrayList<ServerAttribute>();
                }
                
                existings.add( existing );
            }
        }
        
        return existings;
    }


    public List<ServerAttribute> remove( ServerAttribute... serverAttributes ) throws NamingException
    {
        List<ServerAttribute> removedAttributes = new ArrayList<ServerAttribute>();
        
        for ( ServerAttribute serverAttribute:serverAttributes )
        {
            if ( serverAttribute.getType().equals( OBJECT_CLASS_AT ) )
            {
                removeObjectClassAttribute( new ObjectClassAttribute( registries ) );
            }

            if ( serverAttributeMap.containsKey( serverAttribute.getType() ) )
            {
                serverAttributeMap.remove( serverAttribute.getType() );
                removedAttributes.add( serverAttribute );
            }
        }
        
        return removedAttributes;
    }


    /**
     * Stores a new attribute with some values into the entry.
     * <p>
     * The previous attribute with the same attributeType, if any, is returned.
     * 
     * @param attributeType The attributeType to add
     * @param values The associated values
     * @return The existing ServerAttribute, if any
     * @throws NamingException If some values conflict with the attributeType
     * 
     */
    public ServerAttribute put( AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        return put( null, attributeType, values );
    }


    /**
     * Stores a new attribute with some values into an entry, setting
     * the User Provided ID in the same time.
     *
     * @param upId The User provided ID
     * @param attributeType The associated AttributeType
     * @param values The values to store into the new Attribute
     * @return The existing attribute if any
     * @throws NamingException 
     */
    public ServerAttribute put( String upId, AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        upId = getUpId( upId, attributeType );
        attributeType = getAttributeType( upId, attributeType );

        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            // Create a new ObjectclassAttribute and return the previous one
            ServerAttribute previous = this.objectClassAttribute;
            
            objectClassAttribute = new ObjectClassAttribute( registries, upId, values );
            serverAttributeMap.put( OBJECT_CLASS_AT, objectClassAttribute );
            return previous;
        }
        else
        {
            // We simply have to set the current attribute values
            ServerAttribute serverAttribute = new DefaultServerAttribute( upId, attributeType, values );
            return serverAttributeMap.put( attributeType, serverAttribute );
        }
    }


    /**
     * Put an attribute (represented by its ID and some values) into an entry. 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     * <p>
     * The values are stored as Value<?> objects.
     *
     * @param upId The attribute ID
     * @param values The list of Value<?> objects to inject. It can be empty.
     * @return The replaced attribute
     * @throws NamingException If the attribute does not exist
     */
    public ServerAttribute put( String upId, Value<?>... values ) throws NamingException
    {
        return put( upId, getAttributeType( upId ), values );
    }


    /**
     * Stores a new attribute, creating it from its attributeType and String values.
     * <p>
     * The values are Strings, so the attributeType must be humanReadable. Otherwise,
     * we will try to convert values from String to byte[]
     * 
     * @param attributeType The attributeType
     * @param values The String values to add to the attribute
     * @return The existing ServerAttribute which has been replaced, if any
     * @throws NamingException If some values conflict with the attributeType
     */
    public ServerAttribute put( AttributeType attributeType, String... values ) throws NamingException
    {
        return put( null, attributeType, values );
    }

    
    /**
     * Stores a new attribute with some String values into an entry, setting
     * the User Provided ID in the same time.
     *
     * @param upId The User provided ID
     * @param attributeType The associated AttributeType
     * @param values The String values to store into the new Attribute
     * @return The existing attribute if any
     * @throws NamingException 
     */
    public ServerAttribute put( String upId, AttributeType attributeType, String... values ) throws NamingException
    {
        upId = getUpId( upId, attributeType );
        attributeType = getAttributeType( upId, attributeType );

        ServerAttribute serverAttribute = new DefaultServerAttribute( upId, attributeType );

        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            // If the AttributeType is the ObjectClass AttributeType, then
            // we don't add it to the entry, as it has already been added
            // before. But we have to store the upId.
            ServerAttribute previous = objectClassAttribute;
            objectClassAttribute = new ObjectClassAttribute( registries, upId, values );
            serverAttributeMap.put( OBJECT_CLASS_AT, objectClassAttribute );
            return previous;
        }
        else
        {
            // We simply have to set the current attribute values
            serverAttribute.put( values );
            return serverAttributeMap.put( attributeType, serverAttribute );
        }
    }


    /**
     * Stores a new attribute, creating it from its attributeType and byte[] values.
     * <p>
     * The values are byte[], so the attributeType must be non-humanReadable.
     * 
     * @param attributeType The attributeType
     * @param values The byte[] values to add to the attribute
     * @return The existing ServerAttribute which has been replaced, if any
     * @throws NamingException If some values conflict with the attributeType
     */
    public ServerAttribute put( AttributeType attributeType, byte[]... values ) throws NamingException
    {
        return put( null, attributeType, values );
    }


    /**
     * Store a new attribute with some String values into an entry, setting
     * the User Provided ID in the same time.
     *
     * @param upId The User provided ID
     * @param attributeType The associated AttributeType
     * @param values The byte[] values to store into the new Attribute
     * @return The existing attribute if any
     * @throws NamingException 
     */
    public ServerAttribute put( String upId, AttributeType attributeType, byte[]... values ) throws NamingException
    {
        if ( attributeType == null )
        {
            String message = "The attributeType should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        upId = getUpId( upId, attributeType );
        attributeType = getAttributeType( upId, attributeType );

        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            String message = "Only String values supported for objectClass attribute";
            LOG.error(  message  );
            throw new UnsupportedOperationException( message );
        }

        ServerAttribute serverAttribute = new DefaultServerAttribute( upId, attributeType );
        serverAttribute.put( values );
        return serverAttributeMap.put( attributeType, serverAttribute );
    }


    public List<ServerAttribute> remove( AttributeType... attributeTypes ) throws NamingException
    {
        if ( attributeTypes == null )
        {
            return null;
        }
        
        List<ServerAttribute> attributes = new ArrayList<ServerAttribute>( attributeTypes.length );
        
        for ( AttributeType attributeType:attributeTypes )
        {
            if ( serverAttributeMap.containsKey( attributeType ) )
            {
                attributes.add( serverAttributeMap.remove( attributeType ) );
            }
        }
        
        return attributes;
    }


    public List<ServerAttribute> remove( String... upIds ) throws NamingException
    {
        assert registries != null : "The AttributeType registry should not be null";

        if ( upIds == null )
        {
            return null;
        }
        
        List<ServerAttribute> attributes = new ArrayList<ServerAttribute>( upIds.length );
        
        for ( String upId:upIds )
        {
            AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( upId );
    
            attributes.add( serverAttributeMap.remove( attributeType ) );
        }
        
        return attributes;
    }


    public void clear()
    {
        serverAttributeMap.clear();

        setObjectClassAttribute( new ObjectClassAttribute( registries ) );
    }


    public LdapDN getDn()
    {
        return dn;
    }


    public void setDn( LdapDN dn )
    {
        this.dn = dn;
    }


    public Iterator<ServerAttribute> iterator()
    {
        return Collections.unmodifiableMap( serverAttributeMap ).values().iterator();
    }


    public int size()
    {
        return serverAttributeMap.size();
    }
    
    
    /**
     * Clone an entry. All the element are duplicated, so a modification on
     * the original object won't affect the cloned object, as a modification
     * on the cloned object has no impact on the original object
     */
    public ServerEntry clone()
    {
        try
        {
            // First, clone the structure
            DefaultServerEntry clone = (DefaultServerEntry)super.clone();
            
            // A serverEntry has a DN, an ObjectClass attribute
            // and many attributes
            // Clone the DN
            clone.dn = (LdapDN)dn.clone();
            
            // Clone the ObjectClassAttribute
            clone.objectClassAttribute = objectClassAttribute.clone();
            
            // clone the ServerAttribute Map
            clone.serverAttributeMap = (Map<AttributeType, ServerAttribute>)(((HashMap<AttributeType, ServerAttribute>)serverAttributeMap).clone());
            
            // now clone all the servrAttributes
            clone.serverAttributeMap.clear();
            
            for ( AttributeType key:serverAttributeMap.keySet() )
            {
                ServerAttribute value = (ServerAttribute)serverAttributeMap.get( key ).clone();
                clone.serverAttributeMap.put( key, value );
            }
            
            // We are done !
            return clone;
        }
        catch ( CloneNotSupportedException cnse )
        {
            return null;
        }
    }
    

    /**
     * Checks if an entry contains an attribute with a given value.
     *
     * @param attribute The Attribute we are looking for
     * @param value The searched value
     * @return <code>true</code> if the value is found within the attribute
     * @throws NamingException If there is a problem
     */
    public boolean contains( AttributeType attributeType, Value<?> value ) throws NamingException
    {
        if ( attributeType == null )
        {
            return false;
        }
        
        if ( serverAttributeMap.containsKey( attributeType ) )
        {
            return serverAttributeMap.get( attributeType ).contains( (Value<?>)value );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with a given value.
     *
     * @param id The Attribute we are looking for
     * @param value The searched value
     * @return <code>true</code> if the value is found within the attribute
     * @throws NamingException If the attribute does not exists
     */
    public boolean contains( String id, Value<?> value ) throws NamingException
    {
        if ( id == null )
        {
            return false;
        }
        
        AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( id );
        
        if ( attributeType == null )
        {
            return false;
        }
        else if ( serverAttributeMap.containsKey( attributeType ) )
        {
            return serverAttributeMap.get( attributeType ).contains( (Value<?>)value );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with a given value.
     *
     * @param id The Attribute we are looking for
     * @param value The searched value
     * @return <code>true</code> if the value is found within the attribute
     * @throws NamingException If the attribute does not exists
     */
    public boolean contains( String id, String value ) throws NamingException
    {
        if ( id == null )
        {
            return false;
        }
        
        AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( id );
        
        if ( attributeType == null )
        {
            return false;
        }
        else if ( serverAttributeMap.containsKey( attributeType ) )
        {
            return serverAttributeMap.get( attributeType ).contains( value );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with a given value.
     *
     * @param attributeType The Attribute we are looking for
     * @param value The searched value
     * @return <code>true</code> if the value is found within the attribute
     * @throws NamingException If the attribute does not exists
     */
    public boolean contains( AttributeType attributeType, String value ) throws NamingException
    {
        if ( attributeType == null )
        {
            return false;
        }
        else if ( serverAttributeMap.containsKey( attributeType ) )
        {
            return serverAttributeMap.get( attributeType ).contains( value );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with a given value.
     *
     * @param id The Attribute we are looking for
     * @param value The searched value
     * @return <code>true</code> if the value is found within the attribute
     * @throws NamingException If the attribute does not exists
     */
    public boolean contains( String id, byte[] value ) throws NamingException
    {
        if ( id == null )
        {
            return false;
        }
        
        AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( id );
        
        if ( attributeType == null )
        {
            return false;
        }
        else if ( serverAttributeMap.containsKey( attributeType ) )
        {
            return serverAttributeMap.get( attributeType ).contains( value );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with a given value.
     *
     * @param attributeType The Attribute we are looking for
     * @param value The searched value
     * @return <code>true</code> if the value is found within the attribute
     * @throws NamingException If the attribute does not exists
     */
    public boolean contains( AttributeType attributeType, byte[] value ) throws NamingException
    {
        if ( attributeType == null )
        {
            return false;
        }
        else if ( serverAttributeMap.containsKey( attributeType ) )
        {
            return serverAttributeMap.get( attributeType ).contains( value );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Gets all the attributes type (ObjectClasses, May and Must)
     *
     * @return The combined set of all the attributes, including ObjectClass.
     */
    public Set<AttributeType> getAttributeTypes()
    {
        return serverAttributeMap.keySet();
    }
    
    
    /**
     * Add a new ObjectClass, with its upId. If the upId is null,
     * default to "objectClass".
     * 
     * Updates the objectClassAttribute and the serverAttributeMap.
     */
    private void addObjectClass( String upId, String... values ) throws NamingException, InvalidAttributeValueException
    {
        ServerAttribute attribute = serverAttributeMap.get( OBJECT_CLASS_AT );
        
        if ( objectClassAttribute == null )
        {
            objectClassAttribute = new ObjectClassAttribute( registries, upId, values );
            serverAttributeMap.put(  OBJECT_CLASS_AT, objectClassAttribute );
        }
        else
        {
            objectClassAttribute.add( values );
            
            if ( attribute == null )
            {
                serverAttributeMap.put(  OBJECT_CLASS_AT, objectClassAttribute ); 
            }
            else
            {
                attribute.add( values );
            }
        }
    }
    
    
    /**
     * Add a new ObjectClass, with its upId. If the upId is null,
     * default to "objectClass".
     * 
     * Updates the objectClassAttribute and the serverAttributeMap.
     */
    private void addObjectClass( String upId, Value<?>... values ) throws NamingException, InvalidAttributeValueException
    {
        ServerAttribute attribute = serverAttributeMap.get( OBJECT_CLASS_AT );
        
        if ( objectClassAttribute == null )
        {
            objectClassAttribute = new ObjectClassAttribute( registries, upId, values );
            serverAttributeMap.put( OBJECT_CLASS_AT, attribute );
        }
        else
        {
            objectClassAttribute.add( values );
            attribute.add( values );
        }
    }
    
    
    /**
     * Add a new ServerAttribute, with its upId. If the upId is null,
     * default to the AttributeType name.
     * 
     * Updates the serverAttributeMap.
     */
    private void createAttribute( String upId, AttributeType attributeType, Value<?>... values ) throws NamingException, InvalidAttributeValueException
    {
        ServerAttribute attribute = new DefaultServerAttribute( attributeType, values );
        attribute.setUpId( upId, attributeType );
        serverAttributeMap.put( attributeType, attribute );
    }
    
    
    /**
     * Add a new ServerAttribute, with its upId. If the upId is null,
     * default to the AttributeType name.
     * 
     * Updates the serverAttributeMap.
     */
    private void createAttribute( String upId, AttributeType attributeType, String... values ) throws NamingException, InvalidAttributeValueException
    {
        ServerAttribute attribute = new DefaultServerAttribute( attributeType, values );
        attribute.setUpId( upId, attributeType );
        serverAttributeMap.put( attributeType, attribute );
    }
    
    
    /**
     * Add a new ServerAttribute, with its upId. If the upId is null,
     * default to the AttributeType name.
     * 
     * Updates the serverAttributeMap.
     */
    private void createAttribute( String upId, AttributeType attributeType, byte[]... values ) throws NamingException, InvalidAttributeValueException
    {
        ServerAttribute attribute = new DefaultServerAttribute( attributeType, values );
        attribute.setUpId( upId, attributeType );
        serverAttributeMap.put( attributeType, attribute );
    }
    
    
    /*-------------------------------------------------------------------------
     *
     * Add an attribute into this entry. We have many different cases.
     * 1) We are adding a value to an ObjectClass or an ObjectClass to the
     * entry
     * 2) We are adding an attribute or a value to an attribute which is not 
     * an ObjectClass
     * 
     * For (1), we will have to check :
     *  - that the value is not binary
     *  - we won't add an existing objectClass
     *  - and the ObjectClass must be valid
     *  
     * If everything is fine, we will proceed accordingly to the existence
     * of the ObjectClass attribute :
     *  - if the ObjectClass attribute has already been created, we will 
     *  create the ObjectClass, set its value and add an Attribute into 
     *  the values Map
     *  - otherwise, we will just add the value to the Values Map 
     *  
     * For (2), we just have to update the values Map :
     *  - if the attribute already exists, we add the value (no duplicate)
     *  - or we just add a new attribute to the Map
     *-----------------------------------------------------------------------*/

    /**
     * Add some Values to the current Entry.
     *
     * @param upId The user provided ID of the attribute we want to add 
     * some values to
     * @param values The list of Values to add
     * @throws NamingException
     *
    public void add( String upId, Value<?>... values ) throws NamingException
    {
        
    }
    
    
    /**
     * Add an attribute (represented by its ID and some String values) into an 
     * entry.
     * <p> 
     * If we already have an attribute with the same value, nothing is done
     *
     * @param attributeType The attribute Type
     * @param values The list of String values to inject. It can be empty
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
        
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            // This is an objectClass, call the appropriate method
            addObjectClass( null, values );
        }
        else
        {
            ServerAttribute attribute = serverAttributeMap.get( attributeType );
            
            if ( attribute != null )
            {
                // This Attribute already exist, we add the values 
                // into it
                attribute.add( values );
            }
            else
            {
                // We have to create a new Attribute and set the values
                // and the upId
                createAttribute( null, attributeType, values );
            }
        }
    }

    
    /**
     * Add an attribute (represented by its ID and some Binary values) into an 
     * entry.
     * <p> 
     * If we already have an attribute with the same value, nothing is done
     *
     * @param attributeType The attribute Type
     * @param values The list of binary values to inject. It can be empty
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

        ServerAttribute attribute = serverAttributeMap.get( attributeType );
        
        if ( attribute != null )
        {
            // This Attribute already exist, we add the values 
            // into it
            attribute.add( values );
        }
        else
        {
            // We have to create a new Attribute and set the values
            // and the upId
            createAttribute( null, attributeType, values );
        }
    }


    /**
     * Add a new attribute with some Value values into the entry.
     * <p>
     * 
     * @param attributeType The attributeType to add
     * @param values The associated Value values
     * @throws NamingException If some values conflict with the attributeType
     * 
     */
    public void add( AttributeType attributeType, Value<?>... values ) throws NamingException
    {
        if ( attributeType == null )
        {
            String message = "The attributeType should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            // This is an objectClass, call the appropriate method
            addObjectClass( null, values );
        }
        else
        {
            ServerAttribute attribute = serverAttributeMap.get( attributeType );
        
            if ( attribute != null )
            {
                // This Attribute already exist, we add the values 
                // into it
                attribute.add( values );
            }
            else
            {
                createAttribute( null, attributeType, values );
            }
        }
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

        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            // This is an objectClass, call the appropriate method
            addObjectClass( upId, values );
        }
        else
        {
            ServerAttribute attribute = serverAttributeMap.get( attributeType );
            
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
    }


    /**
     * Adds a new attribute with some Binary values into an entry, setting
     * the User Provided ID in the same time.
     *
     * @param upId The User provided ID
     * @param attributeType The associated AttributeType
     * @param values The Binary values to store into the new Attribute
     * @throws NamingException 
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

        ServerAttribute attribute = serverAttributeMap.get( attributeType );
        
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
     * Adds a new attribute with some Value values into an entry, setting
     * the User Provided ID in the same time.
     *
     * @param upId The User provided ID
     * @param attributeType The associated AttributeType
     * @param values The Value values to store into the new Attribute
     * @throws NamingException 
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
        
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            // This is an objectClass, call the appropriate method
            addObjectClass( upId, values );
        }
        else
        {
            ServerAttribute attribute = serverAttributeMap.get( attributeType );
        
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
    }

    
    /**
     * @see Externalizable#writeExternal(ObjectOutput)<p>
     * 
     * This is the place where we serialize entries, and all theirs
     * elements. the reason why we don't call the underlying methods
     * (<code>ServerAttribute.write(), Value.write()</code>) is that we need
     * access to the registries to read back the values.
     * <p>
     * The structure used to store the entry is the following :
     * <li><b>[DN length]</b> : can be -1 if we don't have a DN, 0 if the 
     * DN is empty, otherwise contains the DN's length.<p> 
     * <b>NOTE :</b>This should be unnecessary, as the DN should always exists
     * <p>
     * </li>
     * <li>
     * <b>DN</b> : The entry's DN. Can be empty (rootDSE=<p>
     * </li>
     * We have to store the UPid, and all the values, if any.
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        // First, the DN
        if ( dn == null )
        {
            // Write an empty DN
            LdapDN.EMPTY_LDAPDN.writeExternal( out );
        }
        else
        {
            // Write the DN
            out.writeObject( dn );
        }
        
        // Then the attributes. 
        if ( serverAttributeMap == null )
        {
            out.writeInt( -1 );
        }
        else
        {
            out.writeInt( serverAttributeMap.size() );
            
            // Iterate through the keys. We store the Attribute
            // here, to be able to restore it in the readExternal :
            // we need access to the registries, which are not available
            // in the ServerAttribute class.
            for ( AttributeType attributeType:serverAttributeMap.keySet() )
            {
                // We store the OID, as the AttributeType might have no name
                out.writeUTF( attributeType.getOid() );
                
                // And store the attribute.
                ServerAttribute attribute = serverAttributeMap.get( attributeType );

                // Store the UP id
                out.writeUTF( attribute.getUpId() );
                
                // The number of values
                int nbValues = attribute.size();
                
                if ( nbValues == 0 ) 
                {
                    out.writeInt( 0 );
                }
                else 
                {
                    out.writeInt( nbValues );

                    for ( Value<?> value:attribute )
                    {
                        out.writeObject( value );
                    }
                }
            }

            // Note : we don't store the ObjectClassAttribute. I has already
            // been stored as an attribute.
        }
        
        out.flush();
    }

    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        /*if ( in.available() == 0 )
        {
            String message = "Cannot read an null Attribute";
            LOG.error( message );
            throw new IOException( message );
        }*/

        // Read the DN
        LdapDN dn = (LdapDN)in.readObject();
        
        // Read the number of attributes
        int nbAttributes = in.readInt();
        
        serverAttributeMap = new HashMap<AttributeType, ServerAttribute>();

        // Read the attributes
        for ( int i = 0; i < nbAttributes; i++ )
        {
            String oid = in.readUTF();
            
            try
            {
                AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( oid );
                
                ServerAttribute attribute = new DefaultServerAttribute( attributeType );
                
                // Read the attribute upID
                String upId = in.readUTF();
                attribute.setUpId( upId, attributeType );
                
                // Read the number of values
                int nbValues = in.readInt();
                
                for ( int j = 0; j < nbValues; j++ )
                {
                    Value<?> value = (Value<?>)in.readObject();
                    attribute.add( value );
                }
                
                serverAttributeMap.put(  attributeType, attribute );
            }
            catch ( NamingException ne )
            {
                
            }
        }
    }
    
    
    /**
    * Gets the hashcode of this ServerEntry.
    *
    * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        int result = 37;
        
        result = result*17 + dn.hashCode();
        
        for ( ServerAttribute attribute:serverAttributeMap.values() )
        {
            result = result*17 + attribute.hashCode();
        }

        return result;
    }

    
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        
        if ( ! ( o instanceof DefaultServerEntry ) )
        {
            return false;
        }
        
        DefaultServerEntry other = (DefaultServerEntry)o;
        
        if ( !dn.equals( other.getDn() ) )
        {
            return false;
        }
        
        if ( size() != other.size() )
        {
            return false;
        }
        
        for ( ServerAttribute attribute:other )
        {
            ServerAttribute attr = this.get( attribute.getType() );
            
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
        
        sb.append( "DefaultEntryServer\n" );
        sb.append( "    dn: " ).append( dn ).append( '\n' );
        
        if ( objectClassAttribute != null )
        {
            sb.append( objectClassAttribute );
        }

        if ( serverAttributeMap.size() != 0 )
        {
            for ( ServerAttribute attribute:serverAttributeMap.values() )
            {
                if ( !attribute.getType().equals( OBJECT_CLASS_AT ) )
                {
                    sb.append( attribute );
                }
            }
        }
        
        return sb.toString();
    }
}
