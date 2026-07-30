import { listBizObject } from '@/api/lowcode/admin'

/** 将 Flowable 流程编码解析为低代码业务对象编码。 */
export async function resolveWorkflowBizCode(processKey?: string): Promise<string> {
  if (!processKey) return ''
  const res: any = await listBizObject({})
  const objects = (res?.data || res?.rows || []).flat?.() || []
  const match = objects.find((item: any) =>
    item.processKey === processKey && item.configStatus === 'PUBLISHED' && item.delFlag !== '1',
  )
  return match?.bizCode || processKey
}
