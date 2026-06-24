package com.junsong.system.domain;

import java.util.ArrayList;
import java.util.List;

public class SysDashboardHealth
{
    private double cpuUsage;
    private double memoryUsage;
    private double diskUsage;
    private int availableProcessors;
    private long totalMemoryGb;
    private String hostName;
    private String osName;
    private int overallScore;
    private String level;
    private int serviceCount;
    private int upServiceCount;
    private int downServiceCount;
    private long updateTime;
    private List<SysServiceHealth> services = new ArrayList<>();

    public double getCpuUsage()
    {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage)
    {
        this.cpuUsage = cpuUsage;
    }

    public double getMemoryUsage()
    {
        return memoryUsage;
    }

    public void setMemoryUsage(double memoryUsage)
    {
        this.memoryUsage = memoryUsage;
    }

    public double getDiskUsage()
    {
        return diskUsage;
    }

    public void setDiskUsage(double diskUsage)
    {
        this.diskUsage = diskUsage;
    }

    public int getAvailableProcessors()
    {
        return availableProcessors;
    }

    public void setAvailableProcessors(int availableProcessors)
    {
        this.availableProcessors = availableProcessors;
    }

    public long getTotalMemoryGb()
    {
        return totalMemoryGb;
    }

    public void setTotalMemoryGb(long totalMemoryGb)
    {
        this.totalMemoryGb = totalMemoryGb;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

    public String getOsName()
    {
        return osName;
    }

    public void setOsName(String osName)
    {
        this.osName = osName;
    }

    public int getOverallScore()
    {
        return overallScore;
    }

    public void setOverallScore(int overallScore)
    {
        this.overallScore = overallScore;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public int getServiceCount()
    {
        return serviceCount;
    }

    public void setServiceCount(int serviceCount)
    {
        this.serviceCount = serviceCount;
    }

    public int getUpServiceCount()
    {
        return upServiceCount;
    }

    public void setUpServiceCount(int upServiceCount)
    {
        this.upServiceCount = upServiceCount;
    }

    public int getDownServiceCount()
    {
        return downServiceCount;
    }

    public void setDownServiceCount(int downServiceCount)
    {
        this.downServiceCount = downServiceCount;
    }

    public long getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(long updateTime)
    {
        this.updateTime = updateTime;
    }

    public List<SysServiceHealth> getServices()
    {
        return services;
    }

    public void setServices(List<SysServiceHealth> services)
    {
        this.services = services;
    }

    public static class SystemMetrics
    {
        private double cpuUsage;
        private double memoryUsage;
        private double diskUsage;
        private int availableProcessors;
        private long totalMemoryGb;
        private String hostName;
        private String osName;

        public double getCpuUsage()
        {
            return cpuUsage;
        }

        public void setCpuUsage(double cpuUsage)
        {
            this.cpuUsage = cpuUsage;
        }

        public double getMemoryUsage()
        {
            return memoryUsage;
        }

        public void setMemoryUsage(double memoryUsage)
        {
            this.memoryUsage = memoryUsage;
        }

        public double getDiskUsage()
        {
            return diskUsage;
        }

        public void setDiskUsage(double diskUsage)
        {
            this.diskUsage = diskUsage;
        }

        public int getAvailableProcessors()
        {
            return availableProcessors;
        }

        public void setAvailableProcessors(int availableProcessors)
        {
            this.availableProcessors = availableProcessors;
        }

        public long getTotalMemoryGb()
        {
            return totalMemoryGb;
        }

        public void setTotalMemoryGb(long totalMemoryGb)
        {
            this.totalMemoryGb = totalMemoryGb;
        }

        public String getHostName()
        {
            return hostName;
        }

        public void setHostName(String hostName)
        {
            this.hostName = hostName;
        }

        public String getOsName()
        {
            return osName;
        }

        public void setOsName(String osName)
        {
            this.osName = osName;
        }
    }
}
