package com.junsong.member.mapper;

import java.util.List;
import com.junsong.member.domain.SysMpModuleSort;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysMpModuleSortMapper {

    /** 查询所有排序配置，按 group_name + sort_order + id 升序返回。 */
    List<SysMpModuleSort> selectAll();

    /** 按模块 key 查询；查不到返回 null。 */
    SysMpModuleSort selectByModuleKey(@Param("moduleKey") String moduleKey);

    /** 返回所有已配置排序值的 module_key -> sort_order map 的值列表。 */
    List<SysMpModuleSort> selectList();

    /** 新增一条排序记录。 */
    int insert(SysMpModuleSort record);

    /** 根据 id 更新。 */
    int updateById(SysMpModuleSort record);

    /** 根据 module_key 更新 sort_order/group_name。 */
    int updateByModuleKey(SysMpModuleSort record);

    /** 清空所有排序配置（整体重排前使用）。 */
    int deleteAll();
}
