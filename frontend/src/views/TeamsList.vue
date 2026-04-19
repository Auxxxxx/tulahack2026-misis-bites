<template>
  <div class="teams-list">
    <h2 class="page-title">Команды</h2>
    
    <div v-if="loading" class="loading">Загрузка...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="teams-grid">
      <div 
        v-for="team in teams" 
        :key="team.id" 
        class="card team-card"
        @click="goToTeam(team.id)"
      >
        <div class="card-title">{{ team.name }}</div>
        <div class="card-subtitle">
          {{ team.memberCount }} / {{ team.limit }} участников
        </div>
        <div class="compatibility-badge" :class="getCompatibilityClass(team.totalCompatibilityPercent)">
          {{ team.totalCompatibilityPercent }}% совместимость
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { teamsApi } from '../api'

export default {
  name: 'TeamsList',
  data() {
    return {
      teams: [],
      loading: true,
      error: null
    }
  },
  async mounted() {
    await this.loadTeams()
  },
  methods: {
    async loadTeams() {
      try {
        const response = await teamsApi.getAllTeams()
        const teamsData = response.data
        
        // Load members for each team to get total compatibility
        const teamsWithMembers = await Promise.all(
          teamsData.map(async (team) => {
            try {
              const membersResponse = await teamsApi.getTeamMembers(team.id)
              const members = membersResponse.data
              
              // Calculate average compatibility from members
              let totalCompat = 0
              if (members.length > 0) {
                totalCompat = Math.round(
                  members.reduce((sum, m) => sum + (m.totalCompatibilityPercent || 0), 0) / members.length
                )
              }
              
              return {
                ...team,
                totalCompatibilityPercent: totalCompat
              }
            } catch (err) {
              return { ...team, totalCompatibilityPercent: 0 }
            }
          })
        )
        
        this.teams = teamsWithMembers
      } catch (err) {
        this.error = 'Ошибка загрузки команд'
        console.error(err)
      } finally {
        this.loading = false
      }
    },
    goToTeam(id) {
      this.$router.push(`/team/${id}`)
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

.teams-grid {
  display: grid;
  gap: 12px;
}

.team-card {
  display: block;
}

.compatibility-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  margin-top: 8px;
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
</style>
