import App from '../src/App.vue'
import CodeBlock from '../src/components/CodeBlock.vue'
import DebuggerDialog from '../src/components/DebuggerDialog.vue'
import PerformanceCharts from '../src/components/PerformanceCharts.vue'
import Dashboard from '../src/views/Dashboard.vue'
import DataSources from '../src/views/DataSources.vue'
import JobDetail from '../src/views/JobDetail.vue'
import Jobs from '../src/views/Jobs.vue'
import Runs from '../src/views/Runs.vue'
import Templates from '../src/views/Templates.vue'
import TestSets from '../src/views/TestSets.vue'

describe('split component registrations', () => {
  it.each([
    [App, ['LayoutDashboard', 'PlayCircle', 'FileJson', 'Code2', 'History', 'ShieldCheck', 'ChevronRight', 'Database']],
    [Dashboard, ['RefreshRight', 'Loading', 'PerformanceCharts']],
    [DataSources, ['Plus', 'Edit', 'Delete', 'Connection', 'Search', 'Upload']],
    [Jobs, ['Plus', 'Connection']],
    [JobDetail, ['FileJson', 'Setting', 'DataLine', 'Clock', 'VideoPlay', 'PerformanceCharts']],
    [Runs, ['RefreshRight', 'CaretTop', 'CaretBottom', 'PerformanceCharts']],
    [Templates, ['Plus', 'Search', 'Play', 'DebuggerDialog']],
    [TestSets, ['Plus', 'UploadCloud', 'CodeBlock']],
    [CodeBlock, ['CopyDocument']],
    [DebuggerDialog, ['Play', 'Terminal', 'Activity', 'AlertCircle']],
    [PerformanceCharts, ['VChart']]
  ])('registers template components for %s', (component, expectedNames) => {
    expect(component.components).toBeTruthy()
    expect(Object.keys(component.components)).toEqual(expect.arrayContaining(expectedNames))
  })
})
