
package org.apache.directory.shared.ldap.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class Base32Test
{
    @Test
    public void testEncode()
    {
        String[] data = new String[]{ "", "a", "ab", "abc", "abcd", "abcde", "abcdef" };
        String[] expected = new String[]{ "", "ME======", "MFRA====", "MFRGG===", "MFRGGZA=", "MFRGGZDF", "MFRGGZDFMY======"};
        
        for ( int i = 0; i < data.length; i++ )
        {
            String in = data[i];
        
            String res = Base32.encode( in );
            
            assertEquals( expected[i], res );
        }
    }
}
