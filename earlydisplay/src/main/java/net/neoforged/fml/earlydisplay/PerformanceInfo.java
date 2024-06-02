/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.earlydisplay;

public class PerformanceInfo {
    final PerformanceInfoSource performanceInfoSource;
    float memory;
    private String text = "";

    public PerformanceInfo(PerformanceInfoSource performanceInfoSource) {
        this.performanceInfoSource = performanceInfoSource;
    }

    void update() {
        if (performanceInfoSource == null) {
            return;
        }
        var heapUsed = performanceInfoSource.memoryUsed();
        var heapMax = performanceInfoSource.memoryMax();
        var offHeapUsed = performanceInfoSource.offHeapUsed();

        memory = (float) heapUsed / heapMax;
        var cpuLoad = performanceInfoSource.cpuUsagePercentage();
        var cpuText = String.format("CPU: %.1f%%", cpuLoad * 100f);

        text = String.format("Heap: %d/%d MB (%.1f%%) OffHeap: %d MB  %s", heapUsed >> 20, heapMax >> 20, memory * 100.0, offHeapUsed >> 20, cpuText);
    }

    String text() {
        return text;
    }

    float memory() {
        return memory;
    }

    boolean isDisabled() {
        return performanceInfoSource == null;
    }
}
