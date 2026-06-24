<template>
  <div class="workflow-tracking-viewer">
    <div v-if="loading" class="workflow-tracking-viewer__state">正在加载流程图...</div>
    <div v-else-if="!xml" class="workflow-tracking-viewer__state">当前实例暂无可展示的 BPMN 流程图</div>
    <div v-else ref="canvasRef" class="workflow-tracking-viewer__canvas" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import BpmnViewer from 'bpmn-js/lib/Viewer'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

const props = withDefaults(defineProps<{
  xml?: string
  completedActivityIds?: string[]
  activeActivityIds?: string[]
  loading?: boolean
}>(), {
  xml: '',
  completedActivityIds: () => [],
  activeActivityIds: () => [],
  loading: false,
})

const canvasRef = ref<HTMLDivElement>()
const viewerRef = ref<any>(null)

function clearMarkers() {
  const canvas = viewerRef.value?.get?.('canvas')
  const elementRegistry = viewerRef.value?.get?.('elementRegistry')
  if (!canvas || !elementRegistry) return
  elementRegistry.getAll().forEach((element: any) => {
    if (!element?.id) return
    canvas.removeMarker(element.id, 'workflow-completed')
    canvas.removeMarker(element.id, 'workflow-active')
  })
}

function applyMarkers() {
  const canvas = viewerRef.value?.get?.('canvas')
  if (!canvas) return
  clearMarkers()
  props.completedActivityIds.forEach((id) => canvas.addMarker(id, 'workflow-completed'))
  props.activeActivityIds.forEach((id) => {
    canvas.addMarker(id, 'workflow-active')
  })
}

async function renderDiagram() {
  if (!viewerRef.value || !props.xml || !canvasRef.value) return
  await viewerRef.value.importXML(props.xml)
  viewerRef.value.get('canvas')?.zoom('fit-viewport', 'auto')
  applyMarkers()
}

async function ensureViewer() {
  if (viewerRef.value || !canvasRef.value) return
  viewerRef.value = new BpmnViewer({
    container: canvasRef.value,
  })
}

watch(
  () => props.xml,
  async (value) => {
    if (!value) {
      clearMarkers()
      return
    }
    await nextTick()
    await ensureViewer()
    await renderDiagram()
  },
)

watch(
  () => [props.completedActivityIds, props.activeActivityIds],
  async () => {
    if (!props.xml) return
    await nextTick()
    applyMarkers()
  },
  { deep: true },
)

onMounted(async () => {
  await nextTick()
  await ensureViewer()
  if (props.xml) {
    await renderDiagram()
  }
})

onBeforeUnmount(() => {
  viewerRef.value?.destroy?.()
  viewerRef.value = null
})
</script>

<style scoped>
.workflow-tracking-viewer {
  min-height: 280px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: linear-gradient(180deg, #fbfdff 0%, #f5f8fc 100%);
  overflow: hidden;
}

.workflow-tracking-viewer__state {
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.workflow-tracking-viewer__canvas {
  height: 320px;
}

:deep(.workflow-completed .djs-visual > :nth-child(1)) {
  stroke: #67c23a !important;
  stroke-width: 3px !important;
  fill: #f0f9eb !important;
}

:deep(.workflow-active .djs-visual > :nth-child(1)) {
  stroke: #409eff !important;
  stroke-width: 4px !important;
  fill: #ecf5ff !important;
}

:deep(.workflow-active .djs-outline) {
  stroke: #409eff !important;
}
</style>
