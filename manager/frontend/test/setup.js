import { config } from '@vue/test-utils'
import { defineComponent, h } from 'vue'

if (typeof globalThis.localStorage?.getItem !== 'function') {
  const store = {}
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    value: {
      getItem(key) {
        return Object.prototype.hasOwnProperty.call(store, key) ? store[key] : null
      },
      setItem(key, value) {
        store[key] = String(value)
      },
      removeItem(key) {
        delete store[key]
      },
      clear() {
        Object.keys(store).forEach((key) => delete store[key])
      }
    }
  })
}

const renderChildren = (slots, name = 'default') => slots[name] ? slots[name]() : []

const ElButton = defineComponent({
  name: 'ElButton',
  props: {
    loading: Boolean,
    type: String
  },
  emits: ['click'],
  setup(props, { attrs, emit, slots }) {
    return () => h('button', {
      ...attrs,
      type: 'button',
      disabled: props.loading,
      onClick: (event) => emit('click', event)
    }, renderChildren(slots))
  }
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'clear', 'keyup.enter'],
  setup(props, { emit, slots }) {
    return () => h('label', { class: 'el-input-stub' }, [
      ...renderChildren(slots, 'prefix'),
      h('input', {
        value: props.modelValue ?? '',
        placeholder: props.placeholder,
        onInput: (event) => emit('update:modelValue', event.target.value),
        onKeyup: (event) => {
          if (event.key === 'Enter') {
            emit('keyup.enter', event)
          }
        }
      })
    ])
  }
})

const ElInputNumber = defineComponent({
  name: 'ElInputNumber',
  props: {
    modelValue: {
      type: [String, Number],
      default: 0
    }
  },
  emits: ['update:modelValue'],
  setup(props, { emit, attrs }) {
    return () => h('input', {
      ...attrs,
      type: 'number',
      value: props.modelValue ?? 0,
      onInput: (event) => emit('update:modelValue', Number(event.target.value))
    })
  }
})

const ElSwitch = defineComponent({
  name: 'ElSwitch',
  props: {
    modelValue: Boolean
  },
  emits: ['update:modelValue'],
  setup(props, { emit, attrs }) {
    return () => h('input', {
      ...attrs,
      type: 'checkbox',
      checked: props.modelValue,
      onChange: (event) => emit('update:modelValue', event.target.checked)
    })
  }
})

const ElDialog = defineComponent({
  name: 'ElDialog',
  props: {
    modelValue: Boolean
  },
  setup(props, { slots }) {
    return () => props.modelValue
      ? h('div', { class: 'el-dialog-stub' }, [
          ...renderChildren(slots),
          ...renderChildren(slots, 'footer')
        ])
      : null
  }
})

const ElLink = defineComponent({
  name: 'ElLink',
  emits: ['click'],
  setup(_props, { attrs, emit, slots }) {
    return () => h('a', {
      ...attrs,
      href: '#',
      onClick: (event) => {
        event.preventDefault()
        emit('click', event)
      }
    }, renderChildren(slots))
  }
})

const passthrough = (name, tag = 'div') => defineComponent({
  name,
  props: {
    title: {
      type: String,
      default: ''
    },
    description: {
      type: String,
      default: ''
    }
  },
  setup(_props, { attrs, slots }) {
    return () => h(tag, attrs, [
      _props.title,
      _props.description,
      ...renderChildren(slots)
    ])
  }
})

config.global.directives = {
  loading() {
  }
}

config.global.stubs = {
  'refresh-cw': true,
  'activity': true,
  'database': true,
  'rocket': true,
  'target': true,
  'terminal': true,
  'plus': true,
  ElButton,
  ElInput,
  ElInputNumber,
  ElSwitch,
  ElDialog,
  ElLink,
  ElAlert: passthrough('ElAlert'),
  ElEmpty: passthrough('ElEmpty'),
  ElRow: passthrough('ElRow'),
  ElCol: passthrough('ElCol'),
  ElTag: passthrough('ElTag', 'span'),
  ElTable: passthrough('ElTable'),
  ElTableColumn: defineComponent({
    name: 'ElTableColumn',
    setup() {
      return () => null
    }
  }),
  ElPagination: passthrough('ElPagination'),
  ElForm: passthrough('ElForm', 'form'),
  ElFormItem: passthrough('ElFormItem'),
  ElIcon: passthrough('ElIcon', 'span')
}
