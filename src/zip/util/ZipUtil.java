package zip.util;

import beansoft.util.FileUtil;
import util.StringUtil;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A util class for operate zip files.
 * @author beansoft
 */
public class ZipUtil {
    public static TreeMap getZipFileEntries(String fileName) throws IOException {
        if (StringUtil.isEmpty(fileName)) return null;
        TreeMap map = new TreeMap();
        ZipFile zfile = null;
        try {
            zfile = new ZipFile(fileName);
            int size = zfile.size();

            Enumeration entries = zfile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                map.put(entry.getName(), entry);
            }
            zfile.close();
        } catch (Exception ex) {
            try {
                zfile.close();
            } catch (Exception e) {
            }
            throw new IOException(ex.getMessage());
        }
        return map;
    }

    public static void testZipFile(String zipFileName) throws IOException {
        if (StringUtil.isEmpty(zipFileName)) return;
        ZipFile zfile = null;
        try {
            zfile = new ZipFile(zipFileName);
            int size = zfile.size();


            Enumeration entries = zfile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                // Test read it
                InputStream in = zfile.getInputStream(entry);
                int data;
                while ((data = in.read()) != -1) {

                }
                in.close();
            }
            zfile.close();
        } catch (Exception ex) {
            try {
                zfile.close();
            } catch (Exception e) {
            }
            throw new IOException(ex.getMessage());
        }
    }

    /**
     *     Read an entry's data and return it.
     * @param zipFileName - zip file name
     * @param entryName - entry name
     * @return byte[] zip entry data
     * @throws IOException
     */
    public static byte[] readEntryData(String zipFileName, String entryName)
            throws IOException {
        if (StringUtil.isEmpty(zipFileName)) return null;
        if (StringUtil.isEmpty(entryName)) return null;

        ZipFile file = null;
        StringBuilder xmlSb = new StringBuilder(100);

        try {
            File e = new File(zipFileName);
            file = new ZipFile(e, 1);
            ZipEntry entry = file.getEntry(entryName);
            file.close();
            return readEntryData(zipFileName, entry);
        } catch(Exception ex) {
        } finally {
            if(file != null) {
                file.close();
            }
        }

        return null;
    }

    // Read an entry's data and return it
    public static byte[] readEntryData(String zipFileName, ZipEntry entry)
            throws IOException {
        if (StringUtil.isEmpty(zipFileName)) return null;
        if (entry == null) return null;
        if (StringUtil.isEmpty(entry.getName())) return null;

        ZipFile zip = null;
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            zip = new ZipFile(zipFileName);
            out = new ByteArrayOutputStream();
            in = zip.getInputStream(entry);
            int data;
            while ((data = in.read()) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
            zip.close();
            return out.toByteArray();
        } catch (Exception ex) {
            try {
                in.close();
                out.close();
                zip.close();
            } catch (Exception ex1) {
            }
            throw new IOException(ex.getMessage());
        }
    }

    // Get the entry number in a zip file
    public static int getEntrySize(String zipFileName) {
        ZipFile zfile = null;
        int size = -1;
        try {
            zfile = new ZipFile(zipFileName);
            size = zfile.size();
            zfile.close();
        } catch (IOException ex) {
            try {
                zfile.close();
            } catch (Exception e) {
            }
        }
        return size;
    }

    // Create a new zip file
    public static void newZip(String fileName) throws IOException {
        if (fileName == null || fileName.length() == 0) return;
        ZipOutputStream zout = null;
        File tmpzip = null;
        try {
            // Create new zip file
            tmpzip = new File(fileName);
            // Create an empty entry and put it into the zip file
            ZipEntry ze = new ZipEntry(".");
            zout = new ZipOutputStream(new FileOutputStream(tmpzip));
            zout.putNextEntry(ze);
            zout.closeEntry();
            zout.close();
        } catch (IOException ex) {
            try {
                zout.close();
                tmpzip.delete();
            } catch (Exception ex1) {
            }
            throw ex;
        }
    }

    public static void replaceWARFileConfigToStream(String zipFileName, String configXML, OutputStream out) throws IOException {
        if (StringUtil.isEmpty(zipFileName)) return;
        if (configXML == null || configXML.length() <= 0) return;


        int level = 9;
        ZipOutputStream zout = null;
        ZipInputStream zin = null;
        ZipFile zfile = null;
        try {
            zfile = new ZipFile(zipFileName);
            // Create temporary zip file
            ZipEntry ze;
            zin = new ZipInputStream(new FileInputStream(zipFileName));
            zout = new ZipOutputStream(out);
            zout.setLevel(level);
            int len = 0;
            byte[] b = new byte[10240];// 10KB buffer
            // Copy the old zip entries
            while ((ze = zin.getNextEntry()) != null) {
                zout.putNextEntry(createEntry(ze));

                System.out.println(ze.getName());

                if (ze.getName().equals("WEB-INF/beanmonitor/tomcat.xml")) {
                    System.out.println("---> 写入配置内容");
                    byte[] dat = configXML.getBytes("UTF-8");
                    ze.setSize(dat.length);
                    zout.write(dat);
                } else {
                    while ((len = zin.read(b)) != -1) {
                        zout.write(b, 0, len);
                    }
                }

                zout.closeEntry();
                zin.closeEntry();
            }

            zout.close();
            zin.close();
            zfile.close();
        } catch (IOException ex) {
            // Delete the temporary file
            try {
                zin.close();
                zout.close();
                zfile.close();
            } catch (Exception ex1) {
            }
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * 替换 APK 文件中的二进制 AndroidManifest.xml
     * @param apkFileName
     * @param configXMLFile 配置文件名字
     * @throws IOException
     */
    public static void replaceAPKManifest(String apkFileName, String configXMLFile) throws Exception {
        replaceAPKManifest(apkFileName, FileUtil.readFileBinary(configXMLFile));
    }

    /**
     * 替换 APK 文件中的二进制 AndroidManifest.xml
     * @param apkFileName
     * @param configXML
     * @throws IOException
     */
    public static void replaceAPKManifest(String apkFileName, byte[] configXML) throws IOException {
        if (StringUtil.isEmpty(apkFileName)) return;
        if (configXML == null || configXML.length <= 0) return;


        int level = 9;
        ZipOutputStream zout = null;
        ZipInputStream zin = null;
        ZipFile zfile = null;
        File tmpzip = null;
        try {
            zfile = new ZipFile(apkFileName);
            // Create temporary zip file
            tmpzip = new File(apkFileName + ".tmp");
            ZipEntry ze;
            zin = new ZipInputStream(new FileInputStream(apkFileName));
            zout = new ZipOutputStream(new FileOutputStream(tmpzip));
            zout.setLevel(level);
            int len = 0;
            byte[] b = new byte[10240];// 10KB buffer
            // Copy the old zip entries
            while ((ze = zin.getNextEntry()) != null) {
                zout.putNextEntry(createEntry(ze));

//                System.out.println(ze.getName());

                if (ze.getName().equals("AndroidManifest.xml")) {
                    System.out.println("---> 写入 AndroidManifest 配置内容");
                    ze.setSize(configXML.length);
                    zout.write(configXML);
                } else {
                    while ((len = zin.read(b)) != -1) {
                        zout.write(b, 0, len);
                    }
                }

                zout.closeEntry();
                zin.closeEntry();
            }

            zout.close();
            zin.close();
            zfile.close();
            File oldFile = new File(apkFileName);
            oldFile.delete();
            tmpzip.renameTo(oldFile);
        } catch (IOException ex) {
            // Delete the temporary file
            try {
                zin.close();
                zout.close();
                zfile.close();
                tmpzip.delete();
            } catch (Exception ex1) {
            }
            throw new IOException(ex.getMessage());
        }
    }

    public static void addFiles(String zipFileName, String baseDir,
                                String[] fileNames, int level, boolean replace) throws IOException {
        if (StringUtil.isEmpty(zipFileName)) return;
        if (fileNames == null || fileNames.length <= 0) return;

        // Use a map to facilitate the searching operation
        Map filesMap = new TreeMap();
        for (int i = 0; i < fileNames.length; i++) {
            String fileName = fileNames[i];
            if (fileName != null) {
                if (fileName.equalsIgnoreCase(zipFileName)) {
                    throw new IOException("无法添加文件到自身");
                }
                if (fileName.equalsIgnoreCase(zipFileName + ".tmp")) {
                    throw new IOException("无法添加程序将要创建的临时文件");
                }
                filesMap.put(StringUtil.getEntryName(baseDir, fileName), fileName);
            }
        }

        level = checkZipLevel(level);
        ZipOutputStream zout = null;
        ZipInputStream zin = null;
        ZipFile zfile = null;
        File tmpzip = null;
        try {
            zfile = new ZipFile(zipFileName);
            // Create temporary zip file


            tmpzip = new File(zipFileName + ".tmp");
            ZipEntry ze;
            zin = new ZipInputStream(new FileInputStream(zipFileName));
            zout = new ZipOutputStream(new FileOutputStream(tmpzip));
            zout.setLevel(level);
            int len = 0;
            byte[] b = new byte[10240];// 10KB buffer
            // Copy the old zip entries
            while ((ze = zin.getNextEntry()) != null) {

                // Find out if the replaced file is found
                if (replace == true && filesMap.get(ze.getName()) != null) {
                    continue;
                }
                zout.putNextEntry(createEntry(ze));
                while ((len = zin.read(b)) != -1) {
                    zout.write(b, 0, len);
                }
                zout.closeEntry();
                zin.closeEntry();
            }

            // Add the specified files into the zip file
            for (int i = 0; i < fileNames.length; i++) {
                String fileName = fileNames[i];
                String entryName = StringUtil.getEntryName(baseDir, fileName);
                if ((zfile.getEntry(entryName) == null) ||
                        ((zfile.getEntry(entryName) != null && replace))) {
                    File addedFile = new File(fileName);
                    ze = new ZipEntry(entryName);
                    ze.setMethod(ZipEntry.DEFLATED);
                    ze.setTime(addedFile.lastModified());

                    // Directory only put the entry, not write any data
                    if (addedFile.isDirectory()) {
                        zout.putNextEntry(ze);
                        continue;
                    }
                    FileInputStream in = new FileInputStream(addedFile);
                    ze.setSize(in.available());
                    zout.putNextEntry(ze);
                    while ((len = in.read(b)) != -1) {
                        zout.write(b, 0, len);
                    }
                    in.close();
                    zout.closeEntry();
                }
            }

            zout.close();
            zin.close();
            zfile.close();
            File oldFile = new File(zipFileName);
            oldFile.delete();
            tmpzip.renameTo(oldFile);
        } catch (IOException ex) {
            // Delete the temporary file
            try {
                zin.close();
                zout.close();
                zfile.close();
                tmpzip.delete();
            } catch (Exception ex1) {
            }
            throw new IOException(ex.getMessage());
        }
    }

    public static int checkZipLevel(int level) {
        if (level < 0 || level > 9) level = 7;
        return level;
    }

    /**
     * Add a directory's content to the zip file.
     */
    public static void addDir(String zipFileName, String dirToAdd, int level,
                              boolean recurseSubFolders, boolean replace) throws IOException {
        if (StringUtil.isEmpty(zipFileName) || StringUtil.isEmpty(dirToAdd)) {
            return;
        }
        try {
            File addedFile = new File(dirToAdd);
            if (!addedFile.isDirectory()) {
                throw new IOException("此文件不是目录");
            }

            if (!addedFile.exists()) {
                throw new IOException("此目录不存在");
            }

            // Get all the files
            TreeMap map = scanFilesInDir(dirToAdd, recurseSubFolders, null);
            Object[] files = map.values().toArray();
            String[] fileNames = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                fileNames[i] = files[i] + "";
            }

            addFiles(zipFileName, dirToAdd, fileNames, level, replace);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Add a dir to the zip file, use base dir name, both the two path name can
     * be relative or absolute path, the first parameter must be same level as
     * the base dir or be the base dir's child.
     *
     * @param recurseSubFolders - whether or not recurse to sub folders
     *                          s
     */
    private static TreeMap scanFilesInDir(String baseDir, boolean recurseSubFolders, TreeMap map)
            throws Exception {
        File base = new File(baseDir);
        if (!base.exists()) throw new IOException("目录不存在!");
        String fileName = base.getCanonicalPath();

        // Make sure the directory name ends with file separator
        if (base.isDirectory()) {
            if (!fileName.endsWith(File.separator)) {
                fileName += File.separator;
            }
        }

        if (map == null) {
            map = new TreeMap();
        }
        map.put(fileName, fileName);

        if (base.isFile()) {
            return map;
        }

        // Do not recurse subfolders' file, only one level, then add files in this directory
        if (!recurseSubFolders && base.isDirectory()) {
            File list[] = base.listFiles();
            for (int i = 0; i < list.length; i++) {
                fileName = list[i].getCanonicalPath();
                map.put(fileName, fileName);
            }
            return map;
        } else if (base.isDirectory()) {
            File list[] = base.listFiles();
            for (int i = 0; i < list.length; i++) {
                scanFilesInDir(list[i].getCanonicalPath(), true, map);
            }
        }
        return map;
    }

    // Delete all the selected entries
    public static void deleteFiles(String zipFileName, ZipEntry[] selectedEntries, int level)
            throws IOException {
        if (StringUtil.isEmpty(zipFileName)) return;
        if (selectedEntries == null || selectedEntries.length == 0) return;
        level = checkZipLevel(level);

        if (getEntrySize(zipFileName) < 1) {
            return;
        }
        boolean isDeleteAll = (selectedEntries.length == getEntrySize(zipFileName));

        HashMap map = new HashMap();
        for (int i = 0; i < selectedEntries.length; i++) {
            map.put(selectedEntries[i].getName(), selectedEntries[i]);
        }

        ZipOutputStream zout = null;
        ZipInputStream zin = null;
        File tmpzip = null;
        // Create a monitor


        try {
            // Create temporary zip file
            tmpzip = new File(zipFileName + ".tmp");
            ZipEntry ze;
            zin = new ZipInputStream(new FileInputStream(zipFileName));
            zout = new ZipOutputStream(new FileOutputStream(tmpzip));
            zout.setMethod(ZipOutputStream.DEFLATED);
            zout.setLevel(level);
            int len = 0;
            byte[] b = new byte[10240];
            while ((ze = zin.getNextEntry()) != null) {
                if (map.get(ze.getName()) != null) {
                    continue;
                }
                zout.putNextEntry(createEntry(ze));

                while ((len = zin.read(b)) != -1) {
                    zout.write(b, 0, len);
                }
                zout.closeEntry();
                zin.closeEntry();
            }
            // Check if there's no entry in the file,
            // else create an empty entry and put it into the zip file
            if (isDeleteAll) {
                ze = new ZipEntry(".");
                zout.putNextEntry(ze);
                zout.closeEntry();
            }

            zout.close();
            zin.close();
            File oldFile = new File(zipFileName);
            oldFile.delete();
            tmpzip.renameTo(oldFile);
        } catch (Exception ex) {
            // Delete the temporary file
            try {
                zin.close();
                zout.close();
                tmpzip.delete();
            } catch (Exception ex1) {
            }
            throw new IOException(ex.getMessage());
        }
    }

    public static void extractFiles(String zipFileName, ZipEntry[] selectedEntries,
                                    String extractDir, boolean usePath, boolean replace) throws IOException {
        if (StringUtil.isEmpty(zipFileName)) return;
        if (StringUtil.isEmpty(extractDir)) return;
        if (selectedEntries == null || selectedEntries.length == 0) return;

        ZipFile zfile = null;

        try {
            zfile = new ZipFile(zipFileName);
            // Make sure the directory name ends with a file separator
            if (!extractDir.endsWith(File.separator)) {
                extractDir += File.separator;
            }
            // Check if the extract target directory exists, if not, create it
            File extractDirFile = new File(extractDir);
            if (!extractDirFile.exists()) {
                extractDirFile.mkdirs();
            }


            for (int i = 0; i < selectedEntries.length; i++) {
                String entryName = selectedEntries[i].getName();


                ZipEntry entry = zfile.getEntry(entryName);
                // Check and create the directory contained in the entry
                // Create neccesary directroy
                if (usePath) {
                    String entryDir = StringUtil.getDirName(entryName);
                    if (entryDir == null) entryDir = "";
                    File tempDir = new File(extractDir + entryDir);
                    if (!tempDir.exists()) {
                        tempDir.mkdirs();
                    }
                } // User not want create directory
                else entryName = StringUtil.getFileName(entryName);
                if (!entry.isDirectory()) {
                    File outFile = new File(extractDir + entryName);
                    outFile.setLastModified(entry.getTime());
                    // If file is directory or file exists and not replace it, then jumb it
                    if (outFile.isDirectory() || (outFile.exists() && replace == false)) {
                        continue;
                    }
                    InputStream in = zfile.getInputStream(entry);
                    // Burrer size: 10KB
                    BufferedOutputStream out = new BufferedOutputStream(
                            new FileOutputStream(outFile), 10240);
                    int data;
                    while ((data = in.read()) != -1) {
                        out.write(data);
                    }
                    in.close();
                    out.close();
                }
            }
            zfile.close();
        } catch (Exception ex) {
            try {
                zfile.close();
            } catch (Exception ex1) {
            }
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * 2015-08-31 支持文件名方式解压缩
     *
     * @param zipFileName
     * @param selectedEntries
     * @param extractDir
     * @param usePath
     * @param replace
     * @throws IOException
     */
    public static void extractFiles(String zipFileName, String[] selectedEntries,
                                    String extractDir, boolean usePath, boolean replace) throws IOException {
        if (StringUtil.isEmpty(zipFileName)) return;
        if (StringUtil.isEmpty(extractDir)) return;
        if (selectedEntries == null || selectedEntries.length == 0) return;

        ZipFile zfile = null;

        try {
            zfile = new ZipFile(zipFileName);
            // Make sure the directory name ends with a file separator
            if (!extractDir.endsWith(File.separator)) {
                extractDir += File.separator;
            }
            // Check if the extract target directory exists, if not, create it
            File extractDirFile = new File(extractDir);
            if (!extractDirFile.exists()) {
                extractDirFile.mkdirs();
            }


            for (int i = 0; i < selectedEntries.length; i++) {
                String entryName = selectedEntries[i];


                ZipEntry entry = zfile.getEntry(entryName);
                // Check and create the directory contained in the entry
                // Create neccesary directroy
                if (usePath) {
                    String entryDir = StringUtil.getDirName(entryName);
                    if (entryDir == null) entryDir = "";
                    File tempDir = new File(extractDir + entryDir);
                    if (!tempDir.exists()) {
                        tempDir.mkdirs();
                    }
                } // User not want create directory
                else entryName = StringUtil.getFileName(entryName);
                if (!entry.isDirectory()) {
                    File outFile = new File(extractDir + entryName);
                    outFile.setLastModified(entry.getTime());
                    // If file is directory or file exists and not replace it, then jumb it
                    if (outFile.isDirectory() || (outFile.exists() && replace == false)) {
                        continue;
                    }
                    InputStream in = zfile.getInputStream(entry);
                    // Burrer size: 10KB
                    BufferedOutputStream out = new BufferedOutputStream(
                            new FileOutputStream(outFile), 10240);
                    int data;
                    while ((data = in.read()) != -1) {
                        out.write(data);
                    }
                    in.close();
                    out.close();
                }
            }
            zfile.close();
        } catch (Exception ex) {
            try {
                zfile.close();
            } catch (Exception ex1) {
            }
            throw new IOException(ex.getMessage());
        }
    }

    public static void nativeZipToUTF(String inputZip, String outputZip, int level) throws Exception {
        if (StringUtil.isEmpty(inputZip)) return;
        if (StringUtil.isEmpty(outputZip)) return;
        level = checkZipLevel(level);
        ZipOutputStream zout = null;
        ZipInputStream zin = null; // Why this can't get correct zip entry time, I don't know
        ZipFile zfile = null; // Used to get correct zip entry time
        File tmpzip = null;
        try {
            tmpzip = new File(outputZip + ".tmp");
            ZipEntry ze;
            zfile = new ZipFile(inputZip);
            zin = new ZipInputStream(new FileInputStream(inputZip));
            zout = new ZipOutputStream(new FileOutputStream(tmpzip));
            zout.setLevel(level);
            int len = 0;
            Enumeration entries = zfile.entries();


            byte[] b = new byte[10240];// 10KB buffer
            // Copy the older zip entries
            while ((ze = zin.getNextEntry()) != null) {

                ZipEntry fixDateEntry = (ZipEntry) entries.nextElement();
                ze.setTime(fixDateEntry.getTime());
                zout.putNextEntry(createEntry(ze));

                while ((len = zin.read(b)) != -1) {
                    zout.write(b, 0, len);
                }
                zout.closeEntry();
                zin.closeEntry();

            }
            zout.close();
            zin.close();
            zfile.close();
            File oldFile = new File(outputZip);
            oldFile.delete();
            tmpzip.renameTo(oldFile);
        } catch (IOException ex) {
            // Delete the temporary file
            try {
                zin.close();
                zout.close();
                tmpzip.delete();
                zfile.close();
            } catch (Exception ex1) {
            }
            throw ex;
        }
    }

    public static void renameEntry(String zipFileName, ZipEntry oldEntry,
                                   ZipEntry newEntry, int level) throws IOException {
        if (StringUtil.isEmpty(zipFileName)) return;
        if (oldEntry == null || newEntry == null) return;
        // Same name and same time
        if (oldEntry.getName().equalsIgnoreCase(newEntry.getName())) {
            if (oldEntry.getTime() == newEntry.getTime()) {
                return;
            }
        }

        level = checkZipLevel(level);
        ZipOutputStream zout = null;
        ZipInputStream zin = null;
        File tmpzip = null;
        try {
            // Create temporary zip file


            tmpzip = new File(zipFileName + ".tmp");
            ZipEntry ze;
            zin = new ZipInputStream(new FileInputStream(zipFileName));
            zout = new ZipOutputStream(new FileOutputStream(tmpzip));
            zout.setLevel(level);
            int len = 0;
            byte[] b = new byte[10240];// 10KB buffer
            // Copy the old zip entries
            while ((ze = zin.getNextEntry()) != null) {
                // Find the entry to update
                if (ze.getName().equals(oldEntry.getName())) {
                    ze = createNewEntry(oldEntry, newEntry);
                }
                zout.putNextEntry(createEntry(ze));
                while ((len = zin.read(b)) != -1) {
                    zout.write(b, 0, len);
                }
                zout.closeEntry();
                zin.closeEntry();
            }

            zout.close();
            zin.close();
            File oldFile = new File(zipFileName);
            oldFile.delete();
            tmpzip.renameTo(oldFile);
        } catch (IOException ex) {
            // Delete the temporary file
            try {
                zin.close();
                zout.close();
                tmpzip.delete();
            } catch (Exception ex1) {
            }
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Because sometimes found compressedSize errors when directly put old
     * entry from WinZip created files to ZipOutputStream, so change it.
     */
    private static ZipEntry createEntry(ZipEntry oldEntry) {
        if (oldEntry.getMethod() == ZipEntry.STORED) {
            // Stored entry directly put it out
            return oldEntry;
        } else if (oldEntry.getMethod() == ZipEntry.DEFLATED) {
            // Deflated entry not set size and compressedSize
            ZipEntry entry = new ZipEntry(oldEntry.getName());
            entry.setMethod(oldEntry.getMethod());
            entry.setTime(oldEntry.getTime());
            entry.setExtra(oldEntry.getExtra());
            entry.setComment(oldEntry.getComment());
            return entry;
        }
        return oldEntry;
    }

    /**
     * Because sometimes found compressedSize errors when directly put old
     * entry from WinZip created files to ZipOutputStream, so change it.
     */
    private static ZipEntry createNewEntry(ZipEntry oldEntry, ZipEntry newEntry) {
        if (oldEntry.getMethod() == ZipEntry.STORED) {
            // Stored entry directly copy all the data
            ZipEntry entry = new ZipEntry(newEntry.getName());
            entry.setMethod(oldEntry.getMethod());
            entry.setTime(oldEntry.getTime());
            entry.setExtra(oldEntry.getExtra());
            entry.setComment(oldEntry.getComment());
            entry.setCompressedSize(oldEntry.getCompressedSize());
            entry.setCrc(oldEntry.getCrc());
            entry.setSize(oldEntry.getSize());
            return entry;
        } else if (oldEntry.getMethod() == ZipEntry.DEFLATED) {
            // Deflated entry not set size and compressedSize
            ZipEntry entry = new ZipEntry(newEntry.getName());
            entry.setMethod(oldEntry.getMethod());
            entry.setTime(oldEntry.getTime());
            entry.setExtra(oldEntry.getExtra());
            entry.setComment(oldEntry.getComment());
            return entry;
        }
        return oldEntry;
    }

    // Create a self-extractor zip file with given data zip file
    public static void createSelfExtractor(String zipFileName, String dataZipFile) throws IOException {
        if (StringUtil.isEmpty(zipFileName) || StringUtil.isEmpty(dataZipFile)) return;

        // Entries in self-extractor
        String classFile = "SelfExtractor.class";
        String windowCloser = "SelfExtractor$WindowCloser.class";
        String metaInfoDir = "META-INF/";
        String metaInfoFile = "META-INF/MANIFEST.MF";
        String dataFile = "extractdata.zip";
        String manifestContent = "Manifest-Version: 1.0\n" +
                "Main-Class: SelfExtractor\n" +
                "Created-By: BeanSoft Studio";

        ZipOutputStream zout = null;
        File tmpzip = null;
        try {
            // Create new zip file
            tmpzip = new File(zipFileName);
            zout = new ZipOutputStream(new FileOutputStream(tmpzip));
            zout.setLevel(9);

            int len = 0;
            byte[] b = new byte[10240];// 10KB buffer

            // Copy self-extractor classes into file
            ZipEntry ze = new ZipEntry(classFile);
            zout.putNextEntry(ze);

            InputStream in = getResourceAsStream("/" + classFile);
            while ((len = in.read(b)) != -1) {
                zout.write(b, 0, len);
            }
            in.close();
            zout.closeEntry();

            ze = new ZipEntry(windowCloser);
            zout.putNextEntry(ze);

            in = getResourceAsStream("/" + windowCloser);
            while ((len = in.read(b)) != -1) {
                zout.write(b, 0, len);
            }
            in.close();
            zout.closeEntry();

            ze = new ZipEntry(metaInfoDir);
            zout.putNextEntry(ze);

            zout.closeEntry();

            // Write manifest.mf
            ze = new ZipEntry(metaInfoFile);
            zout.putNextEntry(ze);

            zout.write(manifestContent.getBytes());
            zout.closeEntry();

            ze = new ZipEntry(dataFile);
            zout.putNextEntry(ze);

            in = new FileInputStream(dataZipFile);
            while ((len = in.read(b)) != -1) {
                zout.write(b, 0, len);
            }
            in.close();
            zout.closeEntry();

            zout.close();
        } catch (IOException ex) {
            try {
                zout.close();
                tmpzip.delete();
            } catch (Exception ex1) {
            }
            throw ex;
        }
    }

    /**
     * Use this to avoid resource path problem, then the method can be used both
     * file path or zip/jar file.
     * ///////////////////////////////////////////////////////////////////////////
     * Warning: Use zip/jar file then resource file name is case sensitive !!!!!!!
     * Also you should not use file path like this:"./images/about.gif",
     * use "images/about.gif".
     * ///////////////////////////////////////////////////////////////////////////
     */
    private static InputStream getResourceAsStream(String path) {
        if (path != null) {
            InputStream in = ZipUtil.class.getResourceAsStream(path);
            return in;
        }
        return null;
    }


    public static void main(String[] args) throws Exception {
//        createSelfExtractor("self.jar", "extractdata.zip");
        ZipUtil.replaceAPKManifest("/Users/beansoft/Desktop/bm_tomcat_agent.war", "config content");
        FileOutputStream fout = new FileOutputStream("/Users/beansoft/Desktop/bm_tomcat_agent_edit.war");
        ZipUtil.replaceWARFileConfigToStream("/Users/beansoft/Desktop/bm_tomcat_agent.war", "config content edit", fout);
        fout.close();
    }
}