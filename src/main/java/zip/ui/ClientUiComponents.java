package zip.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;


/**
 * 客户端界面组件
 *
 * @author Ni187
 */
public class ClientUiComponents {

    /**
     * 目标文件
     */
    @FXML
    protected TextField targetDir;

    @FXML
    protected TextArea msgBox;

    /**
     * 开始压缩
     */
    @FXML
    protected Button startCompressBtn;


    /**
     * 开始解压
     */
    @FXML
    protected Button startDecompressBtn;

    /**
     * 文件选择按钮
     */
    @FXML
    protected Button chooseFileBtn;

    /**
     * 目录选择按钮
     */
    @FXML
    protected  Button chooseDirBtn;

    /**
     * 目录或者文件删除按钮
     */
    @FXML
    protected Button removeFileBtn;

    @FXML
    protected Button clearFilesBtn;

    @FXML
    protected Button clearMsgBtn;

    /**
     * 设置按钮
     */
    @FXML
    protected Button setterBtn;

    /**
     * 选择目标文件路径按钮
     */
    @FXML
    public Button chooseTargetBtn;

    @FXML
    protected ListView<java.io.File> sourceFileListView;

    /**
     * 覆盖模式
     */
    @FXML
    protected CheckBox coverageModelCB;

    @FXML
    protected TextField bufferSizeTextField;

    @FXML
    protected TextField charSetTextField;

    @FXML
    protected TextField levelTextField;

    /**
     * 异常信息标签
     */
    @FXML
    protected Text errMsg;


}
