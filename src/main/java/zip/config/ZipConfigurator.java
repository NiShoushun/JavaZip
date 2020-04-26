package zip.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;


/**
 * 管理全局配置
 * 读取zip.config文本内容
 * 设置读写zip文件时的 缓冲区大小，压缩等级，编码格式，是否采用覆盖模式
 *
 * @author Ni187
 */
public class ZipConfigurator {

    /**
     * 配置信息键值对集
     */
    static Properties properties = new Properties();

    //加载类时读取配置文本内容，将信息读取到java.util.Properties对象中
    static {
        try(InputStream inputStream = Object.class.getResourceAsStream("/zip.config");) {
            properties.load(inputStream);
        } catch (IOException e) {
            System.err.println("读取配置文件异常，程序将会使用默认参数.");
            e.printStackTrace();
        }
    }

    /**
     * 默认缓冲区容量，用于构造ZipOutputStream与文件读取的缓冲区大小
     */
    private final static int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * 最大缓冲区容量，用于构造ZipOutputStream与文件读取的缓冲区大小
     */
    private final static int MAXIMUM_BUFFER_SIZE = 65536;

    /**
     * 最小缓冲区容量，用于构造ZipOutputStream与文件读取的缓冲区大小
     */
    private final static int MINIMUM_BUFFER_SIZE = 256;

    /**
     * 默认压缩规格等级
     */
    private final static int DEFAULT_COMPRESSION_LEVEL = 6;

    /**
     * 编码方式采用java的默认编码
     */
    private final static Charset DEFAULT_CHARSET = Charset.defaultCharset();

    /**
     * 读写文件时采用覆盖的模式
     */
    private final static  boolean DEFAULT_COVERAGE_MODEL = true;

    /**
     * 配置信息 key
     */
    private final static String BUFFER_SIZE_PROPERTIES_NAME = "bufferSize";
    private final static String LEVEL_PROPERTIES_NAME = "level";
    private final static String CHARSET_PROPERTIES_NAME = "charset";
    private final static String COVERAGE_MODEL_PROPERTIES_NAME = "CoverageMode";


    public static void setCharset(Charset charset){
        properties.setProperty(CHARSET_PROPERTIES_NAME, charset.name());
    }

    public static  void setBufferSize(int bufferSize){
        properties.setProperty(BUFFER_SIZE_PROPERTIES_NAME, String.valueOf(bufferSize(bufferSize)));

    }

    public static void setLevel(int level){
        properties.setProperty(LEVEL_PROPERTIES_NAME, String.valueOf(level(level)));
    }

    public static void setCoverageModel(boolean coverageModel){
        properties.setProperty(CHARSET_PROPERTIES_NAME, String.valueOf(coverageModel));
    }

    /**
     * 读取配置文件的缓存区容量大小（bufferSize）,如果读取不到返回默认值
     * @return 缓存区容量大小
     */
    public static int getBufferSize(){
        String bufferSizeValue;
        try {
            if ((bufferSizeValue = properties.getProperty(BUFFER_SIZE_PROPERTIES_NAME)) != null) {
                return bufferSize(Integer.parseInt(bufferSizeValue));
            }
        }catch (Exception e){
            System.err.println("读取bufferSize异常，使用默认值"+DEFAULT_BUFFER_SIZE);
            e.printStackTrace();
        }
        return DEFAULT_BUFFER_SIZE;
    }

    /**
     * 读取配置文件的编码方式（charset）,如果读取不到返回默认值
     * @return 编码方式
     */
    public static Charset getCharset(){
        String charSetValue;
        try {
            if ((charSetValue = properties.getProperty(CHARSET_PROPERTIES_NAME)) != null) {
                return Charset.forName(charSetValue);
            }
        }catch (Exception e){
            System.err.println("读取charset异常，使用默认值"+DEFAULT_CHARSET);
            e.printStackTrace();
        }
        return DEFAULT_CHARSET;
    }

    /**
     * 读取配置文件的压缩等级（level）,如果读取不到返回默认值
     * @return 压缩等级
     */
    public static int getLevel(){
        String levelValue;
        try {
            if ((levelValue = properties.getProperty(LEVEL_PROPERTIES_NAME)) != null) {
                return level(Integer.parseInt(levelValue));
            }
        }catch (Exception e){
            System.err.println("读取level异常，使用默认值"+DEFAULT_COMPRESSION_LEVEL);
            e.printStackTrace();
        }
        return DEFAULT_COMPRESSION_LEVEL;
    }

    public static boolean isCoverageMode(){
        String modelValue;
        try{
            if ((modelValue = properties.getProperty(COVERAGE_MODEL_PROPERTIES_NAME)) != null) {
                return Boolean.parseBoolean(modelValue);
            }
        }catch (Exception e){
            System.err.println("读取CoverageMode异常，使用默认值"+DEFAULT_COMPRESSION_LEVEL);
            e.printStackTrace();
        }
        return DEFAULT_COVERAGE_MODEL;
    }

    /**
     * 首先会从配置文件中寻找缓冲区
     * 计算压缩流缓冲区容量，使之映射在2^n (n=0,1,2,4,8,16)
     *
     * @param size 设定的缓冲区容量
     * @return 不超过最大buffer容量且不小于最小buffer容量，且大于设定缓冲区容量的最小的2次幂
     */
    public  static int bufferSize(int size) {
        String str;
        if(( str= properties.getProperty(BUFFER_SIZE_PROPERTIES_NAME))!=null){
            size = Integer.parseInt(str);
        }
        // 将size限制最大缓冲容量与最小缓冲容量之间
        size = size > MAXIMUM_BUFFER_SIZE ? MAXIMUM_BUFFER_SIZE : Math.max(size, MINIMUM_BUFFER_SIZE);
        int n = size;
        // 开始将 n二进制 后面的0全部填充为1
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n >= MAXIMUM_BUFFER_SIZE ? MAXIMUM_BUFFER_SIZE : n + 1;
    }

    /**
     * 限制压缩规格level，超过范围返回默认值
     *
     * @param level 压缩规格
     * @return 在规定范围中的level
     */
    public static int level(int level) {
        if(level>0&&level<9){
            return level;
        }
        return DEFAULT_COMPRESSION_LEVEL;
    }

}
