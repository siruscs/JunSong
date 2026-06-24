<template>
  <div class="icon-select">
    <el-popover placement="bottom-start" width="460" trigger="click">
      <template #reference>
        <el-input v-model="selected" placeholder="点击选择图标" readonly>
          <template #prefix>
            <el-icon v-if="selectedIcon"><component :is="selectedIcon" /></el-icon>
          </template>
        </el-input>
      </template>
      <div class="icon-grid">
        <div
          v-for="(icon, name) in iconMap"
          :key="name"
          class="icon-item"
          :class="{ active: selected === name }"
          @click="selectIcon(name)"
        >
          <el-icon :size="20"><component :is="icon" /></el-icon>
          <div class="icon-name">{{ name }}</div>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, ref, watch } from 'vue'
import {
  Search,
  Plus,
  Edit,
  Delete,
  Setting,
  User,
  Lock,
  HomeFilled,
  Menu,
  Document,
  Folder,
  Picture,
  PictureFilled,
  Upload,
  Download,
  Refresh,
  ArrowDown,
  ArrowUp,
  ArrowLeft,
  ArrowRight,
  CircleClose,
  CircleCheck,
  Warning,
  InfoFilled,
  SuccessFilled,
  Share,
  Star,
  StarFilled,
  Message,
  Bell,
  Calendar,
  Clock,
  Location,
  Phone,
  ChatLineSquare,
  Remove,
  RemoveFilled,
  CirclePlus,
  Files,
  DataAnalysis,
  House,
  Management,
  Connection,
  Histogram,
  Coin,
} from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{ modelValue?: string }>(), {
  modelValue: '',
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
}>()

const iconMap: Record<string, any> = {
  Search: markRaw(Search),
  Plus: markRaw(Plus),
  Edit: markRaw(Edit),
  Delete: markRaw(Delete),
  Setting: markRaw(Setting),
  User: markRaw(User),
  Lock: markRaw(Lock),
  HomeFilled: markRaw(HomeFilled),
  Menu: markRaw(Menu),
  Document: markRaw(Document),
  Folder: markRaw(Folder),
  Picture: markRaw(Picture),
  PictureFilled: markRaw(PictureFilled),
  Upload: markRaw(Upload),
  Download: markRaw(Download),
  Refresh: markRaw(Refresh),
  ArrowDown: markRaw(ArrowDown),
  ArrowUp: markRaw(ArrowUp),
  ArrowLeft: markRaw(ArrowLeft),
  ArrowRight: markRaw(ArrowRight),
  CircleClose: markRaw(CircleClose),
  CircleCheck: markRaw(CircleCheck),
  Warning: markRaw(Warning),
  InfoFilled: markRaw(InfoFilled),
  SuccessFilled: markRaw(SuccessFilled),
  Share: markRaw(Share),
  Star: markRaw(Star),
  StarFilled: markRaw(StarFilled),
  Message: markRaw(Message),
  Bell: markRaw(Bell),
  Calendar: markRaw(Calendar),
  Clock: markRaw(Clock),
  Location: markRaw(Location),
  Phone: markRaw(Phone),
  ChatLineSquare: markRaw(ChatLineSquare),
  Remove: markRaw(Remove),
  RemoveFilled: markRaw(RemoveFilled),
  CirclePlus: markRaw(CirclePlus),
  Files: markRaw(Files),
  DataAnalysis: markRaw(DataAnalysis),
  House: markRaw(House),
  Management: markRaw(Management),
  Connection: markRaw(Connection),
  Histogram: markRaw(Histogram),
  Coin: markRaw(Coin),
}

const selected = ref(props.modelValue)
const selectedIcon = computed(() => (selected.value ? iconMap[selected.value] : null))

watch(
  () => props.modelValue,
  (val) => {
    selected.value = val
  },
)

function selectIcon(name: string) {
  selected.value = name
  emit('update:modelValue', name)
}
</script>

<style scoped>
.icon-select {
  display: inline-block;
  width: 220px;
}
.icon-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
}
.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 4px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.icon-item:hover {
  background-color: #f5f7fa;
}
.icon-item.active {
  background-color: #ecf5ff;
  color: #409eff;
}
.icon-name {
  margin-top: 4px;
  font-size: 11px;
  color: #606266;
  text-align: center;
  word-break: break-all;
}
</style>
