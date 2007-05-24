
package org.apache.directory.server.core.partition.impl.btree.jdbm;

import java.io.IOException;

import org.apache.directory.shared.ldap.util.AttributesSerializerUtils;

import jdbm.helper.Serializer;

public class AttributesSerializer implements Serializer
{
    private static final long serialVersionUID = 1L;


    /* (non-Javadoc)
     * @see jdbm.helper.Serializer#deserialize(byte[])
     */
    public Object deserialize( byte[] data ) throws IOException
    {
        return AttributesSerializerUtils.deserialize( data );
    }


    /* (non-Javadoc)
     * @see jdbm.helper.Serializer#serialize(java.lang.Object)
     */
    public byte[] serialize( Object data ) throws IOException
    {
        return AttributesSerializerUtils.serialize( data );
    }

}
