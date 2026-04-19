import axios from 'axios'

const API_BASE_URL = 'http://72.56.35.92:8080'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

export const teamsApi = {
  getAllTeams() {
    return api.get('/teams')
  },
  getTeamAnalytics(id) {
    return api.get(`/teams/${id}/analytics`)
  },
  getTeamMembers(id) {
    return api.get(`/teams/${id}/members`)
  },
  getMemberAnalytics(teamId, memberId) {
    return api.get(`/teams/${teamId}/members/${memberId}/analytics`)
  },
  getMemberRecommendations(teamId, memberId) {
    return api.get(`/teams/${teamId}/members/${memberId}/recommendations`)
  },
  getTeamOpenRoles(id) {
    return api.get(`/teams/${id}/open-roles`)
  },
  getCandidatesForTeam(teamId, role = null) {
    const params = role ? { role } : {}
    return api.get(`/teams/${teamId}/candidates`, { params })
  },
  getCandidateRecommendations(teamId, candidateId) {
    return api.get(`/teams/${teamId}/candidates/${candidateId}/recommendations`)
  }
}

export default api
