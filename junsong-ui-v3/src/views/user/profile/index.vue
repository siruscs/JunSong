<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="6" :lg="6" :xl="6">
        <el-card shadow="hover" class="user-card">
          <div class="user-info">
            <el-avatar :size="80" :src="userStore.avatar">
              {{ userStore.nickName?.charAt(0) || 'U' }}
            </el-avatar>
            <h3 class="user-name">{{ userStore.nickName || userStore.name || '用户' }}</h3>
            <p class="user-role">{{ userRole }}</p>
            <el-button type="primary" link @click="activeTab = 'avatar'">更换头像</el-button>
          </div>
          <el-divider />
          <div class="user-details">
            <div class="detail-item">
              <span class="label">用户名：</span>
              <span class="value">{{ userStore.name || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">部门：</span>
              <span class="value">{{ userStore.currentDeptName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">手机号：</span>
              <span class="value">{{ userForm.phonenumber || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">邮箱：</span>
              <span class="value">{{ userForm.email || '-' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="18" :lg="18" :xl="18">
        <el-card shadow="hover">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本资料" name="info">
              <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="100px">
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="昵称" prop="nickName">
                      <el-input v-model="userForm.nickName" placeholder="请输入昵称" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="用户名" prop="userName">
                      <el-input v-model="userForm.userName" placeholder="请输入用户名" disabled />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="手机号" prop="phonenumber">
                      <el-input v-model="userForm.phonenumber" placeholder="请输入手机号" maxlength="11" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="邮箱" prop="email">
                      <el-input v-model="userForm.email" placeholder="请输入邮箱" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="性别" prop="sex">
                      <el-radio-group v-model="userForm.sex">
                        <el-radio value="0">男</el-radio>
                        <el-radio value="1">女</el-radio>
                        <el-radio value="2">未知</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="部门">
                      <span class="form-readonly">{{ userStore.currentDeptName || '-' }}</span>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="24">
                    <el-form-item label="备注">
                      <el-input v-model="userForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item>
                  <el-button type="primary" @click="submitUserForm">保存修改</el-button>
                  <el-button @click="resetUserForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="120px">
                <el-form-item label="旧密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入旧密码" show-password />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitPwdForm">确认修改</el-button>
                  <el-button @click="resetPwdForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="绑定手机" name="phone">
              <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-width="120px">
                <el-form-item label="当前手机号">
                  <span class="form-readonly">{{ userForm.phonenumber || '未绑定' }}</span>
                </el-form-item>
                <el-form-item label="新手机号" prop="newPhone">
                  <el-input v-model="phoneForm.newPhone" placeholder="请输入新手机号" maxlength="11" />
                </el-form-item>
                <el-form-item label="验证码" prop="smsCode">
                  <div class="code-row">
                    <el-input v-model="phoneForm.smsCode" placeholder="请输入验证码" style="flex: 1" />
                    <el-button type="primary" :disabled="phoneCountdown > 0" @click="sendPhoneCode">
                      {{ phoneCountdown > 0 ? `${phoneCountdown}s 后重试` : '发送验证码' }}
                    </el-button>
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitPhoneForm">确认绑定</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="绑定邮箱" name="email">
              <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-width="120px">
                <el-form-item label="当前邮箱">
                  <span class="form-readonly">{{ userForm.email || '未绑定' }}</span>
                </el-form-item>
                <el-form-item label="新邮箱" prop="newEmail">
                  <el-input v-model="emailForm.newEmail" placeholder="请输入新邮箱" />
                </el-form-item>
                <el-form-item label="邮箱验证码" prop="emailCode">
                  <div class="code-row">
                    <el-input v-model="emailForm.emailCode" placeholder="请输入验证码" style="flex: 1" />
                    <el-button type="primary" :disabled="emailCountdown > 0" @click="sendEmailCode">
                      {{ emailCountdown > 0 ? `${emailCountdown}s 后重试` : '发送验证码' }}
                    </el-button>
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitEmailForm">确认绑定</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="更换头像" name="avatar">
              <div class="avatar-board">
                <div class="avatar-mode-switch">
                  <button
                    type="button"
                    class="avatar-mode-btn"
                    :class="{ active: avatarMode === 'preset' }"
                    @click="switchAvatarMode('preset')"
                  >
                    <span class="avatar-mode-label">卡通头像</span>
                    <span class="avatar-mode-hint">从系统精选中挑选</span>
                  </button>
                  <button
                    type="button"
                    class="avatar-mode-btn"
                    :class="{ active: avatarMode === 'upload' }"
                    @click="switchAvatarMode('upload')"
                  >
                    <span class="avatar-mode-label">上传图片</span>
                    <span class="avatar-mode-hint">使用自己的照片</span>
                  </button>
                </div>

                <div v-show="avatarMode === 'preset'" class="avatar-preset">
                  <div class="avatar-preset-toolbar">
                    <span class="avatar-preset-title">挑选一个风格</span>
                    <div class="avatar-style-pills">
                      <button
                        v-for="style in avatarStyles"
                        :key="style.id"
                        type="button"
                        class="avatar-style-pill"
                        :class="{ active: activeStyle === style.id }"
                        @click="activeStyle = style.id"
                      >
                        {{ style.label }}
                      </button>
                    </div>
                  </div>

                  <div class="avatar-grid">
                    <button
                      v-for="seed in avatarSeeds"
                      :key="seed"
                      type="button"
                      class="avatar-grid-item"
                      :class="{ active: selectedPresetSeed === seed }"
                      @click="selectPreset(seed)"
                    >
                      <div class="avatar-grid-item-inner" v-html="buildPresetSvg(activeStyle, seed)"></div>
                    </button>
                  </div>
                </div>

                <div v-show="avatarMode === 'upload'" class="avatar-upload">
                  <p class="avatar-hint">支持 JPG / PNG，大小不超过 2MB</p>
                  <el-upload
                    class="upload-demo"
                    action="#"
                    :auto-upload="false"
                    :show-file-list="false"
                    :before-upload="beforeAvatarUpload"
                    :on-change="handleAvatarChange"
                    accept="image/*"
                  >
                    <el-button type="primary" :icon="Upload">选择图片</el-button>
                  </el-upload>
                </div>

                <div class="avatar-stage">
                  <div class="avatar-stage-preview">
                    <div v-if="avatarMode === 'preset' && selectedPresetSeed" v-html="buildPresetSvg(activeStyle, selectedPresetSeed, 192)"></div>
                    <img v-else-if="avatarPreview" :src="avatarPreview" alt="头像预览" />
                    <span v-else class="avatar-stage-placeholder">未选择</span>
                  </div>
                  <div class="avatar-stage-meta">
                    <div class="avatar-stage-title">预览</div>
                    <div class="avatar-stage-tip">
                      {{ avatarMode === 'preset' ? '点击右侧任意头像即可预览，确认后保存' : '上传后会显示在这里，确认后保存' }}
                    </div>
                    <div class="avatar-stage-actions">
                      <el-button
                        type="primary"
                        :loading="avatarSaving"
                        :disabled="!canSaveAvatar"
                        @click="saveAvatar"
                      >
                        保存头像
                      </el-button>
                      <el-button :disabled="!avatarPreview" @click="resetAvatarSelection">取消</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateUserPwd, userAvatar, updateUserProfile, getUserProfile } from '@/api/system/user'
import { createAvatar } from '@dicebear/core'
import { funEmoji, adventurer, avataaars, botttsNeutral } from '@dicebear/collection'

const userStore = useUserStore()
const router = useRouter()

const activeTab = ref('info')

const userForm = reactive<any>({
  userId: undefined,
  userName: '',
  nickName: '',
  phonenumber: '',
  email: '',
  sex: '0',
  remark: '',
})

const userRules = {
  nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  phonenumber: [
    {
      pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
      message: '请输入正确的手机号码',
      trigger: 'blur',
    },
  ],
  email: [
    {
      type: 'email',
      message: '请输入正确的邮箱地址',
      trigger: ['blur', 'change'],
    },
  ],
}

const userFormRef = ref()

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPwd = (rule: any, value: string, callback: any) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 5, max: 20, message: '长度在 5 到 20 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' },
  ],
}

const pwdFormRef = ref()

const phoneForm = reactive({
  newPhone: '',
  smsCode: '',
})

const phoneCountdown = ref(0)
let phoneTimer: number | null = null

const phoneRules = {
  newPhone: [
    { required: true, message: '请输入新手机号', trigger: 'blur' },
    { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
  smsCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const phoneFormRef = ref()

const emailForm = reactive({
  newEmail: '',
  emailCode: '',
})

const emailCountdown = ref(0)
let emailTimer: number | null = null

const emailRules = {
  newEmail: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] },
  ],
  emailCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const emailFormRef = ref()

const avatarPreview = ref('')
const avatarFile = ref<File | null>(null)
const avatarMode = ref<'preset' | 'upload'>('preset')
const avatarSaving = ref(false)
const selectedPresetSeed = ref<string | null>(null)

const avatarStyles = [
  { id: 'fun-emoji', label: '萌趣', style: funEmoji },
  { id: 'adventurer', label: '探险家', style: adventurer },
  { id: 'avataaars', label: '卡通', style: avataaars },
  { id: 'bottts-neutral', label: '机器人', style: botttsNeutral },
] as const
const activeStyle = ref<typeof avatarStyles[number]['id']>(avatarStyles[0].id)
const avatarSeeds = [
  'lemon', 'peach', 'mint', 'mocha', 'sage', 'cocoa',
  'plum', 'maple', 'azure', 'olive', 'rose', 'sand',
  'cobalt', 'ember', 'frost', 'jade', 'wheat', 'rust',
  'pearl', 'cedar',
]

function getStyleDef(id: string) {
  return avatarStyles.find((s) => s.id === id)?.style || funEmoji
}

function buildPresetSvg(styleId: string, seed: string, size = 96) {
  const styleDef = getStyleDef(styleId)
  const avatar = createAvatar(styleDef as any, {
    seed,
    size,
    radius: 50,
    backgroundType: ['gradientLinear', 'solid'],
  })
  return avatar.toString()
}

async function svgToPngFile(svg: string, fileName: string, size = 320): Promise<File> {
  const blob = new Blob([svg], { type: 'image/svg+xml' })
  const url = URL.createObjectURL(blob)
  try {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve()
      img.onerror = () => reject(new Error('SVG 加载失败'))
      img.src = url
    })
    const canvas = document.createElement('canvas')
    canvas.width = size
    canvas.height = size
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('Canvas 上下文创建失败')
    ctx.drawImage(img, 0, 0, size, size)
    const pngBlob: Blob = await new Promise((resolve, reject) => {
      canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('PNG 生成失败'))), 'image/png')
    })
    return new File([pngBlob], fileName, { type: 'image/png' })
  } finally {
    URL.revokeObjectURL(url)
  }
}

function switchAvatarMode(mode: 'preset' | 'upload') {
  if (avatarMode.value === mode) return
  avatarMode.value = mode
  avatarPreview.value = ''
  avatarFile.value = null
  selectedPresetSeed.value = null
}

function selectPreset(seed: string) {
  selectedPresetSeed.value = seed
  avatarFile.value = null
  avatarPreview.value = ''
}

function resetAvatarSelection() {
  avatarPreview.value = ''
  avatarFile.value = null
  selectedPresetSeed.value = null
}

const canSaveAvatar = computed(() => {
  if (avatarSaving.value) return false
  if (avatarMode.value === 'preset') return !!selectedPresetSeed.value
  return !!avatarFile.value
})

const userRole = computed(() => {
  const roles = userStore.roles || []
  if (roles.length === 0) return '普通用户'
  return roles.join(', ')
})

function loadUserInfo() {
  userForm.userId = userStore.id
  userForm.userName = userStore.name
  userForm.nickName = userStore.nickName
  getUserProfile().then((res: any) => {
    const data = res.data || res
    Object.assign(userForm, data)
  })
}

function submitUserForm() {
  if (!userFormRef.value) return
  ;(userFormRef.value as any).validate((valid: boolean) => {
    if (valid) {
      updateUserProfile(userForm).then(() => {
        ElMessage.success('保存成功')
        userStore.getInfo()
      })
    }
  })
}

function resetUserForm() {
  loadUserInfo()
  if (userFormRef.value) {
    ;(userFormRef.value as any).clearValidate()
  }
}

function submitPwdForm() {
  if (!pwdFormRef.value) return
  ;(pwdFormRef.value as any).validate((valid: boolean) => {
    if (valid) {
      updateUserPwd(pwdForm.oldPassword, pwdForm.newPassword).then(() => {
        ElMessage.success('密码修改成功，请重新登录')
        resetPwdForm()
        userStore.fedLogOut()
        router.push('/login')
      })
    }
  })
}

function resetPwdForm() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  if (pwdFormRef.value) {
    ;(pwdFormRef.value as any).clearValidate()
  }
}

function sendPhoneCode() {
  if (!/^1[3|4|5|6|7|8|9][0-9]\d{8}$/.test(phoneForm.newPhone)) {
    ElMessage.warning('请输入正确的手机号码')
    return
  }
  phoneCountdown.value = 60
  phoneTimer = window.setInterval(() => {
    phoneCountdown.value--
    if (phoneCountdown.value <= 0 && phoneTimer) {
      clearInterval(phoneTimer)
      phoneTimer = null
    }
  }, 1000)
  ElMessage.success('验证码已发送')
}

function submitPhoneForm() {
  if (!phoneFormRef.value) return
  ;(phoneFormRef.value as any).validate((valid: boolean) => {
    if (valid) {
      updateUserProfile({
        ...userForm,
        phonenumber: phoneForm.newPhone
      }).then(() => {
        ElMessage.success('手机号绑定成功')
        userForm.phonenumber = phoneForm.newPhone
        userStore.getInfo()
        phoneForm.newPhone = ''
        phoneForm.smsCode = ''
      })
    }
  })
}

function sendEmailCode() {
  const emailRegex = /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/
  if (!emailRegex.test(emailForm.newEmail)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }
  emailCountdown.value = 60
  emailTimer = window.setInterval(() => {
    emailCountdown.value--
    if (emailCountdown.value <= 0 && emailTimer) {
      clearInterval(emailTimer)
      emailTimer = null
    }
  }, 1000)
  ElMessage.success('验证码已发送')
}

function submitEmailForm() {
  if (!emailFormRef.value) return
  ;(emailFormRef.value as any).validate((valid: boolean) => {
    if (valid) {
      updateUserProfile({
        ...userForm,
        email: emailForm.newEmail
      }).then(() => {
        ElMessage.success('邮箱绑定成功')
        userForm.email = emailForm.newEmail
        userStore.getInfo()
        emailForm.newEmail = ''
        emailForm.emailCode = ''
      })
    }
  })
}

function beforeAvatarUpload(file: File) {
  return validateAvatarFile(file)
}

function validateAvatarFile(file: File) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('请上传 JPG、PNG 等图片格式')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

function handleAvatarChange(uploadFile: any) {
  const rawFile = uploadFile.raw as File
  if (!rawFile || !validateAvatarFile(rawFile)) {
    avatarFile.value = null
    avatarPreview.value = ''
    return
  }
  avatarMode.value = 'upload'
  selectedPresetSeed.value = null
  avatarFile.value = rawFile
  const reader = new FileReader()
  reader.onload = (e) => {
    avatarPreview.value = e.target?.result as string
  }
  reader.readAsDataURL(rawFile)
}

async function saveAvatar() {
  if (!canSaveAvatar.value) return
  let fileToUpload: File | null = avatarFile.value
  if (avatarMode.value === 'preset') {
    if (!selectedPresetSeed.value) {
      ElMessage.warning('请先选择一个卡通头像')
      return
    }
    try {
      avatarSaving.value = true
      const svg = buildPresetSvg(activeStyle.value, selectedPresetSeed.value, 320)
      fileToUpload = await svgToPngFile(
        svg,
        `avatar_${activeStyle.value}_${selectedPresetSeed.value}.png`,
        320,
      )
    } catch (e: any) {
      avatarSaving.value = false
      ElMessage.error(e?.message || '卡通头像生成失败')
      return
    }
  }
  if (!fileToUpload) {
    ElMessage.warning('请先选择头像图片')
    return
  }
  try {
    avatarSaving.value = true
    const formData = new FormData()
    formData.append('avatarfile', fileToUpload)
    const res: any = await userAvatar(formData)
    if (res.code === 200 || res.code === 0) {
      await userStore.getInfo()
      ElMessage.success('头像更换成功')
      resetAvatarSelection()
    } else {
      ElMessage.error(res.msg || '头像上传失败')
    }
  } finally {
    avatarSaving.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
}

.user-card {
  text-align: center;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.user-name {
  font-size: 18px;
  color: #303133;
  margin: 10px 0 0 0;
}

.user-role {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.user-details {
  text-align: left;
}

.detail-item {
  display: flex;
  padding: 8px 0;
  font-size: 14px;
}

.detail-item .label {
  color: #909399;
  min-width: 70px;
}

.detail-item .value {
  color: #303133;
  flex: 1;
}

.form-readonly {
  color: #606266;
  font-size: 14px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.avatar-board {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 4px 0 8px;
}

.avatar-mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 6px;
  background: #f4f6fb;
  border-radius: 12px;
}

.avatar-mode-btn {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 14px 18px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.avatar-mode-btn:hover {
  background: #ffffff;
}

.avatar-mode-btn.active {
  background: #ffffff;
  border-color: #dbe3f3;
  box-shadow: 0 4px 14px rgba(64, 87, 158, 0.08);
}

.avatar-mode-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 0.2px;
}

.avatar-mode-btn.active .avatar-mode-label {
  color: var(--el-color-primary);
}

.avatar-mode-hint {
  font-size: 12px;
  color: #909399;
}

.avatar-preset {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.avatar-preset-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.avatar-preset-title {
  font-size: 13px;
  color: #606266;
  letter-spacing: 0.4px;
}

.avatar-style-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.avatar-style-pill {
  padding: 6px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 999px;
  background: #ffffff;
  color: #606266;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.avatar-style-pill:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}

.avatar-style-pill.active {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.25);
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
  gap: 10px;
  padding: 14px;
  background: #fafbfd;
  border: 1px solid #eef0f5;
  border-radius: 12px;
}

.avatar-grid-item {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  padding: 4px;
  border: 2px solid transparent;
  border-radius: 14px;
  background: #ffffff;
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.avatar-grid-item img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  user-select: none;
}

.avatar-grid-item-inner,
.avatar-grid-item-inner :deep(svg) {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 50%;
  pointer-events: none;
}

.avatar-stage-preview :deep(svg) {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 50%;
}

.avatar-grid-item:hover {
  transform: translateY(-2px);
  border-color: #dbe3f3;
  box-shadow: 0 6px 14px rgba(64, 87, 158, 0.12);
}

.avatar-grid-item.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.18);
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 18px 16px;
  background: #fafbfd;
  border: 1px dashed #d6dae3;
  border-radius: 12px;
}

.avatar-hint {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.avatar-stage {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 18px;
  border: 1px solid #eef0f5;
  border-radius: 14px;
  background: linear-gradient(135deg, #ffffff 0%, #f6f8fc 100%);
}

.avatar-stage-preview {
  width: 96px;
  height: 96px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  background: #ffffff;
  border: 2px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-stage-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-stage-placeholder {
  font-size: 12px;
  color: #c0c4cc;
}

.avatar-stage-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.avatar-stage-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 0.2px;
}

.avatar-stage-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.avatar-stage-actions {
  margin-top: 6px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .avatar-mode-switch {
    grid-template-columns: 1fr;
  }

  .avatar-stage {
    flex-direction: column;
    align-items: flex-start;
  }

  .avatar-stage-actions {
    width: 100%;
  }
}
</style>
