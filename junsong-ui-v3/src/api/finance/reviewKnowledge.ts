import request from '../request'

export function listReviewKnowledge(query: any) {
  return request({ url: '/finance/review-knowledge/list', method: 'get', params: query })
}

export function addReviewKnowledge(data: any) {
  return request({ url: '/finance/review-knowledge', method: 'post', data })
}

export function updateReviewKnowledge(data: any) {
  return request({ url: '/finance/review-knowledge', method: 'put', data })
}

export function createKnowledgeFromTask(taskId: number, data: any) {
  return request({ url: `/finance/review-knowledge/from-task/${taskId}`, method: 'post', data })
}

export function recommendKnowledgeForTask(taskId: number | string) {
  return request({ url: `/finance/review-knowledge/recommendations/task/${taskId}`, method: 'get' })
}
