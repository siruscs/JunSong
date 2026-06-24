package com.junsong.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.domain.MemMember;
import com.junsong.member.service.IMemMemberService;
import com.junsong.common.datascope.annotation.DataScope;
import com.junsong.member.util.PinyinUtils;
import org.springframework.util.StringUtils;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.DateUtils;

/**
 * 会员信息Service业务层处理
 */
@Service
public class MemMemberServiceImpl implements IMemMemberService {

    @Autowired
    private MemMemberMapper memMemberMapper;

    /**
     * 查询会员信息
     *
     * @param id 会员信息ID
     * @return 会员信息
     */
    @Override
    @DataScope(deptAlias = "mem_member")
    public MemMember selectMemMemberById(Long id) {
        return memMemberMapper.selectMemMemberByMemberId(id);
    }

    /**
     * 查询会员信息列表
     *
     * @param memMember 会员信息
     * @return 会员信息
     */
    @Override
    @DataScope(deptAlias = "mem_member")
    public List<MemMember> selectMemMemberList(MemMember memMember) {
        return memMemberMapper.selectMemMemberList(memMember);
    }

    /**
     * 新增会员信息
     *
     * @param memMember 会员信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMemMember(MemMember memMember) {
        return memMemberMapper.insertMemMember(memMember);
    }

    /**
     * 修改会员信息
     *
     * @param memMember 会员信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMemMember(MemMember memMember) {
        return memMemberMapper.updateMemMember(memMember);
    }

    /**
     * 删除会员信息
     *
     * @param id 会员信息ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemMemberById(Long id) {
        return memMemberMapper.deleteMemMemberByMemberId(id);
    }

    /**
     * 批量删除会员信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemMemberByIds(Long[] ids) {
        return memMemberMapper.deleteMemMemberByMemberIds(ids);
    }

    /**
     * 校验会员编号是否唯一
     *
     * @param memMember 会员信息
     * @return 结果
     */
    @Override
    public boolean checkMemMemberNoUnique(MemMember memMember) {
        int count = memMemberMapper.checkMemberNoUnique(memMember);
        return count == 0;
    }
    
    /**
     * 生成会员编号
     *
     * @param deptId 部门ID
     * @return 会员编号
     */
    @Override
    public String generateMemberNo(Long deptId) {
        String deptName = memMemberMapper.selectDeptNameById(deptId);
        if (!StringUtils.hasText(deptName)) {
            deptName = "默认";
        }
        
        String prefix = PinyinUtils.getFirstTwoLetters(deptName);
        String memberNo = memMemberMapper.selectNextMemberNo(prefix);
        
        if (!StringUtils.hasText(memberNo)) {
            memberNo = prefix + "00001";
        }
        
        return memberNo;
    }

    /**
     * 根据会员编号查询会员（仅限当前部门）
     */
    @Override
    public MemMember selectMemMemberByNo(String memberNo, Long deptId) {
        return memMemberMapper.selectMemMemberByNoAndDept(memberNo, deptId);
    }

    /**
     * 导入会员信息
     */
    @Override
    @Transactional
    public String importMember(List<MemMember> memberList, boolean updateSupport, String operName, Long deptId) {
        if (memberList == null || memberList.isEmpty()) {
            throw new ServiceException("导入会员数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        int skippedNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        StringBuilder skippedMsg = new StringBuilder();
        for (MemMember member : memberList) {
            String originalMemberNo = member.getMemberNo();
            // 会员姓名为空时跳过该条数据，不计入成功也不计入失败
            String memberName = member.getMemberName();
            if (!StringUtils.hasText(memberName)) {
                skippedNum++;
                skippedMsg.append("<br/>" + skippedNum + "、会员编号 "
                        + (originalMemberNo != null ? originalMemberNo : "(空)")
                        + " 已跳过：会员姓名为空");
                continue;
            }
            try {
                // 验证会员卡类型
                if (member.getCardType() == null || member.getCardType().isEmpty()) {
                    throw new ServiceException("会员卡类型不能为空，可选值：experience(体验卡)、formal(正式会员卡)、star1~star5(一星~五星会员)");
                }
                String cardType = member.getCardType().trim();
                // 支持文字值转换为代码值
                cardType = convertCardType(cardType);
                if (!isValidCardType(cardType)) {
                    throw new ServiceException("会员卡类型无效，可选值：experience(体验卡)、formal(正式会员卡)、star1~star5(一星~五星会员)");
                }
                member.setCardType(cardType);

                // 按系统规则规范化会员编号：截取数字 + 当前部门拼音前缀
                member.setMemberNo(normalizeImportedMemberNo(deptId, originalMemberNo));

                // 验证会员编号是否唯一
                MemMember existMember = memMemberMapper.selectMemMemberByNoAndDept(member.getMemberNo(), deptId);
                if (existMember == null) {
                    // 新增会员
                    member.setDeptId(deptId);
                    member.setCreateBy(operName);
                    member.setStatus("0");
                    if (member.getTotalPoints() == null) {
                        member.setTotalPoints(java.math.BigDecimal.ZERO);
                    }
                    if (member.getAvailablePoints() == null) {
                        member.setAvailablePoints(java.math.BigDecimal.ZERO);
                    }
                    memMemberMapper.insertMemMember(member);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、会员编号 " + member.getMemberNo() + " 导入成功");
                } else if (updateSupport) {
                    // 更新会员
                    member.setMemberId(existMember.getMemberId());
                    member.setDeptId(deptId);
                    member.setUpdateBy(operName);
                    memMemberMapper.updateMemMember(member);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、会员编号 " + member.getMemberNo() + " 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、会员编号 " + member.getMemberNo() + " 已存在");
                }
            } catch (ServiceException se) {
                failureNum++;
                String displayNo = member.getMemberNo() != null ? member.getMemberNo() : originalMemberNo;
                String msg = "<br/>" + failureNum + "、会员编号 " + displayNo + " 导入失败：" + se.getMessage();
                failureMsg.append(msg);
            } catch (Exception e) {
                failureNum++;
                String displayNo = member.getMemberNo() != null ? member.getMemberNo() : originalMemberNo;
                String msg = "<br/>" + failureNum + "、会员编号 " + displayNo + " 导入失败：" + e.getMessage();
                failureMsg.append(msg);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            if (skippedNum > 0) {
                failureMsg.append("<br/>另跳过 " + skippedNum + " 条会员姓名为空的数据：").append(skippedMsg);
            }
            throw new ServiceException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
            if (skippedNum > 0) {
                successMsg.append("<br/>另跳过 " + skippedNum + " 条会员姓名为空的数据：").append(skippedMsg);
            }
        }
        return successMsg.toString();
    }

    /**
     * 按系统规则规范化导入的会员编号：截取原编号中的数字，再用当前部门拼音前缀和 5 位补零生成。
     * 若候选编号已被占用或原编号为空，则退化为按部门顺延的下一个编号。
     */
    private String normalizeImportedMemberNo(Long deptId, String rawMemberNo) {
        Integer digits = extractMemberNoDigits(rawMemberNo);
        if (digits == null) {
            return generateMemberNo(deptId);
        }

        String deptName = memMemberMapper.selectDeptNameById(deptId);
        if (!StringUtils.hasText(deptName)) {
            deptName = "默认";
        }
        String prefix = PinyinUtils.getFirstTwoLetters(deptName);
        String candidate = prefix + String.format("%05d", digits);

        MemMember probe = new MemMember();
        probe.setMemberNo(candidate);
        if (memMemberMapper.checkMemberNoUnique(probe) > 0) {
            return generateMemberNo(deptId);
        }
        return candidate;
    }

    /**
     * 截取会员编号中的数字部分；若没有数字则返回 null。
     */
    private Integer extractMemberNoDigits(String rawMemberNo) {
        if (rawMemberNo == null) {
            return null;
        }
        StringBuilder digitsBuilder = new StringBuilder();
        for (int i = 0; i < rawMemberNo.length(); i++) {
            char ch = rawMemberNo.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digitsBuilder.append(ch);
            }
        }
        if (digitsBuilder.length() == 0) {
            return null;
        }
        try {
            int value = Integer.parseInt(digitsBuilder.toString());
            return value <= 0 ? null : value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 转换会员卡类型（文字值转换为代码值）
     */
    private String convertCardType(String cardType) {
        if (cardType == null) {
            return null;
        }
        // 如果已经是有效的代码值，直接返回
        if (isValidCardType(cardType)) {
            return cardType;
        }
        // 文字值映射
        java.util.Map<String, String> cardTypeMap = new java.util.HashMap<>();
        cardTypeMap.put("体验卡", "experience");
        cardTypeMap.put("正式会员卡", "formal");
        cardTypeMap.put("一星会员", "star1");
        cardTypeMap.put("二星会员", "star2");
        cardTypeMap.put("三星会员", "star3");
        cardTypeMap.put("四星会员", "star4");
        cardTypeMap.put("五星会员", "star5");
        // 支持简写
        cardTypeMap.put("体验", "experience");
        cardTypeMap.put("正式", "formal");
        cardTypeMap.put("一星", "star1");
        cardTypeMap.put("二星", "star2");
        cardTypeMap.put("三星", "star3");
        cardTypeMap.put("四星", "star4");
        cardTypeMap.put("五星", "star5");

        String trimmed = cardType.trim();
        String normalized = trimmed.toLowerCase();
        for (java.util.Map.Entry<String, String> entry : cardTypeMap.entrySet()) {
            if (entry.getKey().equals(trimmed) ||
                entry.getKey().equalsIgnoreCase(trimmed) ||
                entry.getKey().toLowerCase().equals(normalized)) {
                return entry.getValue();
            }
        }
        // 无法转换，返回原值
        return cardType;
    }

    /**
     * 检查会员卡类型是否有效
     */
    private boolean isValidCardType(String cardType) {
        if (cardType == null) {
            return false;
        }
        return "experience".equals(cardType) || "formal".equals(cardType) ||
               "star1".equals(cardType) || "star2".equals(cardType) ||
               "star3".equals(cardType) || "star4".equals(cardType) ||
               "star5".equals(cardType);
    }
}
