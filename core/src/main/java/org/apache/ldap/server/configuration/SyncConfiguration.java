/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.ldap.server.configuration;

/**
 * A {@link Configuration} that syncs ApacheDS backend storage with disk.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyncConfiguration extends Configuration
{
    private static SyncConfiguration instance = new SyncConfiguration();

    private static final long serialVersionUID = -3260859085299322327L;

    /**
     * Creates a new instance.
     */
    public SyncConfiguration()
    {
        if ( instance == null )
        {
            instance = this;
        }
    }


    /**
     * Returns existing static instance or creates a new one if
     * it does not exist.
     *
     * @return a reusable static instance
     */
    public static SyncConfiguration getInstance()
    {
        if ( instance == null )
        {
            instance = new SyncConfiguration();
        }

        return instance;
    }
}
