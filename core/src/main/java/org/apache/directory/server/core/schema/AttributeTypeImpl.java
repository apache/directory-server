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
            return null;
        }
        
        return registries.getMatchingRuleRegistry().lookup( equalityOid );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.AttributeType#getOrdering()
     */
    public MatchingRule getOrdering() throws NamingException
    {
        if ( orderingOid == null )
        {
            return null;
        }
        
        return registries.getMatchingRuleRegistry().lookup( orderingOid );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.AttributeType#getSubstr()
     */
    public MatchingRule getSubstr() throws NamingException
    {
        if ( substrOid == null )
        {
            return null;
        }
        
        return registries.getMatchingRuleRegistry().lookup( substrOid );
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
            return null;
        }
        
        return registries.getSyntaxRegistry().lookup( syntaxOid );
    }
    
    
    public void setSyntaxOid( String syntaxOid )
    {
        this.syntaxOid = syntaxOid;
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
