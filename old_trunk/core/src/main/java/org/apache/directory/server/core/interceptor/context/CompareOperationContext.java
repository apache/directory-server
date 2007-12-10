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

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;

/**
 * A Compare context used for Interceptors. It contains all the informations
 * needed for the compare operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CompareOperationContext  extends AbstractOperationContext
{
    /** The entry OID */
    private String oid;

    /** The value to be compared */
    private Object value;
    
    /**
     * 
     * Creates a new instance of CompareOperationContext.
     *
     */
    public CompareOperationContext()
    {
    	super();
    }

    /**
     * 
     * Creates a new instance of CompareOperationContext.
     *
     */
    public CompareOperationContext( LdapDN dn )
    {
        super( dn );
    }

    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public CompareOperationContext( String oid )
    {
    	super();
        this.oid = oid;
    }

    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public CompareOperationContext( LdapDN dn, String oid )
    {
    	super( dn );
        this.oid = oid;
    }

    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public CompareOperationContext( LdapDN dn, String oid, Object value )
    {
    	super( dn );
        this.oid = oid;
        this.value = value;
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
	public Object getValue() 
	{
		return value;
	}

	/**
	 * Set the value to compare
	 * @param value The value to compare
	 */
	public void setValue( Object value ) 
	{
		this.value = value;
	}

	/**
     * @see Object#toString()
     */
    public String toString()
    {
        return "CompareContext for DN '" + getDn().getUpName() + "'" + 
        	( ( oid != null ) ? ", oid : <" + oid + ">" : "" ) +
        	( ( value != null ) ? ", value :'" +
        			( ( value instanceof String ) ?
        					value :
        					( ( value instanceof byte[] ) ?
        							StringTools.dumpBytes( (byte[])value ) : 
        								"unknown value type" ) )
        				+ "'"
        			: "" );
    }
}
