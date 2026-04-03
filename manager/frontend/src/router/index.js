import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import Traces from '../views/Traces.vue'
import Patterns from '../views/Patterns.vue'
import Acceleration from '../views/Acceleration.vue'
import QueryDatasources from '../views/QueryDatasources.vue'
import CacheManagement from '../views/CacheManagement.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Dashboard },
    { path: '/traces', component: Traces },
    { path: '/patterns', component: Patterns },
    { path: '/query-datasources', component: QueryDatasources },
    { path: '/acceleration', component: Acceleration },
    { path: '/cache', component: CacheManagement }
  ]
})
