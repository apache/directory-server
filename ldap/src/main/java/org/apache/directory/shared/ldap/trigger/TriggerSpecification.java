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

package org.apache.directory.shared.ldap.trigger;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.NullArgumentException;

/**
 * The Trigger Specification Bean.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class TriggerSpecification
{
    
    private LdapOperation ldapOperation;
    
    private ActionTime actionTime;
    
    private List<SPSpec> spSpecs; 
    
    
    
    public TriggerSpecification( LdapOperation ldapOperation, ActionTime actionTime, List<SPSpec> spSpecs )
    {
        super();
        if ( ldapOperation == null || 
            actionTime == null || 
            spSpecs == null )
        {
            throw new NullArgumentException( "TriggerSpecification cannot be initialized with any NULL argument." );
        }
        if ( spSpecs.size() == 0 )
        {
        	throw new IllegalArgumentException( "TriggerSpecification cannot be initialized with emtpy SPSPec list." );
        }
        this.ldapOperation = ldapOperation;
        this.actionTime = actionTime;
        this.spSpecs = spSpecs;
    }

    public ActionTime getActionTime()
    {
        return actionTime;
    }

    public LdapOperation getLdapOperation()
    {
        return ldapOperation;
    }

    public List<SPSpec> getSPSpecs() {
		return spSpecs;
	}
    
    public static class SPSpec
    {
    	private String name;
        
        private List<String> options;
        
        private List<String> parameters;

        public SPSpec(String name, List<String> options, List<String> parameters) {
			super();
			this.name = name;
			this.options = options;
			this.parameters = parameters;
		}
        
		public String getName() {
			return name;
		}

		public List<String> getOptions() {
			return options;
		}

		public List<String> getParameters() {
			return parameters;
		}

		@Override
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + ((name == null) ? 0 : name.hashCode());
			result = PRIME * result + ((options == null) ? 0 : options.hashCode());
			result = PRIME * result + ((parameters == null) ? 0 : parameters.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final SPSpec other = (SPSpec) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			if (parameters == null) {
				if (other.parameters != null)
					return false;
			} else if (!parameters.equals(other.parameters))
				return false;
			return true;
		}

		

    }





	
    
    
}
