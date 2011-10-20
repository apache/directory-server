
package org.apache.directory.server.core.api.partition.index;

import java.io.IOException;
import java.io.Serializable;

public interface Serializer
{
    /**
     * Serialize the content of an object into a byte array.
     *
     * @param obj Object to serialize
     * @return a byte array representing the object's state
     */
    public byte[] serialize( Object obj ) throws IOException;


    /**
     * Deserialize the content of an object from a byte array.
     *
     * @param serialized Byte array representation of the object
     * @return deserialized object
     */
    public Object deserialize( byte[] serialized ) throws IOException;
}
