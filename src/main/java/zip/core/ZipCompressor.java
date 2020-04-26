package zip.core;

import zip.config.ZipConfigurator;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * 功能：将文件或者目录压缩成Zip格式
 * 可一定程度上设置读写文件时的缓冲区大小，压缩等级，编码格式
 * 对参数进行适当调整，保证参数在合理的数值或者范围
 * 使用 ReentrantLock 来保证线程在设置参数或者执行压缩方法时，其它线程必须等待，不会被其他线程篡改，保证一致性（主要是防止在短时间内对该对象进行重复的操作）
 * TODO 113
 *
 * @author Ni187
 * @version 1.0
 */
public class ZipCompressor {

    private int bufferSize;

    private int level;

    private Charset charset;

    private boolean coverageModel;

    /**
     * Zip压缩流，初始化延迟至执行压缩方法时
     */
    private ZipOutputStream zipOutputStream;

    /**
     * 可重入锁，在该对象进行解压开始时锁定，在解压结束后解锁，防止其它线程在该对象进行解压时修改参数
     */
    ReentrantLock reentrantLock = new ReentrantLock();

    public ZipCompressor(int bufferSize, int level, Charset charSet, boolean coverageModel) {
        this.bufferSize = ZipConfigurator.bufferSize(bufferSize);
        this.level = ZipConfigurator.level(level);
        this.charset = charSet;
        this.coverageModel = coverageModel;
    }

    /**
     * 从配置文件构造
     */
    public ZipCompressor() {
        this(ZipConfigurator.getBufferSize()
                , ZipConfigurator.getLevel()
                , ZipConfigurator.getCharset()
                , ZipConfigurator.isCoverageMode());
    }


    /**
     * 通过检查是否处于锁定状态，检查该对象是否立即可用
     *
     * @return 对象是否立即可用
     */
    public boolean usable() {
        return !this.reentrantLock.isLocked();
    }


    public void reset(int size, boolean coverageModel) {
        reset(this.bufferSize
                , this.level
                , coverageModel
                , this.charset);
    }

    /**
     * 重新设置压缩参数,
     * 由于方法内需要先获取锁，所以当在执行压缩方法时，会阻塞在该方法的开始，直至压缩结束释放掉锁
     * @param bufferSize 新的缓冲区容量大小，最后会被替换为不大于最大值且不小于改整数的二次幂或者最大值
     * @param level 新的压缩规格
     * @param coverageModel 覆盖模式
     * @param charset 编码方式
     */
    public void reset(int bufferSize, int level, boolean coverageModel, Charset charset) {
        //加锁
        reentrantLock.lock();
        try {
            //重新设置参数
            this.bufferSize = ZipConfigurator.bufferSize(bufferSize);
            this.level = ZipConfigurator.level(level);
            this.charset = charset;
            this.coverageModel = coverageModel;
        } finally {
            //解锁
            //先关闭zip输出流，释放连接
            close();
        }
    }

    /**
     * 初始化ZipOutStream对象
     *
     * @param target 目标文件
     * @throws IOException 初始化异常
     */
    private void initializeZipOutputStream(File target) throws IOException {
        //如果为null，进行初始化操作
        if (this.zipOutputStream == null) {
            this.zipOutputStream =
                    new ZipOutputStream(
                            new BufferedOutputStream(
                                    new FileOutputStream(target), bufferSize), charset);

            this.zipOutputStream.setLevel(this.level);
        }
        // 如果不为null,需要先对流对象进行关闭处理,再次进行初始化
        else {
            close();
            initializeZipOutputStream(target);
        }
    }

    /**
     * 压缩单个文件或目录
     *
     * @param source 文件或目录
     * @throws FileNotFoundException 未找到指定压缩文价
     */
    public void pack(File source, File target) throws IOException {
        packFiles(new File[]{source}, target);
    }


    /**
     * 开始压缩传入的多个文件或者目录,使用可重入锁来防止在执行该方法时，其他线程来调用该方法，造成数据不一致
     * 如果目标文件已存在，该方法覆盖掉之前文件
     *
     * @param sources 被压缩的文件或目录数组
     * @param target  目标文件夹
     */
    public void packFiles(File[] sources, File target) throws IOException {
        //如果目标文件存在，且不是覆盖模式，提出警告直接返回
        if (target.exists() && !coverageModel) {
            System.err.println("压缩未开始，文件：“"
                    + target.getCanonicalPath()
                    + "” 已经存在, 请尝试重设目标路径 或者 修改配置文件（Now：CoverageModel = false）");
            return ;
        }

        //加锁
        reentrantLock.lock();
        if (sources == null || sources.length == 0) {
            throw new FileNotFoundException("未指定文件");
        }
        try {
            //初始化zip流
            initializeZipOutputStream(target);
            for (File source : sources) {
                //使用改文件的父目录作为在zip文件夹下的根目录
                compress(source,source.getCanonicalFile().getParentFile().getName(), target);
            }
        } finally {
            //释放文件资源并解锁
            close();
        }
    }

    /**
     * 递归算法，将文件或者文件目录压缩到target路径下
     *
     * @param file 要被压缩的文件或目录
     * @param base zip下目录
     * @throws FileNotFoundException 未找到文件
     */
    private void compress(File file, String base, File target) throws FileNotFoundException {
        // 如果文件不存在抛出异常
        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath() + "文件不存在");
        }
        try {
            String canonicalPath = file.getCanonicalPath();
            //文件在zip包下的位置
            String zipPath = base + File.separator + file.getName();
            System.out.println(target + " <- " + zipPath);
            //如果该路径为文件夹
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    //该文件夹为空目录
                    if (files.length == 0) {
                        //最后拼接"/"是为了防止空目录不会被写入到zip文件中
                        zipOutputStream.putNextEntry(new ZipEntry(zipPath + "/"));
                    } else {
                        //遍历该文件夹下的每个文件
                        for (File f : files) {
                            compress(f, zipPath, target);
                        }
                    }
                }
            }
            //该路径为文件,且不是目标文件（如果缺少这一步比较，可能会发生自己不断压缩自己的现象）
            else if (!target.getCanonicalPath().equals(file.getCanonicalPath())) {
                zipOutputStream.putNextEntry(new ZipEntry(zipPath));
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file), this.bufferSize);
                byte[] bytes = new byte[bufferSize];
                int len;
                try {
                    while ((len = bufferedInputStream.read(bytes)) != -1) {
                        zipOutputStream.write(bytes, 0, len);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("压缩过程发生异常");
                } finally {
                    try {
                        bufferedInputStream.close();
                    } catch (IOException ioException) {
                        System.err.println("关闭文件：“" + canonicalPath + "” 时，发生异常：");
                        ioException.printStackTrace();
                    }
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * 关闭zip输出流，置为null 并 解锁
     */
    private void close() {
        try {
            if (this.zipOutputStream != null) {
                this.zipOutputStream.close();
                this.zipOutputStream = null;
            }
        } catch (IOException ioException) {
            throw new RuntimeException("文件关闭时发生异常");
        } finally {
            //只有当zipOutput对象close的时候，才会认为压缩已经结束，可以释放锁
            reentrantLock.unlock();
        }
    }

}
