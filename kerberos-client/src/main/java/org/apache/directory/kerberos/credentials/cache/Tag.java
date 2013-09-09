package org.apache.directory.kerberos.credentials.cache;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Tag
{
    int tag = 0;
    int tagLen = 8;
    int time = 0;
    int usec = 0;
    int length = 2 + 2 + 8; // len(tag) + len(tagLen) + len(tagData);

    public Tag(int tag, int time, int usec)
    {
        this.tag = tag;
        this.time = time;
        this.usec = usec;
    }
}
