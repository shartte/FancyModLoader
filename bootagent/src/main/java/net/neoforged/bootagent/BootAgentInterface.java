package net.neoforged.bootagent;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;

public final class BootAgentInterface {
    static Instrumentation instrumentation;

    private BootAgentInterface() {
    }

    public static void addOpens(Module module, String pn, Module to) {
        if (instrumentation == null) {
            throw new IllegalStateException("The JVM was started without the BootAgent. Please make sure that the -javaagent parameter was not removed.");
        }

        instrumentation.redefineModule(
                module,
                Set.of(),
                Map.of(),
                Map.of(pn, Set.of(to)),
                Set.of(),
                Map.of()
        );
    }
}
