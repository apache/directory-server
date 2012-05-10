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


import static org.apache.directory.shared.ldap.model.message.SearchScope.ONELEVEL;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.model.schema.SchemaUtils;
import org.apache.directory.shared.util.StringConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A context used for search related operations and used by all
 * the Interceptors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class SearchingOperationContext extends AbstractOperationContext
{
    /** The LoggerFactory used by this Interceptor */
    private static Logger LOG = LoggerFactory.getLogger( SearchingOperationContext.class );

    /** A flag describing the way alias should be handled */
    protected AliasDerefMode aliasDerefMode = AliasDerefMode.DEREF_ALWAYS;

    /** The sizeLimit for this search operation */
    protected long sizeLimit = 0;

    /** The timeLimit for this search operation */
    protected int timeLimit = 0;

    /** The scope for this search : default to One Level */
    protected SearchScope scope = ONELEVEL;

    /** A flag set if the returned attributes set contains '+' */
    protected boolean allOperationalAttributes = false;

    /** A flag set if the returned attributes set contains '*' */
    protected boolean allUserAttributes = false;

    /** A flag set if the returned attributes set contains '1.1' */
    protected boolean noAttributes = false;

    /** A set containing the returning attributeTypesOptions */
    protected Set<AttributeTypeOptions> returningAttributes;

    /** The set of attributes to return as String */
    protected String[] returningAttributesString;

    /** A flag if the search operation is abandoned */
    protected boolean abandoned = false;

    /** A flag to tell if only the attribute names to be returned */
    protected boolean typesOnly = false;


    /**
     * Creates a new instance of SearchingOperationContext.
     */
    public SearchingOperationContext( CoreSession session )
    {
        super( session );
    }


    /**
     * Creates a new instance of SearchingOperationContext.
     *
     * @param dn The Dn to get the suffix from
     */
    public SearchingOperationContext( CoreSession session, Dn dn )
    {
        super( session, dn );
    }


    /**
     * Creates a new instance of a SearchingOperationContext using one level
     * scope, with attributes to return.
     *
     * @param dn The Dn to get the suffix from
     * @param aliasDerefMode the alias dereferencing mode to use
     * @throws LdapException
     */
    public SearchingOperationContext( CoreSession session, Dn dn, Set<AttributeTypeOptions> returningAttributes )
    {
        super( session, dn );
        this.returningAttributes = returningAttributes;
    }


    protected void setReturningAttributes( Collection<String> attributesIds )
        throws LdapException
    {
        setReturningAttributes( attributesIds.toArray( StringConstants.EMPTY_STRINGS ) );
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
            if ( noAttributes && ( allUserAttributes || allOperationalAttributes || ( !returningAttributes.isEmpty() ) ) )
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


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "ListOperationContext with Dn '" + getDn().getName() + "'";
    }


    public AliasDerefMode getAliasDerefMode()
    {
        return aliasDerefMode;
    }


    public void setAliasDerefMode( AliasDerefMode aliasDerefMode )
    {
        this.aliasDerefMode = aliasDerefMode;
    }


    /**
     * @param sizeLimit the sizeLimit to set
     */
    public void setSizeLimit( long sizeLimit )
    {
        this.sizeLimit = sizeLimit;
    }


    /**
     * @return the sizeLimit
     */
    public long getSizeLimit()
    {
        return sizeLimit;
    }


    /**
     * @param timeLimit the timeLimit to set
     */
    public void setTimeLimit( int timeLimit )
    {
        this.timeLimit = timeLimit;
    }


    /**
     * @return the timeLimit
     */
    public int getTimeLimit()
    {
        return timeLimit;
    }


    /**
     * @param scope the scope to set
     */
    public void setScope( SearchScope scope )
    {
        this.scope = scope;
    }


    /**
     * @return the scope
     */
    public SearchScope getScope()
    {
        return scope;
    }


    /**
     * @param allOperationalAttributes the allOperationalAttributes to set
     */
    public void setAllOperationalAttributes( boolean allOperationalAttribute )
    {
        this.allOperationalAttributes = allOperationalAttribute;
    }


    /**
     * @return the allOperationalAttributes
     */
    public boolean isAllOperationalAttributes()
    {
        return allOperationalAttributes;
    }


    /**
     * @param allUserAttributes the allUserAttributes to set
     */
    public void setAllUserAttributes( boolean allUserAttributes )
    {
        this.allUserAttributes = allUserAttributes;
    }


    /**
     * @return the allUserAttributes
     */
    public boolean isAllUserAttributes()
    {
        return allUserAttributes;
    }


    /**
     * @param noAttributes the noAttributes to set
     */
    public void setNoAttributes( boolean noAttributes )
    {
        this.noAttributes = noAttributes;
    }


    /**
     * @return the noAttributes
     */
    public boolean isNoAttributes()
    {
        return noAttributes;
    }


    /**
     * @return true, if attribute descriptions alone need to be returned
     */
    public boolean isTypesOnly()
    {
        return typesOnly;
    }


    public void setTypesOnly( boolean typesOnly )
    {
        this.typesOnly = typesOnly;
    }


    /**
     * @param returningAttributes the returningAttributes to set
     */
    public void setReturningAttributes( Set<AttributeTypeOptions> returningAttributes )
    {
        this.returningAttributes = returningAttributes;
    }


    /**
     * @return the returningAttributes
     */
    public Set<AttributeTypeOptions> getReturningAttributes()
    {
        return returningAttributes;
    }


    /**
     * @return the returningAttributesString
     */
    public String[] getReturningAttributesString()
    {
        return returningAttributesString;
    }


    /**
     * @param abandoned the abandoned to set
     */
    public void setAbandoned( boolean abandoned )
    {
        this.abandoned = abandoned;
    }


    /**
     * @return the abandoned
     */
    public boolean isAbandoned()
    {
        return abandoned;
    }
}