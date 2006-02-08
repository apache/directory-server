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

package org.apache.directory.shared.ldap.name;

import java.util.HashMap;
import java.util.Map;

/**
 * The DnOidContainer is a class which ill contain a global Map associating
 * names to thier OIDs and Normalizer. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
public class DnOidContainer 
{
	/** The internaml map containing the name, Oid, Normalizer */ 
	private static Map oidByName = new HashMap();
	
	private static DnOidContainer instance;
	
	static
	{
		instance = new DnOidContainer();
	}
	/**
	 * Private constructor. This class cannot be constructed
	 *
	 */
	private DnOidContainer()
	{
		// Empty private constructor
	}
	
	public static DnOidContainer getInstance()
	{
		return instance;
	}

	/**
	 * Get the OID/Normalizer associated with the given name
	 * @param name The type which OID is asked for
	 * @return The couple OID/Normalizer correspondning to the given name
	 */
	public static Object getOidByName( String name ) 
	{
		return oidByName.get( name );
	}
	
	/** 
	 * Get the Map
	 * @return The Oid Map
	 */
	public static Map getOids()
	{
		return oidByName;
	}

	/**
	 * Set the Map
	 * @param oids The Map to store
	 */
	public static void setOids(Map oids) 
	{
		DnOidContainer.oidByName = oids;
	}
}
