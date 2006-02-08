/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.shared.ldap.schema;

/**
 * The OidNomalizer class contains a couple : and OID with its Normalizer
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OidNormalizer {
	/** The oid */
	private String name;
	
	/** The normalizer to be used with this OID */
	private Normalizer normalizer;
	
	/**
	 * A constructor which accept two parameters
	 * @param oid The oid
	 * @param normalizer The associated normalizer
	 */
	public OidNormalizer( String name, Normalizer normalizer )
	{
		this.name = name;
		this.normalizer = normalizer;
	}
	
	/**
	 * A copy constructor. 
	 * @param oidNormalizer The OidNormalizer to copy from
	 */
	public OidNormalizer( OidNormalizer oidNormalizer )
	{
		name = oidNormalizer.name;
		normalizer = oidNormalizer.normalizer;
	}

	/**
	 * Get the normalizer
	 * @return The normalizer associated to the current OID
	 */
	public Normalizer getNormalizer() {
		return normalizer;
	}

	/**
	 * Set the normalizer
	 * @param The normalizer to be associated to the current OID
	 */
	public void setNormalizer(Normalizer normalizer) {
		this.normalizer = normalizer;
	}

	/**
	 * Get the current name
	 * @return The current name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the current OID
	 * @param The current OID
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Return a String representation of this class
	 */
	public String toString()
	{
		return "OidNormalizer : { " + name + ", " + normalizer.toString() + "}";
	}
}
