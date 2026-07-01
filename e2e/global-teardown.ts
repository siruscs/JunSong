import { unlink } from 'fs/promises'

async function globalTeardown() {
  try {
    await unlink('storage-state.json')
    console.log('[Global Teardown] Storage state cleaned')
  } catch {
    // ignore if file doesn't exist
  }
}

export default globalTeardown
