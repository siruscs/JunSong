<template>
  <div class="app-container region-maintenance">
    <el-row :gutter="20">
      <el-col :span="7" :xs="24">
        <el-card shadow="never" class="region-tree-card">
          <template #header>
            <div class="card-header">
              <span>地址树</span>
              <el-button link type="primary" @click="loadRegionTree">刷新</el-button>
            </div>
          </template>
          <el-input v-model="filterText" placeholder="输入地址名称过滤" clearable class="tree-filter" />
          <el-tree
            ref="treeRef"
            v-loading="treeLoading"
            :data="regionTree"
            node-key="code"
            :props="treeProps"
            :filter-node-method="filterNode"
            highlight-current
            lazy
            :load="loadTreeNode"
            @node-click="handleNodeClick"
          />
        </el-card>
      </el-col>
      <el-col :span="17" :xs="24">
        <el-card shadow="never" class="region-form-card">
          <template #header>
            <div class="card-header">
              <span>{{ formTitle }}</span>
              <div>
                <el-button type="primary" :icon="Plus" @click="handleAddRoot" v-hasPermi="['system:region:add']">新增省级</el-button>
                <el-button type="primary" :icon="Plus" :disabled="!currentRegion || currentRegion.level >= 4" @click="handleAddChild" v-hasPermi="['system:region:add']">新增下级</el-button>
                <el-button type="danger" :icon="Delete" :disabled="!form.code" @click="handleDelete" v-hasPermi="['system:region:remove']">删除</el-button>
              </div>
            </div>
          </template>
          <el-alert v-if="!form.code && formMode === 'view'" title="请选择左侧地址节点，或点击新增按钮维护地址。" type="info" show-icon :closable="false" />
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="region-form">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="地址编码" prop="code">
                  <el-input v-model="form.code" :disabled="formMode === 'edit'" maxlength="12" placeholder="请输入地址编码" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="地址名称" prop="name">
                  <el-input v-model="form.name" maxlength="100" placeholder="请输入地址名称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="上级编码" prop="parentCode">
                  <el-input v-model="form.parentCode" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="地址层级" prop="level">
                  <el-select v-model="form.level" disabled style="width: 100%">
                    <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="显示排序" prop="sort">
                  <el-input-number v-model="form.sort" :min="0" :max="999999" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="状态" prop="status">
                  <el-radio-group v-model="form.status">
                    <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="数据年份">
                  <el-input v-model="form.sourceYear" placeholder="请输入数据年份" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="数据来源">
                  <el-input v-model="form.sourceName" placeholder="请输入数据来源" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" @click="submitForm" v-hasPermi="['system:region:add', 'system:region:edit']">保存</el-button>
              <el-button @click="resetCurrent">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        <div class="children-section">
          <el-form :model="searchParams" ref="searchFormRef" :inline="true" v-show="showSearch" label-width="80px">
            <el-form-item label="地址名称" prop="name">
              <el-input v-model="searchParams.name" placeholder="请输入地址名称" clearable style="width: 240px" @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="searchParams.status" placeholder="地址状态" clearable style="width: 240px">
                <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>

          <RightToolbar v-model:showSearch="showSearch" @query="reloadChildren">
            <el-button type="primary" :icon="Plus" :disabled="!currentRegion || currentRegion.level >= 4" @click="handleAddChild" v-hasPermi="['system:region:add']">新增下级</el-button>
          </RightToolbar>

          <el-table v-loading="childrenLoading" :data="filteredChildrenList" border>
            <el-table-column prop="code" label="编码" width="140" />
            <el-table-column prop="name" label="名称" :show-overflow-tooltip="true" />
            <el-table-column label="层级" width="100" align="center">
              <template #default="scope">{{ levelLabel(scope.row.level) }}</template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" width="90" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="scope">
                <DictTag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button link type="primary" @click="selectChild(scope.row)" v-hasPermi="['system:region:query']">维护</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import DictTag from '@/components/DictTag/index.vue'
import { useDict } from '@/composables/useDict'
import { addRegion, delRegion, getRegion, listRegionChildren, updateRegion } from '@/api/system/region'

const dict = useDict('sys_normal_disable')

const treeLoading = ref(false)
const childrenLoading = ref(false)
const filterText = ref('')
const regionTree = ref<any[]>([])
const childrenList = ref<any[]>([])
const currentRegion = ref<any>(null)
const currentParentCode = ref<string>('')
const showSearch = ref(true)
const searchFormRef = ref()
const formRef = ref()
const treeRef = ref()
const formMode = ref<'view' | 'add' | 'edit'>('view')
const treeProps = { children: 'children', label: 'name', isLeaf: 'leaf' }
const levelOptions = [
  { value: 1, label: '省级' },
  { value: 2, label: '市级' },
  { value: 3, label: '区县级' },
  { value: 4, label: '街道乡镇级' },
]
const form = reactive<any>({
  code: '',
  name: '',
  parentCode: '0',
  level: 1,
  sort: 0,
  status: '0',
  sourceYear: '2023',
  sourceName: '国家统计局2023年度全国统计用区划代码和城乡划分代码',
})

const searchParams = reactive<any>({
  name: undefined as string | undefined,
  status: undefined as string | undefined,
})
const rules = {
  code: [{ required: true, message: '地址编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '地址名称不能为空', trigger: 'blur' }],
  parentCode: [{ required: true, message: '上级编码不能为空', trigger: 'blur' }],
  level: [{ required: true, message: '地址层级不能为空', trigger: 'change' }],
  sort: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
}

const formTitle = computed(() => {
  if (formMode.value === 'add') return '新增地址'
  if (formMode.value === 'edit') return '维护地址'
  return '地址信息'
})

const filteredChildrenList = computed(() => {
  let list = childrenList.value || []
  if (searchParams.name) {
    list = list.filter((item: any) => item.name?.includes(searchParams.name))
  }
  if (searchParams.status !== undefined && searchParams.status !== null && searchParams.status !== '') {
    list = list.filter((item: any) => item.status === searchParams.status)
  }
  return list
})

watch(filterText, (value) => {
  treeRef.value?.filter(value)
})

function resetFormData(data: any = {}) {
  Object.assign(form, {
    code: data.code || '',
    name: data.name || '',
    parentCode: data.parentCode || '0',
    level: data.level || 1,
    sort: data.sort ?? 0,
    status: data.status || '0',
    sourceYear: data.sourceYear || '2023',
    sourceName: data.sourceName || '国家统计局2023年度全国统计用区划代码和城乡划分代码',
  })
  nextTick(() => formRef.value?.clearValidate?.())
}

function levelLabel(level: number) {
  return levelOptions.find((item) => item.value === level)?.label || '-'
}

function filterNode(value: string, data: any) {
  if (!value) return true
  return data.name?.includes(value)
}

function normalizeRegionNodes(nodes: any[]) {
  return nodes.map((node) => ({ ...node, leaf: node.level >= 4 }))
}

function loadRegionTree() {
  treeLoading.value = true
  listRegionChildren('0')
    .then((res: any) => {
      regionTree.value = normalizeRegionNodes(res.data || [])
    })
    .finally(() => {
      treeLoading.value = false
    })
}

function loadTreeNode(node: any, resolve: (data: any[]) => void) {
  if (node.level === 0) {
    resolve(regionTree.value)
    return
  }
  listRegionChildren(node.data.code).then((res: any) => {
    resolve(normalizeRegionNodes(res.data || []))
  })
}

function loadChildren(parentCode: string) {
  childrenLoading.value = true
  listRegionChildren(parentCode)
    .then((res: any) => {
      childrenList.value = res.data || []
    })
    .finally(() => {
      childrenLoading.value = false
    })
}

function reloadChildren() {
  if (currentParentCode.value) {
    loadChildren(currentParentCode.value)
  }
}

function handleSearch() {
  // 基于 searchParams 做前端过滤，无需重新拉取
}

function resetSearch() {
  searchParams.name = undefined
  searchParams.status = undefined
}

function handleNodeClick(node: any) {
  getRegion(node.code).then((res: any) => {
    currentRegion.value = res.data
    formMode.value = 'edit'
    resetFormData(res.data)
    currentParentCode.value = node.code
    resetSearch()
    loadChildren(node.code)
  })
}

function selectChild(row: any) {
  handleNodeClick(row)
  nextTick(() => treeRef.value?.setCurrentKey?.(row.code))
}

function handleAddRoot() {
  currentRegion.value = null
  childrenList.value = []
  formMode.value = 'add'
  resetFormData({ parentCode: '0', level: 1, sort: 0 })
}

function handleAddChild() {
  if (!currentRegion.value) return
  formMode.value = 'add'
  resetFormData({ parentCode: currentRegion.value.code, level: currentRegion.value.level + 1, sort: childrenList.value.length + 1 })
}

function resetCurrent() {
  if (formMode.value === 'add') {
    const parentCode = form.parentCode
    const level = form.level
    resetFormData({ parentCode, level })
    return
  }
  if (form.code) {
    handleNodeClick({ code: form.code })
  }
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    const request = formMode.value === 'add' ? addRegion : updateRegion
    request(form).then(() => {
      ElMessage.success('保存成功')
      formMode.value = 'edit'
      loadRegionTree()
      if (form.parentCode) loadChildren(form.parentCode === '0' ? form.code : form.parentCode)
      getRegion(form.code).then((res: any) => {
        currentRegion.value = res.data
        resetFormData(res.data)
      })
    })
  })
}

function handleDelete() {
  if (!form.code) return
  ElMessageBox.confirm(`是否确认删除地址“${form.name}”？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => delRegion(form.code))
    .then(() => {
      ElMessage.success('删除成功')
      const parentCode = form.parentCode
      resetFormData()
      currentRegion.value = null
      formMode.value = 'view'
      loadRegionTree()
      if (parentCode) loadChildren(parentCode)
    })
    .catch(() => {})
}

onMounted(() => {
  loadRegionTree()
})
</script>

<style scoped>
.region-maintenance {
  display: block;
}

.region-maintenance > .el-row {
  display: flex;
  flex-wrap: nowrap;
  align-items: flex-start;
}

.region-maintenance > .el-row > .el-col {
  display: flex;
  flex-direction: column;
}

/* 左侧第一列：sticky 定位，让树卡片在右侧内容滚动时固定不动 */
.region-maintenance > .el-row > .el-col:first-child {
  position: sticky;
  top: 8px;
  align-self: flex-start;
}

.region-tree-card {
  height: calc(100vh - 104px - 24px);
  min-height: 500px;
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 6px;
}

.region-tree-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.region-tree-card :deep(.el-card__header) {
  flex-shrink: 0;
}

/* 右侧：自然内容高度，随页面整体滚动 */
.region-maintenance > .el-row > .el-col:last-child {
  overflow: visible;
}

.region-form-card {
  margin-bottom: 16px;
  border-radius: 6px;
}

.children-section {
  background: #fff;
  border-radius: 6px;
  padding: 16px;
}

.children-section .el-form {
  margin-bottom: 0;
}

.children-section > .el-table {
  margin-top: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tree-filter {
  margin-bottom: 12px;
}

.region-form {
  margin-top: 8px;
}

.sub-title {
  color: #909399;
  font-size: 13px;
}
</style>
