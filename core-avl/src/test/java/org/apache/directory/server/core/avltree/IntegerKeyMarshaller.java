
package org.apache.directory.server.core.avltree;

public class IntegerKeyMarshaller implements Marshaller<Integer>
{

    public byte[] serialize( Object integerObj )
    {
        Integer i = ( Integer ) integerObj;
        int y = i.intValue();
        byte[] data = new byte[4];
        data[0] = (byte)((y & 0xFF) >>> 24);
        data[1] = (byte)((y & 0xFF) >>> 16);
        data[2] = (byte)((y & 0xFF) >>> 8);
        data[3] = (byte)(y & 0xFF);
        
        return data;
    }


    public Integer deserialize( byte[] data )
    {
        if( data == null || data.length == 0)
        {
            return null;
        }
       
        int y =  ( ( data[0] & 0xFF ) << 24 ) 
                  | (( data[1] & 0xFF ) << 16 )
                  | ( ( data[2] & 0xFF ) << 8 )
                  | ( data[3] & 0xFF );
        
        return y;
    }

}
