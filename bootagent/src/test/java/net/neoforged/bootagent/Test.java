package net.neoforged.bootagent;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.List;

public class Test {
    public static void main(String[] args) throws Exception {
        var testmodulePathString = System.getProperty("testmodule.path");
        System.out.println("Loading testmodule from " + testmodulePathString);
        var testmodulePath = Paths.get(testmodulePathString);

        var configuration = Configuration.resolveAndBind(
                ModuleFinder.of(),
                List.of(ModuleLayer.boot().configuration()),
                ModuleFinder.of(testmodulePath),
                List.of("testmodule")
        );

        var layer = ModuleLayer.defineModulesWithOneLoader(
                configuration,
                List.of(ModuleLayer.boot()),
                ClassLoader.getSystemClassLoader()
        );
        var testmodule = layer.layer().findModule("testmodule").get();
        // Get access to the test class
        layer.addExports(testmodule, "testmodule", Test.class.getModule());

        // try without opening java.base first
        Class<?> testClass = Class.forName(testmodule, "testmodule.AccessPrivateMethod");
        try {
            testClass.getMethod("test").invoke(null);
            throw new IllegalStateException("Expected access to java.lang to fail");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof InaccessibleObjectException) {
                // This one we expected
            } else {
                throw e;
            }
        }

        // open java.base
        BootAgentInterface.addOpens(String.class.getModule(), "java.lang", testmodule);

        // Now calling this should succeed
        testClass.getMethod("test").invoke(null);
        System.out.println("SUCCESS!");
    }
}
