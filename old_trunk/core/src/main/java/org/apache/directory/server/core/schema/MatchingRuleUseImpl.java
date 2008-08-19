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
package org.apache.directory.server.core.schema;


import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.AbstractSchemaObject;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.MutableSchemaObject;


/**
 * A machingRuleUse implementation which dynamically pull applicable attributeTypes 
 * and it's matchingRule from the registries associated with it.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MatchingRuleUseImpl extends AbstractSchemaObject implements MatchingRuleUse, MutableSchemaObject
{
    private static final long serialVersionUID = 1L;

    private static final AttributeType[] EMPTY_ATTRIBUTES = new AttributeType[0];
    private static final String[] EMPTY_STRINGS = new String[0];

    private final Registries registries;
    private AttributeType[] applicableAttributes = EMPTY_ATTRIBUTES;
    private String[] applicableAttributesOids;
    

    /**
     * Creates a new matchingRuleUse.
     * 
     * @param oid the numeric oid of the matchingRule associated with this matchingRuleUse
     * @param registries the registries used to resolve the matchingRule and the applicable attributes
     */
    protected MatchingRuleUseImpl( String oid, Registries registries )
    {
        super( oid );
        this.registries = registries;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.MatchingRuleUse#getMatchingRule()
     */
    public MatchingRule getMatchingRule() throws NamingException
    {
        return registries.getMatchingRuleRegistry().lookup( getOid() );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.MatchingRuleUse#getApplicableAttributes()
     */
    public AttributeType[] getApplicableAttributes() throws NamingException
    {
        if ( applicableAttributesOids == null || applicableAttributesOids == EMPTY_STRINGS )
        {
            return EMPTY_ATTRIBUTES;
        }
        
        for ( int ii = 0; ii < applicableAttributesOids.length; ii++ )
        {
            applicableAttributes[ii] = registries.getAttributeTypeRegistry().lookup( applicableAttributesOids[ii] );
        }
        
        return applicableAttributes;
    }
    
    
    /**
     * Sets the oids used to look up the applicable AttributeTypes.
     * 
     * @param applicableAttributesOids the String[] of attributeType oids
     */
    public void setApplicableAttributesOids( final String[] applicableAttributesOids )
    {
        this.applicableAttributesOids = applicableAttributesOids;
        if ( applicableAttributesOids == null )
        {
            this.applicableAttributesOids = EMPTY_STRINGS;
            this.applicableAttributes = EMPTY_ATTRIBUTES;
        }
        else
        {
            this.applicableAttributesOids = applicableAttributesOids;
            this.applicableAttributes = new AttributeType[applicableAttributesOids.length];
        }
    }
    
    
    /**
     * Sets the names associated with this matchingRuleUse.
     */
    public void setNames( String[] names )
    {
        super.setNames( names );
    }
    
    
    /**
     * Sets the description associated with this matchingRuleUse.
     */
    public void setDescription( String description )
    {
        super.setDescription( description );
    }
    
    
    /**
     * Sets whether or not this matchingRuleUse is obsolete.
     */
    public void setObsolete( boolean obsolete )
    {
        super.setObsolete( obsolete );
    }
    
    
    /**
     * Sets the schema this matchingRuleUse is defined under.
     */
    public void setSchema( String schemaName )
    {
        super.setSchema( schemaName );
    }
}
