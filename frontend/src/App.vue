<template>
  <div id="app">
    <header class="app-header">
      <h1 @click="goHome" class="app-title">MISIS Bites</h1>
    </header>
    <main class="app-content">
      <router-view />
    </main>
  </div>
</template>

<script>
export default {
  name: 'App',
  methods: {
    goHome() {
      this.$router.push('/')
    }
  }
}
</script>

<style>
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 16px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  position: sticky;
  top: 0;
  z-index: 100;
}

.app-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  cursor: pointer;
}

.app-content {
  flex: 1;
  padding: 16px;
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
