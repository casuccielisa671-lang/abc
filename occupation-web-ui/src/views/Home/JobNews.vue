<template>
  <div class="job-news" :class="{ 'job-news--visible': visible }">
    <!-- 分类栏 -->
    <nav class="job-news__cats">
      <button
        v-for="cat in NEWS_CATEGORIES"
        :key="cat.id"
        type="button"
        class="cat-pill"
        :class="{ active: activeCat === cat.id }"
        @click="activeCat = cat.id"
      >
        {{ cat.label }}
      </button>
    </nav>

    <!-- 顶部：横幅 + 热榜 -->
    <div class="job-news__hero">
      <article
        v-if="heroNews"
        class="hero-banner"
        @click="openDetail(heroNews)"
      >
        <img
          :src="heroNews.image"
          :alt="heroNews.title"
          class="hero-banner__img"
          @error="onImgError($event, heroNews.placeholder)"
        />
        <div class="hero-banner__mask" />
        <span class="hero-banner__tag">{{ heroNews.tag }}</span>
        <h2 class="hero-banner__title">{{ heroNews.title }}</h2>
        <p class="hero-banner__summary">{{ heroNews.summary }}</p>
        <time class="hero-banner__time">{{ heroNews.time }}</time>
      </article>

      <aside class="hot-rank">
        <header class="section-head">
          <span>24 小时就业热榜</span>
          <a href="javascript:;" class="more-link">更多</a>
        </header>
        <ul class="hot-rank__list">
          <li
            v-for="(item, idx) in hotRankList"
            :key="item.id"
            class="hot-rank__item"
            @click="openDetail(item)"
          >
            <span class="rank-badge" :class="`rank-${idx + 1}`">{{ idx + 1 }}</span>
            <span class="rank-title">{{ item.title }}</span>
            <span class="rank-hot">{{ item.hot }}</span>
          </li>
        </ul>
      </aside>
    </div>

    <!-- 缩略卡片行 -->
    <div class="job-news__cards">
      <article
        v-for="item in cardNews"
        :key="item.id"
        class="thumb-card"
        @click="openDetail(item)"
      >
        <img
          :src="item.image"
          :alt="item.title"
          class="thumb-card__img"
          @error="onImgError($event, item.placeholder)"
        />
        <div class="thumb-card__mask" />
        <span class="thumb-card__tag">{{ item.tag }}</span>
        <h3 class="thumb-card__title">{{ item.title }}</h3>
      </article>
    </div>

    <!-- 底部三栏 -->
    <div class="job-news__bottom">
      <section class="col-latest">
        <header class="section-head"><span>最新资讯</span></header>
        <ul class="latest-list">
          <li
            v-for="item in filteredList.slice(0, 5)"
            :key="'l-' + item.id"
            class="latest-item"
            @click="openDetail(item)"
          >
            <img
              :src="item.image"
              :alt="item.title"
              class="latest-item__thumb"
              @error="onImgError($event, item.placeholder)"
            />
            <div class="latest-item__body">
              <h4>{{ item.title }}</h4>
              <span class="latest-item__tag">{{ item.tag }}</span>
              <p>{{ item.summary }}</p>
            </div>
          </li>
        </ul>
      </section>

      <section class="col-flash">
        <header class="section-head">
          <span>精选快讯</span>
          <a href="javascript:;" class="more-link">更多</a>
        </header>
        <ul class="flash-list">
          <li v-for="(f, i) in flashNewsList" :key="i" class="flash-item">
            <time>{{ f.time }}</time>
            <span>{{ f.text }}</span>
          </li>
        </ul>
      </section>

      <section class="col-topics">
        <header class="section-head">
          <span>热门话题</span>
          <a href="javascript:;" class="more-link">更多</a>
        </header>
        <div class="topic-grid">
          <div v-for="(t, i) in hotTopics" :key="i" class="topic-card">
            <span class="topic-tag">{{ t.tag }}</span>
            <span class="topic-count">{{ t.count }}</span>
          </div>
        </div>
      </section>
    </div>

    <button type="button" class="back-top-btn" title="回到地图首页" @click="$emit('back-top')">
      <el-icon :size="20"><Top /></el-icon>
    </button>

    <el-dialog v-model="dialogVisible" :title="currentNews?.title" width="560px" class="news-dialog">
      <div v-if="currentNews" class="dialog-body">
        <img
          :src="currentNews.image"
          :alt="currentNews.title"
          class="dialog-body__img"
          @error="onImgError($event, currentNews.placeholder)"
        />
        <div class="dialog-meta">
          <el-tag size="small" type="primary">{{ currentNews.tag }}</el-tag>
          <span>{{ currentNews.time }}</span>
        </div>
        <p>{{ currentNews.content }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Top } from '@element-plus/icons-vue'
import {
  NEWS_CATEGORIES,
  jobNewsList,
  hotRankList,
  flashNewsList,
  hotTopics
} from './jobNewsData'

defineProps({
  visible: { type: Boolean, default: false }
})
defineEmits(['back-top'])

const activeCat = ref('all')
const dialogVisible = ref(false)
const currentNews = ref(null)

const filteredList = computed(() =>
  activeCat.value === 'all'
    ? jobNewsList
    : jobNewsList.filter(n => n.category === activeCat.value)
)

const heroNews = computed(() => filteredList.value.find(n => n.featured) || filteredList.value[0])
const cardNews = computed(() =>
  filteredList.value.filter(n => n.id !== heroNews.value?.id).slice(0, 4)
)

function onImgError(e, placeholder) {
  if (placeholder && e.target.src !== placeholder) e.target.src = placeholder
}

function openDetail(item) {
  currentNews.value = item
  dialogVisible.value = true
}
</script>

<style scoped src="./jobNews.css"></style>
