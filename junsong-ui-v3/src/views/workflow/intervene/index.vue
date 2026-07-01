<template>
  <div class="app-container">
    <el-form :inline="true">
      <el-form-item label="流程实例ID">
        <el-input v-model="processInstanceId" placeholder="请输入流程实例ID" clearable style="width: 360px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadActivityHistory">查询活动历史</el-button>
      </el-form-item>
    </el-form>

    <el-card v-if="activityHistory.length">
      <template #header>活动历史</template>
      <el-table :data="activityHistory" size="small">
        <el-table-column label="节点ID" prop="activityId" min-width="140" />
        <el-table-column label="节点名称" prop="activityName" min-width="120" />
        <el-table-column label="类型" prop="activityType" width="100" />
        <el-table-column label="处理人" prop="assignee" width="100" />
        <el-table-column label="开始时间" prop="startTime" width="160" />
        <el-table-column label="结束时间" prop="endTime" width="160" />
      </el-table>

      <el-divider />
      <el-form :inline="true">
        <el-form-item label="目标节点ID">
          <el-input v-model="targetActivityId" placeholder="输入要跳转到的节点ID" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="handleJump">强制跳转</el-button>
          <el-button type="warning" @click="handleSuspend">挂起实例</el-button>
          <el-button type="success" @click="handleActivate">恢复实例</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-empty v-else description="请输入流程实例ID查询活动历史" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActivityHistory, jumpInstance, suspendInstance, activateInstance } from '@/api/workflow/intervene'

const processInstanceId = ref('')
const targetActivityId = ref('')
const activityHistory = ref<any[]>([])

async function loadActivityHistory() {
  if (!processInstanceId.value) return
  const res: any = await getActivityHistory(processInstanceId.value)
  activityHistory.value = res.data || []
}

async function handleJump() {
  if (!processInstanceId.value || !targetActivityId.value) {
    ElMessage.warning('请输入流程实例ID和目标节点ID')
    return
  }
  await ElMessageBox.confirm('确认强制跳转到目标节点吗？', '危险操作', { type: 'warning' })
  await jumpInstance(processInstanceId.value, targetActivityId.value)
  ElMessage.success('跳转成功')
  loadActivityHistory()
}

async function handleSuspend() {
  if (!processInstanceId.value) return
  await suspendInstance(processInstanceId.value)
  ElMessage.success('实例已挂起')
}

async function handleActivate() {
  if (!processInstanceId.value) return
  await activateInstance(processInstanceId.value)
  ElMessage.success('实例已恢复')
}
</script>
