<template>
  <div class="workflow-designer-page">
    <div class="designer-toolbar">
      <div class="designer-toolbar__left">
        <el-button :icon="ArrowLeft" @click="goBackToDefinitionCenter">返回定义中心</el-button>
        <div class="designer-toolbar__meta">
          <div class="designer-toolbar__title">{{ pageTitle }}</div>
          <div class="designer-toolbar__subtitle">
            {{ isEditMode ? `定义 ID：${String(route.params.definitionId || '-')}` : '新建流程定义' }}
          </div>
        </div>
      </div>

      <div class="designer-toolbar__actions">
        <el-button :icon="Upload" @click="triggerImport">导入 XML</el-button>
        <el-button :icon="Download" @click="exportXml">导出 XML</el-button>
        <el-button type="primary" :icon="CircleCheck" @click="validateCurrentDefinition">
          保存校验
        </el-button>
        <el-button type="success" :icon="Promotion" @click="publishCurrentDefinition">
          发布
        </el-button>
      </div>
      <input
        ref="fileInputRef"
        class="designer-hidden-input"
        type="file"
        accept=".xml,.bpmn,.bpmn20.xml"
        @change="handleImportFile"
      />
    </div>

    <div class="designer-layout">
      <aside class="designer-sidebar designer-sidebar--left">
        <el-card shadow="never" class="designer-card">
          <template #header>
            <div class="designer-card__header">
              <span>设计说明</span>
            </div>
          </template>
          <div class="designer-guide-list">
            <div class="designer-guide-item">
              <span class="designer-guide-item__title">左侧画布工具</span>
              <span class="designer-guide-item__desc">使用 BPMN 内置面板拖拽节点和连线。</span>
            </div>
            <div class="designer-guide-item">
              <span class="designer-guide-item__title">中心画布</span>
              <span class="designer-guide-item__desc">双击节点可快速编辑标题，滚轮可缩放。</span>
            </div>
            <div class="designer-guide-item">
              <span class="designer-guide-item__title">右侧属性面板</span>
              <span class="designer-guide-item__desc">先改常用字段，再按需展开高级配置。</span>
            </div>
          </div>

          <el-divider />

          <div class="designer-outline">
            <div class="designer-outline__title">当前流程</div>
            <div class="designer-outline__value">{{ processForm.processName || '未命名流程' }}</div>
            <div class="designer-outline__sub">{{ processForm.processKey || '未设置流程标识' }}</div>
          </div>

          <el-divider />

          <div class="designer-outline">
            <div class="designer-outline__title">已选元素</div>
            <div class="designer-outline__value">{{ selectedElementTitle }}</div>
            <div class="designer-outline__sub">{{ selectedElementTypeLabel }}</div>
          </div>
        </el-card>
      </aside>

      <main class="designer-canvas-wrap">
        <div ref="canvasRef" class="designer-canvas"></div>
      </main>

      <aside class="designer-sidebar designer-sidebar--right">
        <el-card shadow="never" class="designer-card">
          <template #header>
            <div class="designer-card__header">
              <span>属性与发布</span>
              <el-tag size="small" type="info">{{ selectedElementTypeLabel }}</el-tag>
            </div>
          </template>

          <el-collapse v-model="activePanels">
            <el-collapse-item title="流程主信息" name="process">
              <el-form label-width="92px">
                <el-form-item label="流程名称">
                  <el-input v-model="processForm.processName" placeholder="请输入流程名称" />
                </el-form-item>
                <el-form-item label="流程标识">
                  <el-input v-model="processForm.processKey" placeholder="建议使用英文标识" />
                </el-form-item>
                <el-form-item label="描述">
                  <el-input
                    v-model="processForm.description"
                    type="textarea"
                    :rows="3"
                    placeholder="描述当前流程用途"
                  />
                </el-form-item>
                <el-form-item label="发起用户">
                  <el-input
                    v-model="processForm.candidateStarterUsers"
                    placeholder="多个用户用英文逗号分隔"
                  />
                </el-form-item>
                <el-form-item label="发起角色">
                  <el-input
                    v-model="processForm.candidateStarterGroups"
                    placeholder="多个角色/组用英文逗号分隔"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" plain @click="applyProcessForm">
                    应用流程信息
                  </el-button>
                </el-form-item>
              </el-form>
            </el-collapse-item>

            <el-collapse-item
              v-if="selectedElementKind === 'userTask'"
              title="用户任务"
              name="userTask"
            >
              <el-form label-width="92px">
                <el-form-item label="节点名称">
                  <el-input v-model="taskForm.name" />
                </el-form-item>
                <el-form-item label="审批人表达式">
                  <el-input v-model="taskForm.assignee" placeholder="如：${initiator}" />
                </el-form-item>
                <el-form-item label="候选用户">
                  <el-input v-model="taskForm.candidateUsers" placeholder="userA,userB" />
                </el-form-item>
                <el-form-item label="候选角色">
                  <el-input v-model="taskForm.candidateGroups" placeholder="roleA,roleB" />
                </el-form-item>
                <el-form-item label="表单标识">
                  <el-input v-model="taskForm.formKey" placeholder="可对接后续业务表单" />
                </el-form-item>
                <el-form-item label="到期时间">
                  <el-input v-model="taskForm.dueDate" placeholder="如：P2D 或具体表达式" />
                </el-form-item>
                <el-form-item label="优先级">
                  <el-input v-model="taskForm.priority" placeholder="如：50" />
                </el-form-item>
                <el-form-item label="说明">
                  <el-input v-model="taskForm.description" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" plain @click="applyTaskForm">应用任务属性</el-button>
                </el-form-item>
              </el-form>
            </el-collapse-item>

            <el-collapse-item
              v-if="selectedElementKind === 'gateway'"
              title="网关配置"
              name="gateway"
            >
              <el-form label-width="92px">
                <el-form-item label="网关名称">
                  <el-input v-model="gatewayForm.name" />
                </el-form-item>
                <el-form-item label="说明">
                  <el-input v-model="gatewayForm.description" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" plain @click="applyGatewayForm">应用网关属性</el-button>
                </el-form-item>
              </el-form>

              <el-divider content-position="left">分支概览</el-divider>
              <div v-if="gatewayOutgoingFlows.length" class="gateway-flow-list">
                <div
                  v-for="flow in gatewayOutgoingFlows"
                  :key="flow.id"
                  class="gateway-flow-item"
                >
                  <div>
                    <div class="gateway-flow-item__name">{{ flow.name || flow.id }}</div>
                    <div class="gateway-flow-item__condition">
                      {{ flow.conditionExpression || '未配置条件表达式' }}
                    </div>
                  </div>
                  <el-button link type="primary" @click="focusElementById(flow.id)">
                    编辑连线
                  </el-button>
                </div>
              </div>
              <el-empty v-else description="当前网关暂无流出连线" :image-size="56" />
            </el-collapse-item>

            <el-collapse-item
              v-if="selectedElementKind === 'sequenceFlow'"
              title="连线规则"
              name="sequenceFlow"
            >
              <el-form label-width="92px">
                <el-form-item label="连线名称">
                  <el-input v-model="sequenceForm.name" />
                </el-form-item>
                <el-form-item label="条件表达式">
                  <el-input
                    v-model="sequenceForm.conditionExpression"
                    type="textarea"
                    :rows="4"
                    placeholder="如：${amount > 1000}"
                  />
                </el-form-item>
                <el-form-item label="说明">
                  <el-input v-model="sequenceForm.description" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" plain @click="applySequenceForm">应用连线规则</el-button>
                </el-form-item>
              </el-form>
            </el-collapse-item>

            <el-collapse-item
              v-if="selectedElementKind === 'event'"
              title="事件配置"
              name="event"
            >
              <el-form label-width="92px">
                <el-form-item label="事件名称">
                  <el-input v-model="eventForm.name" />
                </el-form-item>
                <el-form-item v-if="selectedElementType === 'bpmn:StartEvent'" label="发起人变量">
                  <el-input v-model="eventForm.initiator" placeholder="如：initiator" />
                </el-form-item>
                <el-form-item label="说明">
                  <el-input v-model="eventForm.description" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" plain @click="applyEventForm">应用事件属性</el-button>
                </el-form-item>
              </el-form>
            </el-collapse-item>

            <el-collapse-item title="高级字段" name="advanced">
              <el-alert
                title="这里保留当前 BPMN / Flowable 能稳定往返保存的字段。"
                type="info"
                :closable="false"
                show-icon
              />
              <el-descriptions :column="1" border class="advanced-description">
                <el-descriptions-item label="元素 ID">
                  {{ selectedElementId || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="元素类型">
                  {{ selectedElementType || '-' }}
                </el-descriptions-item>
              </el-descriptions>
              <div class="raw-attrs">
                <div
                  v-for="entry in selectedAttributeEntries"
                  :key="entry.key"
                  class="raw-attrs__item"
                >
                  <span class="raw-attrs__key">{{ entry.key }}</span>
                  <span class="raw-attrs__value">{{ entry.value || '-' }}</span>
                </div>
                <el-empty
                  v-if="!selectedAttributeEntries.length"
                  description="当前元素暂无扩展属性"
                  :image-size="56"
                />
              </div>
            </el-collapse-item>

            <el-collapse-item title="校验与发布结果" name="publish">
              <el-alert
                v-if="validationResult"
                :title="validationResult.valid ? '最近一次校验通过' : '最近一次校验未通过'"
                :type="validationResult.valid ? 'success' : 'warning'"
                :closable="false"
                show-icon
              />
              <el-empty
                v-else
                description="还没有校验记录，建议发布前先执行保存校验"
                :image-size="56"
              />
              <ul v-if="validationResult?.validationMessages?.length" class="validation-result-list">
                <li v-for="(message, index) in validationResult.validationMessages" :key="index">
                  {{ message }}
                </li>
              </ul>

              <el-divider />

              <el-descriptions v-if="deployResult" :column="1" border>
                <el-descriptions-item label="发布版本">
                  v{{ deployResult.version }}
                </el-descriptions-item>
                <el-descriptions-item label="部署 ID">
                  {{ deployResult.deploymentId }}
                </el-descriptions-item>
                <el-descriptions-item label="定义 ID">
                  {{ deployResult.definitionId }}
                </el-descriptions-item>
              </el-descriptions>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  CircleCheck,
  Download,
  Promotion,
  Upload,
} from '@element-plus/icons-vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import flowableModdle from './flowable-moddle.json'
import {
  deployWorkflowDefinition,
  getWorkflowDefinitionXml,
  type WorkflowDefinitionDeployResult,
  type WorkflowDefinitionValidationResult,
  validateWorkflowDefinition,
} from '@/api/workflow/definition'

type BpmnElement = any
type BusinessObject = any

const DEFAULT_BPMN_XML = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  xmlns:flowable="http://flowable.org/bpmn"
  id="Definitions_1"
  targetNamespace="http://junsong/workflow">
  <bpmn:process id="process_demo" name="示例流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始" />
    <bpmn:userTask id="Activity_Approve" name="审批节点" flowable:assignee="\${initiator}" />
    <bpmn:endEvent id="Event_End" name="结束" />
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Activity_Approve" />
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Activity_Approve" targetRef="Event_End" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="process_demo">
      <bpmndi:BPMNShape id="Shape_Start" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Shape_Task" bpmnElement="Activity_Approve">
        <dc:Bounds x="290" y="138" width="120" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Shape_End" bpmnElement="Event_End">
        <dc:Bounds x="490" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="216" y="178" />
        <di:waypoint x="290" y="178" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="410" y="178" />
        <di:waypoint x="490" y="178" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

const route = useRoute()
const router = useRouter()

const canvasRef = ref<HTMLDivElement>()
const fileInputRef = ref<HTMLInputElement>()
const activePanels = ref(['process', 'publish'])
const modelerRef = ref<any>(null)
const selectedElement = ref<BpmnElement | null>(null)
const validationResult = ref<WorkflowDefinitionValidationResult | null>(null)
const deployResult = ref<WorkflowDefinitionDeployResult | null>(null)

const processForm = reactive({
  processName: '',
  processKey: '',
  description: '',
  candidateStarterUsers: '',
  candidateStarterGroups: '',
})

const taskForm = reactive({
  name: '',
  assignee: '',
  candidateUsers: '',
  candidateGroups: '',
  formKey: '',
  dueDate: '',
  priority: '',
  description: '',
})

const gatewayForm = reactive({
  name: '',
  description: '',
})

const sequenceForm = reactive({
  name: '',
  conditionExpression: '',
  description: '',
})

const eventForm = reactive({
  name: '',
  initiator: '',
  description: '',
})

const isEditMode = computed(() => route.params.mode === 'edit')
const pageTitle = computed(() => (isEditMode.value ? '流程设计器 / 编辑' : '流程设计器 / 新建'))
const selectedBusinessObject = computed<BusinessObject | null>(
  () => selectedElement.value?.businessObject || null,
)
const selectedElementId = computed(() => selectedElement.value?.id || '')
const selectedElementType = computed(() => selectedBusinessObject.value?.$type || '')
const selectedElementTitle = computed(
  () =>
    selectedBusinessObject.value?.name
    || selectedBusinessObject.value?.id
    || processForm.processName
    || '流程根节点',
)
const selectedElementKind = computed(() => {
  const type = selectedElementType.value
  if (type === 'bpmn:UserTask') return 'userTask'
  if (type === 'bpmn:ExclusiveGateway' || type === 'bpmn:ParallelGateway' || type === 'bpmn:InclusiveGateway') return 'gateway'
  if (type === 'bpmn:SequenceFlow') return 'sequenceFlow'
  if (type === 'bpmn:StartEvent' || type === 'bpmn:EndEvent') return 'event'
  return 'process'
})
const selectedElementTypeLabel = computed(() => {
  switch (selectedElementKind.value) {
    case 'userTask':
      return '用户任务'
    case 'gateway':
      return '网关'
    case 'sequenceFlow':
      return '连线'
    case 'event':
      return selectedElementType.value === 'bpmn:StartEvent' ? '开始事件' : '结束事件'
    default:
      return '流程'
  }
})
const selectedAttributeEntries = computed(() => {
  const businessObject = selectedBusinessObject.value
  if (!businessObject) return []
  const attrs = businessObject.$attrs || {}
  return Object.keys(attrs).map((key) => ({
    key,
    value: String(attrs[key] ?? ''),
  }))
})
const gatewayOutgoingFlows = computed(() => {
  if (selectedElementKind.value !== 'gateway') return []
  const outgoing = selectedBusinessObject.value?.outgoing || []
  return outgoing.map((flow: any) => ({
    id: flow.id,
    name: flow.name || '',
    conditionExpression: flow.conditionExpression?.body || '',
  }))
})

function getModeling() {
  return modelerRef.value?.get('modeling')
}

function getCanvas() {
  return modelerRef.value?.get('canvas')
}

function getSelection() {
  return modelerRef.value?.get('selection')
}

function getElementRegistry() {
  return modelerRef.value?.get('elementRegistry')
}

function getBpmnFactory() {
  return modelerRef.value?.get('bpmnFactory')
}

function getRootElement() {
  return getCanvas()?.getRootElement() || null
}

function getProcessBusinessObject() {
  return getRootElement()?.businessObject || null
}

function syncPanelForms() {
  syncProcessForm()
  const businessObject = selectedBusinessObject.value
  if (!businessObject) return
  taskForm.name = businessObject.name || ''
  taskForm.assignee = businessObject.get?.('flowable:assignee') || businessObject.assignee || ''
  taskForm.candidateUsers = businessObject.get?.('flowable:candidateUsers') || businessObject.candidateUsers || ''
  taskForm.candidateGroups = businessObject.get?.('flowable:candidateGroups') || businessObject.candidateGroups || ''
  taskForm.formKey = businessObject.get?.('flowable:formKey') || businessObject.formKey || ''
  taskForm.dueDate = businessObject.get?.('flowable:dueDate') || businessObject.dueDate || ''
  taskForm.priority = businessObject.get?.('flowable:priority') || businessObject.priority || ''
  taskForm.description = getDocumentationText(businessObject)

  gatewayForm.name = businessObject.name || ''
  gatewayForm.description = getDocumentationText(businessObject)

  sequenceForm.name = businessObject.name || ''
  sequenceForm.conditionExpression = businessObject.conditionExpression?.body || ''
  sequenceForm.description = getDocumentationText(businessObject)

  eventForm.name = businessObject.name || ''
  eventForm.initiator = businessObject.get?.('flowable:initiator') || businessObject.initiator || ''
  eventForm.description = getDocumentationText(businessObject)
}

function syncProcessForm() {
  const processBusinessObject = getProcessBusinessObject()
  if (!processBusinessObject) return
  processForm.processName = processBusinessObject.name || ''
  processForm.processKey = processBusinessObject.id || ''
  processForm.description = getDocumentationText(processBusinessObject)
  processForm.candidateStarterUsers =
    processBusinessObject.get?.('flowable:candidateStarterUsers')
    || processBusinessObject.candidateStarterUsers
    || ''
  processForm.candidateStarterGroups =
    processBusinessObject.get?.('flowable:candidateStarterGroups')
    || processBusinessObject.candidateStarterGroups
    || ''
}

function getDocumentationText(businessObject: BusinessObject) {
  return businessObject?.documentation?.[0]?.text || ''
}

function setDocumentation(element: BpmnElement, text: string) {
  const modeling = getModeling()
  const bpmnFactory = getBpmnFactory()
  if (!modeling || !bpmnFactory || !element) return
  const documentation = text
    ? [bpmnFactory.create('bpmn:Documentation', { text })]
    : []
  modeling.updateProperties(element, { documentation })
}

function applyFlowableProperties(element: BpmnElement, properties: Record<string, string>) {
  const modeling = getModeling()
  if (!modeling || !element) return
  modeling.updateProperties(element, properties)
}

function applyProcessForm() {
  const rootElement = getRootElement()
  const modeling = getModeling()
  if (!rootElement || !modeling) return
  modeling.updateProperties(rootElement, {
    id: processForm.processKey.trim(),
    name: processForm.processName.trim(),
  })
  applyFlowableProperties(rootElement, {
    'flowable:candidateStarterUsers': processForm.candidateStarterUsers.trim(),
    'flowable:candidateStarterGroups': processForm.candidateStarterGroups.trim(),
  })
  setDocumentation(rootElement, processForm.description.trim())
  syncPanelForms()
  ElMessage.success('流程主信息已应用')
}

function applyTaskForm() {
  if (selectedElementKind.value !== 'userTask' || !selectedElement.value) return
  const modeling = getModeling()
  if (!modeling) return
  modeling.updateProperties(selectedElement.value, {
    name: taskForm.name.trim(),
    'flowable:assignee': taskForm.assignee.trim(),
    'flowable:candidateUsers': taskForm.candidateUsers.trim(),
    'flowable:candidateGroups': taskForm.candidateGroups.trim(),
    'flowable:formKey': taskForm.formKey.trim(),
    'flowable:dueDate': taskForm.dueDate.trim(),
    'flowable:priority': taskForm.priority.trim(),
  })
  setDocumentation(selectedElement.value, taskForm.description.trim())
  syncPanelForms()
  ElMessage.success('用户任务属性已应用')
}

function applyGatewayForm() {
  if (selectedElementKind.value !== 'gateway' || !selectedElement.value) return
  const modeling = getModeling()
  if (!modeling) return
  modeling.updateProperties(selectedElement.value, {
    name: gatewayForm.name.trim(),
  })
  setDocumentation(selectedElement.value, gatewayForm.description.trim())
  syncPanelForms()
  ElMessage.success('网关属性已应用')
}

function applySequenceForm() {
  if (selectedElementKind.value !== 'sequenceFlow' || !selectedElement.value) return
  const modeling = getModeling()
  const bpmnFactory = getBpmnFactory()
  if (!modeling || !bpmnFactory) return
  modeling.updateProperties(selectedElement.value, {
    name: sequenceForm.name.trim(),
    conditionExpression: sequenceForm.conditionExpression.trim()
      ? bpmnFactory.create('bpmn:FormalExpression', {
          body: sequenceForm.conditionExpression.trim(),
        })
      : undefined,
  })
  setDocumentation(selectedElement.value, sequenceForm.description.trim())
  syncPanelForms()
  ElMessage.success('连线规则已应用')
}

function applyEventForm() {
  if (selectedElementKind.value !== 'event' || !selectedElement.value) return
  const modeling = getModeling()
  if (!modeling) return
  const nextProperties: Record<string, string> = {
    name: eventForm.name.trim(),
  }
  if (selectedElementType.value === 'bpmn:StartEvent') {
    nextProperties['flowable:initiator'] = eventForm.initiator.trim()
  }
  modeling.updateProperties(selectedElement.value, nextProperties)
  setDocumentation(selectedElement.value, eventForm.description.trim())
  syncPanelForms()
  ElMessage.success('事件属性已应用')
}

async function createModeler() {
  modelerRef.value = new BpmnModeler({
    container: canvasRef.value,
    moddleExtensions: {
      flowable: flowableModdle,
    },
  })

  modelerRef.value.on('selection.changed', (event: any) => {
    const nextSelection = event.newSelection?.[0] || getRootElement()
    selectedElement.value = nextSelection || null
    syncPanelForms()
  })

  modelerRef.value.on('element.changed', (event: any) => {
    if (!selectedElement.value) return
    if (event.element?.id === selectedElement.value.id || event.element?.id === getRootElement()?.id) {
      nextTick(syncPanelForms)
    }
  })
}

async function importXml(xml: string) {
  const modeler = modelerRef.value
  if (!modeler) return
  await modeler.importXML(xml)
  getCanvas()?.zoom('fit-viewport')
  selectedElement.value = getRootElement()
  syncPanelForms()
}

async function loadInitialXml() {
  if (isEditMode.value && route.params.definitionId) {
    const res = await getWorkflowDefinitionXml(String(route.params.definitionId))
    await importXml(res.data.xml)
    return
  }
  await importXml(DEFAULT_BPMN_XML)
}

async function exportCurrentXml() {
  const modeler = modelerRef.value
  if (!modeler) return ''
  const result = await modeler.saveXML({ format: true })
  if (result.xml?.trim()) {
    return normalizeFlowableExpressionXml(result.xml)
  }
  const definitions = modeler.getDefinitions?.() || modeler._definitions
  const moddle = modeler.get?.('moddle') || modeler._moddle
  if (definitions && moddle?.toXML) {
    const fallback = await moddle.toXML(definitions, { format: true })
    return normalizeFlowableExpressionXml(fallback.xml || '')
  }
  return ''
}

function normalizeFlowableExpressionXml(xml: string) {
  return xml
    .replace(/\\\$\{/g, '${')
    .replace(/\\#\{/g, '#{')
}

function ensureProcessBasics() {
  if (!processForm.processName.trim()) {
    ElMessage.warning('请先填写流程名称')
    return false
  }
  if (!processForm.processKey.trim()) {
    ElMessage.warning('请先填写流程标识')
    return false
  }
  return true
}

async function validateCurrentDefinition(showSuccess = true) {
  applyProcessForm()
  if (!ensureProcessBasics()) return null
  const xml = await exportCurrentXml()
  const res = await validateWorkflowDefinition({
    xml,
    processKey: processForm.processKey.trim(),
    processName: processForm.processName.trim(),
  })
  validationResult.value = res.data
  activePanels.value = Array.from(new Set([...activePanels.value, 'publish']))
  if (showSuccess) {
    ElMessage.success(res.data.valid ? '流程校验通过' : '流程校验完成，请查看右侧提示')
  }
  return res.data
}

async function publishCurrentDefinition() {
  applyProcessForm()
  if (!ensureProcessBasics()) return
  const validation = await validateCurrentDefinition(false)
  if (!validation?.valid) {
    activePanels.value = Array.from(new Set([...activePanels.value, 'publish']))
    ElMessage.warning('校验未通过，暂不能发布')
    return
  }
  await ElMessageBox.confirm(
    `确认发布流程“${processForm.processName}”吗？`,
    '发布确认',
    { type: 'warning' },
  )
  const xml = await exportCurrentXml()
  const res = await deployWorkflowDefinition({
    xml,
    processKey: processForm.processKey.trim(),
    processName: processForm.processName.trim(),
    deploymentName: processForm.processName.trim(),
  })
  deployResult.value = res.data
  activePanels.value = Array.from(new Set([...activePanels.value, 'publish']))
  ElMessage.success(`发布成功，当前版本 v${res.data.version}`)
  router.push('/workflow/definition')
}

function triggerImport() {
  fileInputRef.value?.click()
}

async function handleImportFile(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  const xml = await file.text()
  await importXml(xml)
  validationResult.value = null
  deployResult.value = null
  ElMessage.success('XML 已导入')
  target.value = ''
}

async function exportXml() {
  const xml = await exportCurrentXml()
  const blob = new Blob([xml], { type: 'application/xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${processForm.processKey || 'workflow-definition'}.bpmn20.xml`
  link.click()
  URL.revokeObjectURL(url)
}

function focusElementById(id: string) {
  const element = getElementRegistry()?.get(id)
  if (!element) return
  getSelection()?.select(element)
  selectedElement.value = element
  syncPanelForms()
}

function goBackToDefinitionCenter() {
  router.push('/workflow/definition')
}

onMounted(async () => {
  await createModeler()
  await loadInitialXml()
})

onBeforeUnmount(() => {
  modelerRef.value?.destroy?.()
  modelerRef.value = null
})
</script>

<style scoped lang="scss">
.workflow-designer-page {
  min-height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
}

.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
}

.designer-toolbar__left,
.designer-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.designer-toolbar__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.designer-toolbar__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.designer-toolbar__subtitle {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.designer-hidden-input {
  display: none;
}

.designer-layout {
  min-height: calc(100vh - 180px);
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 360px;
  gap: 16px;
}

.designer-sidebar,
.designer-canvas-wrap {
  min-height: 100%;
}

.designer-card,
.designer-canvas-wrap {
  height: 100%;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: #fff;
}

.designer-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.designer-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.designer-canvas-wrap {
  overflow: hidden;
  padding: 0;
}

.designer-canvas {
  width: 100%;
  height: 100%;
  min-height: 720px;
}

.designer-guide-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.designer-guide-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.designer-guide-item__title {
  font-size: 13px;
  font-weight: 600;
}

.designer-guide-item__desc,
.designer-outline__sub,
.gateway-flow-item__condition {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.designer-outline {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.designer-outline__title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.designer-outline__value,
.gateway-flow-item__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.advanced-description {
  margin-top: 12px;
}

.raw-attrs {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.raw-attrs__item,
.gateway-flow-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.raw-attrs__key {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.raw-attrs__value {
  text-align: right;
  word-break: break-all;
  color: var(--el-text-color-primary);
}

.validation-result-list {
  margin: 12px 0 0;
  padding-left: 18px;
}

@media (max-width: 1440px) {
  .designer-layout {
    grid-template-columns: 220px minmax(0, 1fr) 320px;
  }
}

@media (max-width: 1180px) {
  .designer-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .designer-toolbar__left,
  .designer-toolbar__actions {
    flex-wrap: wrap;
  }

  .designer-layout {
    grid-template-columns: 1fr;
  }

  .designer-canvas {
    min-height: 560px;
  }
}
</style>
