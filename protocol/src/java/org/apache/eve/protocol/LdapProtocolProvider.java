/*
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
package org.apache.eve.protocol;


import org.apache.seda.protocol.ProtocolProvider;
import org.apache.seda.protocol.RequestHandler;
import org.apache.ldap.common.NotImplementedException;
import org.apache.commons.codec.stateful.DecoderFactory;
import org.apache.commons.codec.stateful.EncoderFactory;


/**
 * An LDAP protocol provider which transduces LDAP protocol requests into JNDI
 * operations.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapProtocolProvider implements ProtocolProvider
{
    public String getName()
    {
        throw new NotImplementedException( "getName in org.apache.eve.protocol.LdapProtocolProvider not implemented!" );
    }


    public DecoderFactory getDecoderFactory()
    {
        throw new NotImplementedException( "getDecoderFactory in org.apache.eve.protocol.LdapProtocolProvider not implemented!" );
    }


    public EncoderFactory getEncoderFactory()
    {
        throw new NotImplementedException( "getEncoderFactory in org.apache.eve.protocol.LdapProtocolProvider not implemented!" );
    }


    public RequestHandler getHandler( Object request )
    {
        throw new NotImplementedException( "getHandler in org.apache.eve.protocol.LdapProtocolProvider not implemented!" );
    }
}
