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
package org.apache.directory.server.kerberos.kdc;


import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;


public class SelectEncryptionType extends CommandBase
{
    public boolean execute( Context context ) throws Exception
    {
        KdcContext kdcContext = ( KdcContext ) context;
        KdcConfiguration config = kdcContext.getConfig();

        EncryptionType[] requestedTypes = kdcContext.getRequest().getEType();

        EncryptionType bestType = getBestEncryptionType( requestedTypes, config.getEncryptionTypes() );

        if ( bestType == null )
        {
            throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP );
        }

        return CONTINUE_CHAIN;
    }


    protected EncryptionType getBestEncryptionType( EncryptionType[] requestedTypes, EncryptionType[] configuredTypes )
    {
        for ( int ii = 0; ii < requestedTypes.length; ii++ )
        {
            for ( int jj = 0; jj < configuredTypes.length; jj++ )
            {
                if ( requestedTypes[ii] == configuredTypes[jj] )
                {
                    return configuredTypes[jj];
                }
            }
        }

        return null;
    }
}
