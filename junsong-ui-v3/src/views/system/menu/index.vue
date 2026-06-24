<template>
  <div class="app-container menu-maintenance">
    <el-row :gutter="20">
      <!-- 左侧：菜单树 -->
      <el-col :span="7" :xs="24">
        <el-card shadow="never" class="menu-tree-card">
          <template #header>
            <div class="card-header">
              <span>菜单树</span>
              <el-button link type="primary" @click="loadMenuTree">刷新</el-button>
            </div>
          </template>
          <el-input v-model="filterText" placeholder="输入菜单名称过滤" clearable class="tree-filter" />
          <el-tree
            ref="treeRef"
            v-loading="treeLoading"
            :data="menuTree"
            node-key="menuId"
            :props="treeProps"
            :filter-node-method="filterNode"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span>{{ data.menuName || data.label }}</span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：表单 + 下级菜单 -->
      <el-col :span="17" :xs="24">
        <el-card shadow="never" class="menu-form-card">
          <template #header>
            <div class="card-header">
              <span>{{ formTitle }}</span>
              <div>
                <el-button type="primary" :icon="Plus" @click="handleAddRoot" v-hasPermi="['system:menu:add']">新增顶级</el-button>
                <el-button type="primary" :icon="Plus" :disabled="!currentMenu" @click="handleAddChild" v-hasPermi="['system:menu:add']">新增下级</el-button>
                <el-button type="danger" :icon="Delete" :disabled="!form.menuId" @click="handleDelete" v-hasPermi="['system:menu:remove']">删除</el-button>
              </div>
            </div>
          </template>
          <el-alert v-if="!form.menuId && formMode === 'view'" title="请选择左侧菜单节点，或点击新增按钮维护菜单。" type="info" show-icon :closable="false" />
          <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="menu-form">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="上级菜单" prop="parentId">
                  <el-tree-select v-model="form.parentId" :data="menuOptions" :props="{ children: 'children', label: 'label' }" check-strictly node-key="id" :default-expand-all="false" placeholder="请选择上级菜单" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="菜单类型" prop="menuType">
                  <el-radio-group v-model="form.menuType">
                    <el-radio value="M">目录</el-radio>
                    <el-radio value="C">菜单</el-radio>
                    <el-radio value="F">按钮</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="菜单名称" prop="menuName">
                  <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="显示排序" prop="orderNum">
                  <el-input-number v-model="form.orderNum" :min="0" :max="999" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16" v-if="form.menuType !== 'F'">
              <el-col :span="12">
                <el-form-item label="路由地址" prop="path">
                  <el-input v-model="form.path" placeholder="请输入路由地址" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="组件路径" prop="component">
                  <el-input v-model="form.component" placeholder="请输入组件路径" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16" v-if="form.menuType === 'C'">
              <el-col :span="24">
                <el-form-item label="路由参数" prop="query">
                  <el-input v-model="form.query" placeholder='如：{"id": 1, "name": "Jun"}' />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16" v-if="form.menuType !== 'F'">
              <el-col :span="12">
                <el-form-item label="权限标识" prop="perms">
                  <el-input v-model="form.perms" placeholder="如：system:user:list" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="菜单图标" prop="icon">
                  <el-input v-model="form.icon" placeholder="请输入菜单图标">
                    <template #append>
                      <el-button @click="selectIcon">选择</el-button>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16" v-if="form.menuType !== 'F'">
              <el-col :span="12">
                <el-form-item label="是否外链">
                  <el-radio-group v-model="form.isFrame">
                    <el-radio value="0">是</el-radio>
                    <el-radio value="1">否</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否缓存">
                  <el-radio-group v-model="form.isCache">
                    <el-radio value="0">缓存</el-radio>
                    <el-radio value="1">不缓存</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16" v-if="form.menuType !== 'F'">
              <el-col :span="12">
                <el-form-item label="显示状态">
                  <el-radio-group v-model="form.visible">
                    <el-radio value="0">显示</el-radio>
                    <el-radio value="1">隐藏</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="菜单状态">
                  <el-radio-group v-model="form.status">
                    <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16" v-if="form.menuType === 'F'">
              <el-col :span="12">
                <el-form-item label="权限标识" prop="perms">
                  <el-input v-model="form.perms" placeholder="如：system:user:add" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="菜单状态">
                  <el-radio-group v-model="form.status">
                    <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" @click="submitForm" v-hasPermi="['system:menu:add', 'system:menu:edit']">保存</el-button>
              <el-button @click="resetCurrent">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 下级菜单列表 -->
        <div class="children-section">
          <el-form :model="searchParams" ref="searchFormRef" :inline="true" v-show="showSearch" label-width="80px">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="searchParams.menuName" placeholder="请输入菜单名称" clearable style="width: 240px" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="searchParams.status" placeholder="菜单状态" clearable style="width: 240px">
                <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-form>

          <RightToolbar v-model:showSearch="showSearch" @query="loadMenuTree">
            <el-button type="primary" :icon="Plus" :disabled="!currentMenu" @click="handleAddChild" v-hasPermi="['system:menu:add']">新增下级</el-button>
          </RightToolbar>

          <el-table v-loading="childrenLoading" :data="filteredChildrenList" border>
            <el-table-column prop="menuName" label="菜单名称" :show-overflow-tooltip="true" />
            <el-table-column prop="icon" label="图标" width="80" align="center">
              <template #default="scope">
                <svg-icon v-if="scope.row.icon" :icon-class="scope.row.icon" />
              </template>
            </el-table-column>
            <el-table-column label="类型" align="center" width="80">
              <template #default="scope">
                <el-tag v-if="scope.row.menuType === 'M'" type="primary">目录</el-tag>
                <el-tag v-else-if="scope.row.menuType === 'C'" type="success">菜单</el-tag>
                <el-tag v-else type="info">按钮</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="orderNum" label="排序" width="80" align="center" />
            <el-table-column prop="path" label="路由地址" :show-overflow-tooltip="true" />
            <el-table-column prop="perms" label="权限标识" :show-overflow-tooltip="true" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="scope">
                <DictTag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button link type="primary" @click="selectChild(scope.row)" v-hasPermi="['system:menu:query']">维护</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- 图标选择弹窗 -->
    <el-dialog v-model="iconDialog.visible" title="图标选择" width="800px" append-to-body>
      <el-row>
        <el-col :span="4" v-for="item in iconList" :key="item" style="text-align: center; margin-bottom: 20px">
          <div class="icon-item" @click="selectedIcon(item)">
            <svg-icon :icon-class="item" />
            <span>{{ item }}</span>
          </div>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import SvgIcon from '@/components/SvgIcon/index.vue'
import { useDict } from '@/composables/useDict'

import { listMenu, getMenu, addMenu, updateMenu, delMenu, treeselect } from '@/api/system/menu'

const dict = useDict('sys_normal_disable')

const iconList = [
  'lock',
  'messages',
  'setting',
  'user',
  'chart',
  'tree-table',
  'clipboard-list',
  'shopping-card',
  'icon',
  'tree',
  'star',
  'edit',
  'refresh',
  'information',
  'guide',
  'eye-open',
  'dashboard',
  'exit-fullscreen',
  'documentation',
  'bug',
  'date-range',
  'theme',
  '403',
  '404',
]

const treeLoading = ref(false)
const childrenLoading = ref(false)
const filterText = ref('')
const menuFlatList = ref<any[]>([])
const menuTree = ref<any[]>([])
const menuOptions = ref<any[]>([])
const childrenList = ref<any[]>([])
const currentMenu = ref<any>(null)
const showSearch = ref(true)
const iconDialog = reactive({ visible: false })
const formRef = ref()
const treeRef = ref()
const formMode = ref<'view' | 'add' | 'edit'>('view')
const treeProps = { children: 'children', label: 'menuName' }

const searchParams = reactive({
  menuName: undefined as string | undefined,
  status: undefined as string | undefined,
})

const form = reactive<any>({
  menuId: undefined,
  parentId: 0,
  menuName: '',
  menuType: 'M',
  orderNum: 0,
  path: '',
  component: '',
  query: '',
  isFrame: 1,
  isCache: '0',
  visible: '0',
  status: '0',
  perms: '',
  icon: '',
})

const rules = {
  parentId: [{ required: true, message: '上级菜单不能为空', trigger: 'change' }],
  menuName: [{ required: true, message: '菜单名称不能为空', trigger: 'blur' }],
  menuType: [{ required: true, message: '菜单类型不能为空', trigger: 'change' }],
  orderNum: [{ required: true, message: '菜单排序不能为空', trigger: 'blur' }],
}

const formTitle = computed(() => {
  if (formMode.value === 'add') return '新增菜单'
  if (formMode.value === 'edit') return '维护菜单'
  return '菜单信息'
})

const filteredChildrenList = computed(() => {
  let list = childrenList.value || []
  if (searchParams.menuName) {
    list = list.filter((item: any) => item.menuName?.includes(searchParams.menuName))
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
    menuId: data.menuId,
    parentId: data.parentId || 0,
    menuName: data.menuName || '',
    menuType: data.menuType || 'M',
    orderNum: data.orderNum ?? 0,
    path: data.path || '',
    component: data.component || '',
    query: data.query || '',
    isFrame: data.isFrame ?? 1,
    isCache: data.isCache || '0',
    visible: data.visible || '0',
    status: data.status || '0',
    perms: data.perms || '',
    icon: data.icon || '',
  })
  nextTick(() => formRef.value?.clearValidate?.())
}

function filterNode(value: string, data: any) {
  if (!value) return true
  const name = data.menuName || data.label || data.name || ''
  return name.includes(value)
}

function buildMenuTree(list: any[]): any[] {
  const map: Record<string, any> = {}
  const tree: any[] = []
  for (const item of list) {
    map[item.menuId] = { ...item, children: [] }
  }
  for (const item of list) {
    const node = map[item.menuId]
    const parentId = node.parentId
    if (parentId !== 0 && parentId !== null && parentId !== undefined && map[parentId]) {
      map[parentId].children.push(node)
    } else {
      tree.push(node)
    }
  }
  return tree
}

function loadMenuTree() {
  treeLoading.value = true
  listMenu({})
    .then((res: any) => {
      menuFlatList.value = res.data || []
      menuTree.value = buildMenuTree(menuFlatList.value)
      if (currentMenu.value) {
        const parent = findMenuInTree(menuTree.value, currentMenu.value.menuId)
        childrenList.value = parent?.children || []
      }
    })
    .finally(() => {
      treeLoading.value = false
    })
}

function findMenuInTree(tree: any[], menuId: number): any {
  for (const node of tree) {
    if (node.menuId === menuId) return node
    if (node.children && node.children.length > 0) {
      const found = findMenuInTree(node.children, menuId)
      if (found) return found
    }
  }
  return undefined
}

function loadTreeselectOptions() {
  treeselect().then((res: any) => {
    menuOptions.value = [{ id: 0, label: '主类目', children: res.data || res.menus || [] }]
  })
}

function handleNodeClick(data: any) {
  getMenu(data.menuId).then((res: any) => {
    currentMenu.value = res.data
    formMode.value = 'edit'
    resetFormData(res.data)
    childrenList.value = findMenuInTree(menuTree.value, data.menuId)?.children || []
  })
}

function selectChild(row: any) {
  nextTick(() => {
    treeRef.value?.setCurrentKey?.(row.menuId)
    handleNodeClick(row)
  })
}

function handleAddRoot() {
  currentMenu.value = null
  childrenList.value = []
  formMode.value = 'add'
  resetFormData({ parentId: 0, menuType: 'M', orderNum: 0 })
  loadTreeselectOptions()
}

function handleAddChild() {
  if (!currentMenu.value) return
  formMode.value = 'add'
  resetFormData({ parentId: currentMenu.value.menuId, menuType: 'M', orderNum: 0 })
  loadTreeselectOptions()
}

function resetCurrent() {
  if (formMode.value === 'add') {
    const parentId = form.parentId
    resetFormData({ parentId, menuType: 'M', orderNum: 0 })
    return
  }
  if (form.menuId !== undefined) {
    const menuId = form.menuId
    getMenu(menuId).then((res: any) => {
      resetFormData(res.data)
      formMode.value = 'edit'
    })
  }
}

function selectIcon() {
  iconDialog.visible = true
}

function selectedIcon(name: string) {
  form.icon = name
  iconDialog.visible = false
}

function submitForm() {
  if (!formRef.value) return
  formRef.value.validate((valid: boolean) => {
    if (!valid) return
    if (form.menuId !== undefined) {
      updateMenu(form).then(() => {
        ElMessage.success('修改成功')
        formMode.value = 'edit'
        loadMenuTree()
        loadTreeselectOptions()
        getMenu(form.menuId).then((res: any) => {
          currentMenu.value = res.data
          resetFormData(res.data)
        })
      })
    } else {
      addMenu(form).then(() => {
        ElMessage.success('新增成功')
        loadMenuTree()
        loadTreeselectOptions()
        currentMenu.value = null
        formMode.value = 'view'
        resetFormData()
      })
    }
  })
}

function handleDelete() {
  if (!form.menuId) return
  ElMessageBox.confirm(`是否确认删除菜单名称为"${form.menuName}"的数据项？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => delMenu(form.menuId))
    .then(() => {
      ElMessage.success('删除成功')
      resetFormData()
      currentMenu.value = null
      childrenList.value = []
      formMode.value = 'view'
      loadMenuTree()
    })
    .catch(() => {})
}

onMounted(() => {
  loadMenuTree()
  loadTreeselectOptions()
})
</script>

<style scoped>
.menu-maintenance {
  display: block;
}

.menu-maintenance > .el-row {
  display: flex;
  flex-wrap: nowrap;
  align-items: flex-start;
}

.menu-maintenance > .el-row > .el-col {
  display: flex;
  flex-direction: column;
}

/* 左侧：sticky + 固定高度，树内部滚动 */
.menu-maintenance > .el-row > .el-col:first-child {
  position: sticky;
  top: 8px;
  align-self: flex-start;
}

.menu-tree-card {
  height: calc(100vh - 104px - 24px);
  min-height: 500px;
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 6px;
}

.menu-tree-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.menu-tree-card :deep(.el-card__header) {
  flex-shrink: 0;
}

/* 右侧：自然内容高度，随页面滚动 */
.menu-maintenance > .el-row > .el-col:last-child {
  overflow: visible;
}

.menu-form-card {
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

.menu-form {
  margin-top: 8px;
}

.icon-item {
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
}

.icon-item:hover {
  background: #f5f7fa;
}
</style>
