package com.huadows.fastapp.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

public class ZipUtils {
    private static final int BUFFER_SIZE = 8192;

    /**
     * 压缩一个目录到ZIP文件
     * @param sourceDirectory 要压缩的源目录
     * @param destZipFile 目标ZIP文件
     * @throws IOException 压缩异常
     */
    public static void zipDirectory(File sourceDirectory, File destZipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZipFile)))) {
            addDirectoryToZip(sourceDirectory, sourceDirectory.getAbsolutePath(), zos);
        }
    }

    private static void addDirectoryToZip(File file, String basePath, ZipOutputStream zos) throws IOException {
        String entryPath = file.getAbsolutePath().substring(basePath.length());
        if (entryPath.startsWith(File.separator)) {
            entryPath = entryPath.substring(1);
        }
        entryPath = entryPath.replace('\\', '/'); // 统一使用'/'作为zip内的路径分隔符

        if (file.isDirectory()) {
            // 如果是目录（即使是空的），也为其创建一个条目。目录条目必须以'/'结尾。
            if (!entryPath.isEmpty()) {
                if (!entryPath.endsWith("/")) {
                    entryPath += "/";
                }
                ZipEntry dirEntry = new ZipEntry(entryPath);
                zos.putNextEntry(dirEntry);
                zos.closeEntry();
            }

            // 递归处理子文件和子目录
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addDirectoryToZip(child, basePath, zos);
                }
            }
        } else {
            // 如果是文件，创建文件条目并写入内容
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {
                ZipEntry fileEntry = new ZipEntry(entryPath);
                zos.putNextEntry(fileEntry);
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = bis.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    zos.write(buffer, 0, read);
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * 解压ZIP文件到指定目录
     * @param zipFile 要解压的ZIP文件
     * @param destDirectory 目标目录
     * @throws IOException 解压异常
     */
    public static void unzip(File zipFile, File destDirectory) throws IOException {
        if (!destDirectory.exists()) {
            destDirectory.mkdirs();
        }
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry zipEntry;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((zipEntry = zis.getNextEntry()) != null) {
                File newFile = newFile(destDirectory, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(newFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * 从ZIP文件中提取单个文件为字符串
     * @param zipFile zip文件
     * @param fileNameInZip 在zip中的文件名
     * @return 文件内容字符串，或 null
     * @throws IOException IO异常
     */
    public static String extractFileToString(File zipFile, String fileNameInZip) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (fileNameInZip.equals(zipEntry.getName())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    return baos.toString("UTF-8");
                }
                zis.closeEntry();
            }
        }
        return null;
    }

    // 防止Zip Slip漏洞
    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }
}