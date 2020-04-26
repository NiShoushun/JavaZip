package zip.core;


import zip.config.ZipConfigurator;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


/**
 * 实现对zip的解压，可设置缓冲区大小以及编码
 *
 * @author Ni187
 */
public class ZipDecompressor {

    private int bufferSize;

    private Charset charset;

    private boolean coverageModel;

    private ZipDecompressor(int bufferSize, Charset charset, boolean coverageModel) {
        this.bufferSize = bufferSize;
        this.charset = charset;
        this.coverageModel = coverageModel;
    }

    public ZipDecompressor() {
        this(ZipConfigurator.getBufferSize()
                , ZipConfigurator.getCharset()
                , ZipConfigurator.isCoverageMode());
    }


    /**
     * 重新设置缓冲区大小与编码
     *
     * @param bufferSize 缓冲区大小
     * @param charset    编码
     */
    public void reset(int bufferSize, boolean coverageModel, Charset charset) {
        this.bufferSize = ZipConfigurator.bufferSize(bufferSize);
        this.coverageModel = coverageModel;
        this.charset = charset;
    }

    public void reset(int bufferSize, boolean coverageModel) {
        this.bufferSize = ZipConfigurator.bufferSize(bufferSize);
        this.coverageModel = coverageModel;
    }


    /**
     * 解压多个文件到目标文件夹
     * @param sources 文件数组
     * @param targetDir 到目标文件夹
     */
    public void unpackFiles(File[] sources,File targetDir) {
        if (sources == null || sources.length == 0) {
            return;
        }
        for (File source : sources) {
            try {
                unpack(source, targetDir);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

        /**
         * 将zip文件解压至该zip文件所在目录
         *
         * @param source zip文件
         */
    public void unpack(File source) {
        if(source.exists()||source.isDirectory()){
            try {
                System.err.println(source.getCanonicalFile()+" 不存在或者为目录");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return ;
        }
        try {
            unpack(source, source.getCanonicalFile().getParentFile());
        } catch (IOException ioException) {
            try {
                System.err.println("解压 “" + source.getCanonicalPath() + "” 时，发送未知错误，已停止");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 解压zip文件至指定目录
     *
     * @param source    zip文件
     * @param targetDir 解压目录
     */
    public void unpack(File source, File targetDir) throws IOException {

        if (!source.exists()) {
            System.err.println("未找到压缩文件“" + source.getCanonicalPath() + "”");
            throw new IOException("未找到压缩文件");
        }
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                System.err.println("创建文件夹“" + source.getCanonicalPath() + "”失败，请检查文件访问权限后重新尝试");
                throw new IOException("创建文件夹失败");
            }
        }
        decompress(source, targetDir);

    }

    /**
     * 解压zip文件到目标文件夹
     *
     * @param source    源文件
     * @param targetDir 目标文件夹
     * @throws IOException IO异常
     */
    private void decompress(File source, File targetDir) throws IOException {

        try (ZipFile zipFile = new ZipFile(source, charset)) {
            Enumeration entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                //如果为文件夹,直接创建文件夹
                if (entry.isDirectory()) {
                    File dir = new File(targetDir + "/" + entry.getName());
                    if (!dir.mkdirs()) {
                        System.err.println("解压时：“" + source.getCanonicalPath() + "”创建文件夹：“" + dir.getName() + "”失败");
                    }
                }
                //如果为文件
                else {
                    // 如果是文件，就先创建一个文件，然后用io流把内容copy过去
                    File targetFile = new File(targetDir + "/" + entry.getName());
                    // 保证这个文件的父文件夹必须要存在
                    File targetParent = targetFile.getParentFile();
                    if (!targetParent.exists()) {
                        if (!targetParent.mkdirs()) {
                            System.err.println("解压时：“" + source.getCanonicalPath() + "”创建文件夹：“" + targetParent.getName() + "”失败");
                        }
                    }

                    //创建文件
                    if (!targetFile.createNewFile()) {
                        //如果不是覆盖模式，将会提出警告，并跳过
                        if (!coverageModel) {
                            System.err.println("非覆盖模式，跳过“" + targetFile.getCanonicalPath() + "”");
                        }
                    }

                    //开始读取Zip文件并写入
                    try (
                            BufferedInputStream bin = new BufferedInputStream(zipFile.getInputStream(entry), bufferSize);
                            BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(targetFile), bufferSize)
                    ) {
                        int len;
                        byte[] buf = new byte[bufferSize];
                        while ((len = bin.read(buf)) != -1) {
                            bout.write(buf, 0, len);
                        }
                    }
                }
            }
        } catch (ZipException zipException) {
            zipException.printStackTrace();
            System.err.println("解压时发生异常");
        }
    }


}
