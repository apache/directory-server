
package org.apache.directory.server.kerberos.changepwd.io;

import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;

import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswordException;
import org.apache.directory.server.kerberos.changepwd.messages.AbstractPasswordMessage;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordError;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordReply;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordRequest;

public class ChangePasswordDecoder
{
    public static AbstractPasswordMessage decode( ByteBuffer buf, boolean isTcp ) throws ChangePasswordException
    {
        if ( isTcp )
        {
            // For TCP transport, there is a 4 octet header in network byte order
            // that precedes the message and specifies the length of the message.
            buf.getInt();
            buf.mark();
        }
        
        // cause we don't have a special message type value, try decoding as request, reply and error and send whichever succeeds
        
        try
        {
            return ChangePasswordRequest.decode( buf );
        }
        catch( Exception e )
        {
            resetOrRewind( buf );
        }
        
        try
        {
            return ChangePasswordReply.decode( buf );
        }
        catch( Exception e )
        {
            resetOrRewind( buf );
        }
        
        return ChangePasswordError.decode( buf );
    }
    
    
    private static void resetOrRewind( ByteBuffer buf )
    {
        try
        {
            buf.reset();
        }
        catch( InvalidMarkException e )
        {
            buf.rewind();
        }
    }
}
