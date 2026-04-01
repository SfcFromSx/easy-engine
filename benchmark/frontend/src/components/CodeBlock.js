import { computed, ref } from 'vue'
import { CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { highlightSql } from '../utils/sqlHighlight'


export default {
  __name: 'CodeBlock',
  components: {
    CopyDocument
  },
  props: {
  code: { type: String, default: '' },
  label: { type: String, default: 'SQL' },
  compact: { type: Boolean, default: false }
},
  setup(__props, { expose: __expose }) {
  __expose();

const props = __props

const showCopy = ref(false)

const highlightedCode = computed(() => highlightSql(props.code))

function copy() {
  navigator.clipboard.writeText(props.code).then(() => {
    ElMessage.success({ message: 'SQL 复制成功', duration: 1500 })
  })
}

const __returned__ = { props, showCopy, highlightedCode, copy, computed, ref, get CopyDocument() { return CopyDocument }, get ElMessage() { return ElMessage }, get highlightSql() { return highlightSql } }
return __returned__
}

}
