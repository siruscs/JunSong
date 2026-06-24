<template>
  <div class="error-page">
    <h1 class="error-code">404</h1>
    <p class="error-message">抱歉，您访问的页面不存在</p>
    <p class="error-desc">请检查您输入的地址是否正确</p>
    <div class="error-actions">
      <el-button type="primary" @click="goHome">返回首页</el-button>
      <el-button @click="goBack">返回上一页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/index')
}

function goBack() {
  router.back()
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
  color: #f56c6c;
  margin: 0;
  font-weight: 700;
  text-shadow: 2px 2px 8px rgba(245, 108, 108, 0.2);
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
