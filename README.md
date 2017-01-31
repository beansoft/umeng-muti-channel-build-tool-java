# umeng-muti-channel-build-tool-java
友盟渠道打包工具(Windows,Linux,Mac, 纯Java) ,界面版本, 高度Mac集成

传统友盟Android多渠道包打包的界面版本, 高度Mac集成, Dock图标支持打包进度显示, 支持安卓7, 打包速度快, 100个渠道包只需要一分钟, 只依赖Java, 运营妹子也可以用! 懒汉福利: 自动保存输入文件路径和渠道列表.

作者: 刘长炯 BeanSoft@126.com (微信号 weblogic ). 

本项目核心功能与 [友盟官方的开源.NET版](https://github.com/umeng/umeng-muti-channel-build-tool)基本一致, 并为Mac系统进行了优化,
有效利用Dock图标的功能来指示执行进度, 也可运行于Win,Linux平台. 曾稳定用于河狸家Android APP的打包达一年多之久.

打包工具不能完全保证生成的Apk文件的正确性，建议开发者最好做抽样测试。

老规矩上图:

<img src="https://raw.githubusercontent.com/beansoftapp/umeng-muti-channel-build-tool-java/master/screenshot/screenshot.png" width="">

<img src="https://raw.githubusercontent.com/beansoftapp/wallegui/master/screenshot/walle_dock.png">

## 对APK文件的要求
本工具只对 AndroidManifest.xml 有如下要求:

```xml
<meta-data     android:name="UMENG_CHANNEL"     android:value="xxxxx"/>
```

如果您有任何建议, 欢迎联系作者!

开发工具: IntelliJ IDEA 社区版

用法:<br>
1. 如果没有安装JRE, 请访问 http://www.java.com/ 下载安装.<br>
2. 复制 keystore 文件到jar 所在目录, 双击 out/androidchannel.jar, 点击"偏好设置..."按钮,
修改签名所需信息, 然后浏览选择APK文件, 输入渠道列表(以换行隔开), 点击按钮"生成渠道包", 即可在"输出渠道包"目录下看到所有的产生的渠道包.
最终打包结果可以用"渠道包集成检测..."来查看是否成功.

备注: androidchannel.jar 也可复制到其它目录下面单独执行, 同时暂时需要复制conf/目录下的示例配置文件.

## 关于test.keystore
此文件用下列命令生成:
```
keytool -genkey -keyalg RSA -alias keystore -keystore test.keystore -storepass 123456 -keypass 123456 -validity 10000 -dname CN=LiuChangjiong,OU=BeanSoft,O=BeanSoft,L=BJ,ST=BJ,C=ZH -noprompt
```
## 已知问题
最新版Gradle打包出的APK不能被ZIP工具所识别, 会导致重新签名失败. 如果你遇到此问题:

方案1: 使用旧版gradle打包, classpath 'com.android.tools.build:gradle:2.2.0' 测试通过;

方案2: 改用瓦力界面版和瓦力打包方案, 地址: https://github.com/beansoftapp/wallegui 技术细节参考: https://github.com/Meituan-Dianping/walle

## TODO
签名文件信息的配置方式修改为界面方式, 避免手工填写出错并假如校验机制

广告: Walle（瓦力）新一代渠道包打包神器的界面版本, 高度Mac集成, 支持安卓7, 闪电一般的打包速度, 100个渠道包只需要几秒钟, 开源地址: https://github.com/beansoftapp/wallegui

Build on top of following projects:<br>
https://github.com/rampi/properties-util<br>
https://github.com/bulenkov/Darcula<br>
https://github.com/umeng/umeng-muti-channel-build-tool<br>
https://github.com/rednaga/axmlprinter<br>
https://github.com/ntop001/AXMLEditor