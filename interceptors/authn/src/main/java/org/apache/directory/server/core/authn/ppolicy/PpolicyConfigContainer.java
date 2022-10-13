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

package org.apache.directory.server.core.authn.ppolicy;


import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;


/**
 * A container to hold all the password policies defined in the server
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PpolicyConfigContainer
{
    /** a map holding the entry specific password policies */
    private Map<Dn, PasswordPolicyConfiguration> ppolicyConfigMap = new HashMap<>();

    /** the default password policy Dn */
    private Dn defaultPolicyDn;


    /**
     * add a entry specific policy
     *
     * @param configDn the Dn where this entry's password policy is defined
     * @param policyConfig the password policy configuration
     */
    public void addPolicy( Dn configDn, PasswordPolicyConfiguration policyConfig )
    {
        if ( configDn == null )
        {
            throw new IllegalArgumentException( "password policy config's Dn cannot be null" );
        }

        ppolicyConfigMap.put( configDn, policyConfig );
    }


    /**
     * @return true if atleast one entry specific password policy exists, false otherwise
     */
    public boolean hasCustomConfigs()
    {
        return !ppolicyConfigMap.isEmpty();
    }


    /**
     * Get the password policy configuration defined at a given Dn
     *  
     * @param configDn the Dn where password policy was configured
     * @return The found PasswordPolicyConfiguration instance
     */
    public PasswordPolicyConfiguration getPolicyConfig( Dn configDn )
    {
        return ppolicyConfigMap.get( configDn );
    }


    /**
     * @return the default password policy, null if not configured
     */
    public PasswordPolicyConfiguration getDefaultPolicy()
    {
        return getPolicyConfig( defaultPolicyDn );
    }


    /**
     * Set the default password policy configuration's Dn
     * 
     * @param defaultPolicyDn the default password policy configuration's Dn 
     */
    public void setDefaultPolicyDn( Dn defaultPolicyDn )
    {
        this.defaultPolicyDn = defaultPolicyDn;
    }


    /**
     * deactivate an existing password policy.
     *  
     * @param ppolicyConfigDn the Dn of the password policy configuration
     * @return the deactivated password policy config object of the given reference Dn, null otherwise
     */
    public PasswordPolicyConfiguration removePolicyConfig( Dn ppolicyConfigDn )
    {
        return ppolicyConfigMap.remove( ppolicyConfigDn );
    }
}
