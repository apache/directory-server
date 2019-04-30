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
package org.apache.directory.server.core.api.interceptor.context;


import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.SchemaUtils;
import org.apache.directory.api.ldap.model.schema.UsageEnum;
import org.apache.directory.server.core.api.CoreSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A context used to store the filter used to manage the Attributes the user
 * ha srequested. It's used by the Lookup, List and Search operations
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class FilteringOperationContext extends AbstractOperationContext
{
    /** The LoggerFactory used by this Interceptor */
    protected static final Logger LOG = LoggerFactory.getLogger( FilteringOperationContext.class );

    /** A set containing the returning attributeTypesOptions */
    protected Set<AttributeTypeOptions> returningAttributes;

    /** The set of attributes to return as String */
    protected String[] returningAttributesString;

    /** A flag set to true if the user has requested all the operational attributes ( "+" )*/
    private boolean allOperationalAttributes;

    /** A flag set to true if the user has requested all the user attributes ( "*" ) */
    private boolean allUserAttributes;

    /** A flag set to true if the user has requested no attribute to be returned (1.1) */
    private boolean noAttributes;

    /** A flag to tell if only the attribute names to be returned. */
    protected boolean typesOnly = false;


    /**
     * Creates a new instance of FilteringOperationContext.
     *
     * @param session The session to use
     */
    public FilteringOperationContext( CoreSession session )
    {
        // Default to All User Attributes if we don't have any attributes
        this( session, SchemaConstants.ALL_USER_ATTRIBUTES );
    }


    /**
     * Creates a new instance of FilteringOperationContext.
     *
     * @param session The session to use
     * @param dn The Dn
     */
    public FilteringOperationContext( CoreSession session, Dn dn )
    {
        // Default to All User Attributes if we don't have any attributes
        this( session, dn, SchemaConstants.ALL_USER_ATTRIBUTES );
    }


    /**
     * Creates a new instance of LookupOperationContext.
     *
     * @param session The session to use
     * @param returningAttributes The attributes to return
     */
    public FilteringOperationContext( CoreSession session, String... returningAttributes )
    {
        super( session );

        setReturningAttributes( returningAttributes );
    }


    /**
     * Creates a new instance of LookupOperationContext.
     *
     * @param session The session to use
     * @param dn The Dn
     * @param returningAttributes The attributes to return
     */
    public FilteringOperationContext( CoreSession session, Dn dn, String... returningAttributes )
    {
        super( session, dn );

        setReturningAttributes( returningAttributes );
    }


    /**
     * @return the returningAttributes as a Set of AttributeTypeOptions
     */
    public Set<AttributeTypeOptions> getReturningAttributes()
    {
        return returningAttributes;
    }


    /**
     * @return the returning Attributes, as a array of Strings
     */
    public String[] getReturningAttributesString()
    {
        return returningAttributesString;
    }


    /**
     * Tells if an attribute is present in the list of attribute to return
     *
     * @param schemaManager The SchemaManager instance
     * @param attribute The attribute we are looking for
     * @return true if the attribute is present
     */
    public boolean contains( SchemaManager schemaManager, String attribute )
    {
        if ( isNoAttributes() )
        {
            return false;
        }

        try
        {
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( attribute );

            return contains( schemaManager, attributeType );
        }
        catch ( LdapException le )
        {
            return false;
        }
    }


    /**
     * Tells if an attribute is present in the list of attribute to return
     *
     * @param schemaManager The SchemaManager instance
     * @param attributeType The attributeType we are looking for
     * @return true if the attribute is present
     */
    public boolean contains( SchemaManager schemaManager, AttributeType attributeType )
    {
        if ( isNoAttributes() )
        {
            return false;
        }

        if ( ( attributeType.getUsage() == UsageEnum.USER_APPLICATIONS ) && allUserAttributes )
        {
            return true;
        }

        if ( ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS ) && allOperationalAttributes )
        {
            return true;
        }

        // Loop on the returningAttribute, as we have two conditions to check
        if ( returningAttributes == null )
        {
            return false;
        }

        // Shortcut
        if ( returningAttributes.contains( new AttributeTypeOptions( attributeType ) ) )
        {
            return true;
        }

        // Ok, do it the slow way...
        for ( AttributeTypeOptions attributeTypeOptions : returningAttributes )
        {
            if ( attributeTypeOptions.getAttributeType().equals( attributeType )
                || attributeTypeOptions.getAttributeType().isAncestorOf( attributeType ) )
            {
                return true;
            }
        }

        return false;
    }


    public void setReturningAttributes( String... attributeIds )
    {
        if ( ( attributeIds != null ) && ( attributeIds.length != 0 ) && ( attributeIds[0] != null ) )
        {
            // We have something in the list
            // first, ignore all the unkown AT and convert the strings to
            // AttributeTypeOptions
            returningAttributes = new HashSet<>();
            Set<String> attributesString = new HashSet<>();

            Set<AttributeTypeOptions> collectedAttributes = collectAttributeTypes( attributeIds );

            // If we have valid, '*' or '+' attributes, we can get rid of the NoAttributes flag
            if ( !collectedAttributes.isEmpty() || allUserAttributes || allOperationalAttributes )
            {
                noAttributes = false;
            }

            // Now, loop on the list of attributes, and remove all the USER attributes if
            // we have the '*' attribute, and remove all the OPERATIONAL attributes if we
            // have the '+' attribute
            if ( !collectedAttributes.isEmpty() )
            {
                for ( AttributeTypeOptions attributeTypeOption : collectedAttributes )
                {
                    if ( attributeTypeOption.getAttributeType().isUser() && !allUserAttributes )
                    {
                        // We can add the AttributeType in the list of returningAttributeTypes
                        returningAttributes.add( attributeTypeOption );
                        attributesString.add( attributeTypeOption.getAttributeType().getOid() );
                    }

                    if ( attributeTypeOption.getAttributeType().isOperational() && !allOperationalAttributes )
                    {
                        // We can add the AttributeType in the list of returningAttributeTypes
                        returningAttributes.add( attributeTypeOption );
                        attributesString.add( attributeTypeOption.getAttributeType().getOid() );
                    }
                }
            }

            if ( !attributesString.isEmpty() )
            {
                // We have some valid attributes, lt's convert it to String
                returningAttributesString = attributesString.toArray( ArrayUtils.EMPTY_STRING_ARRAY );
            }
            else
            {
                // No valid attributes remaining, that means they were all invalid
                returningAttributesString = ArrayUtils.EMPTY_STRING_ARRAY;
            }
        }
        else
        {
            // Nothing in the list : default to '*'
            allUserAttributes = true;
            returningAttributesString = ArrayUtils.EMPTY_STRING_ARRAY;
        }
    }


    private Set<AttributeTypeOptions> collectAttributeTypes( String... attributesIds )
    {
        Set<AttributeTypeOptions> collectedAttributes = new HashSet<>();

        if ( ( attributesIds != null ) && ( attributesIds.length != 0 ) )
        {
            for ( String returnAttribute : attributesIds )
            {
                if ( returnAttribute == null )
                {
                    continue;
                }

                if ( returnAttribute.equals( SchemaConstants.NO_ATTRIBUTE ) )
                {
                    noAttributes = true;
                    continue;
                }

                if ( returnAttribute.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
                {
                    allOperationalAttributes = true;
                    continue;
                }

                if ( returnAttribute.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) )
                {
                    allUserAttributes = true;
                    continue;
                }

                try
                {
                    String id = SchemaUtils.stripOptions( returnAttribute );
                    Set<String> options = SchemaUtils.getOptions( returnAttribute );

                    AttributeType attributeType = session.getDirectoryService()
                        .getSchemaManager().lookupAttributeTypeRegistry( id );
                    AttributeTypeOptions attrOptions = new AttributeTypeOptions( attributeType, options );

                    collectedAttributes.add( attrOptions );
                }
                catch ( LdapException le )
                {
                    LOG.warn( "Requested attribute {} does not exist in the schema, it will be ignored",
                        returnAttribute );
                    // Unknown attributes should be silently ignored, as RFC 2251 states
                }
            }
        }

        return collectedAttributes;
    }


    /**
     * @param allOperationalAttributes the allOperationalAttributes to set
     */
    public void setAllOperationalAttributes( boolean allOperationalAttributes )
    {
        this.allOperationalAttributes = allOperationalAttributes;
    }


    /**
     * @return The flag telling if the "*" attribute has been used
     */
    public boolean isAllUserAttributes()
    {
        return allUserAttributes;
    }


    /**
     * @param allUserAttributes the allUserAttributes to set
     */
    public void setAllUserAttributes( boolean allUserAttributes )
    {
        this.allUserAttributes = allUserAttributes;
    }


    /**
     * @return The flag telling if the "+" attribute has been used
     */
    public boolean isAllOperationalAttributes()
    {
        return allOperationalAttributes;
    }


    /**
     * @return The flag telling if the "1.1" attribute has been used
     */
    public boolean isNoAttributes()
    {
        return noAttributes;
    }


    /**
     * @param noAttributes the noAttributes to set
     */
    public void setNoAttributes( boolean noAttributes )
    {
        this.noAttributes = noAttributes;
    }


    /**
     * @return true, if attribute descriptions alone need to be returned
     */
    public boolean isTypesOnly()
    {
        return typesOnly;
    }


    /**
     * @param typesOnly true If we want to get back the attributeType only
     */
    public void setTypesOnly( boolean typesOnly )
    {
        this.typesOnly = typesOnly;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "FilteringOperationContext for Dn '" );
        sb.append( dn.getName() ).append( "'" );

        if ( isTypesOnly() )
        {
            sb.append( ", type only" );
        }

        if ( allOperationalAttributes )
        {
            sb.append( ", +" );
        }

        if ( allUserAttributes )
        {
            sb.append( ", *" );
        }

        if ( noAttributes )
        {
            sb.append( ", 1.1" );
        }

        if ( ( returningAttributesString != null ) && ( returningAttributesString.length > 0 ) )
        {
            sb.append( ", attributes : <" );
            boolean isFirst = true;

            for ( String returningAttribute : returningAttributesString )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }

                sb.append( returningAttribute );
            }

            sb.append( ">" );
        }

        return sb.toString();
    }
}
