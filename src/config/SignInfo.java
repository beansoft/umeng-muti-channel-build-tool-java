package config;

import com.xpec.properties.annotations.Properties;
import com.xpec.properties.container.Container;
import util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * APK 签名配置信息.
 * Created by beansoft on 16/9/20.
 */
@Properties("conf/sign.properties")
public class SignInfo {
    /** keystore签名文件名 */
    @Properties
    public String keystoreFile;
    /** keystore签名文件密码 */
    @Properties(key="keystorePassword")
    public String keystorePassword;
    /** 签名的key名称 */
    @Properties
    public String keyName;
    /** 签名的key的密码 */
    @Properties
    public String keyPassword;

    private SignInfo() {}

    @Override
    public String toString() {
        return "SignInfo{" +
                "keystoreFile='" + keystoreFile + '\'' +
                ", keystorePassword='" + keystorePassword + '\'' +
                ", keyName='" + keyName + '\'' +
                ", keyPassword='" + keyPassword + '\'' +
                '}';
    }

    /** 任意字段为空返回true, 即为无效 */
    public boolean validate() {
        return StringUtil.isAnyEmpty(keystoreFile, keystorePassword, keyName, keyPassword);
    }

    /** keystore file can be read? */
    public boolean validateFileReadable() {
        File file = new File(keystoreFile);
        return file.canRead();
    }

    public static SignInfo getInstance() {
        SignInfo u = new SignInfo();

        try {
            Container.injectProperties(u);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        System.out.println(u);
        return u;
    }

    /** 从字符串载入 */
    public static SignInfo getInstance(String input) {
        SignInfo u = new SignInfo();

        try {
            Container.injectPropertiesFromStream(u,
                    new ByteArrayInputStream(input.getBytes("UTF-8")));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(u);
        return u;
    }

    public void save() {
        try {
            Container.storeProperties(this, "APK签名文件信息, 示例配置:\n# keystore签名文件名\n" +
                    "keystoreFile=test.keystore\n" +
                    "# keystore签名文件密码\n" +
                    "keystorePassword=****\n" +
                    "# 签名的key名称\n" +
                    "keyName=xxx\n" +
                    "# 签名的key的密码\n" +
                    "keyPassword=xxx");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}