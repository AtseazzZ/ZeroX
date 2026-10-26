<script setup lang="ts">
const author = {
  name: 'Atsea_zzZ',
  link: 'https://github.com/AtseazzZ',
}
</script>

<template>
  <div class="global-footer">
    <span class="copyright">
      个人开发项目，作者
      <a :href="author.link" target="_blank" rel="noopener noreferrer" class="author-link">
        {{ author.name }}
      </a>
    </span>
  </div>
</template>

<style scoped>
.global-footer {
  width: 100%;
  padding: 20px 24px;
  text-align: center;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.copyright {
  font-size: 13px;
  color: #8c8c8c;
}

.author-link {
  color: #1a1a1a;
  font-weight: 500;
  text-decoration: none;
  margin-left: 2px;
  transition: color 0.2s;
}

.author-link:hover {
  color: #00b96b;
}
</style>
