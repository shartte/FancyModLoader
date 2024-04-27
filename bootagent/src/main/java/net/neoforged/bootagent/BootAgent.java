package net.neoforged.bootagent;

import java.lang.instrument.Instrumentation;

/**
 * See <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.instrument/java/lang/instrument/package-summary.html">java.lang.instrumentation</a>
 */
class BootAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        BootAgentInterface.instrumentation = inst;
    }
}
