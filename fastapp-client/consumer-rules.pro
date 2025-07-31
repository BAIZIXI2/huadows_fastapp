# =========== FastApp Client Library Keep Rules ===========

# 1. 保持所有数据实体（Bean）类不被混淆。
#    这是为了确保 Gson 能够正确地进行 JSON 序列化和反序列化。
#    -keep: 保持类和成员不被移除或重命名。
#    com.huadows.fastapp.client.bean.**: 匹配 bean 包下的所有类。
#    { *; }: 匹配类中的所有字段和方法。
-keep class com.huadows.fastapp.client.bean.** { *; }

# 2. 保持公共 API 类和其公共成员不被混淆。
#    这确保了库的使用者可以稳定地调用其公共方法。
-keep public class com.huadows.fastapp.client.FastAppClient {
    public *;
}
-keep public class com.huadows.fastapp.client.FastAppClient$Builder {
    public *;
}

# 3. 保持回调接口不被混淆。
#    这确保了回调机制的正常工作。
-keep public interface com.huadows.fastapp.client.ApiCallback { *; }
-keep public interface com.huadows.fastapp.client.ConnectionCallback { *; }

# 4. 保持 AIDL 生成的接口不被混淆。
#    这对于跨进程 Binder 通信至关重要。
-keep interface com.huadows.fastapp.auth.IAuthService { *; }

# =========== Gson & TypeToken Keep Rules ===========

# 为了防止 Gson 因为泛型和类型擦除问题在运行时崩溃，
# 我们需要保留所有继承自 TypeToken 的类。
# 这对于 new TypeToken<List<SomeBean>>(){} 这样的写法至关重要。
-keep class * extends com.google.gson.reflect.TypeToken
# ======================= End of Rules =======================