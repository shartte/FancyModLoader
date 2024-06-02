package net.neoforged.fml.earlydisplay;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public interface PerformanceInfoSource {
    long memoryUsed();

    long memoryMax();

    long offHeapUsed();

    int cpuUsagePercentage();
}

class JmxPerformanceInfoSource implements PerformanceInfoSource {
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;

    JmxPerformanceInfoSource() {
        osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        memoryBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public long memoryUsed() {
        return memoryBean.getHeapMemoryUsage().getUsed();
    }

    @Override
    public long memoryMax() {
        return memoryBean.getHeapMemoryUsage().getMax();
    }

    @Override
    public long offHeapUsed() {
        return memoryBean.getNonHeapMemoryUsage().getUsed();
    }

    @Override
    public int cpuUsagePercentage() {
        var cpuLoad = osBean.getProcessCpuLoad();
        if (cpuLoad == -1) {
            return (int) Math.round(osBean.getCpuLoad() * 100.0);
        } else {
            return (int) Math.round(cpuLoad * 100.0);
        }
    }
}

record FixedPerformanceInfo(
        long memoryUsed,
        long memoryMax,
        long offHeapUsed,
        int cpuUsagePercentage
) implements PerformanceInfoSource {
}
