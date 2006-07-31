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
package org.apache.directory.server.core.configuration;


import javax.naming.NamingException;

import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A {@link Configuration} that removed the attached {@link Partition} in
 * the current {@link PartitionNexus}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RemovePartitionConfiguration extends Configuration
{
    private static final long serialVersionUID = -6690435863387769527L;

    private final LdapDN suffix;


    public RemovePartitionConfiguration( String suffix ) throws NamingException
    {
        this( new LdapDN( suffix.trim() ) );
    }


    public RemovePartitionConfiguration( LdapDN suffix )
    {
        if ( suffix == null )
        {
            throw new NullPointerException( "suffix" );
        }

        this.suffix = suffix;
    }


    public LdapDN getSuffix()
    {
        return suffix;
    }
}
