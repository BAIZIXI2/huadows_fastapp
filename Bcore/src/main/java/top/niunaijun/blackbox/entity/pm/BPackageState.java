// Bcore/src/main/java/top/niunaijun/blackbox/entity/pm/BPackageState.java

package top.niunaijun.blackbox.entity.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class BPackageState implements Parcelable {
    public String packageName;
    public String versionName;
    public long versionCode;
    public long totalSize; // 总占用 = 应用 + 数据
    public long appSize;   // 应用大小 (apk)
    public long dataSize;  // 数据大小
    public long cacheSize; // 缓存大小

    public BPackageState() {}

    protected BPackageState(Parcel in) {
        packageName = in.readString();
        versionName = in.readString();
        versionCode = in.readLong();
        totalSize = in.readLong();
        appSize = in.readLong();
        dataSize = in.readLong();
        cacheSize = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeString(versionName);
        dest.writeLong(versionCode);
        dest.writeLong(totalSize);
        dest.writeLong(appSize);
        dest.writeLong(dataSize);
        dest.writeLong(cacheSize);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BPackageState> CREATOR = new Creator<BPackageState>() {
        @Override
        public BPackageState createFromParcel(Parcel in) {
            return new BPackageState(in);
        }

        @Override
        public BPackageState[] newArray(int size) {
            return new BPackageState[size];
        }
    };
}