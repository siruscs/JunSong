<template>
  <div class="app-container form-build-page">
    <div class="builder-sidebar">
      <div class="panel-title">组件</div>
      <el-button v-for="item in componentTypes" :key="item.type" plain @click="addField(item)">
        {{ item.label }}
      </el-button>
    </div>

    <div class="builder-canvas">
      <div class="toolbar">
        <el-button type="primary" @click="previewVisible = true">预览</el-button>
        <el-button @click="copySchema">复制配置</el-button>
        <el-button type="danger" plain @click="fields = []">清空</el-button>
      </div>
      <el-form label-width="100px">
        <el-form-item v-for="field in fields" :key="field.id" :label="field.label">
          <el-input v-if="field.type === 'input'" :placeholder="field.placeholder" />
          <el-input v-else-if="field.type === 'textarea'" type="textarea" :placeholder="field.placeholder" />
          <el-select v-else-if="field.type === 'select'" :placeholder="field.placeholder" style="width: 100%">
            <el-option label="选项一" value="1" />
            <el-option label="选项二" value="2" />
          </el-select>
          <el-date-picker v-else-if="field.type === 'date'" type="date" :placeholder="field.placeholder" style="width: 100%" />
          <el-input-number v-else-if="field.type === 'number'" style="width: 100%" />
          <el-switch v-else-if="field.type === 'switch'" />
        </el-form-item>
        <el-empty v-if="fields.length === 0" description="从左侧添加组件" />
      </el-form>
    </div>

    <div class="builder-props">
      <div class="panel-title">字段</div>
      <el-table :data="fields" size="small">
        <el-table-column prop="label" label="名称" />
        <el-table-column width="70">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeField(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="previewVisible" title="表单配置" width="720px" append-to-body>
      <pre class="schema-preview">{{ JSON.stringify(fields, null, 2) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const componentTypes = [
  { type: 'input', label: '单行文本', placeholder: '请输入内容' },
  { type: 'textarea', label: '多行文本', placeholder: '请输入内容' },
  { type: 'select', label: '下拉选择', placeholder: '请选择' },
  { type: 'date', label: '日期选择', placeholder: '请选择日期' },
  { type: 'number', label: '数字输入', placeholder: '' },
  { type: 'switch', label: '开关', placeholder: '' },
]

const fields = ref<any[]>([])
const previewVisible = ref(false)

function addField(item: any) {
  fields.value.push({
    id: Date.now() + Math.random(),
    type: item.type,
    label: item.label,
    placeholder: item.placeholder,
    prop: `${item.type}${fields.value.length + 1}`,
  })
}

function removeField(id: number) {
  fields.value = fields.value.filter((field) => field.id !== id)
}

function copySchema() {
  navigator.clipboard?.writeText(JSON.stringify(fields.value, null, 2))
  ElMessage.success('配置已复制')
}
</script>

<style scoped>
.form-build-page {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) 280px;
  gap: 16px;
}
.builder-sidebar,
.builder-canvas,
.builder-props {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  background: #fff;
}
.builder-sidebar {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.panel-title {
  margin-bottom: 8px;
  font-weight: 600;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.schema-preview {
  max-height: 480px;
  overflow: auto;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
}
</style>
