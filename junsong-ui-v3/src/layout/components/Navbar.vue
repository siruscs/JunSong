<template>
  <div class="navbar">
    <Hamburger :is-active="appStore.sidebar.opened" @toggle-click="appStore.toggleSideBar" />
    <Breadcrumb class="breadcrumb-container" />

    <!-- 部门切换 -->
    <div v-if="userStore.depts && userStore.depts.length > 0" class="dept-selector">
      <el-dropdown trigger="click" @command="handleDeptChange">
        <div class="dept-display">
          <el-icon><OfficeBuilding /></el-icon>
          <span class="dept-name">{{ userStore.currentDeptName || '选择部门' }}</span>
          <el-icon class="dept-arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="dept in userStore.depts"
              :key="dept.deptId"
              :command="dept.deptId"
              :class="{ 'is-active': dept.deptId === userStore.currentDeptId }"
            >
              {{ dept.deptName }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="right-menu">
      <template v-if="appStore.device !== 'mobile'">
        <!-- 主题切换 -->
        <div class="theme-switcher">
          <div
            v-for="theme in themePresets"
            :key="theme.key"
            class="theme-dot"
            :class="{ active: settingsStore.themePreset === theme.key }"
            :style="{ background: `linear-gradient(135deg, ${theme.primary} 0%, ${theme.primaryLight} 100%)` }"
            :title="theme.name"
            @click="settingsStore.changeThemePreset(theme.key)"
          ></div>
        </div>
        <Screenfull class="right-menu-item hover-effect" />
        <el-dropdown trigger="click" @command="appStore.setSize">
          <span class="toolbar-icon-button hover-effect" title="布局大小">
            <el-tooltip content="布局大小" effect="dark" placement="bottom">
              <el-icon><ScaleToOriginal /></el-icon>
            </el-tooltip>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="large" :disabled="appStore.size === 'large'">大尺寸</el-dropdown-item>
              <el-dropdown-item command="default" :disabled="appStore.size === 'default'">默认</el-dropdown-item>
              <el-dropdown-item command="small" :disabled="appStore.size === 'small'">小尺寸</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-tooltip content="锁定屏幕" effect="dark" placement="bottom">
          <span class="toolbar-icon-button hover-effect" title="锁定屏幕" @click="handleLockScreen">
            <el-icon><Lock /></el-icon>
          </span>
        </el-tooltip>
      </template>
      <el-dropdown class="avatar-container" trigger="click">
        <div class="avatar-wrapper">
          <img v-if="userStore.avatar" :src="userStore.avatar" class="user-avatar" alt="用户头像" />
          <el-icon v-else class="user-avatar-fallback"><UserFilled /></el-icon>
          <span class="user-nickname">{{ userStore.nickName || userStore.name }}</span>
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/user/profile">
              <el-dropdown-item>个人中心</el-dropdown-item>
            </router-link>
            <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useLockStore } from '@/stores/lock'
import { useSettingsStore } from '@/stores/settings'
import { themePresets } from '@/utils/theme'
import { useRoute, useRouter } from 'vue-router'
import { Lock, ScaleToOriginal } from '@element-plus/icons-vue'
import Hamburger from '@/components/Hamburger/index.vue'
import Breadcrumb from '@/components/Breadcrumb/index.vue'
import Screenfull from '@/components/Screenfull/index.vue'

const appStore = useAppStore()
const userStore = useUserStore()
const lockStore = useLockStore()
const settingsStore = useSettingsStore()
const route = useRoute()
const router = useRouter()

function handleDeptChange(deptId: number) {
  if (deptId === userStore.currentDeptId) return
  const dept = userStore.depts.find((d: any) => d.deptId === deptId)
  ElMessageBox.confirm(`确定要切换到【${dept?.deptName}】吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    userStore.switchDept(deptId).then(() => {
      ElMessage.success('切换成功')
      location.reload()
    }).catch(() => {
      ElMessage.error('切换失败')
    })
  }).catch(() => {})
}

async function handleLogout() {
  await ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await userStore.logout()
  window.location.href = '/login'
}

function handleLockScreen() {
  lockStore.lockScreen(route.fullPath)
  router.push('/lock')
}
</script>

<style scoped lang="scss">
.navbar {
  height: 64px;
  position: relative;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 0 24px;
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 255, 255, 0.72);
  background: var(--theme-app-header-bg, rgba(255, 255, 255, 0.82));
  box-shadow: var(--theme-app-header-shadow, 0 8px 24px rgba(31, 45, 82, 0.08));
  backdrop-filter: blur(18px);
}
.breadcrumb-container {
  margin-left: 0;
  color: var(--theme-app-muted, #667085);
}
.dept-selector {
  display: flex;
  align-items: center;
  margin-left: 0;

  .dept-display {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 34px;
    padding: 0 14px;
    border: 1px solid rgba(var(--theme-primary-rgb), 0.2);
    border-radius: 999px;
    background: rgba(var(--theme-primary-rgb), 0.08);
    color: var(--theme-primary-dark);
    cursor: pointer;
    transition: color 0.2s ease, background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;

    &:hover {
      transform: translateY(-1px);
      border-color: rgba(var(--theme-primary-rgb), 0.3);
      background: rgba(var(--theme-primary-rgb), 0.12);
    }

    .dept-name {
      font-size: 13px;
      font-weight: 650;
      margin: 0;
    }

    .dept-arrow {
      font-size: 12px;
    }
  }
}
.right-menu {
  height: 100%;
  line-height: 64px;
  display: flex;
  align-items: center;
  margin-left: auto;
  margin-right: 0;
  gap: 8px;
}
.right-menu-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid var(--theme-app-border, rgba(var(--theme-primary-rgb), 0.12));
  border-radius: 11px;
  background: var(--theme-app-surface, #fff);
  font-size: 18px;
  color: #5b6678;

  &.hover-effect {
    cursor: pointer;
    transition: color 0.2s ease, background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;

    &:hover {
      color: var(--theme-primary);
      border-color: rgba(var(--theme-primary-rgb), 0.25);
      background: rgba(var(--theme-primary-rgb), 0.08);
      transform: translateY(-1px);
    }
  }
}
.toolbar-icon-button {
  width: 34px;
  height: 34px;
  margin: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--theme-app-border, rgba(var(--theme-primary-rgb), 0.12));
  border-radius: 11px;
  background: var(--theme-app-surface, #fff);
  color: #5b6678;
  font-size: 18px;
  line-height: 1;
  vertical-align: middle;

  &.hover-effect {
    cursor: pointer;
    transition: color 0.2s ease, background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;

    &:hover {
      color: var(--theme-primary);
      border-color: rgba(var(--theme-primary-rgb), 0.25);
      background: rgba(var(--theme-primary-rgb), 0.08);
      transform: translateY(-1px);
    }
  }
}
.avatar-wrapper {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 9px;
  padding-left: 8px;
}
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  background: var(--theme-app-surface-muted, #f8fafc);
  box-shadow: 0 0 0 2px rgba(var(--theme-primary-rgb), 0.1);
}
.user-avatar-fallback {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 24px;
  color: #fff;
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-primary-light));
  box-shadow: 0 0 0 2px rgba(var(--theme-primary-rgb), 0.1);
}
.user-nickname {
  margin-left: 0;
  font-size: 14px;
  font-weight: 650;
  color: var(--theme-app-text, #172033);
}
.theme-switcher {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 34px;
  padding: 4px 8px;
  margin-right: 0;
  border: 1px solid var(--theme-app-border, rgba(var(--theme-primary-rgb), 0.12));
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
}
.theme-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.12);

  &:hover {
    transform: scale(1.12);
    box-shadow: 0 0 0 2px rgba(var(--theme-primary-rgb), 0.22);
  }

  &.active {
    box-shadow: 0 0 0 2px rgba(var(--theme-primary-rgb), 0.42);
  }
}
</style>
