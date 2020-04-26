package zip.ui;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import zip.Main;
import zip.config.ZipConfigurator;
import zip.core.ZipCompressor;
import zip.core.ZipDecompressor;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;

/**
 * @author Ni187
 */
public class Controller extends ClientUiComponents implements Initializable {

    /**
     * 工作线程锁，保证通过线程创建的压缩进程或者解压进程只有一个
     */
    ReentrantLock workLock;

    /**
     * 线程池，管理压缩，解压线程，同时间只能有一个线程来执行
     */
    ExecutorService fixedThreadPool;

    /**
     * 选中的文件里诶啊哦
     */
    private ObservableList<File> fileList;

    public Controller() {
        workLock = new ReentrantLock();
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("初始化");
        fileList = new ObservableListWrapper<>(new LinkedList<>());
        this.sourceFileListView.setItems(fileList);
        fixedThreadPool = Executors.newFixedThreadPool(1);
        // 消息框不可写
        msgBox.setWrapText(true);
    }

    /**
     * 开始压缩
     * @param event
     */
    public void startCompress(ActionEvent event) {

        // 开始解压时，首先检查是否有别的进程在工作
        if(isWork()){
            return;
        }
        long startTime = System.currentTimeMillis();
        // 构建压缩进程并提交给线程池，来执行
        fixedThreadPool.execute(()->{
            // 上锁
            workLock.lock();
            try {
                this.errMsg.setText("running");
                // 如果没有选择文件提示后，直接退出
                if (fileList.size() == 0) {
                    String noFileMsg = "压缩时，未选择文件，请重试。\n";
                    System.err.println(noFileMsg);
                    msgBox.appendText(noFileMsg);
                    return;
                }
                String beginMsg = "开始压缩：“" + fileList + ".\n”" +
                        "target=" + targetDir.getText() + "，" +
                        "buffersize=" + ZipConfigurator.getBufferSize() + "， " +
                        "level=" + ZipConfigurator.getLevel() + "， " +
                        "charset=" + ZipConfigurator.getCharset() + ", " +
                        "coverageModel=" + ZipConfigurator.isCoverageMode() + "\n";
                System.out.println(beginMsg);
                msgBox.appendText(beginMsg);

                // 获取压缩目标路径
                String targetPath = this.targetDir.getText();
                File target = new File(targetPath);
                //如果解压时，目标为目录，则在该目录下新建一个zip文件,以时间命名
                if(target.isDirectory()){
                    target = new File(targetPath+File.separator+System.currentTimeMillis()+".zip");
                }
                // 构造压缩器
                ZipCompressor zipCompressor = new ZipCompressor();
                try {
                    // 开始对文件集合压缩到目标文件
                    zipCompressor.packFiles(fileList.toArray(new File[0]), target);
                    System.out.println("解压完成");
                    msgBox.appendText("解压完成\n");
                } catch (IOException ioException) {
                    msgBox.appendText("压缩时，出现异常。");
                    ioException.printStackTrace();
                }
            }catch (Exception e){
                msgBox.appendText("出现未知异常。");
                e.printStackTrace();
            }finally {
                // 解锁
                workLock.unlock();
                event.consume();
                long endTime = System.currentTimeMillis();
                String timeCost = "共耗时："+(double)(endTime-startTime)/1000+"s\n";
                System.out.println(timeCost);
                msgBox.appendText(timeCost);
            }
        });


    }

    /**
     * 开始解压
     * @param event
     */
    public void startDecompress(ActionEvent event) {
        if(isWork()){
            return ;
        }
        long startTime = System.currentTimeMillis();
        fixedThreadPool.execute(()->{
            // 上锁
            workLock.lock();
            try{
                this.errMsg.setText("running");

                // 如果没有选择文件提示后，直接退出
                if (fileList.size() == 0) {
                    String noFileMsg = "解压时，未选择文件，请重试。\n";
                    System.err.println(noFileMsg);
                    msgBox.appendText(noFileMsg);
                    return;
                }
                String beginMsg = "开始解压：“" + fileList + ".\n”" +
                        "target=" + targetDir.getText() + "，" +
                        "buffersize=" + ZipConfigurator.getBufferSize() + "， " +
                        "level=" + ZipConfigurator.getLevel() + "， " +
                        "charset=" + ZipConfigurator.getCharset() + ", " +
                        "coverageModel=" + ZipConfigurator.isCoverageMode() + "\n";
                System.out.println(beginMsg);
                msgBox.appendText(beginMsg);

                // 开始构造解压对象，进行解压操作
                String targetPath = this.targetDir.getText();
                File target = new File(targetPath);
                // 如果目标不是目录
                if(target.exists()&&!target.isDirectory()){
                    System.err.println("输出路径为文件");
                    msgBox.appendText("输出路径为文件/n");
                    return;
                }
                ZipDecompressor zipDecompressor = new ZipDecompressor();
                zipDecompressor.unpackFiles(fileList.toArray(new File[0]), target);

                String finishMsg = "解压完成\n";
                System.out.println(finishMsg);
                msgBox.appendText(finishMsg);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                // 解锁
                workLock.unlock();
                event.consume();
                long endTime = System.currentTimeMillis();
                String timeCost = "共耗时："+(double)(endTime-startTime)/1000+"s\n";
                System.out.println(timeCost);
                msgBox.appendText(timeCost);
            }
        });

    }

    /**
     * 选择并添加文件
     *
     * @param event
     */
    public void chooseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("file", "*");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(Main.stage());
        if(file==null){
            return;
        }
        // 添加文件并
        this.fileList.add(file);
        event.consume();
    }

    /**
     * 选择并添目录
     *
     * @param event
     */
    public void chooseDir(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File dir = dirChooser.showDialog(Main.stage());
        if(dir==null){
            return;
        }
        this.fileList.add(dir);
        event.consume();
    }


    /**
     * 选择目标文件目录
     * @param event
     */
    public void chooseTarget(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File dir = dirChooser.showDialog(Main.stage());
        if(dir==null){
            return;
        }
        try{
            this.targetDir.setText(dir.getCanonicalPath());
        }catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    /**
     * 删除选定的文件
     *
     * @param event
     */
    public void removeFile(ActionEvent event) {
        int index = sourceFileListView.getSelectionModel().getSelectedIndex();
        try {
            if (index >= 0) {
                this.fileList.remove(index);
                this.sourceFileListView.setItems(fileList);
            }
        } catch (Exception e) {
            System.err.println("未选定文件\n");
        }
        event.consume();
    }

    /**
     * 清空选择的文件
     * @param event
     */
    public void clearFiles(ActionEvent event){
        this.fileList.clear();
    }

    /**
     * 清空消息
     * @param event
     */
    public void clearMsg(ActionEvent event){
        this.msgBox.setText("");
    }

    /**
     * 设置参数
     * @param event
     */
    public void setParameter(ActionEvent event) {
        StringBuffer msg = new StringBuffer();
        try {
            String level;
            System.out.println(msg.append(this.levelTextField.getText()));
            if ((level = this.levelTextField.getText()) != null && !"".equals(level.trim())) {
                ZipConfigurator.setLevel(Integer.parseInt(level));
                level = String.valueOf(ZipConfigurator.getLevel());
                System.out.println(msg.append("设置压缩等级：").append(level).append("\n"));
                this.levelTextField.setText(level);
            }

            String charset;
            if ((charset = this.charSetTextField.getText()) != null && !"".equals(charset.trim())) {
                ZipConfigurator.setCharset(Charset.forName(charset));
                charset = ZipConfigurator.getCharset().name();
                System.out.println(msg.append("设置编码格式：").append(charset).append("\n"));
                this.charSetTextField.setText(charset);
            }
            String bufferSize;
            if ((bufferSize = this.bufferSizeTextField.getText()) != null && !"".equals(bufferSize.trim())) {
                ZipConfigurator.setBufferSize(Integer.parseInt(bufferSize));
                bufferSize = String.valueOf(ZipConfigurator.getBufferSize());
                System.out.println(msg.append("设置缓冲区大小：").append(bufferSize).append("\n"));
                this.bufferSizeTextField.setText(bufferSize);
            }
            boolean coverageModel = this.coverageModelCB.isSelected();
            ZipConfigurator.setCoverageModel(coverageModel);
            System.out.println(msg.append("覆盖模式：").append(ZipConfigurator.isCoverageMode() ? "开启" : "关闭"+"\n"));
        } catch (NumberFormatException e) {
            msgBox.appendText("设置错误，请重新检查\n");
            System.err.println("设置错误，请重新检查\n");
        }
        msgBox.appendText(msg.toString());
        event.consume();
    }

    private boolean isWork(){
        if(workLock.isLocked()){
            String lockedMsg = "当前任务忙，请稍后再试\n";
            System.err.println(lockedMsg);
            this.errMsg.setText(lockedMsg);
            return true;
        }
        return false;
    }

}
