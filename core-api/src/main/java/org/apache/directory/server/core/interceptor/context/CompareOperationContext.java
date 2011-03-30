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
package org.apache.directory.server.core.interceptor.context;


import org.apache.directory.server.core.CoreSession;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.message.CompareRequest;
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.util.Strings;


/**
 * A Compare context used for Interceptors. It contains all the informations
 * needed for the compare operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CompareOperationContext extends AbstractOperationContext
{
    /** The entry OID */
    private String oid;
    
    /** The associated AttributeType */
    private AttributeType attributeType;

    /** The value to be compared */
    private Value<?> value;
    
    
    /**
     * 
     * Creates a new instance of CompareOperationContext.
     *
     */
    public CompareOperationContext( CoreSession session )
    {
        super( session );
    }

    
    /**
     * 
     * Creates a new instance of CompareOperationContext.
     *
     */
    public CompareOperationContext( CoreSession session, Dn dn )
    {
        super( session, dn );
    }

    
    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public CompareOperationContext( CoreSession session, String oid )
    {
        super( session );
        this.oid = oid;
    }

    
    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public CompareOperationContext( CoreSession session, Dn dn, String oid )
    {
        super( session, dn );
        this.oid = oid;
    }

    
    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public CompareOperationContext( CoreSession session, Dn dn, String oid, Value<?> value )
    {
        super( session, dn );
        this.oid = oid;
        this.value = value;
    }

    
    public CompareOperationContext( CoreSession session, CompareRequest compareRequest )
    {
        super( session, compareRequest.getName() );
        this.oid = compareRequest.getAttributeId();
        this.value = compareRequest.getAssertionValue();
        this.requestControls = compareRequest.getControls();
        
        if ( requestControls.containsKey( ManageDsaIT.OID ) )
        {
            ignoreReferral();
        }
        else
        {
            throwReferral();
        }
    }


    /**
     * @return The compared OID
     */
    public String getOid() 
    {
        return oid;
    }

    
    /**
     * Set the compared OID
     * @param oid The compared OID
     */
    public void setOid( String  oid ) 
    {
        this.oid = oid;
    }

    
    /**
     * @return The value to compare
     */
    public Value<?> getValue() 
    {
        return value;
    }

    
    /**
     * Set the value to compare
     * @param value The value to compare
     */
    public void setValue( Value<?> value )
    {
        this.value = value;
    }


    /**
     *  @return The AttributeType for the compared value
     */
    public AttributeType getAttributeType()
    {
        return attributeType;
    }


    /**
     * Set the AttributeType associated with the OID
     * 
     * @param attributeType The AttributeType
     */
    public void setAttributeType( AttributeType attributeType )
    {
        this.attributeType = attributeType;
    }
    
    
    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.COMPARE_REQUEST.name();
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "CompareContext for Dn '" + getDn().getName() + "'" +
            ( ( oid != null ) ? ", oid : <" + oid + ">" : "" ) +
            ( ( value != null ) ? ", value :'" +
                    ( ( value.isHumanReadable() ) ?
                            value.getString() :
                            ( ( !value.isHumanReadable() ) ?
                                    Strings.dumpBytes(((BinaryValue) value).getReference()) :
                                        "unknown value type" ) )
                        + "'"
                    : "" );
    }
}
