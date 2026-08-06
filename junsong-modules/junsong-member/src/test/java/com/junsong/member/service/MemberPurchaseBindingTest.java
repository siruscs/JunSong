package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.junsong.member.domain.MemMember;
import com.junsong.member.domain.MemPurchaseOrder;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.mapper.MemPurchaseMapper;
import com.junsong.member.service.impl.MemberPurchaseServiceImpl;

class MemberPurchaseBindingTest
{
    @Test
    void bindWalkInPurchaseToSameDepartmentMemberWithoutRewardingAgain()
    {
        MemPurchaseMapper purchaseMapper = Mockito.mock(MemPurchaseMapper.class);
        MemMemberMapper memberMapper = Mockito.mock(MemMemberMapper.class);
        IMemberIdentityPolicyService identityPolicyService = Mockito.mock(IMemberIdentityPolicyService.class);
        IMemberCampaignPolicyService policyService = Mockito.mock(IMemberCampaignPolicyService.class);
        IMemberGrowthService growthService = Mockito.mock(IMemberGrowthService.class);
        MemberPurchaseServiceImpl service = new MemberPurchaseServiceImpl(
                purchaseMapper, memberMapper, identityPolicyService, policyService, growthService);

        MemPurchaseOrder query = new MemPurchaseOrder();
        query.setPurchaseId(7L);
        query.setTenantId(1L);
        query.setDeptId(10L);
        MemPurchaseOrder order = new MemPurchaseOrder();
        order.setPurchaseId(7L);
        order.setTenantId(1L);
        order.setDeptId(10L);
        order.setCustomerType("WALK_IN");
        order.setOrderStatus("1");
        when(purchaseMapper.selectPurchaseOrderForUpdate(Mockito.any(MemPurchaseOrder.class))).thenReturn(order);

        MemMember member = new MemMember();
        member.setMemberId(88L);
        member.setDeptId(10L);
        member.setMemberName("张三");
        member.setPhone("13800000000");
        member.setStatus("0");
        when(memberMapper.selectMemMemberByMemberId(88L)).thenReturn(member);
        when(purchaseMapper.bindPurchaseMember(Mockito.any(MemPurchaseOrder.class))).thenReturn(1);

        assertEquals(1, service.bindPurchaseMember(7L, 1L, 10L, 88L, "admin"));
        verify(purchaseMapper).bindPurchaseMember(Mockito.argThat(saved ->
                "MEMBER".equals(saved.getCustomerType())
                        && Long.valueOf(88L).equals(saved.getMemberId())
                        && "张三".equals(saved.getCustomerName())));
        Mockito.verifyNoInteractions(growthService);
    }
}
