<template>
  <div class="error-page">
    <h1 class="error-code">401</h1>
    <p class="error-message">抱歉，您无权访问此页面</p>
    <p class="error-desc">请联系管理员获取相应权限</p>
    <div class="error-actions">
      <el-button type="primary" @click="goHome">返回首页</el-button>
      <el-button @click="goLogin">重新登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/index')
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f5f7fa;
}

.error-code {
  font-size: 120px;
  color: #409eff;
  margin: 0;
  font-weight: 700;
  text-shadow: 2px 2px 8px rgba(64, 158, 255, 0.2);
}

.error-message {
  font-size: 20px;
  color: #303133;
  margin: 10px 0;
}

.error-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 30px 0;
}

.error-actions {
  display: flex;
  gap: 15px;
}
</style>
