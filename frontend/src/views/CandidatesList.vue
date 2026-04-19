<template>
  <div class="candidates-list">
    <button class="back-btn" @click="goBack">Назад к команде</button>
    
    <h2 class="page-title">Подбор кандидатов</h2>
    
    <div v-if="openRoles.length > 0" class="roles-filter">
      <span class="filter-label">Открытые роли:</span>
      <div class="roles-tags">
        <span 
          v-for="role in openRoles" 
          :key="role"
          class="role-tag"
          :class="{ active: selectedRole === role }"
          @click="selectRole(role)"
        >
          {{ role }}
        </span>
      </div>
    </div>

    <div v-if="loading" class="loading">Загрузка...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="candidates.length === 0" class="empty-state">
      Нет кандидатов для выбранных ролей
    </div>
    <div v-else class="candidates-grid">
      <div 
        v-for="candidate in candidates" 
        :key="candidate.id" 
        class="card candidate-card"
        @click="goToCandidate(candidate.id)"
      >
        <div class="candidate-info">
          <div class="card-title">{{ candidate.fullName }}</div>
          <div class="card-subtitle">{{ candidate.role }}</div>
        </div>
        <div class="compatibility-badge" :class="getCompatibilityClass(candidate.analytics?.totalCompatibilityPercent)">
          {{ candidate.analytics?.totalCompatibilityPercent }}%
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { teamsApi } from '../api'

export default {
  name: 'CandidatesList',
  data() {
    return {
      team: null,
      openRoles: [],
      selectedRole: null,
      candidates: [],
      loading: true,
      error: null
    }
  },
  async mounted() {
    await this.loadData()
  },
  methods: {
    async loadData() {
      try {
        const teamId = this.$route.params.teamId
        const rolesResponse = await teamsApi.getTeamOpenRoles(teamId)
        this.openRoles = rolesResponse.data
        
        await this.loadCandidates()
      } catch (err) {
        this.error = 'Ошибка загрузки данных'
        console.error(err)
      } finally {
        this.loading = false
      }
    },
    async loadCandidates() {
      try {
        const teamId = this.$route.params.teamId
        const response = await teamsApi.getCandidatesForTeam(teamId, this.selectedRole)
        this.candidates = response.data
      } catch (err) {
        this.error = 'Ошибка загрузки кандидатов'
        console.error(err)
      }
    },
    selectRole(role) {
      if (this.selectedRole === role) {
        this.selectedRole = null
      } else {
        this.selectedRole = role
      }
      this.loadCandidates()
    },
    goBack() {
      this.$router.push(`/team/${this.$route.params.teamId}`)
    },
    goToCandidate(candidateId) {
      this.$router.push(`/team/${this.$route.params.teamId}/candidate/${candidateId}`)
    },
    getCompatibilityClass(percent) {
      if (percent >= 75) return 'high'
      if (percent >= 50) return 'medium'
      return 'low'
    }
  }
}
</script>

<style scoped>
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 16px;
}

.roles-filter {
  margin-bottom: 16px;
}

.filter-label {
  font-size: 13px;
  color: #666;
  display: block;
  margin-bottom: 8px;
}

.roles-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.role-tag {
  padding: 6px 12px;
  background: #f0f0f0;
  border-radius: 20px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.role-tag.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.candidates-grid {
  display: grid;
  gap: 12px;
}

.candidate-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.candidate-info {
  flex: 1;
}

.compatibility-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.compatibility-badge.high {
  background: #e8f5e9;
  color: #27ae60;
}

.compatibility-badge.medium {
  background: #fff3e0;
  color: #f39c12;
}

.compatibility-badge.low {
  background: #ffebee;
  color: #e74c3c;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
  background: white;
  border-radius: 12px;
}
</style>
