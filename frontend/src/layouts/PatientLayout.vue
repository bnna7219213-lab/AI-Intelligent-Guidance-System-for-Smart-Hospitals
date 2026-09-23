<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <span class="logo-text">患者服务</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="layout-menu"
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#fff"
        router
      >
        <el-menu-item index="/patient/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
        <el-menu-item index="/patient/consultation">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI问诊</span>
        </el-menu-item>
        <el-menu-item index="/patient/triage">
          <el-icon><FirstAidKit /></el-icon>
          <span>智能分诊</span>
        </el-menu-item>
        <el-menu-item index="/patient/registration">
          <el-icon><DocumentChecked /></el-icon>
          <span>预约挂号</span>
        </el-menu-item>
        <el-menu-item index="/patient/my-visits">
          <el-icon><Calendar /></el-icon>
          <span>我的就诊</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <span class="page-title">{{ currentPageTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              <span class="username">{{ username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const username = computed(() => userStore.username)
const currentPageTitle = computed(() => route.meta?.title || '')

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: #001529;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.layout-menu {
  border-right: none;
  height: calc(100vh - 60px);
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}

.header-left .page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.username {
  font-size: 14px;
  color: #606266;
}

.layout-main {
  background-color: #f0f2f5;
  padding: 0;
  overflow-y: auto;
}
</style>
