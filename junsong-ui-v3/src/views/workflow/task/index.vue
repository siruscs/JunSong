<template>
  <div class="workflow-runtime-page app-container">
    <section class="runtime-overview">
      <el-card v-for="card in overviewCards" :key="card.key" class="runtime-overview-card" shadow="hover">
        <div class="runtime-overview-card__label">{{ card.label }}</div>
        <div class="runtime-overview-card__value">{{ card.value }}</div>
        <div class="runtime-overview-card__hint">{{ card.hint }}</div>
      </el-card>
    </section>

    <el-card class="runtime-table-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待办任务" name="todo" />
        <el-tab-pane label="已办任务" name="done" />
        <el-tab-pane label="我发起的" name="applied" />
      </el-tabs>

      <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="88px" v-show="showSearch">
        <el-form-item :label="activeTab === 'applied' ? '流程名称' : '任务名称'" prop="keyword">
          <el-input
            v-model="queryParams.keyword"
            :placeholder="activeTab === 'applied' ? '请输入流程名称/标识' : '请输入任务名称'"
            clearable
            @keyup.enter="applyFilter"
          />
        </el-form-item>
        <el-form-item :label="activeTab === 'applied' ? '业务键' : '处理人'" prop="operator">
          <el-input
            v-model="queryParams.operator"
            :placeholder="activeTab === 'applied' ? '请输入业务键' : '请输入处理人'"
            clearable
            @keyup.enter="applyFilter"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item v-if="activeTab === 'todo'" label="排序">
          <el-select v-model="queryParams.sortBy" placeholder="排序方式" style="width: 140px" @change="loadCurrentTab">
            <el-option label="创建时间" value="createTime" />
            <el-option label="优先级" value="priority" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组视图">
          <el-switch v-model="groupView" active-text="按流程类型分组" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyFilter">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <RightToolbar v-model:showSearch="showSearch" @query="loadCurrentTab">
        <el-button
          v-if="activeTab === 'todo'"
          type="primary"
          :icon="Check"
          :disabled="!selectedRows.length"
          @click="openBatchApproveDialog"
          v-hasPermi="['workflow:task:approve']"
        >
          批量审批<template v-if="selectedRows.length">（{{ selectedRows.length }}）</template>
        </el-button>
        <el-button type="success" :icon="Refresh" @click="loadCurrentTab" v-hasPermi="['workflow:task:list']">
          刷新
        </el-button>
      </RightToolbar>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="displayRows"
        row-key="taskId"
        :span-method="tableSpanMethod"
        @selection-change="handleSelectionChange"
      >
        <template v-if="activeTab !== 'applied'">
          <el-table-column v-if="activeTab === 'todo'" type="selection" width="48" :selectable="canSelectRow" />
          <el-table-column label="任务名称" min-width="220">
            <template #default="{ row }">
              <div v-if="row.__isGroupHeader" class="runtime-group-header">
                <el-icon><Folder /></el-icon>
                <span class="runtime-group-header__name">{{ row.__groupName }}</span>
                <el-tag size="small" type="info" effect="plain">{{ row.__groupCount }} 项</el-tag>
              </div>
              <div v-else class="runtime-title">
                <el-button link type="primary" @click="openTaskDetail(row)">{{ safeWorkflowText(row.taskName) }}</el-button>
                <el-tag v-if="row.delegated" size="small" type="warning" style="margin-left: 6px">[代]{{ row.delegatorName }}</el-tag>
                <div class="runtime-title__sub">{{ row.taskId }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="流程实例 ID" prop="processInstanceId" min-width="200" show-overflow-tooltip />
          <el-table-column label="处理人" width="120">
            <template #default="{ row }">
              {{ safeWorkflowText(row.assignee || row.owner) }}
            </template>
          </el-table-column>
          <el-table-column :label="activeTab === 'todo' ? '创建时间' : '完成时间'" min-width="168">
            <template #default="{ row }">
              {{ formatWorkflowDateTime(activeTab === 'todo' ? row.createTime : row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="时长" width="130">
            <template #default="{ row }">
              {{ formatWorkflowDuration(row.durationMs) }}
            </template>
          </el-table-column>
          <el-table-column v-if="activeTab === 'todo'" label="优先级" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.priority != null" :type="priorityTagType(row.priority)" size="small">
                {{ priorityTagLabel(row.priority) }}
              </el-tag>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="mapWorkflowTaskTabStatus(activeTab, row).type">
                {{ mapWorkflowTaskTabStatus(activeTab, row).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="320" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTaskDetail(row)">详情</el-button>
              <el-button
                v-if="canOpenBusiness(row)"
                link
                type="primary"
                @click="goToBusiness(row)"
              >
                业务单
              </el-button>
              <el-button
                v-if="activeTab === 'todo' && canClaim(row)"
                link
                type="primary"
                @click="handleClaim(row)"
                v-hasPermi="['workflow:task:approve']"
              >
                认领
              </el-button>
              <el-button
                v-if="activeTab === 'todo'"
                link
                type="success"
                @click="openApproveDialog(row)"
                v-hasPermi="['workflow:task:approve']"
              >
                审批通过
              </el-button>
              <el-button
                v-if="activeTab === 'todo'"
                link
                type="danger"
                @click="openRejectDialog(row)"
                v-hasPermi="['workflow:task:reject']"
              >
                驳回
              </el-button>
              <el-button
                v-if="activeTab === 'todo'"
                link
                type="warning"
                @click="openTransferDialog(row)"
                v-hasPermi="['workflow:task:delegate']"
              >
                转办
              </el-button>
              <el-button link type="info" @click="goToHistory(row.processInstanceId)" v-hasPermi="['workflow:history:list']">
                历史
              </el-button>
            </template>
          </el-table-column>
        </template>

        <template v-else>
          <el-table-column label="流程名称" min-width="220">
            <template #default="{ row }">
              <div v-if="row.__isGroupHeader" class="runtime-group-header">
                <el-icon><Folder /></el-icon>
                <span class="runtime-group-header__name">{{ row.__groupName }}</span>
                <el-tag size="small" type="info" effect="plain">{{ row.__groupCount }} 项</el-tag>
              </div>
              <div v-else class="runtime-title">
                <el-button link type="primary" @click="goToHistory(row.processInstanceId)">
                  {{ safeWorkflowText(row.processDefinitionName || row.processDefinitionKey) }}
                </el-button>
                <div class="runtime-title__sub">{{ row.processInstanceId }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="流程标识" prop="processDefinitionKey" min-width="160" />
          <el-table-column label="业务键" prop="businessKey" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button v-if="canOpenBusiness(row)" link type="primary" @click="goToBusiness(row)">
                {{ safeWorkflowText(row.businessKey) }}
              </el-button>
              <span v-else>{{ safeWorkflowText(row.businessKey) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="开始时间" min-width="168">
            <template #default="{ row }">
              {{ formatWorkflowDateTime(row.startTime) }}
            </template>
          </el-table-column>
          <el-table-column label="结束时间" min-width="168">
            <template #default="{ row }">
              {{ formatWorkflowDateTime(row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="mapWorkflowTaskTabStatus(activeTab, row).type">
                {{ mapWorkflowTaskTabStatus(activeTab, row).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goToHistory(row.processInstanceId)">历史</el-button>
              <el-button v-if="canOpenBusiness(row)" link type="primary" @click="goToBusiness(row)">业务单</el-button>
            </template>
          </el-table-column>
        </template>
      </el-table>

      <el-empty v-if="!loading && !filteredRows.length" description="当前分页暂无任务数据" />
    </el-card>

    <el-drawer v-model="detailDrawer.visible" title="任务详情" size="720px">
      <el-skeleton :loading="detailDrawer.loading" animated :rows="10">
        <template #default>
          <template v-if="detailDrawer.data">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="任务名称">
                {{ safeWorkflowText(detailDrawer.data.taskName) }}
              </el-descriptions-item>
              <el-descriptions-item label="处理人">
                {{ safeWorkflowText(detailDrawer.data.assignee || detailDrawer.data.owner) }}
              </el-descriptions-item>
              <el-descriptions-item label="任务 ID" :span="2">
                {{ detailDrawer.data.taskId }}
              </el-descriptions-item>
              <el-descriptions-item label="流程实例 ID" :span="2">
                {{ detailDrawer.data.processInstanceId }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatWorkflowDateTime(detailDrawer.data.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="到期时间">
                {{ formatWorkflowDateTime(detailDrawer.data.dueDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="说明" :span="2">
                {{ safeWorkflowText(detailDrawer.data.description) }}
              </el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">流程变量摘要</el-divider>
            <el-descriptions :column="1" border v-if="taskVariableEntries.length">
              <el-descriptions-item v-for="entry in taskVariableEntries" :key="entry.key" :label="entry.key">
                {{ entry.value }}
              </el-descriptions-item>
            </el-descriptions>
            <el-empty v-else description="当前任务暂无流程变量" :image-size="72" />

            <el-divider content-position="left">原始变量 JSON</el-divider>
            <el-input :model-value="rawVariablesText" type="textarea" :rows="8" readonly />

            <el-divider content-position="left">附件</el-divider>
            <div v-if="detailDrawer.data.attachments?.length">
              <div v-for="att in detailDrawer.data.attachments" :key="att.id" class="attachment-item">
                <el-link :href="att.fileUrl" target="_blank" type="primary">
                  <el-icon><Document /></el-icon>
                  {{ att.fileName }}
                </el-link>
                <el-tag size="small" type="info" style="margin-left: 8px">{{ att.actionType === 'approve' ? '审批' : '驳回' }}</el-tag>
              </div>
            </div>
            <el-empty v-else description="暂无附件" :image-size="72" />

            <el-divider content-position="left">审批历史</el-divider>
            <el-timeline v-if="historyComments.length" class="comment-timeline">
              <el-timeline-item
                v-for="comment in historyComments"
                :key="comment.id || `${comment.time}-${comment.userId}`"
                :timestamp="formatWorkflowDateTime(comment.time)"
                :type="commentTimelineType(comment.type)"
                placement="top"
              >
                <div class="comment-item">
                  <div class="comment-head">
                    <span class="comment-user">{{ safeWorkflowText(comment.userId) }}</span>
                    <el-tag :type="commentTagType(comment.type)" size="small" effect="light">
                      {{ mapWorkflowCommentType(comment.type) }}
                    </el-tag>
                  </div>
                  <div class="comment-msg">{{ safeWorkflowText(comment.message) }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无审批历史" :image-size="72" />

            <!-- 会签进度 -->
            <template v-if="detailDrawer.data.multiInstance">
              <el-divider content-position="left">会签进度</el-divider>
              <div class="multi-instance-progress">
                <div class="progress-header">
                  进度：{{ detailDrawer.data.multiInstance.completed }}/{{ detailDrawer.data.multiInstance.total }}
                </div>
                <el-progress
                  :percentage="Math.round((detailDrawer.data.multiInstance.completed / detailDrawer.data.multiInstance.total) * 100)"
                  :status="detailDrawer.data.multiInstance.completed === detailDrawer.data.multiInstance.total ? 'success' : ''"
                />
                <div class="instance-list">
                  <div
                    v-for="inst in detailDrawer.data.multiInstance.instances"
                    :key="inst.taskId"
                    class="instance-item"
                  >
                    <el-avatar :size="24" />
                    <span class="instance-name">{{ inst.assigneeName || inst.assignee }}</span>
                    <el-tag v-if="inst.completed" type="success" size="small">已审批</el-tag>
                    <el-tag v-else type="warning" size="small">待审批</el-tag>
                  </div>
                </div>
              </div>
            </template>

            <div class="runtime-drawer-actions">
              <el-button
                v-if="canOpenBusiness(detailDrawer.data)"
                type="primary"
                @click="goToBusiness(detailDrawer.data)"
              >
                查看业务单
              </el-button>
              <el-button type="primary" plain @click="goToHistory(detailDrawer.data.processInstanceId)">
                查看历史记录
              </el-button>
              <template v-if="activeTab === 'todo' && detailDrawer.data.assignee">
                <el-divider direction="vertical" />
                <el-button
                  v-if="isRejectedToInitiator(detailDrawer.data)"
                  type="primary"
                  @click="openResubmitDialog"
                >
                  修改并重新提交
                </el-button>
                <el-button type="success" @click="openApproveFromDetail">
                  审批通过
                </el-button>
                <el-button type="danger" plain @click="openRejectFromDetail">
                  驳回
                </el-button>
                <el-button type="warning" plain @click="openAddSignDialog">
                  加签
                </el-button>
              </template>
              <el-divider direction="vertical" />
              <el-button type="warning" plain @click="openUrgeDialog">
                催办
              </el-button>
              <el-button type="info" plain @click="openCcDialog">
                抄送
              </el-button>
            </div>
          </template>
        </template>
      </el-skeleton>
    </el-drawer>

    <el-dialog v-model="approveDialog.visible" title="审批通过" width="600px">
      <el-form label-width="88px">
        <el-form-item label="审批意见">
          <div class="common-comments">
            <el-tag
              v-for="c in commonComments"
              :key="c"
              class="common-comment-tag"
              effect="plain"
              @click="approveDialog.form.comment = c"
            >
              {{ c }}
            </el-tag>
          </div>
          <el-input v-model="approveDialog.form.comment" type="textarea" :rows="4" placeholder="请输入审批意见" />
        </el-form-item>
        <el-form-item label="附件">
          <FileUpload v-model="approveDialog.form.attachments" :limit="5" :file-size="50" />
        </el-form-item>
        <el-form-item label="变量 JSON">
          <el-input
            v-model="approveDialog.form.variablesText"
            type="textarea"
            :rows="4"
            placeholder='如需传变量，请输入 JSON，例如：{"approved":true}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialog.visible" title="驳回任务" width="600px">
      <el-form label-width="100px" class="runtime-form-margin">
        <el-form-item label="驳回目标" required>
          <el-radio-group v-model="rejectDialog.form.targetActivityId">
            <el-radio-button
              v-for="t in rejectDialog.targets"
              :key="t.activityId"
              :label="t.activityId"
            >
              {{ t.typeLabel }}（{{ t.activityName }}）
            </el-radio-button>
          </el-radio-group>
          <div v-if="rejectDialog.targets.length === 0 && !rejectDialog.loading" class="text-muted" style="margin-top: 8px;">
            暂无可驳回目标，将终止流程实例
          </div>
        </el-form-item>
        <el-form-item label="驳回意见">
          <div class="common-comments">
            <el-tag
              v-for="c in commonComments"
              :key="c"
              class="common-comment-tag"
              type="danger"
              effect="plain"
              @click="rejectDialog.form.comment = c"
            >
              {{ c }}
            </el-tag>
          </div>
          <el-input v-model="rejectDialog.form.comment" type="textarea" :rows="4" placeholder="请输入驳回原因" />
        </el-form-item>
        <el-form-item label="附件">
          <FileUpload v-model="rejectDialog.form.attachments" :limit="5" :file-size="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" @click="submitReject">
          {{ rejectDialog.form.targetActivityId ? '确认驳回' : '确认终止流程' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferDialog.visible" title="转办任务" width="480px">
      <el-form label-width="88px">
        <el-form-item label="目标用户">
          <el-input v-model="transferDialog.form.toUser" placeholder="请输入接收任务的用户名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer">确认转办</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="urgeDialog.visible" title="催办任务" width="480px">
      <el-form label-width="88px">
        <el-form-item label="催办意见">
          <el-input v-model="urgeDialog.form.comment" type="textarea" :rows="4" placeholder="请输入催办原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="urgeDialog.visible = false">取消</el-button>
        <el-button type="warning" @click="submitUrge">确认催办</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ccDialog.visible" title="抄送流程" width="520px">
      <el-form label-width="88px">
        <el-form-item label="抄送人">
          <el-select
            v-model="ccDialog.form.toUsers"
            multiple
            filterable
            remote
            reserve-keyword
            :remote-method="searchUsers"
            :loading="userSearchLoading"
            placeholder="输入用户名搜索"
            style="width: 100%"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.userName"
              :label="u.nickName || u.userName"
              :value="u.userName"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ccDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitCc">确认抄送</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addSignDialog.visible" title="加签任务" width="480px">
      <el-form label-width="88px">
        <el-form-item label="加签人">
          <el-select
            v-model="addSignDialog.form.addSignUser"
            filterable
            remote
            reserve-keyword
            :remote-method="searchUsers"
            :loading="userSearchLoading"
            placeholder="输入用户名搜索"
            style="width: 100%"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.userName"
              :label="u.nickName || u.userName"
              :value="u.userName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="加签类型">
          <el-radio-group v-model="addSignDialog.form.type">
            <el-radio label="before">前加签（先让加签人审批）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addSignDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitAddSign">确认加签</el-button>
      </template>
    </el-dialog>

    <SchemaForm
      v-model="resubmitDialog.visible"
      :biz-code="resubmitDialog.bizCode"
      :record-id="resubmitDialog.recordId"
      @saved="onResubmitSaved"
    />

    <el-dialog v-model="batchApproveDialog.visible" title="批量审批" width="520px">
      <el-alert
        :title="`已选择 ${selectedRows.length} 个任务，将使用统一审批意见完成审批`"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-form label-width="88px">
        <el-form-item label="审批意见">
          <div class="common-comments">
            <el-tag
              v-for="c in commonComments"
              :key="c"
              class="common-comment-tag"
              effect="plain"
              @click="batchApproveDialog.form.comment = c"
            >
              {{ c }}
            </el-tag>
          </div>
          <el-input v-model="batchApproveDialog.form.comment" type="textarea" :rows="4" placeholder="请输入统一审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchApproveDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="batchApproveDialog.loading" @click="submitBatchApprove">
          确认批量审批
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Document, Folder, Check } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import FileUpload from '@/components/FileUpload/index.vue'
import SchemaForm from '@/views/lowcode/SchemaForm.vue'
import { resetForm as resetFormUtil } from '@/utils/junsong'
import {
  approveWorkflowTask,
  batchApprove,
  claimWorkflowTask,
  getRejectTargets,
  getWorkflowTaskDetail,
  listAppliedWorkflowTasks,
  listDoneWorkflowTasks,
  listTodoWorkflowTasks,
  rejectWorkflowTask,
  transferWorkflowTask,
  type RejectTarget,
  type WorkflowAppliedTaskRow,
  type WorkflowDoneTaskRow,
  type WorkflowTaskDetail,
} from '@/api/workflow/task'
import { urgeWorkflowTask } from '@/api/workflow/urge'
import { ccWorkflowTask } from '@/api/workflow/cc'
import { addSignWorkflowTask } from '@/api/workflow/addsign'
import { listWorkflowHistoryComments } from '@/api/workflow/history'
import { listUser } from '@/api/system/user'
import type { WorkflowTodoTaskRow } from '@/api/workflow/task'
import { formatWorkflowDateTime, formatWorkflowDuration, mapWorkflowCommentType, mapWorkflowTaskTabStatus, resolveWorkflowBusinessTarget, safeWorkflowText } from '../shared/runtime'

type TaskTab = 'todo' | 'done' | 'applied'
type TaskRow = (WorkflowTodoTaskRow | WorkflowDoneTaskRow | WorkflowAppliedTaskRow) & Record<string, any>

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const showSearch = ref(true)
const queryFormRef = ref()
const activeTab = ref<TaskTab>('todo')
const rows = ref<TaskRow[]>([])

const detailDrawer = reactive({
  visible: false,
  loading: false,
  data: null as WorkflowTaskDetail | null,
})

const approveDialog = reactive({
  visible: false,
  row: null as WorkflowTodoTaskRow | null,
  form: {
    comment: '',
    variablesText: '',
    attachments: [] as string[],
  },
})

const rejectDialog = reactive({
  visible: false,
  row: null as WorkflowTodoTaskRow | null,
  loading: false,
  targets: [] as RejectTarget[],
  form: {
    comment: '',
    targetActivityId: '',
    attachments: [] as string[],
  },
})

const transferDialog = reactive({
  visible: false,
  row: null as WorkflowTodoTaskRow | null,
  form: {
    toUser: '',
  },
})

const urgeDialog = reactive({
  visible: false,
  row: null as WorkflowTodoTaskRow | null,
  form: {
    comment: '',
  },
})

const ccDialog = reactive({
  visible: false,
  row: null as WorkflowTodoTaskRow | null,
  form: {
    toUsers: [] as string[],
  },
})

const addSignDialog = reactive({
  visible: false,
  row: null as WorkflowTodoTaskRow | null,
  form: {
    addSignUser: '',
    type: 'before',
  },
})

const batchApproveDialog = reactive({
  visible: false,
  loading: false,
  form: {
    comment: '',
  },
})

const historyComments = ref<any[]>([])

const resubmitDialog = reactive({
  visible: false,
  bizCode: '',
  recordId: null as number | null,
  taskId: '' as string,
})

const userOptions = ref<any[]>([])
const userSearchLoading = ref(false)
let userSearchRequestId = 0

async function searchUsers(keyword = '') {
  const requestId = ++userSearchRequestId
  userSearchLoading.value = true
  try {
    const res: any = await listUser({ userName: keyword.trim() || undefined, pageNum: 1, pageSize: 20 })
    if (requestId !== userSearchRequestId) return
    userOptions.value = res.rows || []
  } finally {
    if (requestId === userSearchRequestId) userSearchLoading.value = false
  }
}

const queryParams = reactive({
  keyword: '',
  operator: '',
  dateRange: [] as string[],
  sortBy: 'createTime' as 'createTime' | 'priority',
})

const groupView = ref(false)
const selectedRows = ref<WorkflowTodoTaskRow[]>([])
const tableRef = ref()
const commonComments = ['同意', '同意，请继续', '已核实，通过', '资料齐全，通过', '不同意', '需补充材料']

const filteredRows = computed(() => {
  return rows.value.filter((item) => {
    const keyword = queryParams.keyword.trim()
    const operator = queryParams.operator.trim()

    if (keyword) {
      const matched =
        String(item.taskName || item.processDefinitionName || '').includes(keyword)
        || String(item.processDefinitionKey || '').includes(keyword)
      if (!matched) return false
    }

    if (operator) {
      const matched =
        String(item.assignee || item.owner || '').includes(operator)
        || String(item.businessKey || '').includes(operator)
      if (!matched) return false
    }

    if (queryParams.dateRange?.length === 2) {
      const sourceTime = item.createTime || item.startTime || item.endTime
      const time = sourceTime ? new Date(sourceTime).getTime() : NaN
      const start = new Date(`${queryParams.dateRange[0]} 00:00:00`).getTime()
      const end = new Date(`${queryParams.dateRange[1]} 23:59:59`).getTime()
      if (!Number.isNaN(time) && (time < start || time > end)) return false
    }

    return true
  })
})

const sortedRows = computed(() => {
  const data = [...filteredRows.value]
  if (queryParams.sortBy === 'priority') {
    const hasPriority = data.some((r) => r.priority != null)
    if (hasPriority) {
      data.sort((a, b) => (Number(b.priority) || 0) - (Number(a.priority) || 0))
    }
  }
  return data
})

const displayRows = computed(() => {
  const data = sortedRows.value
  if (!groupView.value) return data
  const groups = new Map<string, TaskRow[]>()
  for (const row of data) {
    const name = safeWorkflowText((row as any).processDefinitionName || (row as any).processDefinitionKey) || '未分组'
    if (!groups.has(name)) groups.set(name, [])
    groups.get(name)!.push(row)
  }
  const result: any[] = []
  for (const [name, groupRows] of groups) {
    result.push({
      __isGroupHeader: true,
      __groupName: name,
      __groupCount: groupRows.length,
      taskId: `__group__${name}`,
    })
    result.push(...groupRows)
  }
  return result
})

const groupStartCol = computed(() => (activeTab.value === 'todo' ? 1 : 0))

const groupTotalCols = computed(() => {
  if (activeTab.value === 'todo') return 9
  return 7
})

const overviewCards = computed(() => {
  const items = filteredRows.value
  return [
    {
      key: 'total',
      label: activeTab.value === 'applied' ? '我发起的流程数' : '当前任务数',
      value: items.length,
      hint: '基于当前 Tab 与筛选结果汇总',
    },
    {
      key: 'processable',
      label: activeTab.value === 'todo' ? '待处理任务' : activeTab.value === 'done' ? '已处理任务' : '运行中流程',
      value:
        activeTab.value === 'todo'
          ? items.length
          : activeTab.value === 'done'
            ? items.length
            : items.filter((item) => item.running === true || !item.endTime).length,
      hint: activeTab.value === 'todo' ? '可直接审批处理' : '用于快速定位进展',
    },
    {
      key: 'finished',
      label: activeTab.value === 'applied' ? '已结束流程' : '已分配任务',
      value:
        activeTab.value === 'applied'
          ? items.filter((item) => item.endTime).length
          : items.filter((item) => item.assignee || item.owner).length,
      hint: '帮助判断当前工作分布',
    },
    {
      key: 'recent',
      label: '近 7 天记录',
      value: items.filter((item) => {
        const sourceTime = item.createTime || item.startTime || item.endTime
        if (!sourceTime) return false
        return Date.now() - new Date(sourceTime).getTime() <= 1000 * 60 * 60 * 24 * 7
      }).length,
      hint: '观察最近一周流转活跃度',
    },
  ]
})

const taskVariableEntries = computed(() => {
  const variables = detailDrawer.data?.variables || {}
  return Object.entries(variables).map(([key, value]) => ({
    key,
    value: typeof value === 'object' ? JSON.stringify(value) : safeWorkflowText(value as any),
  }))
})

const rawVariablesText = computed(() => JSON.stringify(detailDrawer.data?.variables || {}, null, 2))

function canClaim(row: WorkflowTodoTaskRow) {
  return !row.assignee
}

function applyFilter() {
  rows.value = [...rows.value]
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.operator = ''
  queryParams.dateRange = []
  resetFormUtil(queryFormRef.value)
  applyFilter()
}

function canSelectRow(row: any) {
  return !row.__isGroupHeader
}

function handleSelectionChange(val: WorkflowTodoTaskRow[]) {
  selectedRows.value = val
}

function tableSpanMethod(params: { row: any; columnIndex: number }) {
  const { row, columnIndex } = params
  if (row.__isGroupHeader) {
    if (columnIndex === groupStartCol.value) {
      return { rowspan: 1, colspan: groupTotalCols.value - groupStartCol.value }
    }
    return { rowspan: 0, colspan: 0 }
  }
}

function priorityTagType(priority?: number | null) {
  if (priority == null) return 'info'
  if (priority >= 75) return 'danger'
  if (priority >= 50) return 'warning'
  return 'info'
}

function priorityTagLabel(priority?: number | null) {
  if (priority == null) return '普通'
  if (priority >= 75) return '特急'
  if (priority >= 50) return '紧急'
  return '普通'
}

function openBatchApproveDialog() {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先勾选需要批量审批的任务')
    return
  }
  batchApproveDialog.visible = true
  batchApproveDialog.form.comment = ''
}

async function submitBatchApprove() {
  const taskIds = selectedRows.value.map((r) => r.taskId)
  if (!taskIds.length) return
  batchApproveDialog.loading = true
  try {
    await batchApprove(taskIds, batchApproveDialog.form.comment || '')
    batchApproveDialog.visible = false
    ElMessage.success(`已批量审批 ${taskIds.length} 个任务`)
    selectedRows.value = []
    tableRef.value?.clearSelection()
    loadCurrentTab()
  } finally {
    batchApproveDialog.loading = false
  }
}

async function loadCurrentTab() {
  loading.value = true
  try {
    if (activeTab.value === 'todo') {
      const res = await listTodoWorkflowTasks(queryParams.sortBy || undefined)
      rows.value = res.data || []
      return
    }
    if (activeTab.value === 'done') {
      const res = await listDoneWorkflowTasks()
      rows.value = res.data || []
      return
    }
    const res = await listAppliedWorkflowTasks()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  resetQuery()
  loadCurrentTab()
}

async function openTaskDetail(row: TaskRow) {
  detailDrawer.visible = true
  detailDrawer.loading = true
  historyComments.value = []
  try {
    detailDrawer.data = await getWorkflowTaskDetail(row.taskId).then((res) => res.data)
    const inlineComments = (detailDrawer.data as any)?.comments
    if (Array.isArray(inlineComments) && inlineComments.length) {
      historyComments.value = inlineComments
    } else if (row.processInstanceId) {
      try {
        const res: any = await listWorkflowHistoryComments(row.processInstanceId)
        historyComments.value = res.data || []
      } catch {
        historyComments.value = []
      }
    }
  } finally {
    detailDrawer.loading = false
  }
}

function goToHistory(processInstanceId?: string) {
  if (!processInstanceId) return
  router.push({
    path: '/workflow/history',
    query: { processInstanceId },
  })
}

function canOpenBusiness(row?: Partial<TaskRow> | null) {
  return !!resolveWorkflowBusinessTarget(row?.processDefinitionKey, row?.businessKey)
}

function goToBusiness(row?: Partial<TaskRow> | null) {
  const target = resolveWorkflowBusinessTarget(row?.processDefinitionKey, row?.businessKey)
  if (!target) return
  router.push(target)
}

async function handleClaim(row: WorkflowTodoTaskRow) {
  await claimWorkflowTask(row.taskId)
  ElMessage.success('任务已认领')
  loadCurrentTab()
}

function isRejectedToInitiator(task: WorkflowTaskDetail | null): boolean {
  if (!task) return false
  const anyTask = task as any
  const activityName = String(anyTask.activityName || anyTask.taskName || '')
  const isInitNode = /发起|submit|开始/i.test(activityName)
  const comments: any[] = Array.isArray(anyTask.comments) ? anyTask.comments : []
  const hasRejectComment = comments.some((c: any) => {
    const t = String(c?.type || '').toLowerCase()
    return t === 'reject' || t === 'rejected'
  })
  return isInitNode || hasRejectComment
}

function resolveResubmitTarget(task: WorkflowTaskDetail | null) {
  if (!task) return null
  const variables = (task.variables || {}) as Record<string, any>
  const bizCode = variables.bizCode || variables.biz_code || task.processDefinitionKey
  let recordId: any = variables.recordId ?? variables.record_id ?? variables.businessId ?? variables.business_id
  if (recordId == null && task.businessKey) {
    const parsed = Number(task.businessKey)
    if (!Number.isNaN(parsed)) recordId = parsed
  }
  const numericRecordId = recordId != null ? Number(recordId) : NaN
  if (!bizCode || Number.isNaN(numericRecordId)) return null
  return { bizCode, recordId: numericRecordId }
}

function openResubmitDialog() {
  if (!detailDrawer.data) return
  const target = resolveResubmitTarget(detailDrawer.data)
  if (!target) {
    ElMessage.warning('未关联可编辑的业务单据，无法修改并重新提交')
    return
  }
  resubmitDialog.bizCode = target.bizCode
  resubmitDialog.recordId = target.recordId
  resubmitDialog.taskId = detailDrawer.data.taskId
  resubmitDialog.visible = true
}

function onResubmitSaved() {
  if (resubmitDialog.visible) return
  doResubmitApprove()
}

async function doResubmitApprove() {
  if (!resubmitDialog.taskId) return
  try {
    await approveWorkflowTask(resubmitDialog.taskId, { comment: '已修改重新提交' })
    ElMessage.success('已修改并重新提交')
    detailDrawer.visible = false
    loadCurrentTab()
  } catch (e: any) {
    ElMessage.error(e?.message || '重新提交失败，请稍后重试')
  }
}

function commentTagType(type?: string | null) {
  const t = String(type || '').toLowerCase()
  if (t === 'approve') return 'success'
  if (t === 'reject') return 'danger'
  if (t === 'transfer' || t === 'delegate') return 'warning'
  if (t === 'comment') return 'info'
  return 'info'
}

function commentTimelineType(type?: string | null) {
  const t = String(type || '').toLowerCase()
  if (t === 'approve') return 'success'
  if (t === 'reject') return 'danger'
  if (t === 'transfer' || t === 'delegate') return 'warning'
  return 'primary'
}

function openApproveDialog(row: WorkflowTodoTaskRow) {
  approveDialog.visible = true
  approveDialog.row = row
  approveDialog.form.comment = ''
  approveDialog.form.variablesText = ''
  approveDialog.form.attachments = []
}

async function openRejectDialog(row: WorkflowTodoTaskRow) {
  rejectDialog.visible = true
  rejectDialog.row = row
  rejectDialog.form.comment = ''
  rejectDialog.form.targetActivityId = ''
  rejectDialog.form.attachments = []
  rejectDialog.targets = []
  rejectDialog.loading = true
  try {
    const res: any = await getRejectTargets(row.taskId)
    rejectDialog.targets = res.data || []
    if (rejectDialog.targets.length > 0) {
      rejectDialog.form.targetActivityId = rejectDialog.targets[0].activityId
    }
  } finally {
    rejectDialog.loading = false
  }
}

function openTransferDialog(row: WorkflowTodoTaskRow) {
  transferDialog.visible = true
  transferDialog.row = row
  transferDialog.form.toUser = ''
}

function openApproveFromDetail() {
  if (!detailDrawer.data) return
  const row: any = { taskId: detailDrawer.data.taskId }
  approveDialog.visible = true
  approveDialog.row = row as WorkflowTodoTaskRow
  approveDialog.form.comment = ''
  approveDialog.form.variablesText = ''
  approveDialog.form.attachments = []
}

async function openRejectFromDetail() {
  if (!detailDrawer.data) return
  const row: any = { taskId: detailDrawer.data.taskId }
  rejectDialog.visible = true
  rejectDialog.row = row as WorkflowTodoTaskRow
  rejectDialog.form.comment = ''
  rejectDialog.form.targetActivityId = ''
  rejectDialog.form.attachments = []
  rejectDialog.targets = []
  rejectDialog.loading = true
  try {
    const res: any = await getRejectTargets(row.taskId)
    rejectDialog.targets = res.data || []
    if (rejectDialog.targets.length > 0) {
      rejectDialog.form.targetActivityId = rejectDialog.targets[0].activityId
    }
  } finally {
    rejectDialog.loading = false
  }
}

function parseVariablesInput(raw: string) {
  const text = raw.trim()
  if (!text) return undefined
  try {
    return JSON.parse(text)
  } catch {
    throw new Error('变量 JSON 格式不正确')
  }
}

function buildAttachmentsPayload(urls: string[]): any[] | undefined {
  if (!urls || urls.length === 0) return undefined
  return urls.map((url) => {
    const name = url.split('/').pop() || 'file'
    return { fileName: decodeURIComponent(name), fileUrl: url }
  })
}

async function submitApprove() {
  if (!approveDialog.row) return
  const variables = parseVariablesInput(approveDialog.form.variablesText)
  await approveWorkflowTask(approveDialog.row.taskId, {
    comment: approveDialog.form.comment || undefined,
    variables,
    attachments: buildAttachmentsPayload(approveDialog.form.attachments),
  })
  approveDialog.visible = false
  ElMessage.success('任务已审批通过')
  loadCurrentTab()
}

async function submitReject() {
  if (!rejectDialog.row) return
  const hasTarget = !!rejectDialog.form.targetActivityId
  const confirmText = hasTarget
    ? `确认驳回至【${rejectDialog.targets.find((t: any) => t.activityId === rejectDialog.form.targetActivityId)?.activityName || rejectDialog.form.targetActivityId}】吗？`
    : '未选择驳回目标，将终止整个流程实例，确认继续吗？'
  await ElMessageBox.confirm(confirmText, '驳回确认', {
    type: 'warning',
    confirmButtonText: hasTarget ? '确认驳回' : '确认终止',
    cancelButtonText: '取消',
  })
  await rejectWorkflowTask(rejectDialog.row.taskId, {
    comment: rejectDialog.form.comment || undefined,
    targetActivityId: rejectDialog.form.targetActivityId || undefined,
    attachments: buildAttachmentsPayload(rejectDialog.form.attachments),
  })
  rejectDialog.visible = false
  ElMessage.success(hasTarget ? '任务已驳回至指定节点' : '任务已驳回，流程已终止')
  loadCurrentTab()
}

async function submitTransfer() {
  if (!transferDialog.row) return
  if (!transferDialog.form.toUser.trim()) {
    ElMessage.warning('请输入目标用户名')
    return
  }
  await transferWorkflowTask(transferDialog.row.taskId, transferDialog.form.toUser.trim())
  transferDialog.visible = false
  ElMessage.success('任务已转办')
  loadCurrentTab()
}

function openUrgeDialog() {
  if (!detailDrawer.data) return
  const row: any = { taskId: detailDrawer.data.taskId }
  urgeDialog.visible = true
  urgeDialog.row = row as WorkflowTodoTaskRow
  urgeDialog.form.comment = ''
}

async function submitUrge() {
  if (!urgeDialog.row) return
  await urgeWorkflowTask(urgeDialog.row.taskId, urgeDialog.form.comment || undefined)
  urgeDialog.visible = false
  ElMessage.success('催办已发送')
}

async function openCcDialog() {
  if (!detailDrawer.data) return
  const row: any = { taskId: detailDrawer.data.taskId }
  ccDialog.visible = true
  ccDialog.row = row as WorkflowTodoTaskRow
  ccDialog.form.toUsers = []
  await searchUsers()
}

async function submitCc() {
  if (!ccDialog.row) return
  if (ccDialog.form.toUsers.length === 0) {
    ElMessage.warning('请选择抄送人')
    return
  }
  await ccWorkflowTask(ccDialog.row.taskId, ccDialog.form.toUsers)
  ccDialog.visible = false
  ElMessage.success('抄送已发送')
}

async function openAddSignDialog() {
  if (!detailDrawer.data) return
  const row: any = { taskId: detailDrawer.data.taskId }
  addSignDialog.visible = true
  addSignDialog.row = row as WorkflowTodoTaskRow
  addSignDialog.form.addSignUser = ''
  addSignDialog.form.type = 'before'
  await searchUsers()
}

async function submitAddSign() {
  if (!addSignDialog.row) return
  if (!addSignDialog.form.addSignUser) {
    ElMessage.warning('请选择加签人')
    return
  }
  await addSignWorkflowTask(addSignDialog.row.taskId, {
    addSignUser: addSignDialog.form.addSignUser,
    type: addSignDialog.form.type,
  })
  addSignDialog.visible = false
  ElMessage.success('加签成功')
  loadCurrentTab()
}

onMounted(() => {
  loadCurrentTab().then(() => {
    const taskId = route.query.taskId as string
    if (taskId) {
      const row = rows.value.find((r: any) => r.taskId === taskId)
      if (row) {
        openTaskDetail(row as TaskRow)
      }
    }
  })
})
</script>

<style scoped>
.workflow-runtime-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.runtime-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.runtime-overview-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.runtime-overview-card__value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 600;
}

.runtime-overview-card__hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.runtime-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.runtime-title__sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.runtime-drawer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.runtime-form-margin {
  margin-top: 16px;
}

.attachment-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.attachment-item:last-child {
  border-bottom: none;
}

.comment-timeline {
  margin-top: 8px;
}

.comment-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-user {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.comment-msg {
  font-size: 13px;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-all;
}

.multi-instance-progress {
  margin-top: 8px;
}

.progress-header {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--el-text-color-primary);
}

.instance-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.instance-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.instance-name {
  flex: 1;
  font-size: 13px;
}

.runtime-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.runtime-group-header__name {
  font-size: 14px;
}

.common-comments {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.common-comment-tag {
  cursor: pointer;
}

.text-muted {
  color: var(--el-text-color-secondary);
}

@media (max-width: 1200px) {
  .runtime-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .runtime-overview {
    grid-template-columns: 1fr;
  }
}
</style>
