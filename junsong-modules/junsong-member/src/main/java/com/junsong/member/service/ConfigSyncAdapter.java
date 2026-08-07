package com.junsong.member.service;

import java.util.Map;

public interface ConfigSyncAdapter
{
    String type();
    int create(Map<String, Object> source, Long targetDeptId, String operator);
    int overwrite(Map<String, Object> source, Map<String, Object> target, Long targetDeptId, String operator);
}
