import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getInfo as getInfoApi } from '@/api/login'
import { switchDept as switchDeptApi } from '@/api/system/user'
import { getToken, setToken, removeToken, setExpiresIn } from '@/utils/auth'
import { isEmpty } from '@/utils/validate'
import cache from '@/utils/cache'
import { ElMessageBox } from 'element-plus'
import router from '@/router'

const defAva = ''

const LAST_DEPT_KEY_PREFIX = 'lastLoginDept.'

/**
 * 生成"上次登录部门"持久化 key。
 * 与小程序 mergePersistedUser 对齐：同时按 userId + userName 双写，
 * 优先级：session > local，查询时先按 userId 命中再按 userName 兜底。
 */
function lastDeptKeys(userId?: string, userName?: string) {
  const keys: string[] = []
  if (userId) keys.push(`${LAST_DEPT_KEY_PREFIX}uid:${userId}`)
  if (userName) keys.push(`${LAST_DEPT_KEY_PREFIX}uname:${userName}`)
  return keys
}

function rememberLastDept(deptId: number, userId?: string, userName?: string) {
  const payload = { deptId, t: Date.now() }
  for (const key of lastDeptKeys(userId, userName)) {
    cache.session.setJSON(key, payload)
    cache.local.setJSON(key, payload)
  }
}

function recallLastDept(userId?: string, userName?: string): number | null {
  for (const key of lastDeptKeys(userId, userName)) {
    const s = cache.session.getJSON(key)
    if (s && typeof s.deptId === 'number') return s.deptId
    const l = cache.local.getJSON(key)
    if (l && typeof l.deptId === 'number') return l.deptId
  }
  return null
}

function normalizeAvatar(avatar?: string) {
  if (isEmpty(avatar)) return defAva
  const value = avatar!
  if (/^(data:|blob:)/.test(value)) return value
  const staticsIndex = value.indexOf('/statics/')
  if (staticsIndex >= 0) return value.substring(staticsIndex)
  const normalized = value.startsWith('/') ? value : '/' + value
  return normalized
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const id = ref<string>('')
  const name = ref<string>('')
  const nickName = ref<string>('')
  const avatar = ref<string>('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const depts = ref<any[]>([])
  const currentDeptId = ref<number | null>(null)
  const currentDeptName = ref<string>('')

  async function login(userInfo: { username: string; password: string; code: string; uuid: string; deptId?: number }) {
    const username = userInfo.username.trim()
    const res: any = await loginApi(username, userInfo.password, userInfo.code, userInfo.uuid, userInfo.deptId)
    const data = res.data
    setToken(data.access_token)
    token.value = data.access_token
    setExpiresIn(data.expires_in)
  }

  async function getInfo(loadPermissions = true) {
    const res: any = await getInfoApi()
    const user = res.user
    const avatarVal = normalizeAvatar(user.avatar)
    if (loadPermissions) {
      if (res.roles && res.roles.length > 0) {
        roles.value = res.roles
        permissions.value = res.permissions
      } else {
        roles.value = ['ROLE_DEFAULT']
      }
    }
    id.value = user.userId
    name.value = user.userName
    nickName.value = user.nickName
    avatar.value = avatarVal
    if (res.depts && res.depts.length > 0) {
      depts.value = res.depts
      // 对齐小程序 mergePersistedUser 优先级：后端 res.currentDeptId > res.currentDept.deptId > 缓存 lastDeptId > depts[0]
      const cachedDeptId = recallLastDept(user.userId, user.userName)
      const backendPreferred = res.currentDeptId ?? res.currentDept?.deptId ?? user.deptId ?? user.currentDeptId
      const preselectId = backendPreferred ?? cachedDeptId ?? res.depts[0].deptId
      const currentDept = res.depts.find((d: any) => d.deptId === preselectId) || res.depts[0]
      currentDeptId.value = currentDept.deptId
      currentDeptName.value = currentDept.deptName || ''
      if (typeof currentDept.deptId === 'number') {
        rememberLastDept(currentDept.deptId, user.userId, user.userName)
      }
    } else {
      depts.value = []
      currentDeptId.value = null
      currentDeptName.value = ''
    }
    cache.session.set('pwrChrtype', res.pwdChrtype)
    if (res.isDefaultModifyPwd) {
      ElMessageBox.confirm('您的密码还是初始密码，请修改密码！', '安全提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
        })
        .catch(() => {})
    }
    if (!res.isDefaultModifyPwd && res.isPasswordExpired) {
      ElMessageBox.confirm('您的密码已过期，请尽快修改密码！', '安全提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
        })
        .catch(() => {})
    }
    return res
  }

  async function switchDept(deptId: number) {
    const res = await switchDeptApi(deptId)
    const dept = depts.value.find((d: any) => d.deptId === deptId)
    currentDeptId.value = deptId
    currentDeptName.value = dept ? dept.deptName : ''
    if (typeof deptId === 'number') {
      rememberLastDept(deptId, id.value, name.value)
    }
    return res
  }

  async function logout() {
    const keys = lastDeptKeys(id.value, name.value)
    keys.forEach((k) => { cache.session.remove(k) })
    await logoutApi()
    token.value = ''
    roles.value = []
    permissions.value = []
    depts.value = []
    currentDeptId.value = null
    currentDeptName.value = ''
    removeToken()
  }

  function fedLogOut() {
    const keys = lastDeptKeys(id.value, name.value)
    keys.forEach((k) => { cache.session.remove(k) })
    token.value = ''
    removeToken()
  }

  return {
    token, id, name, nickName, avatar, roles, permissions,
    depts, currentDeptId, currentDeptName,
    login, getInfo, switchDept, logout, fedLogOut,
    recallLastDept, rememberLastDept,
  }
})
