<template>
  <div class="store-map-container">
    <div class="map-header">
      <div class="header-left">
        <span class="map-title">门店地图查询</span>
        <div class="filter-bar">
          <select v-model="filters.provinceCode" @change="onProvinceChange" class="filter-select">
            <option value="">全部省份</option>
            <option v-for="p in provinces" :key="p.code" :value="p.code">{{ p.name }}</option>
          </select>
          <select v-model="filters.cityCode" @change="onCityChange" class="filter-select">
            <option value="">全部城市</option>
            <option v-for="c in cities" :key="c.code" :value="c.code">{{ c.name }}</option>
          </select>
          <select v-model="filters.districtCode" @change="onDistrictChange" class="filter-select">
            <option value="">全部区县</option>
            <option v-for="d in districts" :key="d.code" :value="d.code">{{ d.name }}</option>
          </select>
          <select v-model="filters.streetCode" class="filter-select">
            <option value="">全部街道</option>
            <option v-for="s in streets" :key="s.code" :value="s.code">{{ s.name }}</option>
          </select>
          <input v-model="filters.deptName" placeholder="门店名称" class="filter-input" @keyup.enter="applyFilter" />
          <button class="filter-btn" @click="applyFilter">查询</button>
          <button class="filter-btn reset-btn" @click="resetFilter">重置</button>
        </div>
      </div>
      <div class="map-legend">
        <span class="legend-item"><span class="legend-star legend-store-star">★</span>门店</span>
        <span class="legend-item"><span class="legend-star">★</span>顶级部门</span>
        <span class="legend-count">门店: {{ storeCount }}</span>
      </div>
    </div>
    <div id="storeMap" class="map-box"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getStoreMap } from '@/api/system/storeMap'
import { listRegionChildren } from '@/api/system/region'
import { getConfigKey } from '@/api/system/config'

let map: L.Map | null = null
const markers: L.Marker[] = []
const allStores = ref<any[]>([])
const allTopDepts = ref<any[]>([])
const storeCount = ref(0)
const amapKey = ref('')

const filters = reactive({
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  streetCode: '',
  deptName: '',
})

const provinces = ref<any[]>([])
const cities = ref<any[]>([])
const districts = ref<any[]>([])
const streets = ref<any[]>([])

function initMap() {
  map = L.map('storeMap', {
    center: [39.9042, 116.4074],
    zoom: 5,
    zoomControl: true,
  })

  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    attribution: '高德地图',
  }).addTo(map)
}

function addStoreMarker(item: any) {
  if (!map || !item.longitude || !item.latitude) return
  const lat = Number(item.latitude)
  const lng = Number(item.longitude)

  const icon = L.divIcon({
    className: 'store-marker',
    html: '<svg viewBox="0 0 24 24" width="26" height="26"><path d="M12 2l2.9 6.2 6.8.8-5 4.6 1.3 6.7L12 17.8 5.9 20.3 7.2 13.6l-5-4.6 6.8-.8z" fill="#2563eb" stroke="#fff" stroke-width="1.4" stroke-linejoin="round"/></svg>',
    iconSize: [26, 26],
    iconAnchor: [13, 13],
  })
  const marker = L.marker([lat, lng], { icon }).addTo(map)
  marker.bindPopup(
    `<b>${item.deptName}</b><br/>地址: ${item.provinceName || ''}${item.cityName || ''}${item.districtName || ''}${item.streetName || ''}${item.detailAddress || ''}`
  )
  markers.push(marker)
}

function addTopDeptMarker(item: any) {
  if (!map || !item.longitude || !item.latitude) return
  const lat = Number(item.latitude)
  const lng = Number(item.longitude)

  const icon = L.divIcon({
    className: 'top-dept-marker',
    html: '<div class="top-dept-star">★</div>',
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  })
  const marker = L.marker([lat, lng], { icon }).addTo(map)
  marker.bindPopup(`<b>${item.deptName}</b><br/>类型: 顶级部门`)
  markers.push(marker)
}

function clearMarkers() {
  markers.forEach((m) => m.remove())
  markers.length = 0
}

function matchFilter(item: any): boolean {
  if (filters.provinceCode && item.provinceCode !== filters.provinceCode) return false
  if (filters.cityCode && item.cityCode !== filters.cityCode) return false
  if (filters.districtCode && item.districtCode !== filters.districtCode) return false
  if (filters.streetCode && item.streetCode !== filters.streetCode) return false
  if (filters.deptName && !item.deptName?.includes(filters.deptName)) return false
  return true
}

async function applyFilter() {
  clearMarkers()
  const filteredStores = allStores.value.filter(matchFilter)
  const filteredTopDepts = allTopDepts.value.filter(matchFilter)

  filteredStores.forEach((s: any) => addStoreMarker(s))
  filteredTopDepts.forEach((d: any) => addTopDeptMarker(d))
  storeCount.value = filteredStores.length

  if (markers.length > 0) {
    const group = L.featureGroup(markers)
    map?.fitBounds(group.getBounds().pad(0.1))
  } else if (filters.provinceCode) {
    await focusToRegion()
  }
}

function getSelectedRegionName(): string {
  const parts: string[] = []
  const prov = provinces.value.find((p) => p.code === filters.provinceCode)
  const city = cities.value.find((c) => c.code === filters.cityCode)
  const dist = districts.value.find((d) => d.code === filters.districtCode)
  const street = streets.value.find((s) => s.code === filters.streetCode)
  if (prov) parts.push(prov.name)
  if (city) parts.push(city.name)
  if (dist) parts.push(dist.name)
  if (street) parts.push(street.name)
  return parts.join('')
}

async function focusToRegion() {
  const regionName = getSelectedRegionName()
  if (!regionName || !amapKey.value || !map) return
  try {
    const adcode = filters.districtCode || filters.cityCode || filters.provinceCode || ''
    const url = `https://restapi.amap.com/v3/geocode/geo?key=${amapKey.value}&address=${encodeURIComponent(regionName)}${adcode ? `&city=${adcode}` : ''}`
    const res = await fetch(url)
    const data = await res.json()
    if (data.status === '1' && data.geocodes && data.geocodes.length > 0) {
      const [lng, lat] = data.geocodes[0].location.split(',').map(Number)
      let zoom = 11
      if (filters.streetCode) zoom = 14
      else if (filters.districtCode) zoom = 12
      else if (filters.cityCode) zoom = 11
      else if (filters.provinceCode) zoom = 8
      map.setView([lat, lng], zoom)
    }
  } catch (e) {
    console.error('区域定位失败', e)
  }
}

async function loadAmapKey() {
  try {
    const res: any = await getConfigKey('sys.map.amapKey')
    if (res.code === 200 && res.msg) {
      amapKey.value = res.msg
    }
  } catch (e) {
    console.error('加载高德Key失败', e)
  }
}

function resetFilter() {
  filters.provinceCode = ''
  filters.cityCode = ''
  filters.districtCode = ''
  filters.streetCode = ''
  filters.deptName = ''
  cities.value = []
  districts.value = []
  streets.value = []
  applyFilter()
}

async function loadProvinces() {
  try {
    const res: any = await listRegionChildren('0')
    if (res.code === 200) {
      provinces.value = res.data || []
    }
  } catch (e) {
    console.error('加载省份失败', e)
  }
}

async function onProvinceChange() {
  filters.cityCode = ''
  filters.districtCode = ''
  filters.streetCode = ''
  cities.value = []
  districts.value = []
  streets.value = []
  if (filters.provinceCode) {
    const res: any = await listRegionChildren(filters.provinceCode)
    if (res.code === 200) cities.value = res.data || []
  }
  applyFilter()
}

async function onCityChange() {
  filters.districtCode = ''
  filters.streetCode = ''
  districts.value = []
  streets.value = []
  if (filters.cityCode) {
    const res: any = await listRegionChildren(filters.cityCode)
    if (res.code === 200) districts.value = res.data || []
  }
  applyFilter()
}

async function onDistrictChange() {
  filters.streetCode = ''
  streets.value = []
  if (filters.districtCode) {
    const res: any = await listRegionChildren(filters.districtCode)
    if (res.code === 200) streets.value = res.data || []
  }
  applyFilter()
}

async function loadData() {
  try {
    const res: any = await getStoreMap()
    if (res.code === 200 && res.data) {
      allStores.value = res.data.stores || []
      allTopDepts.value = res.data.topDepts || []
      applyFilter()
    }
  } catch (e) {
    console.error('加载门店地图数据失败', e)
  }
}

onMounted(() => {
  initMap()
  loadProvinces()
  loadAmapKey()
  loadData()
})

onUnmounted(() => {
  map?.remove()
  map = null
})
</script>

<style scoped>
.store-map-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
}
.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: var(--el-bg-color, #fff);
  border-bottom: 1px solid var(--el-border-color-light, #ebeef5);
  flex-wrap: wrap;
  gap: 8px;
}
.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.map-title {
  font-size: 16px;
  font-weight: 600;
}
.filter-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.filter-select {
  padding: 6px 10px;
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 4px;
  font-size: 13px;
  background: var(--el-bg-color, #fff);
  color: var(--el-text-color-primary, #303133);
  outline: none;
  min-width: 120px;
}
.filter-input {
  padding: 6px 10px;
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 4px;
  font-size: 13px;
  background: var(--el-bg-color, #fff);
  color: var(--el-text-color-primary, #303133);
  outline: none;
  min-width: 140px;
}
.filter-btn {
  padding: 6px 16px;
  background: var(--el-color-primary, #409eff);
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
.filter-btn:hover {
  opacity: 0.85;
}
.reset-btn {
  background: var(--el-border-color, #dcdfe6);
  color: var(--el-text-color-primary, #303133);
}
.map-legend {
  display: flex;
  gap: 20px;
  font-size: 13px;
  align-items: center;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}
.legend-store {
  background: #2563eb;
}
.legend-star {
  color: #f59e0b;
  font-size: 18px;
}
.legend-store-star {
  color: #2563eb;
}
.legend-count {
  color: var(--el-text-color-secondary, #909399);
}
.map-box {
  flex: 1;
  width: 100%;
}
</style>

<style>
.store-marker {
  filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.5));
}
.store-marker svg {
  display: block;
}
.top-dept-marker .top-dept-star {
  font-size: 30px;
  color: #f59e0b;
  text-shadow: -1px -1px 0 #fff, 1px -1px 0 #fff, -1px 1px 0 #fff, 1px 1px 0 #fff, 0 2px 4px rgba(0, 0, 0, 0.5);
  line-height: 30px;
  text-align: center;
}
</style>
