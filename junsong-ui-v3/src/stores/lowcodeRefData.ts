import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getRegionTree } from '@/api/system/region'
import { treeselect as deptTreeSelect } from '@/api/system/dept'
import { listRole } from '@/api/system/role'
import { postOptionSelect } from '@/api/system/post'

export const useLowcodeRefDataStore = defineStore('lowcodeRefData', () => {
  // 省市区树
  const regionTree = ref<any[]>([])
  const regionTreeLoading = ref(false)

  // 部门树
  const deptTree = ref<any[]>([])
  const deptTreeLoading = ref(false)

  // 角色列表
  const roleList = ref<any[]>([])
  const roleListLoading = ref(false)

  // 岗位列表
  const postList = ref<any[]>([])
  const postListLoading = ref(false)

  async function loadRegionTree() {
    if (regionTree.value.length > 0 || regionTreeLoading.value) return
    regionTreeLoading.value = true
    try {
      const res: any = await getRegionTree()
      regionTree.value = res.data || []
    } finally {
      regionTreeLoading.value = false
    }
  }

  async function loadDeptTree() {
    if (deptTree.value.length > 0 || deptTreeLoading.value) return
    deptTreeLoading.value = true
    try {
      const res: any = await deptTreeSelect()
      deptTree.value = res.data || []
    } finally {
      deptTreeLoading.value = false
    }
  }

  async function loadRoleList() {
    if (roleList.value.length > 0 || roleListLoading.value) return
    roleListLoading.value = true
    try {
      const res: any = await listRole()
      roleList.value = res.rows || res.data || []
    } finally {
      roleListLoading.value = false
    }
  }

  async function loadPostList() {
    if (postList.value.length > 0 || postListLoading.value) return
    postListLoading.value = true
    try {
      const res: any = await postOptionSelect()
      postList.value = res.data || []
    } finally {
      postListLoading.value = false
    }
  }

  return {
    regionTree, regionTreeLoading, loadRegionTree,
    deptTree, deptTreeLoading, loadDeptTree,
    roleList, roleListLoading, loadRoleList,
    postList, postListLoading, loadPostList,
  }
})
