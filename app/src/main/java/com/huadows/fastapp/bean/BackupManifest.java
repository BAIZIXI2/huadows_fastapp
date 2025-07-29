package com.huadows.fastapp.bean;

import java.util.List;

public class BackupManifest {
    public long backupTimestamp;
    public String deviceModel;
    public int androidVersion;
    public List<ManifestAppInfo> backedUpApps;
}