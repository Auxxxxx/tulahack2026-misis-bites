<template>
  <div class="team-detail">
    <button class="back-btn" @click="goBack">Назад к командам</button>
    
    <div v-if="loading" class="loading">Загрузка...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else>
      <div class="team-header">
        <h2 class="team-name">{{ team.name }}</h2>
        <div class="team-stats">
          <span class="stat-badge">{{ members.length }} / {{ team.limit }} участников</span>
        </div>
      </div>

      <div class="section-title">Участники команды</div>
      <div v-if="members.length === 0" class="empty-state">
        В команде пока нет участников
      </div>
      <div v-else>
        <div 
          v-for="member in members" 
          :key="member.id" 
          class="card member-card"
          @click="goToMember(member.id)"
        >
          <div class="member-info">
            <div class="card-title">{{ member.fullName }}</div>
            <div class="card-subtitle">{{ member.role }}</div>
          </div>
          <div class="compatibility-badge" :class="getCompatibilityClass(member.totalCompatibilityPercent)">
            {{ member.totalCompatibilityPercent }}%
          </div>
        </div>
      </div>

      <button class="btn find-candidates-btn" @click="goToCandidates">
        🔍 Подобрать кандидата
      </button>
    </div>
  </div>
</template>

<script>
import { teamsApi } from '../api'

export default {
  name: 'TeamDetail',
  data() {
    return {
      team: null,
      members: [],
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
        const teamId = Number(this.$route.params.id)
        
        // Get teams list and find the one we need
        const teamsResponse = await teamsApi.getAllTeams()
        const team = teamsResponse.data.find(t => Number(t.id) === teamId)
        
        if (team) {
          this.team = team
        }
        
        const membersResponse = await teamsApi.getTeamMembers(teamId)
        this.members = membersResponse.data
        
        if (!this.team && this.members.length > 0) {
          // If we have members but no team info, create minimal team object
          this.team = {
            id: teamId,
            name: 'Команда #' + teamId,
            limit: this.members.length
          }
        }
      } catch (err) {
        this.error = 'Ошибка загрузки данных команды'
        console.error(err)
      } finally {
        this.loading = false
      }
    },
    goBack() {
      this.$router.push('/')
    },
    goToMember(memberId) {
      this.$router.push(`/team/${this.$route.params.id}/member/${memberId}`)
    },
    goToCandidates() {
      this.$router.push(`/team/${this.$route.params.id}/candidates`)
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
.team-header {
  margin-bottom: 16px;
}

.team-name {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 12px;
}

.team-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stat-badge {
  display: inline-block;
  padding: 6px 12px;
  background: #f0f0f0;
  border-radius: 20px;
  font-size: 13px;
  color: #666;
}

.analytics-card {
  margin-bottom: 16px;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.analytics-item {
  text-align: center;
}

.analytics-label {
  font-size: 11px;
  color: #666;
  margin-bottom: 6px;
}

.analytics-value {
  font-size: 20px;
  font-weight: 700;
}

.analytics-value.high {
  color: #27ae60;
}

.analytics-value.medium {
  color: #f39c12;
}

.analytics-value.low {
  color: #e74c3c;
}

.member-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.member-info {
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
  margin-bottom: 12px;
}

.find-candidates-btn {
  margin-top: 16px;
}
</style>
