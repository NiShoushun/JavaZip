package ZipTest;

import zip.core.ZipCompressor;

import java.io.File;

public class ZipInputTest {


    public static void main(String[] args) throws Exception{
        ZipCompressor zipCompressor = new ZipCompressor();
//        try {
//            zipCompressor.reset(new File(Thread.currentThread().getName()+"third.zip").getName());
//            zipCompressor.startCompressFiles(new String[]{"E:\\JavaStudy\\JavaZip", "src"});
//        }catch (Exception e){
//            e.printStackTrace();
//        }
        for (int i = 0; i < 3; i++) {
            new Thread(()->{
                try {
                    zipCompressor.reset(1024,true);
                    zipCompressor.packFiles(new File[]{new File("E:\\JavaStudy\\JavaZip")},new File(Thread.currentThread().getName()+".zip"));

                }catch (Exception e){
                }
            }).start();
        }


    }
}
