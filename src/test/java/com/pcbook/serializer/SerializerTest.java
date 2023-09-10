package com.pcbook.serializer;

import com.pbj.Laptop;
import com.pcbook.sample.Generator;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SerializerTest {

    @Test
    public void writeAndReadBinaryFile() throws IOException {
        String binaryFile = "laptop.bin";
        Laptop laptop1 = new Generator().NewLaptop();

        Serializer serializer = new Serializer();
        serializer.WriteBinaryFile(laptop1, binaryFile);

        Laptop laptop2 = serializer.ReadBinaryFile(binaryFile);
        Assert.assertEquals(laptop1, laptop2);

    }

}