<template>
  <div id="tags-view-container" class="tags-view-container">
    <el-scrollbar class="tags-view-wrapper">
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :to="{ path: tag.path, query: tag.query }"
        class="tags-view-item"
        :class="{ active: isActive(tag) }"
        @click.middle="!isAffix(tag) && closeSelectedTag(tag)"
        @contextmenu.prevent="openContextMenu(tag, $event)"
      >
        {{ tag.title }}
        <el-icon v-if="!isAffix(tag)" class="el-icon-close" @click.prevent.stop="closeSelectedTag(tag)">
          <Close />
        </el-icon>
      </router-link>
    </el-scrollbar>
    <ul v-show="visible" class="contextmenu" :style="{ left: left + 'px', top: top + 'px' }">
      <li @click="refreshSelectedTag(selectedTag)">刷新页面</li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)">关闭当前</li>
      <li @click="closeOthersTags">关闭其他</li>
      <li @click="closeAllTags(selectedTag)">关闭所有</li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore } from '@/stores/tagsView'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()
const permissionStore = usePermissionStore()

const visible = ref(false)
const left = ref(0)
const top = ref(0)
const selectedTag = ref<any>({})
const visitedViews = computed(() => tagsViewStore.visitedViews)
const isActive = (tag: any) => tag.path === route.path
const isAffix = (tag: any) => tag.meta?.affix

function closeSelectedTag(view: any) {
  tagsViewStore.delView(view).then(({ visitedViews }) => {
    if (isActive(view)) {
      toLastView(visitedViews, view)
    }
  })
}

function closeOthersTags() {
  router.push(selectedTag.value)
  tagsViewStore.delOthersViews(selectedTag.value)
}

function closeAllTags(view: any) {
  tagsViewStore.delAllViews().then(({ visitedViews }) => {
    if (visitedViews.some((v) => v.path === view.path)) return
    toLastView(visitedViews, view)
  })
}

function toLastView(visitedViews: any[], view: any) {
  const latestView = visitedViews.slice(-1)[0]
  if (latestView) {
    router.push(latestView.fullPath || latestView.path)
  } else {
    router.push('/')
  }
}

function refreshSelectedTag(view: any) {
  tagsViewStore.delCachedView(view).then(() => {
    const { fullPath } = view
    nextTick(() => {
      router.replace({ path: '/redirect' + fullPath })
    })
  })
}

function openContextMenu(tag: any, e: MouseEvent) {
  const menuMinWidth = 105
  const offsetLeft = (e.currentTarget as HTMLElement).getBoundingClientRect().left
  const offsetWidth = (e.currentTarget as HTMLElement).offsetWidth
  const maxLeft = window.innerWidth - menuMinWidth
  left.value = offsetLeft + offsetWidth + 15 > maxLeft ? maxLeft : offsetLeft + offsetWidth + 15
  top.value = e.clientY
  visible.value = true
  selectedTag.value = tag
}

function closeContextMenu() {
  visible.value = false
}

watch(route, () => {
  addTags()
})

watch(visible, (val) => {
  if (val) document.addEventListener('click', closeContextMenu)
  else document.removeEventListener('click', closeContextMenu)
})

function addTags() {
  if (route.name && !route.meta?.hidden) {
    tagsViewStore.addView(route)
  }
}

function initTags() {
  const routes = permissionStore.routes
  let affixTags = routes.filter((route: any) => route.meta?.affix)
  for (const tag of affixTags) {
    if (tag.name) {
      tagsViewStore.addVisitedView(tag)
    }
  }
}

onMounted(() => {
  initTags()
  addTags()
})
</script>

<style scoped lang="scss">
.tags-view-container {
  height: 40px;
  width: 100%;
  background: rgba(255, 255, 255, 0.62);
  border-bottom: 1px solid var(--theme-app-border, rgba(var(--theme-primary-rgb), 0.12));
  box-shadow: none;
  backdrop-filter: blur(14px);
}
.tags-view-wrapper .tags-view-item {
  display: inline-block;
  position: relative;
  cursor: pointer;
  height: 28px;
  line-height: 26px;
  border: 1px solid rgba(27, 39, 63, 0.1);
  border-radius: 999px;
  color: #526072;
  background: var(--theme-app-surface, #fff);
  padding: 0 11px;
  font-size: 12px;
  margin-left: 8px;
  margin-top: 6px;
  text-decoration: none;
  box-shadow: 0 4px 10px rgba(31, 45, 82, 0.04);
  transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    color: var(--theme-primary);
    border-color: rgba(var(--theme-primary-rgb), 0.35);
    background: var(--theme-hover-bg);
  }

  &.active {
    color: #fff;
    border-color: transparent;
    background: linear-gradient(135deg, var(--theme-primary-light) 0%, var(--theme-primary) 62%, var(--theme-primary-dark) 100%);
    box-shadow: 0 3px 10px rgba(var(--theme-primary-rgb), 0.22);

    &:hover {
      color: #fff;
      border-color: var(--theme-primary);
      background: linear-gradient(135deg, var(--theme-primary-light) 0%, var(--theme-primary) 58%, var(--theme-primary-dark) 100%);
    }

    .el-icon-close {
      color: rgba(255, 255, 255, 0.88);

      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.18);
      }
    }
  }
}
.el-icon-close {
  font-size: 10px;
  margin-left: 4px;
  border-radius: 50%;
  transition: color 0.18s ease, background 0.18s ease;

  &:hover {
    color: #fff;
    background: var(--theme-primary);
  }
}
.contextmenu {
  margin: 0;
  background: var(--theme-app-surface, #fff);
  z-index: 3000;
  position: absolute;
  list-style-type: none;
  padding: 5px 0;
  border: 1px solid var(--theme-app-border, rgba(var(--theme-primary-rgb), 0.12));
  border-radius: 12px;
  font-size: 12px;
  font-weight: 400;
  color: var(--theme-app-text, #172033);
  box-shadow: var(--theme-card-shadow, 0 18px 50px rgba(31, 45, 82, 0.1));
  li {
    margin: 0;
    padding: 7px 16px;
    cursor: pointer;
    &:hover {
      color: var(--theme-primary);
      background: var(--theme-hover-bg);
    }
  }
}
</style>
