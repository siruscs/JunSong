import { ElMessage, ElMessageBox, ElNotification, ElLoading } from 'element-plus'

let loadingInstance: ReturnType<typeof ElLoading.service>

export function useModal() {
  function msg(content: string) { ElMessage.info(content) }
  function msgError(content: string) { ElMessage.error(content) }
  function msgSuccess(content: string) { ElMessage.success(content) }
  function msgWarning(content: string) { ElMessage.warning(content) }
  function alert(content: string) { ElMessageBox.alert(content, '系统提示') }
  function alertError(content: string) { ElMessageBox.alert(content, '系统提示', { type: 'error' }) }
  function alertSuccess(content: string) { ElMessageBox.alert(content, '系统提示', { type: 'success' }) }
  function alertWarning(content: string) { ElMessageBox.alert(content, '系统提示', { type: 'warning' }) }
  function notify(content: string) { ElNotification.info(content) }
  function notifyError(content: string) { ElNotification.error(content) }
  function notifySuccess(content: string) { ElNotification.success(content) }
  function notifyWarning(content: string) { ElNotification.warning(content) }
  function confirm(content: string, tip = '系统提示') {
    return ElMessageBox.confirm(content, tip, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  }
  function prompt(content: string) {
    return ElMessageBox.prompt(content, '系统提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  }
  function loading(content: string) {
    loadingInstance = ElLoading.service({ lock: true, text: content, background: 'rgba(0, 0, 0, 0.7)' })
  }
  function closeLoading() { loadingInstance.close() }

  return {
    msg, msgError, msgSuccess, msgWarning,
    alert, alertError, alertSuccess, alertWarning,
    notify, notifyError, notifySuccess, notifyWarning,
    confirm, prompt, loading, closeLoading,
  }
}
