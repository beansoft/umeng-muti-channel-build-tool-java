package apk;

import beansoft.util.FileOperate;
import com.umeng.editor.ChannelEditor;
import com.umeng.editor.decode.AXMLDoc;
import com.umeng.editor.decode.BTagNode;
import com.umeng.editor.decode.BXMLNode;
import com.umeng.editor.decode.StringBlock;
import config.SignInfo;
import util.StringUtil;
import zip.util.ZipUtil;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

/**
 * 修改AndroidManifest文件的UMeng渠道信息。
 * Created by beansoft on 15/8/31.
 */
public class UMengManifestUtil {
    public static final String CH_DIR = "temp/channes/";// 临时渠道XML文件存放路径
    public static final String OUT_DIR = "输出渠道包";// 输出路径

    // 渠道包数据相关, taken from ChannelEditor.java {{{
    private static final String META_DATA = "meta-data";
    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static String mChannelName = "UMENG_CHANNEL";

    private static int meta_data;
    private static int attr_name;
    private static int channel_name;
    private static int channel_value = -1;
    // }}}

    /**
     * 创建渠道XML文件。
     * @param channels 渠道列表
     */
    public static void createChannelManifestFile(String... channels) {
        try{
            new FileOperate().newFolder(CH_DIR);
            AXMLDoc doc = new AXMLDoc();
            doc.parse(new FileInputStream("temp/AndroidManifest.xml"));

            if(channels != null) {
                for(String ch : channels) {
                    if(!StringUtil.isEmpty(ch)) {
                        ChannelEditor editor = new ChannelEditor(doc);
                        editor.setChannel(ch);
                        editor.commit();
                        String maFilePath = CH_DIR + ch + ".xml";
                        doc.build(new FileOutputStream(maFilePath));
                        // TODO 校验 channel id
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 创建渠道XML文件。
     * @param channels
     */
    public static void createSignChannelAPKFile(String inputAPK, String... channels) throws
            Exception {
//        try{
            FileOperate fileOperate = new FileOperate();
            fileOperate.newFolder(OUT_DIR);
            String apkFileName = StringUtil.getFileName(inputAPK);

            if(channels != null) {
                for(String ch : channels) {
                    if(!StringUtil.isEmpty(ch)) {
                        signSingle(inputAPK, ch, apkFileName);
                    }
                }
            }
//        }catch(Exception e){
//            e.printStackTrace();
//        }
    }

    public static void signSingle(String inputAPK, String ch, String apkFileName) throws
    Exception {
        String maFilePath = CH_DIR + ch + ".xml";
        String outAPKFile = OUT_DIR +
                "/unsigned_" + ch + "_" + apkFileName;
        String signedAPKFile = OUT_DIR +
                "/" + ch + "_" + apkFileName;

        FileOperate fileOperate = new FileOperate();
        // 删除旧文件
        fileOperate.delFile(signedAPKFile);

        fileOperate.copyFile(inputAPK, outAPKFile);
        ZipUtil.replaceAPKManifest(outAPKFile, maFilePath);



        SignInfo signInfo = SignInfo.getInstance();
        if(signInfo != null) {
            int opresult = SignApk.sign(signInfo.keystoreFile, signInfo.keystorePassword,
                    signInfo.keyName, signInfo.keyPassword, outAPKFile, signedAPKFile);
            if(opresult == 0) {
                fileOperate.delFile(outAPKFile);
            }
        }
    }

    /**
     * 分析二进制 AndroidManifest XML 文件内容, 读取Umeng 渠道值.
     *
     * @param doc
     */
    public static String readChannelName(AXMLDoc doc) {
        StringBlock stringBlock = doc.getStringBlock();
        registStringBlock(stringBlock);

        BXMLNode application = doc.getApplicationNode(); //manifest node
        List<BXMLNode> children = application.getChildren();

        BTagNode umeng_meta = null;

        end:for(BXMLNode node : children){
            BTagNode m = (BTagNode)node;
            //it's a risk that the value for "android:name" maybe not String
            if((meta_data == m.getName()) && (m.getAttrStringForKey(attr_name) == channel_name)){
                umeng_meta = m;
                break end;
            }
        }

        if(umeng_meta != null){// 这里会有两个BAttribute, 第一个是channel_name, 第二个就是渠道名了
            BTagNode.Attribute[] attributes = umeng_meta.getAttribute();

            for(BTagNode.Attribute attribute: attributes) {
                if(attribute.mString != channel_name) {
                    channel_value = attribute.mValue;
                    break;
                }
            }

            return stringBlock.getStringFor(channel_value);
        }

        return null;
    }

    /**
     * Read umeng channel name from an apk file.
     * @param binaryAndroidManifestPath - the path of the binary AndroidManifestPath file
     * @return
     */
    public static String readChannelName(String binaryAndroidManifestPath) {
        AXMLDoc doc = new AXMLDoc();
        try {
            doc.parse(new FileInputStream(binaryAndroidManifestPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return readChannelName(doc);
    }

    /**
     * Read umeng channel name from an apk file.
     * @param apkPath - the path of the apk file
     * @return
     */
    public static String readApkChannelName(String apkPath) {
        try {
            byte[] data = ZipUtil.readEntryData(apkPath, "AndroidManifest.xml");
            AXMLDoc doc = new AXMLDoc();
            doc.parse(new ByteArrayInputStream(data));
            return readChannelName(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //First add resource and get mapping ids
    private static void registStringBlock(StringBlock sb){
        meta_data = sb.putString(META_DATA);
        attr_name = sb.putString(NAME);
        channel_name = sb.putString(mChannelName);
    }
}