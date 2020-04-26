package ZipTest;

import zip.core.ZipDecompressor;

import java.io.File;
import java.io.IOException;

public class ZipDecompressorTest {
    public static void main(String[] args) {
        ZipDecompressor zipDecompressor = new ZipDecompressor();
        zipDecompressor.unpack(new File("Thread-0second.zip"));
        System.out.println("finish");
        zipDecompressor.reset(2048,false);
        try {
            zipDecompressor.unpack(new File("Thread-0second.zip"),new File("newdir"));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        System.out.println("finish2");

    }
}
