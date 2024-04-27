package testmodule;

public class AccessPrivateMethod {
    public static void test() throws Exception {
        // Grab a private method
        var method = Module.class.getDeclaredMethod("implAddOpens", String.class);
        method.setAccessible(true);
        method.invoke(String.class.getModule(), "java.lang");
    }
}
