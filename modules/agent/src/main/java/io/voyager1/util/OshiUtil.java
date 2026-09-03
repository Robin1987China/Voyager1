/*
 * Copyright (c) 2026 Voyager1
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.voyager1.util;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.util.List;

/**
 * Oshi 工具，"" {@code .system.oshi.OshiUtil}。
 */
public class OshiUtil {

    private static final SystemInfo SYSTEM_INFO = new SystemInfo();

    public static OperatingSystem getOs() {
        return SYSTEM_INFO.getOperatingSystem();
    }

    public static HardwareAbstractionLayer getHardware() {
        return SYSTEM_INFO.getHardware();
    }

    public static CentralProcessor getProcessor() {
        return SYSTEM_INFO.getHardware().getProcessor();
    }

    public static GlobalMemory getMemory() {
        return SYSTEM_INFO.getHardware().getMemory();
    }

    public static List<NetworkIF> getNetworkIFs() {
        return SYSTEM_INFO.getHardware().getNetworkIFs();
    }

    /**
     * 获取 CPU 使用率信息。
     *
     * @param wait 采样间隔毫秒
     * @return CpuInfo
     */
    public static CpuInfo getCpuInfo(long wait) {
        CentralProcessor processor = getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        try {
            Thread.sleep(Math.max(0, wait));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long[] ticks = processor.getSystemCpuLoadTicks();

        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long sys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];

        long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;
        if (totalCpu <= 0) {
            totalCpu = 1;
        }
        double total = totalCpu;

        CpuInfo info = new CpuInfo();
        info.setFree(round(idle * 100.0 / total));
        info.setUsed(round((user + nice + sys + iowait + irq + softirq + steal) * 100.0 / total));
        info.setSys(round(sys * 100.0 / total));
        info.setUser(round(user * 100.0 / total));
        info.setWait(round(iowait * 100.0 / total));
        info.setNice(round(nice * 100.0 / total));
        return info;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
