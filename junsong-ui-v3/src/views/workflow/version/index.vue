<template>
  <div class="app-container">
    <el-form :inline="true">
      <el-form-item label="流程标识">
        <el-input v-model="processKey" placeholder="如：leave-apply" clearable style="width: 220px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadVersions">查询版本</el-button>
      </el-form-item>
    </el-form>

    <el-card v-if="versions.length">
      <template #header>流程版本列表</template>
      <el-table :data="versions" size="small">
        <el-table-column label="版本号" prop="version" width="80" />
        <el-table-column label="定义ID" prop="definitionId" min-width="200" show-overflow-tooltip />
        <el-table-column label="流程名称" prop="name" min-width="120" />
        <el-table-column label="流程标识" prop="key" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.suspended" type="danger" size="small">已挂起</el-tag>
            <el-tag v-else type="success" size="small">已激活</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button v-if="!row.suspended" link type="warning" size="small" @click="handleSuspend(row)">挂起</el-button>
            <el-button v-else link type="success" size="small" @click="handleActivate(row)">激活</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-else description="请输入流程标识查询版本列表" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDefinitionVersions, suspendDefinitionVersion, activateDefinitionVersion } from '@/api/workflow/version'

const processKey = ref('')
const versions = ref<any[]>([])

async function loadVersions() {
  if (!processKey.value) return
  const res: any = await getDefinitionVersions(processKey.value)
  versions.value = res.data || []
}

async function handleSuspend(row: any) {
  await suspendDefinitionVersion(row.definitionId)
  ElMessage.success('版本已挂起')
  loadVersions()
}

async function handleActivate(row: any) {
  await activateDefinitionVersion(row.definitionId)
  ElMessage.success('版本已激活')
  loadVersions()
}
</script>
