import test from 'node:test'
import assert from 'node:assert/strict'
import { buildTaskCenterItems } from '../src/utils/taskCenter.js'

const now = new Date('2026-07-19T12:00:00+08:00')

test('normalizes approval and verification work into one task model', () => {
  const items = buildTaskCenterItems({
    approvals: [{ taskId: 't-1', taskName: '采购审批', processDefinitionKey: 'purchase', createTime: '2026-07-19 09:00:00' }],
    expenses: [{ expenseId: 8, expenseContent: '门店水费', expenseAmount: 320, expenseDate: '2026-07-18' }],
    now
  })

  assert.equal(items.length, 2)
  assert.deepEqual(items.map((item) => item.type).sort(), ['approval', 'verification'])
  assert.equal(items.find((item) => item.type === 'approval').taskId, 't-1')
  assert.equal(items.find((item) => item.type === 'verification').expenseId, 8)
})

test('sorts overdue, due-soon, attention, and normal tasks by urgency', () => {
  const items = buildTaskCenterItems({
    approvals: [
      { taskId: 'normal', taskName: '普通审批', createTime: '2026-07-19 10:00:00' },
      { taskId: 'soon', taskName: '临期审批', dueDate: '2026-07-20 06:00:00', createTime: '2026-07-19 08:00:00' },
      { taskId: 'late', taskName: '逾期审批', dueDate: '2026-07-19 11:00:00', createTime: '2026-07-18 08:00:00' }
    ],
    expenses: [{ expenseId: 9, expenseContent: '待核销费用', expenseDate: '2026-07-15' }],
    now
  })

  assert.deepEqual(items.map((item) => item.urgency), ['overdue', 'soon', 'attention', 'normal'])
})

test('filters invalid records and keeps task keys unique across sources', () => {
  const items = buildTaskCenterItems({
    approvals: [{ taskId: '12', taskName: '审批' }, { taskName: '无编号' }],
    expenses: [{ expenseId: 12, expenseContent: '核销' }, { expenseContent: '无编号' }],
    now
  })

  assert.deepEqual(items.map((item) => item.key).sort(), ['approval:12', 'verification:12'])
})

test('preserves backend order for completed approvals and displays completion time', () => {
  const items = buildTaskCenterItems({
    approvals: [
      { taskId: 'new', taskName: '最近完成', startTime: '2026-07-18 08:00:00', endTime: '2026-07-19 10:00:00' },
      { taskId: 'old', taskName: '较早完成', startTime: '2026-07-01 08:00:00', endTime: '2026-07-02 10:00:00' }
    ],
    preserveOrder: true,
    now
  })

  assert.deepEqual(items.map((item) => item.taskId), ['new', 'old'])
  assert.equal(items[0].timeText, '2026-07-19 10:00:00')
})

test('expense urgency uses local calendar days instead of elapsed hours', () => {
  const morning = new Date(2026, 6, 19, 1, 0, 0)
  const evening = new Date(2026, 6, 19, 23, 0, 0)
  const input = { expenses: [{ expenseId: 20, expenseDate: '2026-07-16' }] }
  assert.equal(buildTaskCenterItems({ ...input, now: morning })[0].urgency, 'attention')
  assert.equal(buildTaskCenterItems({ ...input, now: evening })[0].urgency, 'attention')
})
