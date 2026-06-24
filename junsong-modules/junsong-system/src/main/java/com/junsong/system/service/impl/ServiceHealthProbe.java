package com.junsong.system.service.impl;

import com.junsong.system.domain.SysServiceHealth;

import java.util.List;

public interface ServiceHealthProbe
{
    List<SysServiceHealth> probeServices();
}
