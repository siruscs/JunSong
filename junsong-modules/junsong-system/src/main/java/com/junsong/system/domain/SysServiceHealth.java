package com.junsong.system.domain;

public class SysServiceHealth
{
    private String name;
    private String code;
    private String url;
    private String status;
    private String message;
    private Long responseTime;

    public static SysServiceHealth up(String name, String code, String url, Long responseTime)
    {
        SysServiceHealth health = new SysServiceHealth();
        health.setName(name);
        health.setCode(code);
        health.setUrl(url);
        health.setStatus("UP");
        health.setMessage("运行正常");
        health.setResponseTime(responseTime);
        return health;
    }

    public static SysServiceHealth down(String name, String code, String url, String message)
    {
        SysServiceHealth health = new SysServiceHealth();
        health.setName(name);
        health.setCode(code);
        health.setUrl(url);
        health.setStatus("DOWN");
        health.setMessage(message);
        health.setResponseTime(0L);
        return health;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Long getResponseTime()
    {
        return responseTime;
    }

    public void setResponseTime(Long responseTime)
    {
        this.responseTime = responseTime;
    }
}
