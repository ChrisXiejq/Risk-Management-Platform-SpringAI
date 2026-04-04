
<template>
  <div style="display: flex; justify-content: center;width: 100%;">
    <div style="padding: 20px;width: 100%; max-width: 1600px;">
      <TopNav style="width: 100%;"></TopNav>
      <div style="width: 100%;min-height: calc(100vh - 280px);">
        <router-view></router-view>
      </div>
      <div style="display: flex; justify-content: center; padding: 24px 0 8px;">
        <a href="https://beian.miit.gov.cn" id="beian" target="_blank" rel="noopener">沪ICP备2024066576号-1</a>
      </div>
    </div>
  </div>
  <el-backtop :right="100" :bottom="100" />
</template>


<script setup>
import TopNav from './components/TopNav.vue';
</script>

<style>
body {
  font-family: Arial, sans-serif;
}
</style>
