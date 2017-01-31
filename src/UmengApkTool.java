import apk.UMengManifestUtil;
import beansoft.swing.OptionPane;
import beansoft.util.FileOperate;
import config.SavedState;
import config.SignInfo;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import preferences.PreferencesDialog;
import util.ApkFileFilter;
import util.OSXSupport;
import util.StringUtil;
import zip.util.ZipUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 友盟Android多渠道包打包的界面版本。
 * Created by beansoft on 15/8/31.
 */
public class UmengApkTool {

    private JTextField textFieldSourceAPK;
    private JPanel contentPane;
    private JButton genButton;
    private JButton exitButton;
    private JLabel statusBar;
    private JProgressBar progressBar;
    private JButton checkButton;
    private JButton browseButton;
    private JButton settingButton;
    private JScrollPane channelsPane;
    private JPanel channelListPane;
    private JFileChooser chooser;

    private RTextScrollPane scrollPane;
    private RSyntaxTextArea textArea;

    private static ExecutorService threadPool = Executors.newFixedThreadPool(4);
    private int totalFinished = 0;// 总共完成的任务数

    public UmengApkTool() {
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        genButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doChannel();
            }
        });
        checkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCheck();
            }
        });
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseInputApk();
            }
        });

        settingButton.setAction(new PreferencesAction());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initUI();
            }
        });
    }

    private void initUI() {
        textArea = createTextArea();
        scrollPane = new RTextScrollPane(textArea, true);

        channelsPane.getViewport().add(scrollPane);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                delayUpdateTask();
            }
        });
    }

    private void delayUpdateTask() {
        // Init directorys
        FileOperate op = new FileOperate();
        op.newFolder("输出渠道包");
        op.newFolder("conf");

        SavedState savedState = SavedState.getInstance();
        if(savedState.channelList != null) {
            textArea.setText(savedState.channelList);
            textFieldSourceAPK.setText(savedState.lastFile);
        }
    }

    /**
     * Creates the text area for this application.
     *
     * @return The text area.
     */
    private RSyntaxTextArea createTextArea() {
        final RSyntaxTextArea textArea = new RSyntaxTextArea(25, 70);
        textArea.setTabSize(3);
        textArea.setCaretPosition(0);
        textArea.setHighlightCurrentLine(true);
        textArea.requestFocusInWindow();
        textArea.setMarkOccurrences(true);
        textArea.setCodeFoldingEnabled(true);
        textArea.setClearWhitespaceLinesEnabled(false);
//        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);

        try {
            InputStream in = getClass().getResourceAsStream("/dark.xml");
            Theme theme = Theme.load(in);
            theme.apply(textArea);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        textArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                saveChanneList();
            }
        });

        return textArea;
    }

    // 变化时保存渠道列表
    private void saveChanneList() {
        SavedState savedState = SavedState.getInstance();

        if(! (savedState.channelList !=null &&  textArea.getText().equals(savedState.channelList)) ) {
            savedState.channelList = textArea.getText();
            savedState.save();
        }
    }

    // 浏览输入APK
    private void browseInputApk() {
        SavedState savedState = SavedState.getInstance();
        initFileChooser(savedState.lastFile);
        chooser.setDialogTitle("选择需要打包的APK文件");
        int returnVal = chooser.showOpenDialog(this.contentPane);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            textFieldSourceAPK.setText(chooser.getSelectedFile().getPath());
            savedState.lastFile = textFieldSourceAPK.getText();
            savedState.save();
        }
    }

    // 初始化文件选择器
    private void initFileChooser(String currentFilePath) {
        if( chooser == null) {
            if(StringUtil.isEmpty(currentFilePath)) {
                chooser = new JFileChooser(new File("."));
            } else {
                File currentFile = new File(currentFilePath);
                chooser = new JFileChooser(currentFile);
            }
//            chooser.setCurrentDirectory();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFont(new Font("Tahoma", Font.PLAIN, 12));
            chooser.setFileFilter(new ApkFileFilter());
            chooser.setMultiSelectionEnabled(false);
        }
    }


    // 检查渠道包
    protected void doCheck() {
        initFileChooser(UMengManifestUtil.OUT_DIR);
        chooser.setDialogTitle("选择需要验证的APK安装包");
        int returnVal = chooser.showDialog(this.contentPane, "验证");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                String channelName = UMengManifestUtil.readApkChannelName(chooser.getSelectedFile().getCanonicalFile().getPath());
                OptionPane.showInfoMessageDialog(contentPane, "渠道名称为:\n" + channelName, "检验成功");
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            OptionPane.showErrorMessageDialog(contentPane, "渠道名称无法读取, 请检查", "检验失败");
        }
    }

    // 执行APK渠道打包工作
    private void doChannel() {
        final String inputApk = textFieldSourceAPK.getText();
        if(StringUtil.isEmpty(inputApk)) {
            textFieldSourceAPK.setBorder(BorderFactory.createLineBorder(Color.RED));
            OptionPane.showErrorMessageDialog(this.contentPane, "请输入源APK",
                    "提示");
            return;
        } else if(!new File(inputApk).canRead()) {
            textFieldSourceAPK.setBorder(BorderFactory.createLineBorder(Color.RED));
            OptionPane.showErrorMessageDialog(this.contentPane, "输入的APK文件路径无法读取",
                    "提示");
            return;
        } else {
            textFieldSourceAPK.setBorder(null);
        }

        try {
            String channelName = UMengManifestUtil.readApkChannelName(inputApk);
            if(StringUtil.isEmpty(channelName)) {
                OptionPane.showErrorMessageDialog(contentPane,
                        "选择的源APK文件渠道名称无法读取, 请检查APK文件是否有效并包含友盟渠道信息.", "初始化失败");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String channel = textArea.getText();
        if(StringUtil.isEmpty(channel)) {
            textArea.setBorder(BorderFactory.createLineBorder(Color.RED));
            OptionPane.showErrorMessageDialog(this.contentPane, "请输入渠道列表",
                    "提示");
            return;
        } else {
            textArea.setBorder(null);
        }

        SignInfo signInfo = SignInfo.getInstance();
        if(signInfo != null) {
            if(signInfo.validate()) {
                OptionPane.showErrorMessageDialog(contentPane,
                        "签名keystore文件名及密码, 签名key名称及密码任一项都不能为空,请修改后重试!", "签名信息无效");
                new PreferencesAction().actionPerformed(null);
                return;
            }

            if(!signInfo.validateFileReadable()) {
                OptionPane.showErrorMessageDialog(contentPane,
                        "签名keystore文件 " + signInfo.keystoreFile + " 无法读取,请修改后重试!", "签名信息错误");
                new PreferencesAction().actionPerformed(null);
                return;
            }
        }

        final String fullInputApkPath = inputApk;

        File apkFile = new File(fullInputApkPath);
        if(!apkFile.exists()) {
            OptionPane.showErrorMessageDialog(this.contentPane, "输入APK文件不可用",
                    "错误");
            return;
        }

        genButton.setEnabled(false);

        final String[] channels = channel.split("\\\n");
        System.out.println(channels.length);

        FileOperate op = new FileOperate();
        op.newFolder("输出渠道包");
        op.newFolder("temp");
//        op.delAllFile("输出渠道包");
        op.delAllFile("temp");

        new Thread() {
          public void run() {
              doChannelAsync(fullInputApkPath, channels);
          }
        }.start();
    }

    // 异步处理生成渠道包
    public void doChannelAsync(final String inputApk, String[] channels) {

        try {
            statusBar.setText("处理Manifest文件...");
            // 解压缩Manifest文件
            ZipUtil.extractFiles(inputApk,
                    new String[]{"AndroidManifest.xml"}, "temp", true, true);
            UMengManifestUtil.createChannelManifestFile(channels);

            statusBar.setText("正在生成渠道包...");
            OSXSupport.setDockIconBadge("0%");
            final int length = channels.length;
            totalFinished = 0;

            class SignRunnable implements Runnable {
                private String _channelF;
                public SignRunnable(String channel) {
                    this._channelF = channel;
                }

                @Override
                public void run() {
                    try {
                        UMengManifestUtil.createSignChannelAPKFile(inputApk, _channelF);
                    } catch (Exception e) {
                        e.printStackTrace();
                        OptionPane.showErrorMessageDialog(contentPane, "错误详情:\n" + e.getMessage(),
                                "生成渠道包出错了");
                        return;
                    }

                    totalFinished++;
                    progressBar.setValue(totalFinished * 100 / length);
                    statusBar.setText("完成生成渠道包 " + _channelF + ".");
                    OSXSupport.setDockIconBadge((totalFinished * 100 / length) + "%");
                    // 打包完成
                    if(totalFinished >= length) {
                        genButton.setEnabled(true);
                        OptionPane.showInfoMessageDialog(contentPane, "操作成功",
                                "恭喜");
                        statusBar.setText("生成渠道包成功");

                        try {
                            Desktop.getDesktop().open(new File(UMengManifestUtil.OUT_DIR));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            try {
//                UMengManifestUtil.createSignChannelAPKFile(inputApk, channels[0]);
                new SignRunnable(channels[0]).run();
            } catch (Exception e) {
                e.printStackTrace();
                OptionPane.showErrorMessageDialog(contentPane, "错误详情:\n" + e.getMessage(),
                        "生成渠道包出错了");
                genButton.setEnabled(true);
                return;
            }

            for(int i = 1; i < length; i++) {
                threadPool.execute(new SignRunnable(channels[i]));
            }
        } catch (Exception e) {
            e.printStackTrace();
            OptionPane.showErrorMessageDialog(this.contentPane, "错误详情:\n" + e.getMessage(),
                    "解压缩安装包出错了");
        }
    }

    private static JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("友盟渠道包批量打包签名工具");
        menu.add(new JMenuItem(new AboutAction()));
        menu.add(new JMenuItem(new PreferencesAction()));
        menu.add(new JMenuItem(new HomePageAction()) );
        menu.addSeparator();
        menu.add(new JMenuItem(new QuitAction()));

        mb.add(menu);
        return mb;
    }


    private static class AboutAction extends AbstractAction {

        public AboutAction() {
            putValue(NAME, "关于");
        }
        public void actionPerformed(ActionEvent e) {
            OptionPane.showInfoMessageDialog(null, "友盟渠道包工具1.1", "关于");
        }

    }

    private static class HomePageAction extends AbstractAction {

        public HomePageAction() {
            putValue(NAME, "访问网站");
        }
        public void actionPerformed(ActionEvent e) {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/beansoftapp/umeng-muti-channel-build-tool-java"));
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }
        }

    }

    // 退出Action
    private static class QuitAction extends AbstractAction {

        public QuitAction() {
            putValue(NAME, "退出");
        }
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }

    }

    // 偏好设置
    private static class PreferencesAction extends AbstractAction {

        public PreferencesAction() {
            putValue(NAME, "偏好设置...");
        }
        public void actionPerformed(ActionEvent e) {
//            OptionPane.showInfoMessageDialog(null, "友盟渠道包工具1.1", "偏好设置");
            PreferencesDialog dialog = new PreferencesDialog();
            dialog.pack();
            dialog.setVisible(true);
        }

    }


    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");// 整合Mac菜单
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "友盟渠道包工具");// 仅Mac自带的Java 6有效
        Locale.setDefault(Locale.CHINESE);

        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
        } catch(Exception ex) {

        }

        JFrame frame = new JFrame("友盟渠道包快速打包工具");
        try {
            Image iconImg =
            new ImageIcon(UmengApkTool.class.getResource("icon/chuck-highlight-image.png")).getImage();
//            Application.getApplication().setDockIconImage(iconImg);
            OSXSupport.initializeMacOSX(new AboutAction(), new QuitAction(), new PreferencesAction(), iconImg, null);
            OSXSupport.setDockIconBadge("友盟");
            frame.setIconImage(iconImg);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        if(!OSXSupport.isOSX()) {
            frame.getRootPane().setJMenuBar(createMenuBar());
//        }

        final UmengApkTool tool = new UmengApkTool();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                System.out.println(" +++++++ windowClosing ++++++ ");
                tool.saveChanneList();
            }
        });

        frame.setContentPane(tool.contentPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);// 最大化显示
        frame.setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}