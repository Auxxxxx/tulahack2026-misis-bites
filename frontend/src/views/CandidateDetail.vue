<template>
  <div class="candidate-detail">
    <button class="back-btn" @click="goBack">Назад к кандидатам</button>
    
    <div v-if="loading" class="loading">Загрузка...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="recommendations">
      <div class="candidate-header">
        <h2 class="candidate-name">{{ recommendations.DISC_D ? candidate.fullName : '' }}</h2>
        <div class="role-badge">{{ candidate.role }}</div>
      </div>

      <div class="card">
        <h3 class="card-title">DISC профиль</h3>
        <div class="chart-container">
          <div class="chart-bar">
            <span class="chart-label">D</span>
            <div class="chart-track">
              <div class="chart-fill" :style="{ width: (recommendations.DISC_D * 100) + '%' }"></div>
            </div>
            <span class="chart-value">{{ (recommendations.DISC_D * 100).toFixed(0) }}%</span>
          </div>
          <div class="chart-bar">
            <span class="chart-label">I</span>
            <div class="chart-track">
              <div class="chart-fill" :style="{ width: (recommendations.DISC_I * 100) + '%' }"></div>
            </div>
            <span class="chart-value">{{ (recommendations.DISC_I * 100).toFixed(0) }}%</span>
          </div>
          <div class="chart-bar">
            <span class="chart-label">S</span>
            <div class="chart-track">
              <div class="chart-fill" :style="{ width: (recommendations.DISC_S * 100) + '%' }"></div>
            </div>
            <span class="chart-value">{{ (recommendations.DISC_S * 100).toFixed(0) }}%</span>
          </div>
          <div class="chart-bar">
            <span class="chart-label">C</span>
            <div class="chart-track">
              <div class="chart-fill" :style="{ width: (recommendations.DISC_C * 100) + '%' }"></div>
            </div>
            <span class="chart-value">{{ (recommendations.DISC_C * 100).toFixed(0) }}%</span>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">Мотивация (Герчиков)</h3>
        <div class="chart-container">
          <div class="chart-bar">
            <span class="chart-label">IN</span>
            <div class="chart-track">
              <div class="chart-fill" :style="{ width: (recommendations.GERCHIKOV_INSTRUMENTAL * 100) + '%' }"></div>
            </div>
            <span class="chart-value">{{ (recommendations.GERCHIKOV_INSTRUMENTAL * 100).toFixed(0) }}%</span>
          </div>
          <div class="chart-bar">
            <span class="chart-label">PR</span>
            <div class="chart-track">
              <div class="chart-fill" :style="{ width: (recommendations.GERCHIKOV_PROFESSIONAL * 100) + '%' }"></div>
            </div>
            <span class="chart-value">{{ (recommendations.GERCHIKOV_PROFESSIONAL * 100).toFixed(0) }}%</span>
          </div>
          <div class="chart-bar">
            <span class="chart-label">PA</span>
            <div class="chart-track">
              <div class="chart-fill" :style="{ width: (recommendations.GERCHIKOV_PATRIOTIC * 100) + '%' }"></div>
            </div>
            <span class="chart-value">{{ (recommendations.GERCHIKOV_PATRIOTIC * 100).toFixed(0) }}%</span>
          </div>
          <div class="chart-bar">
            <span class="chart-label">HO</span>
            <div class="chart-track">
              <div class="chart-fill" :style="{ width: (recommendations.GERCHIKOV_MASTER * 100) + '%' }"></div>
            </div>
            <span class="chart-value">{{ (recommendations.GERCHIKOV_MASTER * 100).toFixed(0) }}%</span>
          </div>
          <div class="chart-bar">
            <span class="chart-label">LU</span>
            <div class="chart-track">
              <div class="chart-fill" :style="{ width: (recommendations.GERCHIKOV_AVOIDING * 100) + '%' }"></div>
            </div>
            <span class="chart-value">{{ (recommendations.GERCHIKOV_AVOIDING * 100).toFixed(0) }}%</span>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">Совместимость с командой</h3>
        <div class="compatibility-grid">
          <div class="compatibility-item">
            <div class="compatibility-label">DISC</div>
            <div class="compatibility-value" :class="getValueClass(recommendations.analytics?.compatibilityDiscPercent)">
              {{ recommendations.analytics?.compatibilityDiscPercent }}%
            </div>
          </div>
          <div class="compatibility-item">
            <div class="compatibility-label">Герчиков</div>
            <div class="compatibility-value" :class="getValueClass(recommendations.analytics?.compatibilityGerchikovPercent)">
              {{ recommendations.analytics?.compatibilityGerchikovPercent }}%
            </div>
          </div>
          <div class="compatibility-item">
            <div class="compatibility-label">Общая</div>
            <div class="compatibility-value" :class="getValueClass(recommendations.analytics?.totalCompatibilityPercent)">
              {{ recommendations.analytics?.totalCompatibilityPercent }}%
            </div>
          </div>
        </div>
      </div>

      <div class="recommendation-box" v-if="recommendations.recommendation">
        <h4>Рекомендация системы</h4>
        <p>{{ recommendations.recommendation }}</p>
      </div>

      <div class="pros-cons" v-if="recommendations.pros?.length || recommendations.cons?.length">
        <div class="pros" v-if="recommendations.pros?.length">
          <h4>✓ Сильные стороны</h4>
          <ul>
            <li v-for="(pro, index) in recommendations.pros" :key="index">{{ pro }}</li>
          </ul>
        </div>
        <div class="cons" v-if="recommendations.cons?.length">
          <h4>⚠ Зоны роста</h4>
          <ul>
            <li v-for="(con, index) in recommendations.cons" :key="index">{{ con }}</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { teamsApi } from '../api'

export default {
  name: 'CandidateDetail',
  data() {
    return {
      candidate: {},
      recommendations: null,
      loading: true,
      error: null
    }
  },
  async mounted() {
    await this.loadRecommendations()
  },
  methods: {
    async loadRecommendations() {
      try {
        const { teamId, candidateId } = this.$route.params
        const response = await teamsApi.getCandidateRecommendations(teamId, candidateId)
        this.recommendations = response.data
        this.candidate = {
          id: candidateId,
          fullName: 'Кандидат',
          role: response.data.role || ''
        }
      } catch (err) {
        this.error = 'Ошибка загрузки данных'
        console.error(err)
      } finally {
        this.loading = false
      }
    },
    goBack() {
      this.$router.push(`/team/${this.$route.params.teamId}/candidates`)
    },
    getValueClass(percent) {
      if (percent >= 75) return 'high'
      if (percent >= 50) return 'medium'
      return 'low'
    }
  }
}
</script>

<style scoped>
.candidate-header {
  margin-bottom: 16px;
}

.candidate-name {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.role-badge {
  display: inline-block;
  padding: 6px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.chart-container {
  margin: 16px 0;
}

.chart-bar {
  display: flex;
  align-items: center;
  margin: 10px 0;
}

.chart-label {
  width: 35px;
  font-size: 13px;
  font-weight: 700;
  color: #666;
}

.chart-track {
  flex: 1;
  height: 14px;
  background: #e0e0e0;
  border-radius: 7px;
  overflow: hidden;
  margin: 0 10px;
}

.chart-fill {
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 7px;
  transition: width 0.5s ease;
}

.chart-value {
  width: 45px;
  font-size: 12px;
  font-weight: 600;
  color: #1a1a2e;
  text-align: right;
}

.compatibility-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.compatibility-item {
  text-align: center;
}

.compatibility-label {
  font-size: 11px;
  color: #666;
  margin-bottom: 6px;
}

.compatibility-value {
  font-size: 18px;
  font-weight: 700;
}

.compatibility-value.high {
  color: #27ae60;
}

.compatibility-value.medium {
  color: #f39c12;
}

.compatibility-value.low {
  color: #e74c3c;
}

.recommendation-box {
  background: #f8f9ff;
  border-left: 4px solid #667eea;
  padding: 16px;
  border-radius: 0 8px 8px 0;
  margin: 16px 0;
}

.recommendation-box h4 {
  color: #667eea;
  font-size: 14px;
  margin-bottom: 8px;
}

.recommendation-box p {
  color: #333;
  font-size: 14px;
  line-height: 1.5;
}

.pros-cons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 16px 0;
}

.pros, .cons {
  padding: 14px;
  border-radius: 10px;
  font-size: 13px;
}

.pros {
  background: #e8f5e9;
  color: #2e7d32;
}

.cons {
  background: #ffebee;
  color: #c62828;
}

.pros h4, .cons h4 {
  font-size: 13px;
  margin-bottom: 8px;
}

.pros ul, .cons ul {
  margin: 0;
  padding-left: 18px;
}

.pros li, .cons li {
  margin: 5px 0;
}

@media (max-width: 480px) {
  .pros-cons {
    grid-template-columns: 1fr;
  }
}
</style>
