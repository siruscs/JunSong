<template>
  <div class="app-container report-page">
    <div class="pause-notice-wrapper">
      <el-card class="pause-notice-card" shadow="never">
        <el-result icon="warning" title="库存报表暂未开放">
          <template #sub-title>
            <p class="pause-description">
              库存报表数据口径尚未稳定，正在进行库存流水和库存快照表的设计与建设，待口径稳定后将开放使用。
            </p>
            <div class="pause-building-list">
              <p class="pause-list-title">当前正在建设的内容：</p>
              <ul>
                <li>库存流水表设计（记录每笔库存变动的完整流水）</li>
                <li>库存快照表设计（按日汇总的库存快照数据）</li>
                <li>业务入口改造：采购入库</li>
                <li>业务入口改造：销售扣减</li>
                <li>业务入口改造：盘点调整</li>
                <li>业务入口改造：退货入库</li>
              </ul>
            </div>
          </template>
          <template #extra>
            <el-button type="primary" @click="$router.back()">返回上一页</el-button>
          </template>
        </el-result>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  name: "StockReport"
};
</script>

<style scoped>
.report-page {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.pause-notice-wrapper {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 80px;
}

.pause-notice-card {
  max-width: 640px;
  width: 100%;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.pause-notice-card :deep(.el-card__body) {
  padding: 0;
}

.pause-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 16px;
}

.pause-building-list {
  text-align: left;
  background: #f5f7fb;
  border-radius: 6px;
  padding: 16px 20px;
}

.pause-list-title {
  color: #303133;
  font-weight: 600;
  font-size: 14px;
  margin: 0 0 10px;
}

.pause-building-list ul {
  margin: 0;
  padding-left: 18px;
}

.pause-building-list li {
  color: #606266;
  font-size: 13px;
  line-height: 2;
}
</style>
