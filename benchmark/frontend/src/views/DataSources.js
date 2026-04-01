import { computed, ref, onMounted } from 'vue'
import { Plus, Edit, Delete, Connection, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import client from '../api/client'
import {
  API_ENDPOINTS,
  DATA_SOURCE_BY_ID,
  DATA_SOURCE_TEST,
  DRIVER_UPLOAD
} from '../api/endpoints'
import { filterDataSources, validateDriverUploadFile } from '../utils/benchmarkViewHelpers'


export default {
  __name: 'DataSources',
  components: {
    Plus,
    Edit,
    Delete,
    Connection,
    Search,
    Upload
  },
  setup(__props, { expose: __expose }) {
  __expose();

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const driversLoading = ref(false)
const uploadingDriver = ref(false)
const dataSources = ref([])
const uploadedDrivers = ref([])
const searchTerm = ref('')
const selectedDriverClass = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const driverFileInput = ref(null)
const form = ref({
  id: null,
  name: '',
  jdbcUrl: '',
  jdbcUser: '',
  jdbcPassword: '',
  driverClass: 'org.apache.kylin.jdbc.Driver'
})

const driverClassOptions = computed(() => {
  return [...new Set(
    dataSources.value
      .map((item) => String(item.driverClass || '').trim())
      .filter(Boolean)
  )].sort((left, right) => left.localeCompare(right))
})

const filteredDataSources = computed(() => filterDataSources(
  dataSources.value,
  searchTerm.value,
  selectedDriverClass.value
))

async function fetchDataSources() {
  loading.value = true
  try {
    const { data } = await client.get(API_ENDPOINTS.DATASOURCES)
    dataSources.value = data
  } catch (err) {
    ElMessage.error(t('common.error'))
  } finally {
    loading.value = false
  }
}

async function fetchDrivers() {
  driversLoading.value = true
  try {
    const { data } = await client.get(API_ENDPOINTS.DRIVERS)
    uploadedDrivers.value = data
  } catch (err) {
    ElMessage.error(err.response?.data?.message || t('common.error'))
  } finally {
    driversLoading.value = false
  }
}

function openDriverPicker() {
  driverFileInput.value?.click()
}

function handleAdd() {
  isEdit.value = false
  form.value = {
    id: null,
    name: '',
    jdbcUrl: '',
    jdbcUser: '',
    jdbcPassword: '',
    driverClass: 'org.apache.kylin.jdbc.Driver'
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function saveDataSource() {
  if (!form.value.name || !form.value.jdbcUrl || !form.value.driverClass) {
    ElMessage.warning('Required fields missing')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await client.put(DATA_SOURCE_BY_ID(form.value.id), form.value)
    } else {
      await client.post(API_ENDPOINTS.DATASOURCES, form.value)
    }
    ElMessage.success(t('common.success'))
    dialogVisible.value = false
    fetchDataSources()
  } catch (err) {
    ElMessage.error(t('common.error'))
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      t('jobs.confirmDelete', { name: row.name }),
      t('common.warning'),
      { type: 'warning' }
    )
    await client.delete(DATA_SOURCE_BY_ID(row.id))
    ElMessage.success(t('common.success'))
    fetchDataSources()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err.response?.data?.message || t('common.error'))
    }
  }
}

async function testConnection(row) {
  try {
    const { data } = await client.post(DATA_SOURCE_TEST, row)
    if (data === 'SUCCESS') {
      ElMessage.success(t('datasources.testSuccess'))
    } else {
      ElMessage.error(t('datasources.testFailed', { msg: data }))
    }
  } catch (err) {
    ElMessage.error(t('common.error'))
  }
}

async function handleDriverFileChange(event) {
  const [file] = event.target.files || []
  event.target.value = ''
  const validation = validateDriverUploadFile(file)
  if (!validation.valid) {
    if (validation.reason === 'invalid_extension') {
      ElMessage.warning(t('datasources.uploadTypeError'))
    }
    return
  }

  const payload = new FormData()
  payload.append('file', file)

  uploadingDriver.value = true
  try {
    await client.post(DRIVER_UPLOAD, payload, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    ElMessage.success(t('datasources.uploadSuccess', { name: file.name }))
    await fetchDrivers()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || t('common.error'))
  } finally {
    uploadingDriver.value = false
  }
}

onMounted(async () => {
  await Promise.all([fetchDataSources(), fetchDrivers()])
})

const __returned__ = { t, loading, saving, driversLoading, uploadingDriver, dataSources, uploadedDrivers, searchTerm, selectedDriverClass, dialogVisible, isEdit, driverFileInput, form, driverClassOptions, filteredDataSources, fetchDataSources, fetchDrivers, openDriverPicker, handleAdd, handleEdit, saveDataSource, handleDelete, testConnection, handleDriverFileChange, computed, ref, onMounted, get Plus() { return Plus }, get Edit() { return Edit }, get Delete() { return Delete }, get Connection() { return Connection }, get Search() { return Search }, get Upload() { return Upload }, get ElMessage() { return ElMessage }, get ElMessageBox() { return ElMessageBox }, get useI18n() { return useI18n }, get client() { return client }, get API_ENDPOINTS() { return API_ENDPOINTS }, get DATA_SOURCE_BY_ID() { return DATA_SOURCE_BY_ID }, get DATA_SOURCE_TEST() { return DATA_SOURCE_TEST }, get DRIVER_UPLOAD() { return DRIVER_UPLOAD }, get filterDataSources() { return filterDataSources }, get validateDriverUploadFile() { return validateDriverUploadFile } }
return __returned__
}

}
