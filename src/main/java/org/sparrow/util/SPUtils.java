package org.sparrow.util;

import org.sparrow.config.DatabaseDescriptor;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by mauricio on 30/12/2015.
 */
public class SPUtils
{
    public static int hash32(String data)
    {
        return hash32(data, 0);
    }

    public static int hash32(String data, int seed)
    {
        return MurmurHash.hash32(ByteBuffer.wrap(data.getBytes()), 0, data.length(), seed);
    }

    public static long hash64(String data)
    {
        return MurmurHash.hash2_64(ByteBuffer.wrap(data.getBytes()), 0, data.length(), 0);
    }

    public static String getDbPath(String ... names)
    {
        StringBuilder b = new StringBuilder();
        b.append(DatabaseDescriptor.getDataFilePath());
        Arrays.stream(names).forEach(x -> {
            if (!x.startsWith("."))
                b.append(System.getProperty("file.separator"));
            b.append(x);
        });
        return b.toString();
    }
}