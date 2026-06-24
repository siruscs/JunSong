<template>
  <div class="density-container">
    <div class="density-header">
      <span class="density-title">门店密度查询</span>
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
        <button class="filter-btn" @click="onQuery">查询</button>
      </div>
      <div class="density-info">
        <span>覆盖半径: {{ coverageRadius }}米</span>
        <span>门店数: {{ storeCount }}</span>
      </div>
    </div>
    <div id="densityMap" class="density-map-box"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getStoreDensity } from '@/api/system/storeMap'
import { listRegionChildren } from '@/api/system/region'
import { getConfigKey } from '@/api/system/config'

let map: L.Map | null = null
let circleLayer: L.LayerGroup | null = null
let markerLayer: L.LayerGroup | null = null

const coverageRadius = ref(1500)
const storeCount = ref(0)
const amapKey = ref('')

const filters = reactive({
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  streetCode: '',
})

const provinces = ref<any[]>([])
const cities = ref<any[]>([])
const districts = ref<any[]>([])
const streets = ref<any[]>([])

function initMap() {
  map = L.map('densityMap', {
    center: [39.9042, 116.4074],
    zoom: 5,
    zoomControl: true,
  })

  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    attribution: '高德地图',
  }).addTo(map)

  circleLayer = L.layerGroup().addTo(map)
  markerLayer = L.layerGroup().addTo(map)
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
}

async function onDistrictChange() {
  filters.streetCode = ''
  streets.value = []
  if (filters.districtCode) {
    const res: any = await listRegionChildren(filters.districtCode)
    if (res.code === 200) streets.value = res.data || []
  }
}

async function loadCoverageRadius() {
  try {
    const res: any = await getConfigKey('sys.dept.coverageRadius')
    if (res.code === 200 && res.msg) {
      coverageRadius.value = parseInt(res.msg) || 1500
    }
  } catch (e) {
    console.error('加载覆盖半径配置失败', e)
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
      // 根据筛选层级决定缩放级别
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

async function loadData() {
  try {
    const res: any = await getStoreDensity(filters)
    if (res.code === 200 && res.data) {
      circleLayer?.clearLayers()
      markerLayer?.clearLayers()

      const stores = res.data || []
      storeCount.value = stores.length

      const allMarkers: L.Marker[] = []

      stores.forEach((s: any) => {
        if (!s.longitude || !s.latitude) return
        const lat = Number(s.latitude)
        const lng = Number(s.longitude)

        // 深色覆盖圆（表示已覆盖区域）
        const circle = L.circle([lat, lng], {
          radius: coverageRadius.value,
          color: '#1a1a2e',
          fillColor: '#1a1a2e',
          fillOpacity: 0.45,
          weight: 1,
          opacity: 0.6,
        })
        circle.addTo(circleLayer!)
        circle.bindPopup(`<b>${s.deptName}</b><br/>覆盖半径: ${coverageRadius.value}米`)

        // 门店标记点
        const icon = L.divIcon({
          className: 'density-store-marker',
          html: '<svg viewBox="0 0 24 24" width="26" height="26"><path d="M12 2l2.9 6.2 6.8.8-5 4.6 1.3 6.7L12 17.8 5.9 20.3 7.2 13.6l-5-4.6 6.8-.8z" fill="#fbbf24" stroke="#fff" stroke-width="1.4" stroke-linejoin="round"/></svg>',
          iconSize: [26, 26],
          iconAnchor: [13, 13],
        })
        const marker = L.marker([lat, lng], { icon }).addTo(markerLayer!)
        marker.bindPopup(
          `<b>${s.deptName}</b><br/>地址: ${s.provinceName || ''}${s.cityName || ''}${s.districtName || ''}${s.streetName || ''}${s.detailAddress || ''}`
        )
        allMarkers.push(marker)
      })

      // 有门店：适应门店视野；无门店但有筛选条件：聚焦到查询区域
      if (allMarkers.length > 0) {
        const group = L.featureGroup(allMarkers)
        map?.fitBounds(group.getBounds().pad(0.2))
      } else if (filters.provinceCode) {
        await focusToRegion()
      }
    }
  } catch (e) {
    console.error('加载门店密度数据失败', e)
  }
}

async function onQuery() {
  await loadData()
  // 查询后始终聚焦到选中区域（若有筛选条件）
  if (filters.provinceCode) {
    await focusToRegion()
  }
}

onMounted(async () => {
  initMap()
  await Promise.all([loadProvinces(), loadCoverageRadius(), loadAmapKey()])
  await loadData()
})

onUnmounted(() => {
  map?.remove()
  map = null
})
</script>

<style scoped>
.density-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
}
.density-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 16px;
  background: var(--el-bg-color, #fff);
  border-bottom: 1px solid var(--el-border-color-light, #ebeef5);
}
.density-title {
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
.density-info {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: var(--el-text-color-secondary, #909399);
}
.density-map-box {
  flex: 1;
  width: 100%;
}
</style>

<style>
.density-store-marker {
  filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.7));
}
.density-store-marker svg {
  display: block;
}
</style>
