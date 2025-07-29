package com.huadows.fastapp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;

public class CryptoUtils {

    /**
     * 计算文件的MD5哈希值
     * @param file 要计算的文件
     * @return 文件的MD5字符串，如果文件不存在或发生错误则返回 null
     */
    public static String calculateFileMd5(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] md5sum = digest.digest();
            return bytesToHex(md5sum);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 计算目录的MD5哈希值。
     * 该哈希值基于目录下所有文件的内容和相对路径，确保结构和内容都一致。
     * @param directory 要计算的目录
     * @return 目录的MD5字符串，如果目录为空或不存在则返回 null
     */
    public static String calculateDirectoryMd5(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            updateDigestForDirectory(directory, directory.getAbsolutePath(), digest);
            byte[] md5sum = digest.digest();
            return bytesToHex(md5sum);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void updateDigestForDirectory(File directory, String rootPath, MessageDigest digest) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        // 对文件进行排序，确保每次计算结果一致
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            // 将文件的相对路径加入摘要
            String relativePath = file.getAbsolutePath().substring(rootPath.length());
            digest.update(relativePath.getBytes());

            if (file.isDirectory()) {
                updateDigestForDirectory(file, rootPath, digest);
            } else {
                // 将文件内容加入摘要
                try (InputStream in = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        digest.update(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}