<template>
  <el-dialog
    v-model="visible"
    title="地图选点"
    width="820px"
    append-to-body
    destroy-on-close
    @opened="onOpened"
    @closed="onClosed"
  >
    <div class="map-picker">
      <div class="map-picker-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索地点（如：天安门、万达广场、xx路xx号）"
          clearable
          @keyup.enter="onSearch"
        >
          <template #append>
            <el-button :icon="Search" :loading="searchLoading" @click="onSearch">搜索</el-button>
          </template>
        </el-input>
        <ul v-if="searchResults.length > 0" class="search-result-list">
          <li
            v-for="(item, idx) in searchResults"
            :key="idx"
            class="search-result-item"
            @click="onSelectSearchResult(item)"
          >
            <div class="search-result-name">{{ item.name }}</div>
            <div class="search-result-addr">{{ item.address || item.district || '' }}</div>
          </li>
        </ul>
      </div>
      <div class="map-picker-info">
        <span>经度: {{ pickedLng != null ? pickedLng.toFixed(7) : '-' }}</span>
        <span>纬度: {{ pickedLat != null ? pickedLat.toFixed(7) : '-' }}</span>
        <span class="addr">地址: {{ pickedAddress || (amapKey ? '点击地图获取' : '未配置高德Key，仅回填坐标') }}</span>
      </div>
      <div ref="mapContainerRef" class="map-picker-map"></div>
      <div class="map-picker-tip">
        <span>点击地图选择位置，或在上方搜索地点</span>
        <span v-if="!amapKey" class="tip-warn">（提示：配置系统参数 sys.map.amapKey 后可自动填充详细地址）</span>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="pickedLng == null || pickedLat == null" @click="onConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getConfigKey } from '@/api/system/config'

const props = defineProps<{
  modelValue: boolean
  initialLng?: number
  initialLat?: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'confirm', payload: {
    lng: number
    lat: number
    address: string
    region?: {
      provinceName: string
      cityName: string
      districtName: string
      adcode: string
      townName: string
      towncode: string
    }
  }): void
}>()

const visible = ref(props.modelValue)
const mapContainerRef = ref<HTMLElement>()
let map: L.Map | null = null
let marker: L.Marker | null = null

const pickedLng = ref<number | undefined>(undefined)
const pickedLat = ref<number | undefined>(undefined)
const pickedAddress = ref('')
const pickedRegion = ref<{
  provinceName: string
  cityName: string
  districtName: string
  adcode: string
  townName: string
  towncode: string
} | undefined>(undefined)
const amapKey = ref('')

const searchKeyword = ref('')
const searchLoading = ref(false)
const searchResults = ref<any[]>([])

watch(() => props.modelValue, (val) => { visible.value = val })
watch(visible, (val) => { emit('update:modelValue', val) })

async function loadAmapKey() {
  try {
    const res: any = await getConfigKey('sys.map.amapKey')
    if (res.code === 200 && res.msg) {
      amapKey.value = res.msg
    }
  } catch {
    amapKey.value = ''
  }
}

async function onOpened() {
  pickedLng.value = props.initialLng
  pickedLat.value = props.initialLat
  pickedAddress.value = ''
  pickedRegion.value = undefined

  await loadAmapKey()
  await nextTick()
  initMap()
}

function initMap() {
  if (!mapContainerRef.value) return

  const hasInitial = props.initialLat != null && props.initialLng != null
  const center: [number, number] = hasInitial
    ? [Number(props.initialLat), Number(props.initialLng)]
    : [39.9042, 116.4074]

  map = L.map(mapContainerRef.value, {
    center,
    zoom: hasInitial ? 16 : 11,
    zoomControl: true,
  })

  L.tileLayer(
    'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
    { subdomains: ['1', '2', '3', '4'], maxZoom: 18, attribution: '高德地图' },
  ).addTo(map)

  if (hasInitial) {
    marker = createMarker(Number(props.initialLat), Number(props.initialLng))
    marker.addTo(map)
  } else {
    // 没有初始坐标时，尝试浏览器定位
    locateUser()
  }

  map.on('click', onMapClick)
}

function locateUser() {
  if (!navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(
    (position) => {
      if (!map) return
      const { latitude, longitude } = position.coords
      map.setView([latitude, longitude], 14)
    },
    () => {
      // 定位失败，保持默认中心点
    },
    { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 },
  )
}

function createMarker(lat: number, lng: number): L.Marker {
  const icon = L.divIcon({
    className: 'map-picker-marker',
    html: '<div class="map-picker-pin"></div>',
    iconSize: [20, 20],
    iconAnchor: [10, 10],
  })
  return L.marker([lat, lng], { icon })
}

function onMapClick(e: any) {
  const { lat, lng } = e.latlng
  pickedLng.value = lng
  pickedLat.value = lat
  pickedAddress.value = amapKey.value ? '正在获取地址...' : ''
  pickedRegion.value = undefined

  if (marker) {
    marker.setLatLng([lat, lng])
  } else if (map) {
    marker = createMarker(lat, lng)
    marker.addTo(map)
  }

  if (amapKey.value) {
    reverseGeocode(lng, lat)
  }
}

async function reverseGeocode(lng: number, lat: number) {
  try {
    const url = `https://restapi.amap.com/v3/geocode/regeo?key=${amapKey.value}&location=${lng.toFixed(6)},${lat.toFixed(6)}&extensions=all`
    const res = await fetch(url)
    const data = await res.json()
    if (data.status === '1' && data.regeocode) {
      pickedAddress.value = data.regeocode.formatted_address || ''
      const ac = data.regeocode.addressComponent || {}
      const provinceName = Array.isArray(ac.province) ? (ac.province[0] || '') : (ac.province || '')
      const cityName = Array.isArray(ac.city) ? (ac.city[0] || '') : (ac.city || '')
      const districtName = Array.isArray(ac.district) ? (ac.district[0] || '') : (ac.district || '')
      const townName = Array.isArray(ac.township) ? (ac.township[0] || '') : (ac.township || '')
      const towncode = Array.isArray(ac.towncode) ? (ac.towncode[0] || '') : (ac.towncode || '')
      const adcode = ac.adcode || ''
      pickedRegion.value = {
        provinceName,
        cityName: cityName || provinceName,
        districtName,
        adcode,
        townName,
        towncode,
      }
    } else {
      pickedAddress.value = ''
      pickedRegion.value = undefined
    }
  } catch {
    pickedAddress.value = ''
    pickedRegion.value = undefined
  }
}

async function onSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    searchResults.value = []
    return
  }
  if (!amapKey.value) {
    ElMessage.warning('未配置高德Key，无法使用搜索功能')
    return
  }
  searchLoading.value = true
  try {
    const url = `https://restapi.amap.com/v3/place/text?key=${amapKey.value}&keywords=${encodeURIComponent(keyword)}&offset=10&page=1&extensions=base`
    const res = await fetch(url)
    const data = await res.json()
    if (data.status === '1' && Array.isArray(data.pois)) {
      searchResults.value = data.pois.map((poi: any) => ({
        name: poi.name,
        address: poi.address || poi.typecode || '',
        location: poi.location,
      }))
    } else {
      searchResults.value = []
      ElMessage.info('未搜索到相关地点')
    }
  } catch {
    searchResults.value = []
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    searchLoading.value = false
  }
}

function onSelectSearchResult(item: any) {
  if (!item.location || !map) return
  const [lngStr, latStr] = item.location.split(',')
  const lng = Number(lngStr)
  const lat = Number(latStr)

  pickedLng.value = lng
  pickedLat.value = lat
  pickedAddress.value = item.address ? `${item.name}（${item.address}）` : item.name
  pickedRegion.value = undefined

  if (marker) {
    marker.setLatLng([lat, lng])
  } else {
    marker = createMarker(lat, lng)
    marker.addTo(map)
  }

  map.setView([lat, lng], 16)
  searchResults.value = []
  searchKeyword.value = item.name

  if (amapKey.value) {
    reverseGeocode(lng, lat)
  }
}

function onConfirm() {
  if (pickedLng.value != null && pickedLat.value != null) {
    emit('confirm', {
      lng: pickedLng.value,
      lat: pickedLat.value,
      address: pickedAddress.value,
      region: pickedRegion.value,
    })
  }
  visible.value = false
}

function onClosed() {
  map?.remove()
  map = null
  marker = null
}
</script>

<style scoped>
.map-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.map-picker-search {
  position: relative;
}
.search-result-list {
  position: absolute;
  top: 32px;
  left: 0;
  right: 0;
  z-index: 1000;
  max-height: 260px;
  overflow-y: auto;
  margin: 4px 0 0;
  padding: 0;
  list-style: none;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
.search-result-item {
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}
.search-result-item:last-child {
  border-bottom: none;
}
.search-result-item:hover {
  background: #f5f7fa;
}
.search-result-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}
.search-result-addr {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}
.map-picker-info {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #606266;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  flex-wrap: wrap;
}
.map-picker-info .addr {
  flex: 1;
  min-width: 200px;
}
.map-picker-map {
  width: 100%;
  height: 420px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}
.map-picker-tip {
  font-size: 12px;
  color: #909399;
  text-align: center;
}
.map-picker-tip .tip-warn {
  color: #e6a23c;
}
</style>

<style>
.map-picker-marker .map-picker-pin {
  width: 20px;
  height: 20px;
  border-radius: 50% 50% 50% 0;
  background: #e74c3c;
  transform: rotate(-45deg);
  border: 2px solid #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}
</style>
