package apk;

import sun.misc.BASE64Encoder;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.util.DerValue;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.jar.*;
import java.util.regex.Pattern;

/**
 * SignApk, apk 签名工具, 支持命令行方式签名。Modified from
 * https://github.com/umeng/umeng-muti-channel-build-tool/blob/master/VSProject/UmengTools/tools/SignApk.jar
 * Created by beansoft on 2015/9/1.
 */
public class SignApk {
    private static final String CERT_SF_NAME = "META-INF/CERT.SF";
    private static final String CERT_RSA_NAME = "META-INF/CERT.RSA";
    private static Pattern stripPattern = Pattern.compile("^META-INF/(.*)[.](SF|RSA|DSA)$");

    public SignApk() {
    }

    private static String readPassword(File keyFile) {
        System.out.print("Enter password for " + keyFile + " (password will not be hidden): ");
        System.out.flush();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        try {
            return stdin.readLine();
        } catch (IOException var3) {
            return null;
        }
    }

    private static KeySpec decryptPrivateKey(byte[] encryptedPrivateKey, File keyFile) throws GeneralSecurityException {
        EncryptedPrivateKeyInfo epkInfo;
        try {
            epkInfo = new EncryptedPrivateKeyInfo(encryptedPrivateKey);
        } catch (IOException var9) {
            return null;
        }

        char[] password = readPassword(keyFile).toCharArray();
        SecretKeyFactory skFactory = SecretKeyFactory.getInstance(epkInfo.getAlgName());
        SecretKey key = skFactory.generateSecret(new PBEKeySpec(password));
        Cipher cipher = Cipher.getInstance(epkInfo.getAlgName());
        cipher.init(2, key, epkInfo.getAlgParameters());

        try {
            return epkInfo.getKeySpec(cipher);
        } catch (InvalidKeySpecException var8) {
            System.err.println("signapk: Password for " + keyFile + " may be bad.");
            throw var8;
        }
    }

    private static Manifest addDigestsToManifest(JarFile jar) throws IOException, GeneralSecurityException {
        Manifest input = jar.getManifest();
        Manifest output = new Manifest();
        Attributes main = output.getMainAttributes();
        if(input != null) {
            main.putAll(input.getMainAttributes());
        } else {
            main.putValue("Manifest-Version", "1.0");
            main.putValue("Created-By", "1.0 (Android SignApk)");
        }

        BASE64Encoder base64 = new BASE64Encoder();
        MessageDigest md = MessageDigest.getInstance("SHA1");
        byte[] buffer = new byte[4096];
        TreeMap byName = new TreeMap();
        Enumeration entry = jar.entries();

        while(entry.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry)entry.nextElement();
            byName.put(jarEntry.getName(), jarEntry);
        }

        Iterator entry3 = byName.values().iterator();

        while(true) {
            String name;
            JarEntry entry2;
            do {
                do {
                    do {
                        do {
                            do {
                                if(!entry3.hasNext()) {
                                    return output;
                                }

                                entry2 = (JarEntry)entry3.next();
                                name = entry2.getName();
                            } while(entry2.isDirectory());
                        } while(name.equals("META-INF/MANIFEST.MF"));
                    } while(name.equals(CERT_SF_NAME));
                } while(name.equals(CERT_RSA_NAME));
            } while(stripPattern != null && stripPattern.matcher(name).matches());

            InputStream data = jar.getInputStream(entry2);

            int num;
            while((num = data.read(buffer)) > 0) {
                md.update(buffer, 0, num);
            }

            Attributes attr = null;
            if(input != null) {
                attr = input.getAttributes(name);
            }

            attr = attr != null?new Attributes(attr):new Attributes();
            attr.putValue("SHA1-Digest", base64.encode(md.digest()));
            output.getEntries().put(name, attr);
        }
    }

    private static void writeSignatureFile(Manifest manifest, OutputStream out) throws IOException, GeneralSecurityException {
        Manifest sf = new Manifest();
        Attributes main = sf.getMainAttributes();
        main.putValue("Signature-Version", "1.0");
        main.putValue("Created-By", "1.0 (Android SignApk)");
        BASE64Encoder base64 = new BASE64Encoder();
        MessageDigest md = MessageDigest.getInstance("SHA1");
        PrintStream print = new PrintStream(new DigestOutputStream(new ByteArrayOutputStream(), md), true, "UTF-8");
        manifest.write(print);
        print.flush();
        main.putValue("SHA1-Digest-Manifest", base64.encode(md.digest()));
        Map entries = manifest.getEntries();
        Iterator entryIterator = entries.entrySet().iterator();

        while(entryIterator.hasNext()) {
            Map.Entry entry = (Map.Entry)entryIterator.next();
            print.print("Name: " + (String)entry.getKey() + "\r\n");
            Iterator var11 = ((Attributes)entry.getValue()).entrySet().iterator();

            while(var11.hasNext()) {
                Map.Entry sfAttr = (Map.Entry)var11.next();
                print.print(sfAttr.getKey() + ": " + sfAttr.getValue() + "\r\n");
            }

            print.print("\r\n");
            print.flush();
            Attributes sfAttr1 = new Attributes();
            sfAttr1.putValue("SHA1-Digest", base64.encode(md.digest()));
            sf.getEntries().put((String)entry.getKey(), sfAttr1);
        }

        sf.write(out);
    }

    private static void writeSignatureBlock(Signature signature, X509Certificate publicKey, OutputStream out) throws IOException, GeneralSecurityException {
        SignerInfo signerInfo = new SignerInfo(new X500Name(publicKey.getIssuerX500Principal().getName()), publicKey.getSerialNumber(), AlgorithmId.get("SHA1"), AlgorithmId.get("RSA"), signature.sign());
        PKCS7 pkcs7 = new PKCS7(new AlgorithmId[]{AlgorithmId.get("SHA1")}, new ContentInfo(ContentInfo.DATA_OID, (DerValue)null), new X509Certificate[]{publicKey}, new SignerInfo[]{signerInfo});
        pkcs7.encodeSignedData(out);
    }

    private static void copyFiles(Manifest manifest, JarFile in,
                                  JarOutputStream out, long timestamp) throws IOException {
        byte[] buffer = new byte[4096];
        Map entries = manifest.getEntries();
        ArrayList names = new ArrayList(entries.keySet());
        Collections.sort(names);
        Iterator namesIterator = names.iterator();

        while(namesIterator.hasNext()) {
            String name = (String)namesIterator.next();
            JarEntry inEntry = in.getJarEntry(name);
            JarEntry outEntry = null;
            if(inEntry.getMethod() == 0) {
                outEntry = new JarEntry(inEntry);
            } else {
                outEntry = new JarEntry(name);
            }

            outEntry.setTime(timestamp);
            out.putNextEntry(outEntry);
            InputStream data = in.getInputStream(inEntry);

            int num;
            while((num = data.read(buffer)) > 0) {
                out.write(buffer, 0, num);
            }

//            out.flush();
        }
    }

    public static void main(String[] args)  throws Exception {
//        sign(new String[] {
//                "xx.keystore", "xxx", "xx", "xxx",
//                "Baidu_Claim_unsigned.apk", "Baidu_Claim_signed_helijia.apk"
//        });
    }

    /**
     * 对APK进行文件签名。
     * @param keystoreFile 输入的keystore文件名，例如 xx.keystore
     * @param keystorePassword
     * @param keyname 签名的key名称
     * @param key_password 签名的key的密码
     * @param inputApk 输入APK路径
     * @param outputApk 输出APK路径
     * @return 0 没有错误，其它值则有错误
     */
    public static int sign(String keystoreFile, String keystorePassword, String keyname, String key_password,
                           String inputApk, String outputApk)  throws Exception {
        return sign(new String[] {
                keystoreFile, keystorePassword, keyname, key_password, inputApk, outputApk
        });
    }

    public static int sign(String[] args) throws Exception {
        String keyStorePassword;
        if(args.length != 6) {
            System.err.println("Usage: signapk file.{keystore} keystore_password key_entry key_password\ninput.jar \noutput.jar");
            StringBuilder pathToKeyStore = new StringBuilder();
            String[] pathToInputApk = args;
            int keyPassword = args.length;

            for(int keyEntry = 0; keyEntry < keyPassword; ++keyEntry) {
                keyStorePassword = pathToInputApk[keyEntry];
                pathToKeyStore.append(keyStorePassword);
                pathToKeyStore.append('\n');
            }

            System.err.println(pathToKeyStore.toString());
//            System.exit(2);
            return 2;
//            return;
        }

        String keystoreFile = args[0];
        keyStorePassword = args[1];
        String key_entry = args[2];
        String key_password = args[3];
        String inputJarFile = args[4];
        String pathToOutputApk = args[5];
        JarFile inputJar = null;
        JarOutputStream outputJar = null;

        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream fis = new FileInputStream(keystoreFile);
            keyStore.load(fis, keyStorePassword.toCharArray());
            fis.close();
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(key_entry, new KeyStore.PasswordProtection(key_password.toCharArray()));

            if(entry == null) {
                throw new Exception("Null Entry Exception");
            }

            PrivateKey mPrivateKey = entry.getPrivateKey();
            X509Certificate mCertificate = (X509Certificate)entry.getCertificate();
            long timestamp = mCertificate.getNotBefore().getTime() + 3600000L;
            inputJar = new JarFile(new File(inputJarFile), false);
            outputJar = new JarOutputStream(new FileOutputStream(pathToOutputApk));
            outputJar.setLevel(9);
            Manifest manifest = addDigestsToManifest(inputJar);
            JarEntry je = new JarEntry("META-INF/MANIFEST.MF");
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            manifest.write(outputJar);
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(mPrivateKey);
            je = new JarEntry(CERT_SF_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            writeSignatureFile(manifest, new SignApk.SignatureOutputStream(outputJar, signature));
            je = new JarEntry(CERT_RSA_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            writeSignatureBlock(signature, mCertificate, outputJar);
            copyFiles(manifest, inputJar, outputJar, timestamp);
        } catch (Exception e) {
            e.printStackTrace();
//            System.exit(1);
            throw e;
        } finally {
            try {
                if(inputJar != null) {
                    inputJar.close();
                }

                if(outputJar != null) {
                    outputJar.close();
                }
            } catch (IOException var26) {
                var26.printStackTrace();
//                System.exit(1);
                return 1;
            }

        }

        return 0;
    }

    private static class SignatureOutputStream extends FilterOutputStream {
        private Signature mSignature;

        public SignatureOutputStream(OutputStream out, Signature sig) {
            super(out);
            this.mSignature = sig;
        }

        public void write(int b) throws IOException {
            try {
                this.mSignature.update((byte)b);
            } catch (SignatureException var3) {
                throw new IOException("SignatureException: " + var3);
            }

            super.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            try {
                this.mSignature.update(b, off, len);
            } catch (SignatureException var5) {
                throw new IOException("SignatureException: " + var5);
            }

            super.write(b, off, len);
        }
    }
}