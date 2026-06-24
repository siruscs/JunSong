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
      <el-table-column label="已选" align="center" width="60">
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
          <el-divider content-position="left">财务管理</el-divider>
          <el-checkbox-group v-model="configForm.moduleKeys">
            <el-checkbox v-for="m in financeModules" :key="m.key" :label="m.key">{{ m.name }}</el-checkbox>
          </el-checkbox-group>
          <el-divider content-position="left">系统管理</el-divider>
          <el-checkbox-group v-model="configForm.moduleKeys">
            <el-checkbox v-for="m in systemModules" :key="m.key" :label="m.key">{{ m.name }}</el-checkbox>
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
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from "element-plus"
import { Delete, Setting } from "@element-plus/icons-vue"
import RightToolbar from "@/components/RightToolbar/index.vue"
import { listMpPerm, getMpPermRoles, getMpPermModules, saveMpPerm, deleteMpPerm, deleteMpPermByRole } from "@/api/member/mpPerm"

const DEFAULT_MODULES = [
  { key: "member", name: "会员管理", group: "会员服务" },
  { key: "pointsGoods", name: "积分商品", group: "会员服务" },
  { key: "pointsRecord", name: "积分记录", group: "会员服务" },
  { key: "pointsExchange", name: "积分兑换", group: "会员服务" },
  { key: "seckill", name: "秒杀活动", group: "会员服务" },
  { key: "seckillRecord", name: "秒杀记录", group: "会员服务" },
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
  { key: "userManage", name: "用户管理", group: "系统管理" },
  { key: "deptManage", name: "部门管理", group: "系统管理" }
]

export default {
  name: "MpPerm",
  components: { RightToolbar },
  data() {
    return {
      Delete,
      Setting,
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      permList: [],
      roles: [],
      rawPermRows: [],
      allModules: [],
      memberModules: [],
      financeModules: [],
      systemModules: [],
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
      roleModuleMap: {}
    }
  },
  created() {
    this.loadBaseData()
    this.getList()
  },
  methods: {
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
      this.allModules = Object.values(moduleMap)
      this.memberModules = this.allModules.filter(m => m.group === "会员服务")
      this.financeModules = this.allModules.filter(m => m.group === "财务管理")
      this.systemModules = this.allModules.filter(m => m.group === "系统管理")
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
    }
  }
}
</script>

<style scoped>
.el-divider--horizontal {
  margin: 12px 0;
}
</style>
