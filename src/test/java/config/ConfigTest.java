package config;

import java.io.*;
import java.util.Properties;

public class ConfigTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        InputStream inputStream = Object.class.getResourceAsStream("/zip.config");
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(properties.get("bufferSize"));
        try{
            properties.setProperty("bufferSize", "123456");
            properties.store(new FileWriter("src/main/resources/zip.config"), "Buffer size when the program is compressed");
        }catch (IOException ioException){
            ioException.printStackTrace();
        }

    }
}
