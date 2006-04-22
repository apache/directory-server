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
package org.apache.directory.server.kerberos.kdc.authentication;


import java.util.Map;

import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.server.kerberos.shared.crypto.checksum.Crc32Checksum;
import org.apache.directory.server.kerberos.shared.crypto.checksum.RsaMd4Checksum;
import org.apache.directory.server.kerberos.shared.crypto.checksum.RsaMd5Checksum;
import org.apache.directory.server.kerberos.shared.crypto.checksum.Sha1Checksum;
import org.apache.directory.server.kerberos.shared.replay.InMemoryReplayCache;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.service.LockBox;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;


public class ConfigureAuthenticationChain implements IoHandlerCommand
{
    private static final ReplayCache replayCache = new InMemoryReplayCache();
    private static final LockBox lockBox = new LockBox();

    private String contextKey = "context";

    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        AuthenticationContext authContext = ( AuthenticationContext ) session.getAttribute( getContextKey() );

        authContext.setReplayCache( replayCache );
        authContext.setLockBox( lockBox );

        Map checksumEngines = authContext.getChecksumEngines();
        checksumEngines.put( ChecksumType.CRC32, new Crc32Checksum() );
        checksumEngines.put( ChecksumType.RSA_MD4, new RsaMd4Checksum() );
        checksumEngines.put( ChecksumType.RSA_MD5, new RsaMd5Checksum() );
        checksumEngines.put( ChecksumType.SHA1, new Sha1Checksum() );

        next.execute( session, message );
    }


    public String getContextKey()
    {
        return ( this.contextKey );
    }
}
