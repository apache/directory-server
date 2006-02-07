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
package org.apache.kerberos.kdc.authentication;

import java.util.Map;

import org.apache.kerberos.crypto.checksum.ChecksumType;
import org.apache.kerberos.crypto.checksum.Crc32Checksum;
import org.apache.kerberos.crypto.checksum.RsaMd4Checksum;
import org.apache.kerberos.crypto.checksum.RsaMd5Checksum;
import org.apache.kerberos.crypto.checksum.Sha1Checksum;
import org.apache.kerberos.replay.InMemoryReplayCache;
import org.apache.kerberos.replay.ReplayCache;
import org.apache.kerberos.service.LockBox;
import org.apache.protocol.common.chain.Context;
import org.apache.protocol.common.chain.impl.CommandBase;

public class ConfigureAuthenticationChain extends CommandBase
{
    private static final ReplayCache replayCache = new InMemoryReplayCache();
    private static final LockBox lockBox = new LockBox();

    public boolean execute( Context context ) throws Exception
    {
        AuthenticationContext authContext = (AuthenticationContext) context;

        authContext.setReplayCache( replayCache );
        authContext.setLockBox( lockBox );

        Map checksumEngines = authContext.getChecksumEngines();
        checksumEngines.put( ChecksumType.CRC32, new Crc32Checksum() );
        checksumEngines.put( ChecksumType.RSA_MD4, new RsaMd4Checksum() );
        checksumEngines.put( ChecksumType.RSA_MD5, new RsaMd5Checksum() );
        checksumEngines.put( ChecksumType.SHA1, new Sha1Checksum() );

        return CONTINUE_CHAIN;
    }
}
