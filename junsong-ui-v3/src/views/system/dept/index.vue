<template>
  <div class="app-container dept-maintenance">
    <el-row :gutter="20">
      <!-- 左侧：部门树 -->
      <el-col :span="7" :xs="24">
        <el-card shadow="never" class="dept-tree-card">
          <template #header>
            <div class="card-header">
              <span>部门树</span>
              <el-button link type="primary" @click="loadDeptTree">刷新</el-button>
            </div>
          </template>
          <el-input v-model="filterText" placeholder="输入部门名称过滤" clearable class="tree-filter" />
          <el-tree
            ref="treeRef"
            v-loading="treeLoading"
            :data="deptTree"
            node-key="deptId"
            :props="treeProps"
            :filter-node-method="filterNode"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span>{{ data.deptName || data.label }}</span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：表单 + 下级部门 -->
      <el-col :span="17" :xs="24">
        <el-card shadow="never" class="dept-form-card">
          <template #header>
            <div class="card-header">
              <span>{{ formTitle }}</span>
              <div>
                <el-button type="primary" :icon="Plus" @click="handleAddRoot" v-hasPermi="['system:dept:add']">新增顶级</el-button>
                <el-button type="primary" :icon="Plus" :disabled="!currentDept" @click="handleAddChild" v-hasPermi="['system:dept:add']">新增下级</el-button>
                <el-button type="danger" :icon="Delete" :disabled="!form.deptId" @click="handleDelete" v-hasPermi="['system:dept:remove']">删除</el-button>
              </div>
            </div>
          </template>
          <el-alert v-if="!form.deptId && formMode === 'view'" title="请选择左侧部门节点，或点击新增按钮维护部门。" type="info" show-icon :closable="false" />
          <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="dept-form">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="上级部门" prop="parentId">
                  <el-tree-select v-model="form.parentId" :data="deptOptions" :props="{ children: 'children', label: 'label' }" check-strictly node-key="id" :default-expand-all="false" placeholder="请选择上级部门" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="部门名称" prop="deptName">
                  <el-input v-model="form.deptName" placeholder="请输入部门名称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="显示排序" prop="orderNum">
                  <el-input-number v-model="form.orderNum" :min="0" :max="999" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="负责人" prop="leader">
                  <el-input v-model="form.leader" placeholder="请输入负责人" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="联系电话" prop="phone">
                  <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="状态">
                  <el-radio-group v-model="form.status">
                    <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="24">
                <el-form-item label="所在地址">
                  <el-cascader v-model="selectedRegion" :options="regionOptions" :props="regionProps" clearable filterable placeholder="请选择省市区街道" style="width: 100%" @change="handleRegionChange" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="24">
                <el-form-item label="详细地址">
                  <el-input v-model="form.detailAddress" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="请输入详细地址" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="门店类型">
                  <el-switch v-model="form.deptType" active-value="1" inactive-value="0" active-text="门店" inactive-text="普通部门" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="经度">
                  <el-input-number v-model="form.longitude" :precision="7" :step="0.0000001" :controls="false" placeholder="经度" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="纬度">
                  <el-input-number v-model="form.latitude" :precision="7" :step="0.0000001" :controls="false" placeholder="纬度" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="24">
                <el-form-item label="地图选点">
                  <el-button type="primary" plain :icon="Location" @click="openMapPicker">在地图上选择位置</el-button>
                  <span v-if="form.longitude != null && form.latitude != null" class="geo-display">当前坐标: {{ form.longitude.toFixed(7) }}, {{ form.latitude.toFixed(7) }}</span>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" @click="submitForm" v-hasPermi="['system:dept:add', 'system:dept:edit']">保存</el-button>
              <el-button @click="resetCurrent">重置</el-button>
            </el-form-item>
          </el-form>
          <MapPicker v-model="mapPickerVisible" :initial-lng="form.longitude" :initial-lat="form.latitude" @confirm="onMapPickerConfirm" />
        </el-card>

        <!-- 下级部门列表 -->
        <div class="children-section">
          <el-form :model="searchParams" ref="searchFormRef" :inline="true" v-show="showSearch" label-width="80px">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="searchParams.deptName" placeholder="请输入部门名称" clearable style="width: 240px" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="searchParams.status" placeholder="部门状态" clearable style="width: 240px">
                <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-form>

          <RightToolbar v-model:showSearch="showSearch" @query="loadDeptTree">
            <el-button type="primary" :icon="Plus" :disabled="!currentDept" @click="handleAddChild" v-hasPermi="['system:dept:add']">新增下级</el-button>
          </RightToolbar>

          <el-table v-loading="childrenLoading" :data="filteredChildrenList" border>
            <el-table-column prop="deptName" label="部门名称" :show-overflow-tooltip="true" />
            <el-table-column prop="orderNum" label="排序" width="80" align="center" />
            <el-table-column prop="leader" label="负责人" width="120" align="center" />
            <el-table-column prop="phone" label="联系电话" width="140" align="center" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="scope">
                <DictTag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button link type="primary" @click="selectChild(scope.row)" v-hasPermi="['system:dept:query']">维护</el-button>
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
import { Delete, Plus, Location } from '@element-plus/icons-vue'
import MapPicker from '@/components/MapPicker/index.vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import { useDict } from '@/composables/useDict'

import { listDept, getDept, addDept, updateDept, delDept, treeselect } from '@/api/system/dept'
import { getRegionTree } from '@/api/system/region'
import type { RegionOption } from '@/utils/regionOptions'

const dict = useDict('sys_normal_disable')

const treeLoading = ref(false)
const childrenLoading = ref(false)
const filterText = ref('')
const deptFlatList = ref<any[]>([])
const deptTree = ref<any[]>([])
const deptOptions = ref<any[]>([])
const childrenList = ref<any[]>([])
const currentDept = ref<any>(null)
const showSearch = ref(true)
const selectedRegion = ref<string[]>([])
const regionOptions = ref<RegionOption[]>([])
const regionProps = { value: 'value', label: 'label', children: 'children', checkStrictly: true }
const formRef = ref()
const treeRef = ref()
const formMode = ref<'view' | 'add' | 'edit'>('view')
const treeProps = { children: 'children', label: 'deptName' }
const mapPickerVisible = ref(false)

function openMapPicker() {
  mapPickerVisible.value = true
}

function onMapPickerConfirm(payload: { lng: number; lat: number; address: string; region?: any }) {
  form.longitude = payload.lng
  form.latitude = payload.lat
  if (payload.address) {
    form.detailAddress = payload.address
  }
  if (payload.region) {
    matchRegionFromMap(payload.region)
  }
}

function matchRegionFromMap(region: {
  provinceName: string
  cityName: string
  districtName: string
  adcode: string
  townName?: string
  towncode?: string
}) {
  const adcode = region.adcode || ''
  const towncode = region.towncode || ''
  // 重置
  form.provinceCode = ''
  form.provinceName = ''
  form.cityCode = ''
  form.cityName = ''
  form.districtCode = ''
  form.districtName = ''
  form.streetCode = ''
  form.streetName = ''

  if (/^\d{6}$/.test(adcode)) {
    // 按国标 adcode 规则推导：前2位省、中2位市、后2位区
    const provinceCode = adcode.substring(0, 2) + '0000'
    const cityCode = adcode.substring(0, 4) + '00'
    const districtCode = adcode

    const prov = findRegionNode(regionOptions.value, provinceCode)
    const city = findRegionNode(regionOptions.value, cityCode)
    const dist = findRegionNode(regionOptions.value, districtCode)

    form.provinceCode = provinceCode
    form.provinceName = prov?.label || region.provinceName || ''
    // 直辖市的市级 code 可能不存在（如 110100），存在才回填
    if (city) {
      form.cityCode = cityCode
      form.cityName = city.label || region.cityName || ''
    } else {
      form.cityCode = provinceCode
      form.cityName = region.cityName || prov?.label || ''
    }
    if (dist) {
      form.districtCode = districtCode
      form.districtName = dist.label || region.districtName || ''
    } else {
      form.districtCode = districtCode
      form.districtName = region.districtName || ''
    }

    // 街道匹配：高德 towncode 为 12 位，数据库街道 code 取前 9 位
    if (/^\d{12}$/.test(towncode)) {
      const streetCode = towncode.substring(0, 9)
      const street = findRegionNode(regionOptions.value, streetCode)
      if (street) {
        form.streetCode = streetCode
        form.streetName = street.label || region.townName || ''
      } else {
        // 树中无该街道节点，仍回填 code 和高德返回的名称
        form.streetCode = streetCode
        form.streetName = region.townName || ''
      }
    }
  } else {
    // 无 adcode 时退化为名称匹配
    matchRegionByName(region)
  }

  syncSelectedRegionFromForm()
}

function matchRegionByName(region: { provinceName: string; cityName: string; districtName: string; townName?: string }) {
  const { provinceName, districtName } = region
  const cityName = region.cityName || provinceName
  const townName = region.townName || ''
  for (const prov of regionOptions.value) {
    if (isNameMatch(prov.label, provinceName)) {
      form.provinceCode = prov.value
      form.provinceName = prov.label
      for (const city of prov.children || []) {
        if (isNameMatch(city.label, cityName)) {
          form.cityCode = city.value
          form.cityName = city.label
          for (const dist of city.children || []) {
            if (isNameMatch(dist.label, districtName)) {
              form.districtCode = dist.value
              form.districtName = dist.label
              if (townName) {
                for (const street of dist.children || []) {
                  if (isNameMatch(street.label, townName)) {
                    form.streetCode = street.value
                    form.streetName = street.label
                    break
                  }
                }
              }
              break
            }
          }
          break
        }
      }
      break
    }
  }
}

function isNameMatch(label: string, name: string): boolean {
  if (!label || !name) return false
  if (label === name) return true
  // 模糊匹配：去除"省/市/区/县/自治区/特别行政区"等后缀后比较
  const normalize = (s: string) => s.replace(/(省|市|区|县|自治区|特别行政区|壮族|回族|维吾尔|蒙古|藏族|彝族|苗族|土家族|傣族|黎族|傈僳族|佤族|畲族|瑶族|白族|哈尼族|哈萨克族|侗族|朝鲜族|满族|鄂温克族|鄂伦春族|达斡尔族|赫哲族|高山族|拉祜族|水族|东乡族|纳西族|景颇族|柯尔克孜族|土族|撒拉族|塔吉克族|保安族|裕固族|塔塔尔族|独龙族|珞巴族|基诺族)$/g, '')
  return normalize(label) === normalize(name) || label.includes(name) || name.includes(label)
}

const searchParams = reactive({
  deptName: undefined as string | undefined,
  status: undefined as string | undefined,
})

const form = reactive<any>({
  deptId: undefined,
  parentId: 0,
  deptName: '',
  orderNum: 0,
  leader: '',
  phone: '',
  email: '',
  provinceCode: '',
  provinceName: '',
  cityCode: '',
  cityName: '',
  districtCode: '',
  districtName: '',
  streetCode: '',
  streetName: '',
  detailAddress: '',
  longitude: undefined as number | undefined,
  latitude: undefined as number | undefined,
  deptType: '0',
  status: '0',
})

const rules = {
  parentId: [{ required: true, message: '上级部门不能为空', trigger: 'change' }],
  deptName: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }],
  orderNum: [{ required: true, message: '部门排序不能为空', trigger: 'blur' }],
}

const formTitle = computed(() => {
  if (formMode.value === 'add') return '新增部门'
  if (formMode.value === 'edit') return '维护部门'
  return '部门信息'
})

const filteredChildrenList = computed(() => {
  let list = childrenList.value || []
  if (searchParams.deptName) {
    list = list.filter((item: any) => item.deptName?.includes(searchParams.deptName))
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
    deptId: data.deptId,
    parentId: data.parentId || 0,
    deptName: data.deptName || '',
    orderNum: data.orderNum ?? 0,
    leader: data.leader || '',
    phone: data.phone || '',
    email: data.email || '',
    provinceCode: data.provinceCode || '',
    provinceName: data.provinceName || '',
    cityCode: data.cityCode || '',
    cityName: data.cityName || '',
    districtCode: data.districtCode || '',
    districtName: data.districtName || '',
    streetCode: data.streetCode || '',
    streetName: data.streetName || '',
    detailAddress: data.detailAddress || '',
    longitude: data.longitude ?? undefined,
    latitude: data.latitude ?? undefined,
    deptType: data.deptType || '0',
    status: data.status || '0',
  })
  syncSelectedRegionFromForm()
  nextTick(() => formRef.value?.clearValidate?.())
}

function filterNode(value: string, data: any) {
  if (!value) return true
  const name = data.deptName || data.label || data.name || ''
  return name.includes(value)
}

function buildDeptTree(list: any[]): any[] {
  const map: Record<string, any> = {}
  const tree: any[] = []
  for (const item of list) {
    map[item.deptId] = { ...item, children: [] }
  }
  for (const item of list) {
    const node = map[item.deptId]
    const parentId = node.parentId
    if (parentId !== 0 && parentId !== null && parentId !== undefined && map[parentId]) {
      map[parentId].children.push(node)
    } else {
      tree.push(node)
    }
  }
  return tree
}

function loadDeptTree() {
  treeLoading.value = true
  listDept({})
    .then((res: any) => {
      deptFlatList.value = res.data || []
      deptTree.value = buildDeptTree(deptFlatList.value)
      if (currentDept.value) {
        const parent = findDeptInTree(deptTree.value, currentDept.value.deptId)
        childrenList.value = parent?.children || []
      }
    })
    .finally(() => {
      treeLoading.value = false
    })
}

function findDeptInTree(tree: any[], deptId: number): any {
  for (const node of tree) {
    if (node.deptId === deptId) return node
    if (node.children && node.children.length > 0) {
      const found = findDeptInTree(node.children, deptId)
      if (found) return found
    }
  }
  return undefined
}

function loadTreeselectOptions() {
  treeselect().then((res: any) => {
    deptOptions.value = [{ id: 0, label: '主类目', children: res.data || res.depts || [] }]
  })
}

function loadRegionOptions() {
  getRegionTree().then((res: any) => {
    regionOptions.value = res.data || []
  })
}

function findRegionNode(options: RegionOption[], value: string): RegionOption | undefined {
  for (const option of options) {
    if (option.value === value) return option
    if (option.children) {
      const child = findRegionNode(option.children, value)
      if (child) return child
    }
  }
  return undefined
}

function applyRegionSelection(values: string[]) {
  const fields = [
    ['provinceCode', 'provinceName'],
    ['cityCode', 'cityName'],
    ['districtCode', 'districtName'],
    ['streetCode', 'streetName'],
  ]
  fields.forEach(([codeKey, nameKey], index) => {
    const code = values[index] || ''
    form[codeKey] = code
    form[nameKey] = code ? findRegionNode(regionOptions.value, code)?.label || '' : ''
  })
}

function syncSelectedRegionFromForm() {
  selectedRegion.value = [form.provinceCode, form.cityCode, form.districtCode, form.streetCode].filter(Boolean)
}

function handleRegionChange(values: any) {
  applyRegionSelection(Array.isArray(values) ? values : [])
}

function handleNodeClick(data: any) {
  getDept(data.deptId).then((res: any) => {
    currentDept.value = res.data
    formMode.value = 'edit'
    resetFormData(res.data)
    childrenList.value = findDeptInTree(deptTree.value, data.deptId)?.children || []
  })
}

function selectChild(row: any) {
  nextTick(() => {
    treeRef.value?.setCurrentKey?.(row.deptId)
    handleNodeClick(row)
  })
}

function handleAddRoot() {
  currentDept.value = null
  childrenList.value = []
  formMode.value = 'add'
  resetFormData({ parentId: 0, orderNum: 0 })
  loadTreeselectOptions()
}

function handleAddChild() {
  if (!currentDept.value) return
  formMode.value = 'add'
  resetFormData({ parentId: currentDept.value.deptId, orderNum: 0 })
  loadTreeselectOptions()
}

function resetCurrent() {
  if (formMode.value === 'add') {
    const parentId = form.parentId
    resetFormData({ parentId, orderNum: 0 })
    return
  }
  if (form.deptId !== undefined) {
    const deptId = form.deptId
    getDept(deptId).then((res: any) => {
      resetFormData(res.data)
      formMode.value = 'edit'
    })
  }
}

function submitForm() {
  if (!formRef.value) return
  formRef.value.validate((valid: boolean) => {
    if (!valid) return
    if (form.deptId !== undefined) {
      updateDept(form).then(() => {
        ElMessage.success('修改成功')
        formMode.value = 'edit'
        loadDeptTree()
        loadTreeselectOptions()
        getDept(form.deptId).then((res: any) => {
          currentDept.value = res.data
          resetFormData(res.data)
        })
      })
    } else {
      addDept(form).then(() => {
        ElMessage.success('新增成功')
        loadDeptTree()
        loadTreeselectOptions()
        currentDept.value = null
        formMode.value = 'view'
        resetFormData()
      })
    }
  })
}

function handleDelete() {
  if (!form.deptId) return
  ElMessageBox.confirm(`是否确认删除部门名称为"${form.deptName}"的数据项？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => delDept(form.deptId))
    .then(() => {
      ElMessage.success('删除成功')
      resetFormData()
      currentDept.value = null
      childrenList.value = []
      formMode.value = 'view'
      loadDeptTree()
    })
    .catch(() => {})
}

onMounted(() => {
  loadDeptTree()
  loadTreeselectOptions()
  loadRegionOptions()
})
</script>

<style scoped>
.dept-maintenance {
  display: block;
}

.dept-maintenance > .el-row {
  display: flex;
  flex-wrap: nowrap;
  align-items: flex-start;
}

.dept-maintenance > .el-row > .el-col {
  display: flex;
  flex-direction: column;
}

/* 左侧：sticky + 固定高度，树内部滚动 */
.dept-maintenance > .el-row > .el-col:first-child {
  position: sticky;
  top: 8px;
  align-self: flex-start;
}

.dept-tree-card {
  height: calc(100vh - 104px - 24px);
  min-height: 500px;
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 6px;
}

.dept-tree-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.dept-tree-card :deep(.el-card__header) {
  flex-shrink: 0;
}

/* 右侧：自然内容高度，随页面滚动 */
.dept-maintenance > .el-row > .el-col:last-child {
  overflow: visible;
}

.dept-form-card {
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

.dept-form {
  margin-top: 8px;
}

.geo-display {
  margin-left: 12px;
  font-size: 13px;
  color: #909399;
}
</style>
