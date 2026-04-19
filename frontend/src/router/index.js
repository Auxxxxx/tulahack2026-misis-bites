import { createRouter, createWebHistory } from 'vue-router'
import TeamsList from '../views/TeamsList.vue'
import TeamDetail from '../views/TeamDetail.vue'
import MemberDetail from '../views/MemberDetail.vue'
import CandidatesList from '../views/CandidatesList.vue'
import CandidateDetail from '../views/CandidateDetail.vue'

const routes = [
  {
    path: '/',
    name: 'TeamsList',
    component: TeamsList
  },
  {
    path: '/team/:id',
    name: 'TeamDetail',
    component: TeamDetail
  },
  {
    path: '/team/:teamId/member/:memberId',
    name: 'MemberDetail',
    component: MemberDetail
  },
  {
    path: '/team/:teamId/candidates',
    name: 'CandidatesList',
    component: CandidatesList
  },
  {
    path: '/team/:teamId/candidate/:candidateId',
    name: 'CandidateDetail',
    component: CandidateDetail
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
