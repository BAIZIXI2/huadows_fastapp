// Bcore/src/main/java/top/niunaijun/blackbox/entity/pm/BStorageInfo.java
package top.niunaijun.blackbox.entity.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class BStorageInfo implements Parcelable {
    public long totalSize; // 总大小
    public long appSize;   // 应用大小 (apk)
    public long dataSize;  // 数据大小
    public long cacheSize; // 缓存大小

    public BStorageInfo() {}

    protected BStorageInfo(Parcel in) {
        totalSize = in.readLong();
        appSize = in.readLong();
        dataSize = in.readLong();
        cacheSize = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(totalSize);
        dest.writeLong(appSize);
        dest.writeLong(dataSize);
        dest.writeLong(cacheSize);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BStorageInfo> CREATOR = new Creator<BStorageInfo>() {
        @Override
        public BStorageInfo createFromParcel(Parcel in) {
            return new BStorageInfo(in);
        }

        @Override
        public BStorageInfo[] newArray(int size) {
            return new BStorageInfo[size];
        }
    };

    @Override
    public String toString() {
        return "BStorageInfo{" +
                "totalSize=" + totalSize +
                ", appSize=" + appSize +
                ", dataSize=" + dataSize +
                ", cacheSize=" + cacheSize +
                '}';
    }
}