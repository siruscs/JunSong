package com.junsong.member.controller;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemberSegmentController 单测：脱敏、分层条件、建议动作、权限解析。
 * 使用手写 fake（不使用 Mockito）以避免 JDK 26+ 兼容性问题。
 */
class MemberSegmentControllerTest {

    private MemberSegmentController controller;
    private FakeRemoteUserService fakeRemoteUserService;

    @BeforeEach
    void setUp() throws Exception {
        controller = new MemberSegmentController();
        fakeRemoteUserService = new FakeRemoteUserService();
        setField(controller, "remoteUserService", fakeRemoteUserService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    // ==================== maskPhone ====================

    @Test
    void maskPhone_11DigitsMasksMiddle4() {
        assertEquals("138****1234", controller.maskPhone("13812341234"));
    }

    @Test
    void maskPhone_nullReturnsEmpty() {
        assertEquals("", controller.maskPhone(null));
    }

    @Test
    void maskPhone_emptyReturnsEmpty() {
        assertEquals("", controller.maskPhone(""));
        assertEquals("", controller.maskPhone("   "));
    }

    @Test
    void maskPhone_shortNumberReturnsEmpty() {
        assertEquals("", controller.maskPhone("12345"));
    }

    @Test
    void maskPhone_longerThan11MasksMiddle() {
        String masked = controller.maskPhone("138123456789");
        assertTrue(masked.startsWith("138"));
        assertTrue(masked.endsWith("6789"));
        assertTrue(masked.contains("****"));
    }

    // ==================== normalizeSegmentType ====================

    @Test
    void normalizeSegmentType_acceptsValidTypes() {
        assertEquals("NEW", controller.normalizeSegmentType("new"));
        assertEquals("SILENT", controller.normalizeSegmentType("SILENT"));
        assertEquals("HIGH_VALUE", controller.normalizeSegmentType(" high_value "));
    }

    @Test
    void normalizeSegmentType_rejectsInvalid() {
        assertEquals("", controller.normalizeSegmentType(null));
        assertEquals("", controller.normalizeSegmentType(""));
        assertEquals("", controller.normalizeSegmentType("UNKNOWN"));
    }

    // ==================== buildSegmentFilter ====================

    @Test
    void buildSegmentFilter_newUsesCreateTime() {
        String f = controller.buildSegmentFilter("NEW");
        assertTrue(f.contains("m.create_time"));
        assertTrue(f.contains("INTERVAL 30 DAY"));
    }

    @Test
    void buildSegmentFilter_silentUsesLastOrderTime() {
        String f = controller.buildSegmentFilter("SILENT");
        assertTrue(f.contains("agg.last_order_time"));
        assertTrue(f.contains("IS NULL"));
    }

    @Test
    void buildSegmentFilter_highValueUsesTotalSales() {
        String f = controller.buildSegmentFilter("HIGH_VALUE");
        assertTrue(f.contains("agg.total_sales"));
        assertTrue(f.contains("1000"));
    }

    @Test
    void buildSegmentFilter_emptyReturnsAll() {
        assertEquals("1=1", controller.buildSegmentFilter(""));
        assertEquals("1=1", controller.buildSegmentFilter(null));
    }

    @Test
    void buildSegmentFilter_pointsSegments() {
        assertTrue(controller.buildSegmentFilter("LOW_POINTS").contains("pr.balance"));
        assertTrue(controller.buildSegmentFilter("HIGH_POINTS").contains("pr.balance"));
    }

    // ==================== suggestedAction ====================

    @Test
    void suggestedAction_returnsActionForEachType() {
        assertNotNull(controller.suggestedAction("NEW"));
        assertNotNull(controller.suggestedAction("SILENT"));
        assertNotNull(controller.suggestedAction("HIGH_VALUE"));
        assertEquals("", controller.suggestedAction(null));
        assertEquals("", controller.suggestedAction("UNKNOWN"));
    }

    // ==================== resolveDeptIds (permission) ====================

    @Test
    void resolveDeptIds_adminReturnsAsIs() {
        setAdminUser();
        List<Long> result = controller.resolveDeptIds(Arrays.asList(100L, 200L));
        assertEquals(2, result.size());
        assertTrue(result.contains(100L));
    }

    @Test
    void resolveDeptIds_nonAdminIntersection() {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));
        List<Long> result = controller.resolveDeptIds(Collections.singletonList(100L));
        assertEquals(1, result.size());
        assertTrue(result.contains(100L));
    }

    @Test
    void resolveDeptIds_nonAdminUnauthorizedReturnsSentinel() {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));
        List<Long> result = controller.resolveDeptIds(Collections.singletonList(300L));
        assertEquals(1, result.size());
        assertEquals(-1L, result.get(0));
    }

    @Test
    void resolveDeptIds_adminEmptyDoesNotProduceInNull() {
        setAdminUser();
        List<Long> result = controller.resolveDeptIds(Collections.emptyList());
        String filter = controller.inFilter("m.dept_id", result);
        assertEquals("1=1", filter);
        assertFalse(filter.contains("NULL"));
    }

    // ==================== helpers ====================

    private static SysDept buildDept(Long deptId) {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        return dept;
    }

    private static void setAdminUser() {
        SecurityContextHolder.setUserId("1");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setUsername("admin");
        loginUser.setDeptId(null);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void setNonAdminUser(String username, Long deptId) {
        SecurityContextHolder.setUserId("2");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername(username);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    static class FakeRemoteUserService implements RemoteUserService {
        List<SysDept> deptList = Collections.emptyList();
        boolean shouldFail = false;

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source) {
            if (shouldFail) return R.fail("simulated failure");
            return R.ok(deptList);
        }

        @Override
        public R<LoginUser> getUserInfo(String username, String source) {
            return R.ok(null);
        }

        @Override
        public R<LoginUser> getUserInfoById(Long userId, String source) {
            return R.ok(null);
        }

        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> isWechatLoginEnabled(Long tenantId, String source) {
            return R.ok(false);
        }

        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source) {
            return R.ok(true);
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) {
            return R.ok(Collections.emptyList());
        }
    }
}
