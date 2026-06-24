<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item.children, item) && (!onlyChild.children || onlyChild.noShowingChildren) && !item.alwaysShow">
      <app-link v-if="onlyChild.meta" :to="resolvePath(onlyChild.path)">
        <el-menu-item :index="resolvePath(onlyChild.path)">
          <svg-icon v-if="onlyChild.meta.icon || (item.meta && item.meta.icon)" :icon-class="onlyChild.meta.icon || item.meta.icon" />
          <template #title>
            <span>{{ onlyChild.meta.title }}</span>
          </template>
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)" teleported>
      <template #title>
        <svg-icon v-if="item.meta && item.meta.icon" :icon-class="item.meta.icon" />
        <span>{{ item.meta && item.meta.title }}</span>
      </template>
      <SidebarItem
        v-for="(child, index) in item.children"
        :key="child.path + index"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SvgIcon from '@/components/SvgIcon/index.vue'
import AppLink from './Link.vue'

const props = defineProps<{
  item: any
  basePath: string
}>()

const onlyChild = ref<any>({})

function hasOneShowingChild(children: any[] = [], parent: any) {
  if (!children) {
    children = []
  }
  const showingChildren = children.filter((item: any) => {
    if (item.hidden) {
      return false
    }
    return true
  })

  if (showingChildren.length === 1) {
    onlyChild.value = showingChildren[0]
    return true
  }

  if (showingChildren.length === 0) {
    onlyChild.value = { ...parent, path: '', noShowingChildren: true }
    return true
  }

  return false
}

function resolvePath(routePath: string) {
  if (isExternalLink(routePath)) return routePath
  if (isExternalLink(props.basePath)) return props.basePath
  if (routePath === '') return props.basePath
  if (routePath.startsWith('/')) return routePath
  return props.basePath + '/' + routePath
}

function isExternalLink(path: string): boolean {
  return /^(https?:|mailto:|tel:)/.test(path)
}
</script>
