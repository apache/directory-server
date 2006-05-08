/* 
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.directory.shared.ldap.ldif;

import javax.naming.ldap.Control;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.util.StringTools;

/**
 * The LdifControl class stores a control defined for an entry found in
 * a ldif file.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifControl implements Control
{
	static final long serialVersionUID = 1L;
	
	/** The control OID */
	private OID oid;
	
	/** The control criticality */
	private boolean criticality;
	
	/** The control BER encoded value */
	private byte[] value;
	
	/**
	 * Create a new Control
	 * @param oid OID of the created control
	 */
	public LdifControl( OID oid )
	{
		this.oid = oid;
		criticality = false;
		value = null;
	}

	/**
	 *	Returns the criticality of the current control 
	 */
	public boolean isCritical() 
	{
		return criticality;
	}

	/**
	 * Set the criticality
	 * @param criticality True or false.
	 */
	public void setCriticality( boolean criticality ) 
	{
		this.criticality = criticality;
	}

	/**
	 * Return the control's OID as a String
	 */
	public String getID() 
	{
		return oid.toString();
	}

	/**
	 * Set the control's OID
	 * 
	 * @param oid The control's OID
	 */
	public void setOid( OID oid ) 
	{
		this.oid = oid;
	}

	/**
	 * Returns the BER encoded value of the control
	 */
	public byte[] getEncodedValue() 
	{
		return value;
	}

	/**
	 * Set the BER encoded value of the control
	 * 
	 * @param value BER encodec value
	 */
	public void setValue( byte[] value ) 
	{
		this.value = value;
	}
	
	public String toString()
	{
		return "LdifControl : {" + oid.toString() + ", " + criticality + ", " + 
			StringTools.dumpBytes( value ) + "}";
	}
}
