package net.neoforged.fml.earlydisplay;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.earlywindow.ImmediateWindowProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Callback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lwjgl.glfw.GLFW.glfwInit;

class DisplayWindowTest {
    @TempDir
    Path gameDir;

    DisplayWindow window;
    private Callback callback;

    @BeforeEach
    void setUp() {
        glfwInit();

        FMLPaths.loadAbsolutePaths(gameDir);
        window = new DisplayWindow();
        window.setDebugContext(true);
        window.performanceInfoSource = new FixedPerformanceInfo(
                1000L,
                2000L,
                500L,
                25
        );
    }

    @AfterEach
    void tearDown() {
        if (callback != null) {
            callback.free();
        }
        window.close();
        GLFW.glfwTerminate();
    }

    @Test
    void testDisplayWindowIsRegisteredAsService() {
        var provider = ServiceLoader.load(ImmediateWindowProvider.class).stream().map(ServiceLoader.Provider::get).toList();
        assertThat(provider).hasSize(1);
        assertThat(provider.getFirst()).isInstanceOf(DisplayWindow.class);
    }

    @Test
    void testCanCloseWithoutInitializing() {
        window.close();
    }

    @Test
    void testShowsWindow() throws Exception {
        var processEvents = window.initialize(new String[]{
                "--width", "800",
                "--height", "600"
        });

        for (var i = 0; i < 100; i++) {
            processEvents.run();
            Thread.sleep(10L);
        }
        var screenshot = window.takeScreenshot();
        Files.write(Paths.get("test.png"), screenshot);
    }
}
