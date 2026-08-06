const ZERO_NUMBER = /^-?0(?:\.0+)?$/

function normalizeNumberInput(target: EventTarget | null) {
  const input = target as HTMLInputElement | null
  if (!input || !input.matches('.el-input-number input') || input.disabled || input.readOnly) return
  if (!input.getAttribute('placeholder')) input.setAttribute('placeholder', '0.00')
  if (ZERO_NUMBER.test(input.value)) {
    input.value = ''
    input.dispatchEvent(new Event('input', { bubbles: true }))
  }
}

let installed = false

export function setupNumberInputBehavior() {
  if (installed || typeof document === 'undefined') return
  installed = true
  document.addEventListener('focusin', (event) => normalizeNumberInput(event.target))
}
