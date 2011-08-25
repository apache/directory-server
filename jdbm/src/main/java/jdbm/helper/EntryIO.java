package jdbm.helper;

import java.io.IOException;

public interface EntryIO<K, V>
{
	public V read( K key, Serializer serializer) throws IOException;
	public void write( K key, V value, Serializer serializer ) throws IOException;
}