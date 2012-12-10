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


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.model.schema.SchemaUtils;
import org.apache.directory.shared.ldap.model.schema.UsageEnum;
import org.apache.directory.shared.util.StringConstants;
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
    protected static Logger LOG = LoggerFactory.getLogger( FilteringOperationContext.class );

    private static final String[] EMPTY = new String[]
        {};

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
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public FilteringOperationContext( CoreSession session )
    {
        super( session );
    }


    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public FilteringOperationContext( CoreSession session, Dn dn )
    {
        super( session, dn );
    }


    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public FilteringOperationContext( CoreSession session, Set<AttributeTypeOptions> returningAttributes )
    {
        super( session );
        setReturningAttributes( returningAttributes );
    }


    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public FilteringOperationContext( CoreSession session, Dn dn, Set<AttributeTypeOptions> returningAttributes )
    {
        super( session, dn );
        setReturningAttributes( returningAttributes );
    }


    /**
     * Add an attribute ID to the current list, creating the list if necessary
     *
     * @param attrId the Id to add
     *
    public void addAttrsId( String attrId )
    {
        if ( noAttributes == null )
        {
            if ( attrId.equals( SchemaConstants.NO_ATTRIBUTE ) )
            {
                noAttributes = true;

                if ( attrsId != null )
                {
                    attrsId.clear();
                }

                return;
            }

            if ( attrId.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) )
            {
                allUserAttributes = true;

                return;
            }

            if ( attrId.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
            {
                allOperationalAttributes = true;

                return;
            }

            if ( attrsId == null )
            {
                attrsId = new ArrayList<String>();
            }

            attrsId.add( attrId );
        }
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
     * @param attribute The attribute we are looking for
     * @return true if the attribute is present
     */
    public boolean contains( String attribute )
    {
        if ( isNoAttributes() )
        {
            return false;
        }
        
        try
        {
            AttributeType attributeType = getSession().getDirectoryService().
                getSchemaManager().lookupAttributeTypeRegistry( attribute );
            
            if ( ( attributeType.getUsage() == UsageEnum.USER_APPLICATIONS ) && isAllUserAttributes() )
            {
                return true;
            }

            if ( ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS ) && isAllOperationalAttributes() )
            {
                return true;
            }

            AttributeTypeOptions attributeTypeOption = new AttributeTypeOptions( attributeType );
            
            boolean present = returningAttributes.contains( attributeTypeOption );
            
            return present;
        }
        catch ( LdapException le )
        {
            return false;
        }
    }


    /**
     * @param returningAttributes the returningAttributes to set
     */
    public void setReturningAttributes( Set<AttributeTypeOptions> returningAttributes )
    {
        this.returningAttributes = returningAttributes;
    }
    
    
    public void setReturningAttributes( String... attributesIds ) throws LdapException
    {
        if ( ( attributesIds != null ) && ( attributesIds.length != 0 ) )
        {
            returningAttributes = new HashSet<AttributeTypeOptions>();
            Set<String> attributesString = new HashSet<String>();
            int nbInvalid = 0;
            
            for ( String returnAttribute : attributesIds )
            {
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

                    returningAttributes.add( attrOptions );
                    attributesString.add( attributeType.getOid() );
                }
                catch ( LdapNoSuchAttributeException nsae )
                {
                    LOG.warn( "Requested attribute {} does not exist in the schema, it will be ignored",
                        returnAttribute );
                    // Unknown attributes should be silently ignored, as RFC 2251 states
                    nbInvalid++;
                }
            }

            // reset the noAttribute flag if it is already set cause that will be ignored if any other AT is requested
            if ( isNoAttributes() && 
                ( isAllUserAttributes() || 
                  isAllOperationalAttributes() || 
                  ( ( returningAttributes != null ) && ( returningAttributes.size() > 0 ) ) ) )
            {
                noAttributes = false;
            }
            
            if ( attributesString.size() > 0 )
            {
                returningAttributesString = attributesString.toArray( ArrayUtils.EMPTY_STRING_ARRAY );
            }
            else if ( nbInvalid > 0 )
            {
                returningAttributesString = ArrayUtils.EMPTY_STRING_ARRAY;
            }
            else
            {
                String[] ret = new String[3];
                
                int nbElem = 0;
                
                if ( isNoAttributes() )
                {
                    ret[nbElem++] = SchemaConstants.NO_ATTRIBUTE;
                }
                
                if ( isAllOperationalAttributes() )
                {
                    ret[nbElem++] = SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES;
                }
                
                if ( isAllUserAttributes() )
                {
                    ret[nbElem++] = SchemaConstants.ALL_USER_ATTRIBUTES;
                }
                
                if ( nbElem == 0 )
                {
                    returningAttributesString = new String[]{ SchemaConstants.ALL_USER_ATTRIBUTES };
                    allUserAttributes = true;
                }
                else
                {
                    returningAttributesString = new String[nbElem];
                    System.arraycopy( ret, 0, returningAttributesString, 0, nbElem );
                }
            }
        }
    }


    
    
    public void addReturningAttributes( String... attributesIds ) throws LdapException
    {
        if ( ( attributesIds != null ) && ( attributesIds.length != 0 ) )
        {
            Set<String> attributesString = new HashSet<String>();
            int nbInvalid = 0;
            
            for ( String returnAttribute : attributesIds )
            {
                if ( returnAttribute.equals( SchemaConstants.NO_ATTRIBUTE ) )
                {
                    noAttributes = true;
                    attributesString.add( SchemaConstants.NO_ATTRIBUTE );
                    continue;
                }

                if ( returnAttribute.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
                {
                    allOperationalAttributes = true;
                    attributesString.add( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES );
                    continue;
                }

                if ( returnAttribute.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) )
                {
                    allUserAttributes = true;
                    attributesString.add( SchemaConstants.ALL_USER_ATTRIBUTES );
                    continue;
                }

                try
                {
                    String id = SchemaUtils.stripOptions( returnAttribute );
                    Set<String> options = SchemaUtils.getOptions( returnAttribute );

                    AttributeType attributeType = session.getDirectoryService()
                        .getSchemaManager().lookupAttributeTypeRegistry( id );
                    AttributeTypeOptions attrOptions = new AttributeTypeOptions( attributeType, options );

                    returningAttributes.add( attrOptions );
                    
                    attributesString.add( attributeType.getOid() );
                }
                catch ( LdapNoSuchAttributeException nsae )
                {
                    LOG.warn( "Requested attribute {} does not exist in the schema, it will be ignored",
                        returnAttribute );
                    // Unknown attributes should be silently ignored, as RFC 2251 states
                    nbInvalid++;
                }
            }

            // reset the noAttribute flag if it is already set cause that will be ignored if any other AT is requested
            if ( isNoAttributes() && ( isAllUserAttributes() || isAllOperationalAttributes() || ( !returningAttributes.isEmpty() ) ) )
            {
                noAttributes = false;
            }
            
            if ( attributesString.size() > 0 )
            {
                returningAttributesString = attributesString.toArray( ArrayUtils.EMPTY_STRING_ARRAY );
            }
            else if ( nbInvalid > 0 )
            {
                returningAttributesString = ArrayUtils.EMPTY_STRING_ARRAY;
            }
            else
            {
                returningAttributesString = new String[]{ SchemaConstants.ALL_USER_ATTRIBUTES };
                allUserAttributes = true;
            }
        }
    }


    protected void setReturningAttributes( Collection<String> attributesIds )
        throws LdapException
    {
        setReturningAttributes( attributesIds.toArray( StringConstants.EMPTY_STRINGS ) );
    }


    /**
     * @param allOperationalAttributes the allOperationalAttributes to set
     */
    public void setAllOperationalAttributes( boolean allOperationalAttribute )
    {
        this.allOperationalAttributes = allOperationalAttribute;
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
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "FilteringOperationContext for Dn '" );
        sb.append( getDn().getName() ).append( "'" );

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
            
            sb.append(  ">" );
        }

        return sb.toString();
    }
}
