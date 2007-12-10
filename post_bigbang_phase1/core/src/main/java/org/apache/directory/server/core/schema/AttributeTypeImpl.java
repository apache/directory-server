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
package org.apache.directory.server.core.schema;


import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.AbstractAttributeType;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MutableSchemaObject;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.UsageEnum;


/**
 * An AttributeType implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
class AttributeTypeImpl extends AbstractAttributeType implements MutableSchemaObject
{
    private static final long serialVersionUID = 1L;

    private final Registries registries;
    
    private String syntaxOid;
    private String equalityOid;
    private String substrOid;
    private String orderingOid;
    private String superiorOid;
    
    
    public AttributeTypeImpl( String oid, Registries registries )
    {
        super( oid );
        this.registries = registries;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.AttributeType#getEquality()
     */
    public MatchingRule getEquality() throws NamingException
    {
        if ( equalityOid == null )
        {
            return findEquality( getSuperior() );
        }
        
        return registries.getMatchingRuleRegistry().lookup( equalityOid );
    }


    /**
     * Recursively the equality matchingRule if one exists within the attribute heirarchy.
     * 
     * @param at the attribute to find a equality matchingRule for
     * @return the equality MatchingRule or null if none exists for the attributeType
     * @throws NamingException if there are problems accessing the attribute heirarchy
     */
    private MatchingRule findEquality( AttributeType at ) throws NamingException
    {
        if ( at == null )
        {
            return null;
        }
        
        MatchingRule mr = at.getEquality();
        if ( mr == null )
        {
            return findEquality( at.getSuperior() );
        }
        else
        {
            return mr;
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.AttributeType#getOrdering()
     */
    public MatchingRule getOrdering() throws NamingException
    {
        if ( orderingOid == null )
        {
            return findOrdering( getSuperior() );
        }
        
        return registries.getMatchingRuleRegistry().lookup( orderingOid );
    }


    /**
     * Recursively the ordering matchingRule if one exists within the attribute heirarchy.
     * 
     * @param at the attribute to find a ordering matchingRule for
     * @return the ordering MatchingRule or null if none exists for the attributeType
     * @throws NamingException if there are problems accessing the attribute heirarchy
     */
    private MatchingRule findOrdering( AttributeType at ) throws NamingException
    {
        if ( at == null )
        {
            return null;
        }
        
        MatchingRule mr = at.getOrdering();
        if ( mr == null )
        {
            return findOrdering( at.getSuperior() );
        }
        else
        {
            return mr;
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.AttributeType#getSubstr()
     */
    public MatchingRule getSubstr() throws NamingException
    {
        if ( substrOid == null )
        {
            return findSubstr( getSuperior() );
        }
        
        return registries.getMatchingRuleRegistry().lookup( substrOid );
    }


    /**
     * Recursively gets the substring matchingRule if one exists within the attribute heirarchy.
     * 
     * @param at the attribute to find a substring matchingRule for
     * @return the substring MatchingRule or null if none exists for the attributeType
     * @throws NamingException if there are problems accessing the attribute heirarchy
     */
    private MatchingRule findSubstr( AttributeType at ) throws NamingException
    {
        if ( at == null )
        {
            return null;
        }
        
        MatchingRule mr = at.getSubstr();
        if ( mr == null )
        {
            return findSubstr( at.getSuperior() );
        }
        else
        {
            return mr;
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.AttributeType#getSuperior()
     */
    public AttributeType getSuperior() throws NamingException
    {
        if ( superiorOid == null )
        {
            return null;
        }
        
        return registries.getAttributeTypeRegistry().lookup( superiorOid );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.AttributeType#getSyntax()
     */
    public Syntax getSyntax() throws NamingException
    {
        if ( syntaxOid == null )
        {
            return findSyntax( getSuperior() );
        }
        
        return registries.getSyntaxRegistry().lookup( syntaxOid );
    }
    
    
    /**
     * Recursively walks up the ancestors to find the syntax for an attributeType.
     * 
     * @param at the attributeType to get the syntax for
     * @return the Syntax for the attributeType
     * @throws NamingException if no syntax can be found for the attributeType
     */
    private Syntax findSyntax( AttributeType at ) throws NamingException
    {
        if ( at == null )
        {
            throw new LdapNamingException( "Cannot find syntax for attributeType " + getName() 
                + " after walking ancestors.", ResultCodeEnum.OTHER );
        }
        
        if ( at.getSyntax() != null )
        {
            return at.getSyntax();
        }
        
        return findSyntax( at.getSuperior() );
    }
    

    public void setSyntaxOid( String syntaxOid )
    {
        this.syntaxOid = syntaxOid;
    }
    
    
    public void setSchema( String schema )
    {
        super.setSchema( schema );
    }
    
    
    public void setSuperiorOid( String superiorOid )
    {
        this.superiorOid = superiorOid;
    }

    
    public void setEqualityOid( String equalityOid )
    {
        this.equalityOid = equalityOid;
    }

    
    public void setSubstrOid( String substrOid )
    {
        this.substrOid = substrOid;
    }
    
    
    public void setOrderingOid( String orderingOid )
    {
        this.orderingOid = orderingOid;
    }
    
    
    public void setDescription( String description )
    {
        super.setDescription( description );
    }
    
    
    public void setNames( String[] names )
    {
        super.setNames( names );
    }
    
    
    public void setObsolete( boolean obsolete )
    {
        super.setObsolete( obsolete );
    }
    
    
    public void setCollective( boolean collective )
    {
        super.setCollective( collective );
    }
    
    
    public void setCanUserModify( boolean canUserModify )
    {
        super.setCanUserModify( canUserModify );
    }
    
    
    public void setLength( int length )
    {
        super.setLength( length );
    }
    
    
    public void setSingleValue( boolean singleValue )
    {
        super.setSingleValue( singleValue );
    }
    
    
    public void setUsage( UsageEnum usage )
    {
        super.setUsage( usage );
    }
}
