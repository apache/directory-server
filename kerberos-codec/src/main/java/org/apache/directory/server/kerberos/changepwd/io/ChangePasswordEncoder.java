
package org.apache.directory.server.kerberos.changepwd.io;

import java.nio.ByteBuffer;

import org.apache.directory.server.kerberos.changepwd.messages.AbstractPasswordMessage;
import org.apache.directory.api.asn1.EncoderException;

public class ChangePasswordEncoder
{
    public static ByteBuffer encode( AbstractPasswordMessage chngPwdMsg, boolean isTcp ) throws EncoderException
    {
        int len = chngPwdMsg.computeLength();
        
        ByteBuffer buf;
        if( isTcp )
        {
            buf = ByteBuffer.allocate( len + 4 );
            buf.putInt( len );
        }
        else
        {
            buf = ByteBuffer.allocate( len );
        }
        
        buf = chngPwdMsg.encode( buf );
        buf.flip();
        
        return buf;
    }
}
