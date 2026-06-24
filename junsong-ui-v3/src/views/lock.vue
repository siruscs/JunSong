<template>
  <div class="lock-container">
    <div class="lock-box">
      <div class="user-avatar">
        <el-avatar :size="100" :src="userStore.avatar">
          {{ userStore.nickName?.charAt(0) || 'U' }}
        </el-avatar>
      </div>

      <h2 class="welcome-text">欢迎回来</h2>
      <p class="user-name">{{ userStore.nickName || userStore.name || '用户' }}</p>
      <p class="hint-text">请输入密码解锁屏幕</p>

      <el-form ref="lockFormRef" :model="lockForm" :rules="lockRules" class="lock-form">
        <el-form-item prop="password">
          <el-input
            v-model="lockForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleUnlock"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="unlock-btn"
          :loading="loading"
          @click="handleUnlock"
        >
          {{ loading ? '解锁中...' : '解 锁' }}
        </el-button>

        <div class="lock-links">
          <router-link to="/login" class="logout-link">退出并重新登录</router-link>
        </div>
      </el-form>

      <div class="lock-footer">
        <p>© 2024 峻松管理系统. All Rights Reserved.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { unlockScreen } from '@/api/login'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)

const lockForm = reactive({
  password: '',
})

const lockRules = {
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const lockFormRef = ref()

function handleUnlock() {
  if (!lockFormRef.value) return
  ;(lockFormRef.value as any).validate((valid: boolean) => {
    if (valid) {
      loading.value = true
      unlockScreen(lockForm.password)
        .then(() => {
          ElMessage.success('解锁成功')
          router.push('/index')
        })
        .catch(() => {
          ElMessage.error('密码错误，请重试')
          lockForm.password = ''
        })
        .finally(() => {
          loading.value = false
        })
    }
  })
}
</script>

<style scoped>
.lock-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2b4b66 0%, #1a2735 100%);
  padding: 20px;
}

.lock-box {
  width: 400px;
  background: #fff;
  border-radius: 8px;
  padding: 40px 35px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  text-align: center;
}

.user-avatar {
  margin-bottom: 20px;
}

.welcome-text {
  font-size: 20px;
  color: #303133;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.user-name {
  font-size: 16px;
  color: #606266;
  margin: 0 0 20px 0;
}

.hint-text {
  font-size: 13px;
  color: #909399;
  margin: 0 0 20px 0;
}

.lock-form {
  margin-top: 10px;
}

.unlock-btn {
  width: 100%;
  letter-spacing: 4px;
  margin-top: 10px;
}

.lock-links {
  margin-top: 15px;
}

.logout-link {
  color: #909399;
  font-size: 13px;
  text-decoration: none;
}

.logout-link:hover {
  color: #409eff;
}

.lock-footer {
  margin-top: 30px;
}

.lock-footer p {
  font-size: 12px;
  color: #c0c4cc;
  margin: 0;
}
</style>
