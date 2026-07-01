package com.junsong.system.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.core.constant.UserConstants;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.redis.service.RedisService;
import com.junsong.system.domain.SysPost;
import com.junsong.system.mapper.SysPostMapper;
import com.junsong.system.mapper.SysUserPostMapper;
import com.junsong.system.service.ISysPostService;

/**
 * 岗位信息 服务层处理
 * 
 * @author junsong
 */
@Service
public class SysPostServiceImpl implements ISysPostService
{
    @Autowired
    private SysPostMapper postMapper;

    @Autowired
    private SysUserPostMapper userPostMapper;

    @Autowired
    private RedisService redisService;

    private static final String POST_ALL_CACHE_KEY = CacheConstants.SYS_POST_KEY + "all";
    private static final long POST_CACHE_TTL_MINUTES = 60;

    /**
     * 查询岗位信息集合
     * 
     * @param post 岗位信息
     * @return 岗位信息集合
     */
    @Override
    public List<SysPost> selectPostList(SysPost post)
    {
        return postMapper.selectPostList(post);
    }

    /**
     * 查询所有岗位
     * 
     * @return 岗位列表
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<SysPost> selectPostAll()
    {
        List<SysPost> cached = redisService.getCacheObject(POST_ALL_CACHE_KEY);
        if (cached != null)
        {
            return cached;
        }
        List<SysPost> posts = postMapper.selectPostAll();
        redisService.setCacheObject(POST_ALL_CACHE_KEY, posts, POST_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return posts;
    }

    /**
     * 通过岗位ID查询岗位信息
     * 
     * @param postId 岗位ID
     * @return 角色对象信息
     */
    @Override
    public SysPost selectPostById(Long postId)
    {
        return postMapper.selectPostById(postId);
    }

    /**
     * 根据用户ID获取岗位选择框列表
     * 
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    @Override
    public List<Long> selectPostListByUserId(Long userId)
    {
        return postMapper.selectPostListByUserId(userId);
    }

    /**
     * 校验岗位名称是否唯一
     * 
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostNameUnique(SysPost post)
    {
        Long postId = StringUtils.isNull(post.getPostId()) ? -1L : post.getPostId();
        SysPost info = postMapper.checkPostNameUnique(post.getPostName());
        if (StringUtils.isNotNull(info) && info.getPostId().longValue() != postId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验岗位编码是否唯一
     * 
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostCodeUnique(SysPost post)
    {
        Long postId = StringUtils.isNull(post.getPostId()) ? -1L : post.getPostId();
        SysPost info = postMapper.checkPostCodeUnique(post.getPostCode());
        if (StringUtils.isNotNull(info) && info.getPostId().longValue() != postId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 通过岗位ID查询岗位使用数量
     * 
     * @param postId 岗位ID
     * @return 结果
     */
    @Override
    public int countUserPostById(Long postId)
    {
        return userPostMapper.countUserPostById(postId);
    }

    /**
     * 删除岗位信息
     * 
     * @param postId 岗位ID
     * @return 结果
     */
    @Override
    public int deletePostById(Long postId)
    {
        return postMapper.deletePostById(postId);
    }

    /**
     * 批量删除岗位信息
     * 
     * @param postIds 需要删除的岗位ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePostByIds(Long[] postIds)
    {
        for (Long postId : postIds)
        {
            SysPost post = selectPostById(postId);
            if (countUserPostById(postId) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", post.getPostName()));
            }
        }
        int result = postMapper.deletePostByIds(postIds);
        redisService.deleteObject(POST_ALL_CACHE_KEY);
        return result;
    }

    /**
     * 新增保存岗位信息
     * 
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public int insertPost(SysPost post)
    {
        int result = postMapper.insertPost(post);
        redisService.deleteObject(POST_ALL_CACHE_KEY);
        return result;
    }

    /**
     * 修改保存岗位信息
     * 
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public int updatePost(SysPost post)
    {
        int result = postMapper.updatePost(post);
        redisService.deleteObject(POST_ALL_CACHE_KEY);
        return result;
    }
}
