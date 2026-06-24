import request from '../request'

export function listPost(query: any) {
  return request({ url: '/system/post/list', method: 'get', params: query })
}

export function postOptionSelect() {
  return request({ url: '/system/post/optionselect', method: 'get' })
}

export function getPost(postId: number) {
  return request({ url: '/system/post/' + postId, method: 'get' })
}

export function addPost(data: any) {
  return request({ url: '/system/post', method: 'post', data })
}

export function updatePost(data: any) {
  return request({ url: '/system/post', method: 'put', data })
}

export function delPost(postId: number) {
  return request({ url: '/system/post/' + postId, method: 'delete' })
}
