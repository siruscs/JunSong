<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="角色" prop="roleId">
        <el-select v-model="queryParams.roleId" placeholder="全部角色" clearable style="width: 200px">
          <el-option v-for="r in roles" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" plain :icon="Setting" @click="handleConfig" v-hasPermi="['member:mpPerm:add']">配置权限</el-button>
      <el-button type="success" plain :icon="Rank" @click="openSortDialog" v-hasPermi="['member:mpPerm:list']">功能模块调整</el-button>
      <el-button type="danger" plain :icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['member:mpPerm:remove']">删除</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="permList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="角色名称" align="center" prop="roleName" width="140">
        <template #default="scope">
          <el-tag>{{ scope.row.roleName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色标识" align="center" prop="roleKey" width="100" />
      <el-table-column label="会员服务模块" align="left" min-width="200">
        <template #default="scope">
          <template v-for="m in memberModules">
            <el-tag
              :key="m.key + '-enabled'"
              v-if="getModulesForRole(scope.row.roleId).includes(m.key)"
              size="small"
              effect="dark"
              type="success"
              style="margin: 2px 4px 2px 0;"
            >{{ m.name }}</el-tag>
            <el-tag
              :key="m.key + '-disabled'"
              v-else
              size="small"
              effect="plain"
              type="info"
              style="margin: 2px 4px 2px 0; opacity: 0.45;"
            >{{ m.name }}</el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="会员运营模块" align="left" min-width="180">
        <template #default="scope">
          <template v-for="m in operationModules">
            <el-tag
              :key="m.key + '-enabled'"
              v-if="getModulesForRole(scope.row.roleId).includes(m.key)"
              size="small"
              effect="dark"
              type="success"
              style="margin: 2px 4px 2px 0;"
            >{{ m.name }}</el-tag>
            <el-tag
              :key="m.key + '-disabled'"
              v-else
              size="small"
              effect="plain"
              type="info"
              style="margin: 2px 4px 2px 0; opacity: 0.45;"
            >{{ m.name }}</el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="财务管理模块" align="left" min-width="260">
        <template #default="scope">
          <template v-for="m in financeModules">
            <el-tag
              :key="m.key + '-enabled'"
              v-if="getModulesForRole(scope.row.roleId).includes(m.key)"
              size="small"
              effect="dark"
              type="warning"
              style="margin: 2px 4px 2px 0;"
            >{{ m.name }}</el-tag>
            <el-tag
              :key="m.key + '-disabled'"
              v-else
              size="small"
              effect="plain"
              type="info"
              style="margin: 2px 4px 2px 0; opacity: 0.45;"
            >{{ m.name }}</el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="系统管理模块" align="left" min-width="120">
        <template #default="scope">
          <template v-for="m in systemModules">
            <el-tag
              :key="m.key + '-enabled'"
              v-if="getModulesForRole(scope.row.roleId).includes(m.key)"
              size="small"
              effect="dark"
              type="danger"
              style="margin: 2px 4px 2px 0;"
            >{{ m.name }}</el-tag>
            <el-tag
              :key="m.key + '-disabled'"
              v-else
              size="small"
              effect="plain"
              type="info"
              style="margin: 2px 4px 2px 0; opacity: 0.45;"
            >{{ m.name }}</el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="移动办公模块" align="left" min-width="160">
        <template #default="scope">
          <template v-for="m in officeModules">
            <el-tag
              :key="m.key + '-enabled'"
              v-if="getModulesForRole(scope.row.roleId).includes(m.key)"
              size="small"
              effect="dark"
              type="primary"
              style="margin: 2px 4px 2px 0;"
            >{{ m.name }}</el-tag>
            <el-tag
              :key="m.key + '-disabled'"
              v-else
              size="small"
              effect="plain"
              type="info"
              style="margin: 2px 4px 2px 0; opacity: 0.45;"
            >{{ m.name }}</el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="已选" align="center" width="90">
        <template #default="scope">
          <span style="color: #409EFF; font-weight: bold;">{{ getModulesForRole(scope.row.roleId).length }}</span>
          <span style="color: #909399;">/{{ allModules.length }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template #default="scope">
          <el-button link type="primary" @click="handleConfigRole(scope.row)" v-hasPermi="['member:mpPerm:add']">配置</el-button>
          <el-button link type="danger" @click="handleDeleteRole(scope.row)" v-hasPermi="['member:mpPerm:remove']">清空</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="configTitle" v-model="configOpen" width="680px" append-to-body>
      <el-form ref="configForm" :model="configForm" :rules="configRules" label-width="100px">
        <el-form-item label="选择角色" prop="roleId">
          <el-select v-model="configForm.roleId" placeholder="请选择角色" style="width: 100%;" :disabled="configForm.mode === 'edit'" @change="onConfigRoleChange">
            <el-option v-for="r in roles" :key="r.roleId" :label="r.roleName + ' (' + r.roleKey + ')'" :value="r.roleId" />
          </el-select>
        </el-form-item>
        <el-form-item label="小程序模块" prop="moduleKeys">
          <div style="line-height: 1; margin-bottom: 10px; color: #909399; font-size: 12px;">
            勾选该角色在小程序中可以使用的功能模块（<b>高亮</b>为已选，<span style="opacity:0.5">灰色</span>为未选）
          </div>
          <el-divider content-position="left">会员服务</el-divider>
          <el-checkbox-group v-model="configForm.moduleKeys">
            <el-checkbox v-for="m in memberModules" :key="m.key" :label="m.key">{{ m.name }}</el-checkbox>
          </el-checkbox-group>
          <el-divider content-position="left">会员运营</el-divider>
          <el-checkbox-group v-model="configForm.moduleKeys">
            <el-checkbox v-for="m in operationModules" :key="m.key" :label="m.key">{{ m.name }}</el-checkbox>
          </el-checkbox-group>
          <el-divider content-position="left">财务管理</el-divider>
          <el-checkbox-group v-model="configForm.moduleKeys">
            <el-checkbox v-for="m in financeModules" :key="m.key" :label="m.key">{{ m.name }}</el-checkbox>
          </el-checkbox-group>
          <el-divider content-position="left">系统管理</el-divider>
          <el-checkbox-group v-model="configForm.moduleKeys">
            <el-checkbox v-for="m in systemModules" :key="m.key" :label="m.key">{{ m.name }}</el-checkbox>
          </el-checkbox-group>
          <el-divider content-position="left">移动办公</el-divider>
          <el-checkbox-group v-model="configForm.moduleKeys">
            <el-checkbox v-for="m in officeModules" :key="m.key" :label="m.key">{{ m.name }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitConfig" :loading="configLoading">保 存</el-button>
          <el-button @click="configOpen = false">取 消</el-button>
          <el-button type="info" plain @click="selectAllModules">全选</el-button>
          <el-button plain @click="configForm.moduleKeys = []">清空</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog title="功能模块调整" v-model="sortOpen" width="1100px" append-to-body @opened="onSortOpened" @close="onSortClosed">
      <div class="sort-tip">
        <el-icon color="#909399"><InfoFilled /></el-icon>
        拖动「分组标题栏」可调整大分组顺序；拖动任意「模块卡片」可调整组内顺序。保存后小程序端与 PC「小程序权限」列表都会按新顺序展示。
      </div>
      <div v-loading="sortLoading">
        <div class="sort-groups-container" ref="sortGroupsContainer">
          <div v-for="group in sortGroups" :key="group.name" class="sort-group" :data-group-name="group.name">
            <div class="sort-group-title sort-group-handle">
              <span class="sort-group-drag">
                <el-icon><Rank /></el-icon>
              </span>
              <span class="sort-group-dot" :style="{ background: groupColor(group.name) }"></span>
              <span class="sort-group-name">{{ group.name }}</span>
              <span class="sort-group-count">{{ group.items.length }} 个功能</span>
            </div>
            <div class="sort-group-list" :data-group="group.name">
              <div v-for="(mod, idx) in group.items" :key="mod.key" class="sort-item" :data-key="mod.key">
                <span class="sort-item-handle" title="拖动排序">
                  <el-icon><Rank /></el-icon>
                </span>
                <div class="sort-index">{{ idx + 1 }}</div>
                <div class="sort-body">
                  <div class="sort-name">{{ mod.name }}</div>
                  <div class="sort-key">{{ mod.key }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="sortOpen = false">取 消</el-button>
        <el-button @click="resetSortToDefault" plain>恢复默认顺序</el-button>
        <el-button type="primary" :loading="sortSaving" @click="submitSort">保存顺序</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from "element-plus"
import { Delete, Setting, Rank, InfoFilled } from "@element-plus/icons-vue"
import Sortable from "sortablejs"
import RightToolbar from "@/components/RightToolbar/index.vue"
import {
  listMpPerm,
  getMpPermRoles,
  getMpPermModules,
  saveMpPerm,
  deleteMpPerm,
  deleteMpPermByRole,
  getMpPermModuleSort,
  saveMpPermModuleSort
} from "@/api/member/mpPerm"

// 与后端 MpModuleCatalog#definitions() 保持一致；仅在接口无法返回时兜底
const DEFAULT_MODULES = [
  // 会员服务
  { key: "member", name: "会员管理", group: "会员服务" },
  { key: "memberPurchase", name: "购买记录", group: "会员服务" },
  { key: "memberPurchaseReturn", name: "退货/退款", group: "会员服务" },
  { key: "memberLevel", name: "等级配置", group: "会员服务" },
  { key: "campaignPolicy", name: "销售政策", group: "会员服务" },
  { key: "pointsGoods", name: "积分商品", group: "会员服务" },
  { key: "pointsRule", name: "积分规则", group: "会员服务" },
  { key: "pointsRecord", name: "积分记录", group: "会员服务" },
  { key: "pointsExchange", name: "积分兑换", group: "会员服务" },
  { key: "seckill", name: "秒杀活动", group: "会员服务" },
  { key: "seckillRecord", name: "秒杀记录", group: "会员服务" },
  // 系统管理
  { key: "userManage", name: "用户管理", group: "系统管理" },
  { key: "deptManage", name: "部门管理", group: "系统管理" },
  { key: "configSync", name: "配置同步", group: "系统管理" },
  // 会员运营
  { key: "dashboard", name: "会员运营看板", group: "会员运营" },
  { key: "growth", name: "成长体系", group: "会员运营" },
  { key: "actions", name: "增长动作", group: "会员运营" },
  { key: "points", name: "积分运营", group: "会员运营" },
  // 财务管理
  { key: "expense", name: "费用管理", group: "财务管理" },
  { key: "advance", name: "借支管理", group: "财务管理" },
  { key: "product", name: "商品管理", group: "财务管理" },
  { key: "supplier", name: "供应商管理", group: "财务管理" },
  { key: "purchase", name: "进货管理", group: "财务管理" },
  { key: "sale", name: "销售管理", group: "财务管理" },
  { key: "investorPayment", name: "投资人返款", group: "财务管理" },
  { key: "investor", name: "投资人管理", group: "财务管理" },
  { key: "investRecord", name: "投资款记录", group: "财务管理" },
  { key: "deptProfitConfig", name: "店面分润配置", group: "财务管理" },
  { key: "accountingPeriod", name: "核算周期", group: "财务管理" },
  { key: "profitShare", name: "分润结转", group: "财务管理" },
  { key: "costAccounting", name: "成本核算", group: "财务管理" },
  { key: "stockCost", name: "库存与成本", group: "财务管理" },
  { key: "stockAdjustment", name: "库存调整", group: "财务管理" },
  { key: "verificationRecord", name: "核销记录", group: "财务管理" },
  // 系统管理
  { key: "userManage", name: "用户管理", group: "系统管理" },
  { key: "deptManage", name: "部门管理", group: "系统管理" },
  // 移动办公
  { key: "wfTodo", name: "待办任务", group: "移动办公" },
  { key: "wfDone", name: "已办任务", group: "移动办公" },
  { key: "wfNotify", name: "消息通知", group: "移动办公" }
]

const GROUP_ORDER = ["会员服务", "会员运营", "财务管理", "系统管理", "移动办公"]
const GROUP_COLORS = {
  "会员服务": "#10B981",
  "会员运营": "#8B5CF6",
  "财务管理": "#F59E0B",
  "系统管理": "#EF4444",
  "移动办公": "#087CF0"
}

export default {
  name: "MpPerm",
  components: { RightToolbar },
  data() {
    return {
      Delete,
      Setting,
      Rank,
      InfoFilled,
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      permList: [],
      roles: [],
      rawPermRows: [],
      allModules: [],
      memberModules: [],
      operationModules: [],
      financeModules: [],
      systemModules: [],
      officeModules: [],
      queryParams: {
        roleId: undefined
      },
      configOpen: false,
      configTitle: "",
      configLoading: false,
      configForm: {
        roleId: undefined,
        deptId: undefined,
        moduleKeys: [],
        mode: "add"
      },
      configRules: {
        roleId: [
          { required: true, message: "请选择角色", trigger: "change" }
        ]
      },
      roleModuleMap: {},
      // ===== 功能模块调整 dialog =====
      sortOpen: false,
      sortLoading: false,
      sortSaving: false,
      sortGroups: [],
      _sortables: []
    }
  },
  created() {
    this.loadBaseData()
    this.getList()
  },
  beforeUnmount() {
    this.destroySortables()
  },
  methods: {
    groupColor(name) {
      return GROUP_COLORS[name] || "#909399"
    },
    setModules(modules) {
      const moduleMap = {}
      DEFAULT_MODULES.forEach(m => {
        moduleMap[m.key] = m
      })
      ;(modules || []).forEach(m => {
        if (m && m.key) {
          moduleMap[m.key] = m
        }
      })
      // 按 DEFAULT_MODULES 顺序 + 后端返回排序组合：先遍历后端返回顺序（已排好），
      // 再补上 DEFAULT 里有但后端没返回的，保证不漏项。
      const ordered = []
      const seen = new Set()
      ;(modules || []).forEach(m => {
        if (m && m.key && moduleMap[m.key] && !seen.has(m.key)) {
          ordered.push(moduleMap[m.key])
          seen.add(m.key)
        }
      })
      DEFAULT_MODULES.forEach(m => {
        if (!seen.has(m.key)) {
          ordered.push(m)
        }
      })
      this.allModules = ordered
      this.memberModules = ordered.filter(m => m.group === "会员服务")
      this.operationModules = ordered.filter(m => m.group === "会员运营")
      this.financeModules = ordered.filter(m => m.group === "财务管理")
      this.systemModules = ordered.filter(m => m.group === "系统管理")
      this.officeModules = ordered.filter(m => m.group === "移动办公")
    },
    loadBaseData() {
      this.setModules(DEFAULT_MODULES)
      getMpPermRoles().then(res => {
        this.roles = res.data || []
        this.refreshPermList()
      })
      getMpPermModules().then(res => {
        this.setModules(res.data || [])
      }).catch(() => {
        this.setModules(DEFAULT_MODULES)
      })
    },
    getList() {
      this.loading = true
      listMpPerm(this.queryParams).then(response => {
        this.rawPermRows = response.data || response.rows || []
        this.refreshPermList()
        this.loading = false
      })
    },
    refreshPermList() {
      const roleMap = {}
      this.roleModuleMap = {}
      const queryRoleId = this.queryParams.roleId ? String(this.queryParams.roleId) : ""
      this.roles
        .filter(r => !queryRoleId || String(r.roleId) === queryRoleId)
        .forEach(r => {
          roleMap[r.roleId] = {
            roleId: r.roleId,
            roleName: r.roleName,
            roleKey: r.roleKey || ""
          }
          this.roleModuleMap[r.roleId] = []
        })
      this.rawPermRows.forEach(r => {
        if (!roleMap[r.roleId]) {
          roleMap[r.roleId] = {
            roleId: r.roleId,
            roleName: r.roleName,
            roleKey: r.roleKey || "",
            id: r.id
          }
          this.roleModuleMap[r.roleId] = []
        }
        if (r.moduleKey && !this.roleModuleMap[r.roleId].includes(r.moduleKey)) {
          this.roleModuleMap[r.roleId].push(r.moduleKey)
        }
      })
      this.permList = Object.values(roleMap)
    },
    getModulesForRole(roleId) {
      return this.roleModuleMap[roleId] || []
    },
    handleQuery() {
      this.getList()
    },
    resetQuery() {
      this.queryParams.roleId = undefined
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.roleId)
      this.multiple = !selection.length
    },
    handleConfig() {
      this.configForm = { roleId: undefined, deptId: undefined, moduleKeys: [], mode: "add" }
      this.configTitle = "配置小程序权限"
      this.configOpen = true
    },
    handleConfigRole(row) {
      this.configForm = {
        roleId: row.roleId,
        deptId: undefined,
        moduleKeys: [...(this.roleModuleMap[row.roleId] || [])],
        mode: "edit"
      }
      this.configTitle = "配置「" + row.roleName + "」小程序权限"
      this.configOpen = true
    },
    onConfigRoleChange(roleId) {
      this.configForm.moduleKeys = [...(this.roleModuleMap[roleId] || [])]
    },
    selectAllModules() {
      this.configForm.moduleKeys = this.allModules.map(m => m.key)
    },
    submitConfig() {
      this.$refs["configForm"].validate(valid => {
        if (valid) {
          this.configLoading = true
          saveMpPerm({
            roleId: this.configForm.roleId,
            deptId: this.configForm.deptId,
            moduleKeys: this.configForm.moduleKeys
          }).then(() => {
            ElMessage.success("配置保存成功")
            this.configOpen = false
            this.getList()
          }).finally(() => {
            this.configLoading = false
          })
        }
      })
    },
    handleDelete(row) {
      const roleIds = row.roleId ? [row.roleId] : this.ids
      ElMessageBox.confirm('是否确认删除所选角色的全部小程序权限？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const promises = roleIds.map(rid => deleteMpPermByRole(rid, this.configForm.deptId || 0))
        return Promise.all(promises)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    handleDeleteRole(row) {
      ElMessageBox.confirm('是否确认清空「' + row.roleName + '」的全部小程序权限？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return deleteMpPermByRole(row.roleId, 0)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    // ===== 功能模块调整：对话框 + 拖拽 =====
    openSortDialog() {
      this.sortOpen = true
      this.sortLoading = true
      this.sortGroups = []
      getMpPermModuleSort().then(res => {
        // 后端返回结构已升级为 { definitions:[{key,name,group}], groupOrder:["会员服务",...] }
        // 兼容旧结构：若 data 直接是数组则视为 definitions，用本地 GROUP_ORDER 兜底。
        let definitions = DEFAULT_MODULES
        let groupOrder = GROUP_ORDER
        const raw = res && res.data
        if (Array.isArray(raw)) {
          definitions = raw.length ? raw : DEFAULT_MODULES
        } else if (raw && typeof raw === "object") {
          if (Array.isArray(raw.definitions) && raw.definitions.length) definitions = raw.definitions
          if (Array.isArray(raw.groupOrder) && raw.groupOrder.length) groupOrder = raw.groupOrder
        }
        this.buildSortGroups(definitions, groupOrder)
      }).catch(() => {
        this.buildSortGroups(DEFAULT_MODULES, GROUP_ORDER)
      }).finally(() => {
        this.sortLoading = false
      })
    },
    buildSortGroups(modules, groupOrder) {
      const orderList = Array.isArray(groupOrder) && groupOrder.length ? groupOrder : GROUP_ORDER
      const map = {}
      orderList.forEach(g => { map[g] = [] })
      ;(modules || []).forEach(m => {
        if (!m || !m.key || !m.group) return
        if (!map[m.group]) map[m.group] = []
        map[m.group].push({ key: m.key, name: m.name, group: m.group })
      })
      // 如果有在 DEFAULT_MODULES 里但 modules 没返回的，补加到对应分组末尾（兜底漏项）
      DEFAULT_MODULES.forEach(m => {
        const group = map[m.group] || []
        if (!group.some(it => it.key === m.key)) {
          group.push({ key: m.key, name: m.name, group: m.group })
        }
      })
      this.sortGroups = orderList
        .filter(g => map[g] && map[g].length > 0)
        .map(g => ({ name: g, items: map[g] }))
      // 兜底：把 DEFAULT 里出现过但 groupOrder 没列到的分组追加到末尾，避免漏展示。
      GROUP_ORDER.forEach(g => {
        if (map[g] && map[g].length && !this.sortGroups.some(x => x.name === g)) {
          this.sortGroups.push({ name: g, items: map[g] })
        }
      })
    },
    resetSortToDefault() {
      this.buildSortGroups(DEFAULT_MODULES, GROUP_ORDER)
      this.$nextTick(() => this.initSortables())
      ElMessage.info("已恢复到默认顺序，请点击【保存顺序】生效")
    },
    destroySortables() {
      if (this._sortables) {
        this._sortables.forEach(s => {
          try { s.destroy() } catch (_) {}
        })
      }
      this._sortables = []
      if (this._groupSortable) {
        try { this._groupSortable.destroy() } catch (_) {}
        this._groupSortable = null
      }
    },
    initSortables() {
      this.destroySortables()
      // 1. 分组级拖拽：.sort-groups-container 下每个 .sort-group 都是一个整体可拖
      //    注意：el-dialog append-to-body 时 DOM 会被搬运到 <body>，所以必须优先用 this.$refs 而不是 this.$el
      const groupContainer = this.$refs.sortGroupsContainer || this.$el.querySelector(".sort-groups-container")
      if (!groupContainer) return
      this._groupSortable = Sortable.create(groupContainer, {
        animation: 200,
        handle: ".sort-group-handle",
        filter: ".sort-group-list, .sort-item, .sort-item-handle",
        preventOnFilter: false,
        ghostClass: "sort-group--ghost",
        chosenClass: "sort-group--chosen",
        dragClass: "sort-group--drag",
        onEnd: () => {
          // 按当前 DOM 的 .sort-group[data-group-name] 顺序重排 sortGroups
          const names = Array.from(groupContainer.querySelectorAll(".sort-group[data-group-name]"))
            .map(n => n.getAttribute("data-group-name"))
          const byName = {}
          this.sortGroups.forEach(g => { byName[g.name] = g })
          const reordered = names.map(n => byName[n]).filter(Boolean)
          Object.values(byName).forEach(g => { if (!reordered.includes(g)) reordered.push(g) })
          this.sortGroups = reordered
        }
      })
      // 2. 组内模块级拖拽：每个分组的 .sort-group-list 自己独立拖
      //    必须从 groupContainer（真实挂载点）下查，不能 this.$el —— el-dialog append-to-body 会搬 DOM
      const containers = groupContainer.querySelectorAll(".sort-group-list")
      containers.forEach(el => {
        const s = Sortable.create(el, {
          animation: 180,
          forceFallback: true,
          fallbackOnBody: true,
          fallbackTolerance: 4,
          fallbackClass: "sort-item--fallback",
          ghostClass: "sort-item--ghost",
          chosenClass: "sort-item--chosen",
          dragClass: "sort-item--drag",
          onStart: (evt) => {
            if (evt && evt.originalEvent) {
              try { evt.originalEvent.stopPropagation() } catch (_) {}
            }
            try {
              const sel = window.getSelection ? window.getSelection() : null
              if (sel && typeof sel.removeAllRanges === "function") sel.removeAllRanges()
            } catch (_) {}
          },
          onEnd: () => {
            const dataGroup = el.getAttribute("data-group")
            const keys = Array.from(el.querySelectorAll(".sort-item[data-key]")).map(n => n.getAttribute("data-key"))
            const group = this.sortGroups.find(g => g.name === dataGroup)
            if (!group) return
            const byKey = {}
            group.items.forEach(it => { byKey[it.key] = it })
            const reordered = keys.map(k => byKey[k]).filter(Boolean)
            Object.values(byKey).forEach(it => {
              if (!reordered.includes(it)) reordered.push(it)
            })
            group.items = reordered
          }
        })
        this._sortables.push(s)
      })
    },
    onSortOpened() {
      this.$nextTick(() => this.initSortables())
    },
    onSortClosed() {
      this.destroySortables()
    },
    submitSort() {
      // 先按 sortGroups 当前顺序（分组拖拽已生效）写入 @GROUP@ 哨兵行；
      // 再按每个分组内部 items 顺序写入普通模块排序行。
      // 后端 saveBatch 会 deleteAll + 重新按入参顺序以 10 步进写入 sort_order，所以顺序就是入参前后顺序。
      const payload = []
      this.sortGroups.forEach(g => {
        payload.push({
          moduleKey: "@GROUP@" + g.name,
          groupName: "",
          remark: "分组哨兵行：控制大分组显示顺序"
        })
      })
      this.sortGroups.forEach(g => {
        g.items.forEach(it => {
          payload.push({ moduleKey: it.key, groupName: it.group || g.name })
        })
      })
      this.sortSaving = true
      saveMpPermModuleSort(payload).then(() => {
        ElMessage.success("模块与分组顺序保存成功，刷新后生效")
        this.sortOpen = false
        this.loadBaseData()
      }).finally(() => {
        this.sortSaving = false
      })
    }
  }
}
</script>

<style scoped>
.el-divider--horizontal {
  margin: 12px 0;
}

.sort-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 14px;
  background: #f4faff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}
.sort-groups-container {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.sort-group {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px 8px;
  background: #fcfcfd;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}
.sort-group-title {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  padding: 4px 6px;
  gap: 8px;
  border-radius: 6px;
  cursor: grab;
  user-select: none;
}
.sort-group-title:hover {
  background: #f4faff;
}
.sort-group-title:active {
  cursor: grabbing;
}
.sort-group-drag {
  display: flex;
  align-items: center;
  color: #c0c4cc;
  width: 18px;
}
.sort-group-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.sort-group-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 14px;
}
.sort-group-count {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}
.sort-group-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
.sort-item {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 6px 8px;
  cursor: grab;
  user-select: none;
  -webkit-user-select: none;
  transition: box-shadow 0.15s ease, border-color 0.15s ease, transform 0.15s ease;
  min-height: 44px;
}
.sort-item, .sort-item * {
  user-select: none !important;
  -webkit-user-select: none !important;
  -webkit-touch-callout: none !important;
}
.sort-item-handle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-right: 6px;
  color: #909399;
  cursor: grab;
  border-radius: 4px;
  flex-shrink: 0;
}
.sort-item-handle:hover {
  color: #409eff;
  background: #ecf5ff;
}
.sort-item-handle:active {
  cursor: grabbing;
}
.sort-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}
.sort-item:active {
  cursor: grabbing;
}
.sort-index {
  width: 20px;
  height: 20px;
  line-height: 20px;
  text-align: center;
  font-size: 11px;
  color: #fff;
  background: #c0c4cc;
  border-radius: 50%;
  margin-right: 8px;
  flex-shrink: 0;
}
.sort-body {
  flex: 1;
  min-width: 0;
}
.sort-name {
  color: #1f2937;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sort-key {
  color: #c0c4cc;
  font-size: 11px;
  line-height: 1.2;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sort-item--chosen {
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.25);
}
.sort-item--drag {
  opacity: 0.9;
  background: #ecf5ff;
}
.sort-item--ghost {
  opacity: 0.4;
  background: #f0f9eb;
  border: 1px dashed #67c23a;
}
.sort-group--chosen {
  border-color: #409eff;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.2);
}
.sort-group--ghost {
  opacity: 0.45;
  background: #f0f9eb;
  border: 1px dashed #67c23a;
}
.sort-group--drag {
  opacity: 0.92;
}
@media screen and (max-width: 1100px) {
  .sort-group-list { grid-template-columns: repeat(3, 1fr); }
}
@media screen and (max-width: 760px) {
  .sort-group-list { grid-template-columns: repeat(2, 1fr); }
}
@media screen and (max-width: 520px) {
  .sort-group-list { grid-template-columns: 1fr; }
}
</style>
