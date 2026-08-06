<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="mb8">
      <el-form-item label="顾客"><el-input v-model="query.customerName" placeholder="请输入顾客姓名" clearable /></el-form-item>
      <el-form-item label="顾客类型">
        <el-select v-model="query.customerType" clearable style="width: 130px">
          <el-option label="会员" value="MEMBER" /><el-option label="非会员" value="CUSTOMER" /><el-option label="散客" value="WALK_IN" />
        </el-select>
      </el-form-item>
      <el-form-item label="收款状态">
        <el-select v-model="query.paymentStatus" clearable style="width: 120px">
          <el-option label="未付款" value="0" /><el-option label="部分付款" value="1" /><el-option label="已付清" value="2" />
        </el-select>
      </el-form-item>
        <el-form-item label="购买日期"><el-date-picker class="purchase-date-range" v-model="purchaseDateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="handleQuery">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <div class="mb8 table-toolbar">
      <el-button type="primary" @click="openCreate" v-hasPermi="['member:purchase:add']">新建购买单</el-button>
      <el-button @click="load">刷新</el-button>
      <el-button type="warning" @click="handleExport" v-hasPermi="['member:purchase:export']">导出 XLSX</el-button>
      <el-button type="info" @click="openReturnList" v-hasPermi="['member:purchaseReturn:list']">退货记录</el-button>
    </div>
    <el-row :gutter="12" class="mb8 purchase-summary" v-if="summary">
      <el-col :xs="12" :sm="8" :lg="4"><div class="purchase-summary-card purchase-summary-card--orders"><span class="purchase-summary-label">购买单数</span><strong>{{ summary.purchaseOrderCount || 0 }}</strong><small>当前筛选结果</small></div></el-col>
      <el-col :xs="12" :sm="8" :lg="4"><div class="purchase-summary-card purchase-summary-card--quantity"><span class="purchase-summary-label">购买数量</span><strong>{{ quantity(summary.purchaseQuantity) }}</strong><small>销售数量</small></div></el-col>
      <el-col :xs="12" :sm="8" :lg="4"><div class="purchase-summary-card purchase-summary-card--gift"><span class="purchase-summary-label">赠送数量</span><strong>{{ quantity(summary.giftQuantity) }}</strong><small>活动赠送</small></div></el-col>
      <el-col :xs="12" :sm="8" :lg="4"><div class="purchase-summary-card purchase-summary-card--receivable"><span class="purchase-summary-label">应收金额</span><strong>{{ money(summary.totalAmount) }}</strong><small>订单总应收</small></div></el-col>
      <el-col :xs="12" :sm="8" :lg="4"><div class="purchase-summary-card purchase-summary-card--paid"><span class="purchase-summary-label">已收金额</span><strong>{{ money(summary.paidAmount) }}</strong><small>累计已收</small></div></el-col>
      <el-col :xs="12" :sm="8" :lg="4"><div class="purchase-summary-card purchase-summary-card--debt"><span class="purchase-summary-label">欠款金额</span><strong>{{ money(summary.receivableAmount) }}</strong><small>待收余额</small></div></el-col>
    </el-row>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="purchaseNo" label="购买单号" min-width="170" />
      <el-table-column prop="customerName" label="顾客" min-width="120" />
      <el-table-column prop="customerType" label="类型" width="100"><template #default="scope">{{ customerTypeLabel(scope.row.customerType) }}</template></el-table-column>
      <el-table-column prop="purchaseQuantity" label="购买数量" width="110" />
      <el-table-column prop="giftQuantity" label="赠送数量" width="110" />
      <el-table-column prop="totalAmount" label="应收金额" width="140"><template #default="scope"><span style="color: #F56C6C; font-weight: bold;">{{ money(scope.row.totalAmount) }}</span></template></el-table-column>
      <el-table-column prop="paidAmount" label="已收" width="140"><template #default="scope"><span style="color: #67C23A; font-weight: bold;">{{ money(scope.row.paidAmount) }}</span></template></el-table-column>
      <el-table-column prop="receivableAmount" label="欠款" width="140"><template #default="scope"><span style="color: #E6A23C; font-weight: bold;">{{ money(scope.row.receivableAmount) }}</span></template></el-table-column>
      <el-table-column prop="paymentStatus" label="收款状态" width="110"><template #default="scope">{{ paymentStatusLabel(scope.row.paymentStatus) }}</template></el-table-column>
      <el-table-column prop="deliveryStatus" label="领取状态" width="110"><template #default="scope">{{ deliveryStatusLabel(scope.row.deliveryStatus) }}</template></el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170"><template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template></el-table-column>
    <el-table-column label="操作" fixed="right" width="270" class-name="purchase-operation-column">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)" v-hasPermi="['member:purchase:query']">查看</el-button>
          <el-button v-if="!isCancelled(scope.row)" v-hasPermi="['member:purchase:edit']" link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button v-if="canReceive(scope.row)" v-hasPermi="['member:purchase:payment']" link type="success" @click="openPayment(scope.row)">收款</el-button>
          <el-button v-if="canDeliver(scope.row)" v-hasPermi="['member:purchase:delivery']" link type="warning" @click="openDelivery(scope.row)">领取</el-button>
          <el-button v-if="scope.row.customerType === 'WALK_IN' && !scope.row.memberId" v-hasPermi="['member:purchase:bind']" link type="primary" @click="openBind(scope.row)">绑定会员</el-button>
          <el-button v-if="canCancel(scope.row)" v-hasPermi="['member:purchase:cancel']" link type="danger" @click="cancel(scope.row)">作废</el-button>
          <el-button v-if="canReturn(scope.row)" v-hasPermi="['member:purchaseReturn:add']" link type="warning" @click="openReturn(scope.row)">退货</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="load" />
    <el-dialog v-model="createOpen" title="新建购买单" width="860px" @open="loadCreateOptions">
      <el-form :model="createForm" label-width="150px">
        <el-form-item label="顾客类型"><el-select v-model="createForm.customerType"><el-option label="会员" value="MEMBER" /><el-option label="散客" value="WALK_IN" /><el-option label="非会员" value="CUSTOMER" /></el-select></el-form-item>
        <el-form-item v-if="createForm.customerType === 'MEMBER'" label="会员"><MemberSelect v-model="createForm.memberId" @change="handleMemberChange" /></el-form-item>
        <el-form-item v-else label="顾客姓名"><el-input v-model="createForm.customerName" placeholder="可不填" /></el-form-item>
        <el-form-item label="购买日期"><el-date-picker v-model="createForm.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择购买日期" style="width: 100%" /></el-form-item>
        <el-form-item label="核算周期"><el-input v-model="createPeriodLabel" disabled /></el-form-item>
        <el-divider content-position="left">首个购买明细</el-divider>
        <el-form-item label="商品"><el-select v-model="createForm.item.productId" filterable placeholder="请选择商品" style="width: 100%" @change="handleCreateProductChange"><el-option v-for="product in productOptions" :key="product.productId" :label="`${product.productName}（${product.productCode || product.productId}）`" :value="product.productId" /></el-select></el-form-item>
        <el-form-item label="销售政策（可选）"><el-select v-model="createForm.item.policyId" filterable clearable placeholder="不参加活动，按单价购买" style="width: 100%" :disabled="!createForm.item.productId" @change="handleCreatePolicyChange"><el-option v-for="policy in policyOptions" :key="policy.policyId" :label="`${policy.policyName}（${policy.policyNo}）`" :value="policy.policyId" /></el-select></el-form-item>
        <el-form-item label="购买套餐（可选）"><el-select v-model="createForm.item.packageId" clearable placeholder="不选套餐" style="width: 100%" :disabled="!createForm.item.policyId" @change="handleCreatePackageChange"><el-option v-for="item in packageOptions" :key="item.packageId" :label="`${item.packageName}：买${item.purchaseQuantity}送${item.giftQuantity}`" :value="item.packageId" /></el-select></el-form-item>
        <el-form-item label="购买数量"><el-input-number v-model="createForm.item.purchaseQuantity" :min="0.001" :precision="3" :step="0.001" placeholder="0.000" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="单价（可修改）"><el-input-number v-model="createForm.item.unitPrice" :precision="2" :min="0.01" :step="0.01" placeholder="0.00" controls-position="right" :disabled="!!createForm.item.policyId" style="width: 100%" /></el-form-item>
        <el-form-item label="赠送数量"><el-input-number v-model="createForm.item.giftQuantity" :precision="3" :step="0.001" placeholder="0.000" controls-position="right" disabled style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createOpen = false">取消</el-button><el-button type="primary" @click="create">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="returnOpen" title="新建退货/退款单" width="700px">
      <el-form :model="returnForm" label-width="130px">
        <el-form-item label="原购买单号">{{ returnForm.purchaseNo }}</el-form-item>
        <el-form-item label="退货办理周期"><el-select v-model="returnForm.returnPeriodId" placeholder="请选择当前机构核算周期" style="width: 100%"><el-option v-for="period in returnPeriods" :key="period.periodId" :label="periodLabel(period)" :value="period.periodId" /></el-select></el-form-item>
        <el-table :data="returnForm.items" border>
          <el-table-column prop="productNameSnapshot" label="商品" min-width="180" />
          <el-table-column prop="availableSaleQuantity" label="原正品数量" width="110" />
          <el-table-column prop="availableGiftQuantity" label="原赠品数量" width="110" />
          <el-table-column label="退正品" width="145"><template #default="scope"><el-input-number v-model="scope.row.returnSaleQuantity" :min="0" :max="scope.row.availableSaleQuantity" :precision="3" :step="0.001" controls-position="right" /></template></el-table-column>
          <el-table-column label="退赠品" width="145"><template #default="scope"><el-input-number v-model="scope.row.returnGiftQuantity" :min="0" :max="scope.row.availableGiftQuantity" :precision="3" :step="0.001" controls-position="right" /></template></el-table-column>
        </el-table>
        <el-form-item label="退货原因"><el-input v-model="returnForm.reason" type="textarea" :rows="2" placeholder="请填写退货原因" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="returnOpen = false">取消</el-button><el-button type="primary" @click="submitReturn">保存退货单</el-button></template>
    </el-dialog>
    <el-dialog v-model="returnListOpen" title="退货/退款记录" width="980px">
      <el-table v-loading="returnLoading" :data="returnRows" border>
        <el-table-column prop="returnNo" label="退货单号" min-width="190" />
        <el-table-column prop="purchaseId" label="原购买单ID" width="110" />
        <el-table-column prop="customerName" label="顾客" width="120" />
        <el-table-column prop="returnDate" label="退货日期" min-width="170"><template #default="scope">{{ formatDateTime(scope.row.returnDate) }}</template></el-table-column>
        <el-table-column prop="refundAmount" label="退款金额" width="140"><template #default="scope"><span class="money-danger">{{ money(scope.row.refundAmount) }}</span></template></el-table-column>
        <el-table-column prop="status" label="状态" width="110"><template #default="scope">{{ returnStatusLabel(scope.row.status) }}</template></el-table-column>
        <el-table-column label="操作" width="150"><template #default="scope"><el-button link type="primary" @click="openReturnDetail(scope.row)">详情</el-button><el-button v-if="String(scope.row.status) === 'DRAFT'" v-hasPermi="['member:purchaseReturn:complete']" link type="success" @click="completeReturn(scope.row)">完成退货</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!returnLoading && !returnRows.length" description="暂无退货记录" :image-size="60" />
    </el-dialog>
    <el-dialog v-model="returnDetailOpen" title="退货/退款单详情" width="760px">
      <el-descriptions v-if="returnDetail" :column="2" border>
        <el-descriptions-item label="退货单号">{{ returnDetail.returnNo }}</el-descriptions-item>
        <el-descriptions-item label="原购买单ID">{{ returnDetail.purchaseId }}</el-descriptions-item>
        <el-descriptions-item label="顾客">{{ returnDetail.customerName || '未登记顾客' }}</el-descriptions-item>
        <el-descriptions-item label="退货办理日期">{{ formatDateTime(returnDetail.returnDate) }}</el-descriptions-item>
        <el-descriptions-item label="原购买周期">{{ returnDetail.originalPeriodId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退货办理周期">{{ returnDetail.returnPeriodId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退款金额"><span class="money-danger">{{ money(returnDetail.refundAmount) }}</span></el-descriptions-item>
        <el-descriptions-item label="状态">{{ returnStatusLabel(returnDetail.status) }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="returnDetail?.items?.length" :data="returnDetail.items" border class="mt12">
        <el-table-column prop="productNameSnapshot" label="商品" />
        <el-table-column prop="returnSaleQuantity" label="退正品" width="120" />
        <el-table-column prop="returnGiftQuantity" label="退赠品" width="120" />
        <el-table-column prop="returnTotalQuantity" label="退货合计" width="120" />
        <el-table-column prop="refundUnitPrice" label="含赠单价" width="130"><template #default="scope">{{ money(scope.row.refundUnitPrice) }}</template></el-table-column>
        <el-table-column prop="refundAmount" label="退款金额" width="130"><template #default="scope"><span class="money-danger">{{ money(scope.row.refundAmount) }}</span></template></el-table-column>
      </el-table>
      <template #footer><el-button @click="returnDetailOpen = false">关闭</el-button><el-button v-if="returnDetail && String(returnDetail.status) === 'DRAFT'" v-hasPermi="['member:purchaseReturn:complete']" type="success" @click="completeReturn(returnDetail)">完成退货</el-button></template>
    </el-dialog>
    <el-dialog v-model="detailOpen" :title="detailEditMode ? '编辑购买单' : '购买单详情'" width="1100px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="购买单号">{{ detail.purchaseNo }}</el-descriptions-item>
        <el-descriptions-item label="顾客"><el-input v-if="detailEditMode" v-model="detail.customerName" /><span v-else>{{ detail.customerName || '散客' }}</span></el-descriptions-item>
        <el-descriptions-item label="联系电话"><el-input v-if="detailEditMode" v-model="detail.customerPhone" /><span v-else>{{ detail.customerPhone || '-' }}</span></el-descriptions-item>
        <el-descriptions-item label="应收"><span style="color: #F56C6C; font-weight: bold;">{{ money(detail.totalAmount) }}</span></el-descriptions-item>
        <el-descriptions-item label="已收"><span style="color: #67C23A; font-weight: bold;">{{ money(detail.paidAmount) }}</span></el-descriptions-item>
        <el-descriptions-item label="欠款"><span style="color: #E6A23C; font-weight: bold;">{{ money(detail.receivableAmount) }}</span></el-descriptions-item>
        <el-descriptions-item label="领取状态">{{ deliveryStatusLabel(detail.deliveryStatus) }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ orderStatusLabel(detail.orderStatus) }}</el-descriptions-item>
      </el-descriptions>
      <el-form v-if="detailEditMode" label-width="80px" class="mt12"><el-form-item label="备注"><el-input v-model="detail.remark" type="textarea" :rows="2" /></el-form-item></el-form>
      <el-table v-if="detail?.items" :data="detail.items" class="mt12" border>
        <el-table-column prop="itemId" label="明细ID" width="90" /><el-table-column prop="productNameSnapshot" label="商品" />
        <el-table-column prop="purchaseQuantity" label="购买" width="140"><template #default="scope"><el-input-number v-if="detailEditMode" v-model="scope.row.purchaseQuantity" :min="scope.row.deliveredSaleQuantity || 0" :precision="3" :step="0.001" controls-position="right" /><span v-else>{{ scope.row.purchaseQuantity }}</span></template></el-table-column>
        <el-table-column prop="giftQuantity" label="赠送" width="140"><template #default="scope"><el-input-number v-if="detailEditMode" v-model="scope.row.giftQuantity" :min="scope.row.deliveredGiftQuantity || 0" :precision="3" :step="0.001" controls-position="right" /><span v-else>{{ scope.row.giftQuantity }}</span></template></el-table-column>
        <el-table-column v-if="detailEditMode" label="单价" width="160"><template #default="scope"><el-input-number v-model="scope.row.unitPrice" :min="0.01" :precision="2" :step="0.01" controls-position="right" /></template></el-table-column>
        <el-table-column prop="remainingQuantity" label="待领取" width="90" />
      </el-table>
      <el-divider content-position="left">收款记录</el-divider>
      <el-table v-if="detail?.payments?.length" :data="detail.payments" border>
        <el-table-column prop="paymentNo" label="收款单号" min-width="190" />
        <el-table-column prop="paymentAmount" label="收款金额" width="160"><template #default="scope"><el-input-number v-if="detailEditMode" v-model="scope.row.paymentAmount" :min="0.01" :precision="2" :step="0.01" controls-position="right" /><span v-else style="color: #67C23A; font-weight: bold;">{{ money(scope.row.paymentAmount) }}</span></template></el-table-column>
        <el-table-column prop="paymentMethod" label="收款方式" width="140"><template #default="scope"><el-select v-if="detailEditMode" v-model="scope.row.paymentMethod"><el-option label="现金" value="CASH" /><el-option label="微信" value="WECHAT" /><el-option label="支付宝" value="ALIPAY" /><el-option label="银行卡" value="BANK" /></el-select><span v-else>{{ paymentMethodLabel(scope.row.paymentMethod) }}</span></template></el-table-column>
        <el-table-column prop="paymentDate" label="收款日期" min-width="190"><template #default="scope"><el-date-picker v-if="detailEditMode" v-model="scope.row.paymentDate" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss.SSSZ" /><span v-else>{{ formatDateTime(scope.row.paymentDate) }}</span></template></el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="110" />
        <el-table-column prop="remark" label="备注" min-width="140"><template #default="scope"><el-input v-if="detailEditMode" v-model="scope.row.remark" /><span v-else>{{ scope.row.remark || '-' }}</span></template></el-table-column>
        <el-table-column v-if="detailEditMode" label="操作" width="130"><template #default="scope"><el-button link type="primary" @click="savePaymentRecord(scope.row)">编辑收款记录</el-button></template></el-table-column>
      </el-table>
      <el-empty v-else description="暂无收款记录" :image-size="60" />
      <el-divider content-position="left">领取记录</el-divider>
      <el-table v-if="detail?.deliveries?.length" :data="detail.deliveries" border>
        <el-table-column prop="deliveryNo" label="领取单号" min-width="190" />
        <el-table-column label="商品" min-width="140"><template #default="scope">{{ deliveryProductName(scope.row.itemId) }}</template></el-table-column>
        <el-table-column prop="saleDeliveryQuantity" label="正品领取" width="140"><template #default="scope"><el-input-number v-if="detailEditMode" v-model="scope.row.saleDeliveryQuantity" :min="0" :precision="3" :step="0.001" controls-position="right" /><span v-else>{{ scope.row.saleDeliveryQuantity }}</span></template></el-table-column>
        <el-table-column prop="giftDeliveryQuantity" label="赠品领取" width="140"><template #default="scope"><el-input-number v-if="detailEditMode" v-model="scope.row.giftDeliveryQuantity" :min="0" :precision="3" :step="0.001" controls-position="right" /><span v-else>{{ scope.row.giftDeliveryQuantity }}</span></template></el-table-column>
        <el-table-column prop="totalDeliveryQuantity" label="领取合计" width="105" />
        <el-table-column prop="deliveryDate" label="领取日期" min-width="190"><template #default="scope"><el-date-picker v-if="detailEditMode" v-model="scope.row.deliveryDate" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss.SSSZ" /><span v-else>{{ formatDateTime(scope.row.deliveryDate) }}</span></template></el-table-column>
        <el-table-column prop="receiverName" label="领取人" width="130"><template #default="scope"><el-input v-if="detailEditMode" v-model="scope.row.receiverName" /><span v-else>{{ scope.row.receiverName || '-' }}</span></template></el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="110" />
        <el-table-column v-if="detailEditMode" label="操作" width="130"><template #default="scope"><el-button link type="primary" @click="saveDeliveryRecord(scope.row)">编辑领取记录</el-button></template></el-table-column>
      </el-table>
      <el-empty v-else description="暂无领取记录" :image-size="60" />
      <template #footer><el-button @click="detailOpen = false">关闭</el-button><el-button v-if="detailEditMode" type="primary" @click="saveEdit">保存购买单</el-button></template>
    </el-dialog>
    <el-dialog v-model="paymentOpen" title="登记收款" width="460px">
      <el-form :model="paymentForm" label-width="100px"><el-form-item label="购买单号">{{ paymentForm.purchaseNo }}</el-form-item><el-form-item label="剩余应收金额">{{ money(paymentForm.remainingAmount) }}</el-form-item><el-form-item label="本次收款"><el-input-number v-model="paymentForm.paymentAmount" :min="0.01" :max="paymentForm.remainingAmount" :precision="2" :step="0.01" placeholder="0.00" controls-position="right" style="width: 100%" /></el-form-item><el-form-item label="收款方式"><el-select v-model="paymentForm.paymentMethod"><el-option label="现金" value="CASH" /><el-option label="微信" value="WECHAT" /><el-option label="支付宝" value="ALIPAY" /><el-option label="银行卡" value="BANK" /></el-select></el-form-item><el-form-item label="备注"><el-input v-model="paymentForm.remark" /></el-form-item></el-form>
      <template #footer><el-button @click="paymentOpen = false">取消</el-button><el-button type="primary" @click="receivePayment">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="deliveryOpen" title="登记领取" width="520px">
      <el-form :model="deliveryForm" label-width="100px"><el-form-item label="购买单号">{{ deliveryForm.purchaseNo }}</el-form-item><el-form-item label="购买明细"><el-select v-model="deliveryForm.itemId" @change="handleDeliveryItemChange"><el-option v-for="item in deliveryItems" :key="item.itemId" :label="`${item.productNameSnapshot}（待领取 ${item.remainingQuantity}）`" :value="item.itemId" /></el-select></el-form-item><el-form-item label="购买数量"><el-input-number v-model="deliveryForm.saleDeliveryQuantity" :min="0" :precision="3" :step="0.001" placeholder="0.000" controls-position="right" style="width: 100%" /></el-form-item><el-form-item label="赠送数量"><el-input-number v-model="deliveryForm.giftDeliveryQuantity" :min="0" :precision="3" :step="0.001" placeholder="0.000" controls-position="right" style="width: 100%" /></el-form-item><el-form-item label="领取人"><el-input v-model="deliveryForm.receiverName" /></el-form-item></el-form>
      <template #footer><el-button @click="deliveryOpen = false">取消</el-button><el-button type="primary" @click="deliver">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="bindOpen" title="绑定会员" width="420px">
      <el-form label-width="100px">
        <el-form-item label="购买单号">{{ bindForm.purchaseNo }}</el-form-item>
        <el-form-item label="会员ID">
          <el-input v-model.number="bindForm.memberId" type="number" placeholder="请输入当前机构会员ID" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="bindOpen = false">取消</el-button><el-button type="primary" @click="bind">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MemberSelect from '@/components/MemberSelect/index.vue'
import { listProductSelector } from '@/api/finance/product'
import { getCurrentAccountingPeriod } from '@/api/finance/accountingPeriod'
import { useUserStore } from '@/stores/user'
import { listCampaignPolicies } from '@/api/member/campaignPolicy'
import { bindMemberPurchase, cancelMemberPurchase, completeMemberPurchaseReturn, createMemberPurchase, createMemberPurchaseReturn, deliverMemberPurchase, getMemberPurchase, getMemberPurchaseReturn, getMemberPurchaseStatistics, listMemberPurchaseReturns, listMemberPurchases, receiveMemberPurchasePayment, updateMemberPurchase, updateMemberPurchaseDelivery, updateMemberPurchasePayment } from '@/api/member/purchase'
import { download } from '@/api/request'
import { money } from '@/utils/money'
import { formatDateTime } from '@/utils/junsong'

const loading = ref(false)
const rows = ref<Record<string, unknown>[]>([])
const total = ref(0)
const summary = ref<any>(null)
const query = reactive({ customerName: '', customerType: '', paymentStatus: '', deliveryStatus: '', beginTime: '', endTime: '', pageNum: 1, pageSize: 20 })
const purchaseDateRange = ref<string[]>([])
const bindOpen = ref(false)
const bindForm = reactive<{ purchaseId?: number, purchaseNo?: string, memberId?: number }>({})
const returnOpen = ref(false)
const returnPeriods = ref<any[]>([])
const returnForm = reactive<any>({ purchaseId: undefined, purchaseNo: '', returnPeriodId: undefined, reason: '', items: [] })
const returnListOpen = ref(false)
const returnLoading = ref(false)
const returnRows = ref<any[]>([])
const returnDetailOpen = ref(false)
const returnDetail = ref<any>(null)
const createOpen = ref(false)
const createForm = reactive<any>({ customerType: 'MEMBER', customerName: '', memberId: undefined, purchaseDate: '', periodId: undefined, item: { productId: undefined, purchaseQuantity: undefined, unitPrice: undefined, giftQuantity: undefined, policyId: undefined, packageId: undefined } })
const detailOpen = ref(false)
const detail = ref<any>(null)
const detailEditMode = ref(false)
const editOpen = ref(false)
const editForm = reactive<any>({ purchaseId: undefined, purchaseNo: '', customerName: '', customerPhone: '', remark: '' })
const paymentOpen = ref(false)
const paymentForm = reactive<any>({ purchaseId: undefined, purchaseNo: '', remainingAmount: 0, paymentAmount: undefined, paymentMethod: 'WECHAT', remark: '' })
const deliveryOpen = ref(false)
const deliveryForm = reactive<any>({ purchaseId: undefined, purchaseNo: '', itemId: undefined, saleDeliveryQuantity: undefined, giftDeliveryQuantity: undefined, receiverName: '' })
const deliveryItems = ref<any[]>([])
const productOptions = ref<any[]>([])
const policyOptions = ref<any[]>([])
const packageOptions = ref<any[]>([])
const userStore = useUserStore()
const createPeriodLabel = computed(() => createForm.periodId ? `${createForm.periodNo || '当前核算周期'}（${createForm.periodId}）` : '正在获取当前核算周期')

async function load() {
  loading.value = true
  try {
    const [response, statistics] = await Promise.all([listMemberPurchases(query), getMemberPurchaseStatistics(query)]) as any
    rows.value = response.rows || response.data?.rows || response.data || []
    total.value = Number(response.total || response.data?.total || 0)
    summary.value = statistics.data || statistics
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  query.beginTime = purchaseDateRange.value?.[0] || ''
  query.endTime = purchaseDateRange.value?.[1] || ''
  query.pageNum = 1
  load()
}
function resetQuery() {
  Object.assign(query, { customerName: '', customerType: '', paymentStatus: '', deliveryStatus: '', beginTime: '', endTime: '', pageNum: 1 })
  purchaseDateRange.value = []
  load()
}
function summaryMoney(value: number) { return money(value) }
function quantity(value: any) { return Number(value || 0).toFixed(3) }
function currentPurchaseDate() { const now = new Date(); const pad = (value: number) => String(value).padStart(2, '0'); return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}` }
function toIsoDateTime(value: string) { return new Date(`${value}T00:00:00`).toISOString() }
function handleExport() { download('/member/purchase/export', { ...query, pageNum: undefined, pageSize: undefined }, `member_purchase_${Date.now()}.xlsx`) }

function openBind(row: any) {
  bindForm.purchaseId = row.purchaseId
  bindForm.purchaseNo = row.purchaseNo
  bindForm.memberId = undefined
  bindOpen.value = true
}

function isCancelled(row: any) { return String(row.orderStatus) === '4' }
function customerTypeLabel(value: any) { return ({ MEMBER: '会员', WALK_IN: '散客', CUSTOMER: '非会员' } as Record<string, string>)[String(value)] || '未知' }
function paymentStatusLabel(value: any) { return ({ '0': '未付款', '1': '部分付款', '2': '已付清', '3': '已退款' } as Record<string, string>)[String(value)] || '未知' }
function paymentMethodLabel(value: any) { return ({ CASH: '现金', WECHAT: '微信', ALIPAY: '支付宝', BANK: '银行卡' } as Record<string, string>)[String(value)] || '其他' }
function deliveryStatusLabel(value: any) { return ({ '0': '未领取', '1': '部分领取', '2': '已全部领取' } as Record<string, string>)[String(value)] || '未知' }
function orderStatusLabel(value: any) { return ({ '0': '草稿', '1': '已确认', '2': '已完成', '3': '已关闭', '4': '已作废' } as Record<string, string>)[String(value)] || '未知' }
function deliveryProductName(itemId: any) { return detail.value?.items?.find((item: any) => String(item.itemId) === String(itemId))?.productNameSnapshot || '未知商品' }
function canReceive(row: any) { return !isCancelled(row) && Number(row.receivableAmount || 0) > 0 }
function canDeliver(row: any) { return !isCancelled(row) && Number(row.deliveryStatus) !== 2 && String(row.deliveryStatus) !== '2' }
function canReturn(row: any) { return !isCancelled(row) }
function canCancel(row: any) { return !isCancelled(row) && String(row.deliveryStatus) !== '2' }
function returnStatusLabel(value: any) { return ({ DRAFT: '草稿', PENDING: '待审核', APPROVED: '已批准', COMPLETED: '已完成', REFUNDED: '已退款', REJECTED: '已驳回', CANCELLED: '已作废' } as Record<string, string>)[String(value)] || '未知' }
async function openReturnList() {
  returnListOpen.value = true
  returnLoading.value = true
  try {
    const response: any = await listMemberPurchaseReturns({ pageNum: 1, pageSize: 100 })
    returnRows.value = response.rows || response.data?.rows || response.data || []
  } finally { returnLoading.value = false }
}
async function openReturnDetail(row: any) {
  const response: any = await getMemberPurchaseReturn(row.returnId)
  returnDetail.value = response.data || response
  returnDetailOpen.value = true
}
async function completeReturn(row: any) {
  await ElMessageBox.confirm(`确认将退货单 ${row.returnNo || row.returnId} 标记为已完成吗？`, '操作确认', { type: 'warning' })
  await completeMemberPurchaseReturn(row.returnId)
  ElMessage.success('退货单已完成')
  await openReturnList()
  if (returnDetail.value && String(returnDetail.value.returnId) === String(row.returnId)) await openReturnDetail(row)
}
function periodLabel(period: any) { return `${period.periodNo || period.periodId}（${formatDateTime(period.startTime)} 至 ${period.endTime ? formatDateTime(period.endTime) : '当前'}）` }
async function openReturn(row: any) {
  const response: any = await getMemberPurchase(row.purchaseId)
  const value: any = response.data || response
  const periodResponse: any = await getCurrentAccountingPeriod(userStore.currentDeptId)
  const period: any = periodResponse?.data || periodResponse
  returnPeriods.value = period ? [period] : []
  Object.assign(returnForm, {
    purchaseId: row.purchaseId,
    purchaseNo: row.purchaseNo,
    returnPeriodId: period?.periodId,
    reason: '',
    items: (value.items || []).map((item: any) => ({ ...item, availableSaleQuantity: Number(item.purchaseQuantity || 0), availableGiftQuantity: Number(item.giftQuantity || 0), returnSaleQuantity: 0, returnGiftQuantity: 0 }))
  })
  returnOpen.value = true
}
async function submitReturn() {
  const items = returnForm.items.filter((item: any) => Number(item.returnSaleQuantity || 0) > 0 || Number(item.returnGiftQuantity || 0) > 0)
    .map((item: any) => ({ itemId: item.itemId, returnSaleQuantity: item.returnSaleQuantity || 0, returnGiftQuantity: item.returnGiftQuantity || 0 }))
  if (!returnForm.purchaseId || !returnForm.returnPeriodId || !items.length) return ElMessage.warning('请选择退货办理周期并填写退货数量')
  await createMemberPurchaseReturn({ purchaseId: returnForm.purchaseId, returnPeriodId: returnForm.returnPeriodId, reason: returnForm.reason, items, idempotencyKey: `pc-return-${Date.now()}` })
  ElMessage.success('退货单已创建'); returnOpen.value = false; await load()
}
function resetCreateForm() {
  Object.assign(createForm, { customerType: 'MEMBER', customerName: '', memberId: undefined, purchaseDate: currentPurchaseDate(), periodId: undefined, periodNo: undefined, item: { productId: undefined, purchaseQuantity: undefined, unitPrice: undefined, giftQuantity: undefined, policyId: undefined, packageId: undefined } })
  policyOptions.value = []
  packageOptions.value = []
}
function openCreate() { resetCreateForm(); createOpen.value = true }
async function loadCreateOptions() {
  const deptId = userStore.currentDeptId
  const [productResponse, periodResponse] = await Promise.all([listProductSelector(), deptId ? getCurrentAccountingPeriod(deptId) : Promise.resolve({ data: null })])
  const products: any = productResponse as any
  productOptions.value = products.rows || products.data?.rows || products.data || []
  const period: any = (periodResponse as any).data || periodResponse
  createForm.periodId = period?.periodId
  createForm.periodNo = period?.periodNo
  if (createForm.item.productId) await loadPolicies()
}
async function loadPolicies() {
  if (!createForm.item.productId || !createForm.periodId) { policyOptions.value = []; packageOptions.value = []; return }
  const response: any = await listCampaignPolicies({ periodId: createForm.periodId, productId: createForm.item.productId, status: '1' })
  policyOptions.value = response.rows || response.data?.rows || response.data || []
  packageOptions.value = []
}
function handleMemberChange(member: any) { if (member) createForm.customerName = member.memberName; createForm.customerPhone = member?.phone }
async function handleCreateProductChange(productId: number) { const product = productOptions.value.find(item => item.productId === productId); const salePrice = product?.salePrice ?? product?.sale_price; createForm.item.unitPrice = salePrice == null ? undefined : Number(salePrice); createForm.item.policyId = undefined; createForm.item.packageId = undefined; createForm.item.purchaseQuantity = undefined; createForm.item.giftQuantity = undefined; await loadPolicies() }
function handleCreatePolicyChange(policyId: number) { const policy = policyOptions.value.find(item => item.policyId === policyId); packageOptions.value = policy?.packages || []; createForm.item.packageId = undefined; createForm.item.purchaseQuantity = undefined; createForm.item.giftQuantity = undefined }
function handleCreatePackageChange(packageId: number) { const item = packageOptions.value.find(value => value.packageId === packageId); if (!item) return; createForm.item.purchaseQuantity = Number(item.purchaseQuantity); createForm.item.giftQuantity = Number(item.giftQuantity || 0); if (item.packagePrice != null && Number(item.purchaseQuantity) > 0) createForm.item.unitPrice = Number(item.packagePrice) / Number(item.purchaseQuantity) }
function openPayment(row: any) { const remainingAmount = Number(row.receivableAmount || 0); Object.assign(paymentForm, { purchaseId: row.purchaseId, purchaseNo: row.purchaseNo, remainingAmount, paymentAmount: remainingAmount, paymentMethod: 'WECHAT', remark: '' }); paymentOpen.value = true }
async function loadDetail(row: any, edit = false) { const response: any = await getMemberPurchase(row.purchaseId); detail.value = response.data || response; detailEditMode.value = edit; detailOpen.value = true }
async function openDetail(row: any) { await loadDetail(row, false) }
async function openEdit(row: any) { await loadDetail(row, true) }
function remainingDeliveryQuantity(item: any, type: 'sale' | 'gift') {
  if (!item) return 0
  const delivered = type === 'sale' ? item.deliveredSaleQuantity : item.deliveredGiftQuantity
  const total = type === 'sale' ? item.purchaseQuantity : item.giftQuantity
  return Math.max(0, Number(total || 0) - Number(delivered || 0))
}
function handleDeliveryItemChange(itemId: number) {
  const item = deliveryItems.value.find(value => String(value.itemId) === String(itemId))
  deliveryForm.saleDeliveryQuantity = remainingDeliveryQuantity(item, 'sale')
  deliveryForm.giftDeliveryQuantity = remainingDeliveryQuantity(item, 'gift')
}
async function openDelivery(row: any) { const response: any = await getMemberPurchase(row.purchaseId); const value: any = response.data || response; deliveryItems.value = (value.items || []).filter((item: any) => Number(item.remainingQuantity || 0) > 0); const item = deliveryItems.value[0]; Object.assign(deliveryForm, { purchaseId: row.purchaseId, purchaseNo: row.purchaseNo, itemId: item?.itemId, saleDeliveryQuantity: remainingDeliveryQuantity(item, 'sale'), giftDeliveryQuantity: remainingDeliveryQuantity(item, 'gift'), receiverName: value.customerName || '' }); deliveryOpen.value = true }

async function create() {
  if (!createForm.periodId || !createForm.item.productId) return ElMessage.warning('请选择核算周期和商品')
  if (createForm.item.purchaseQuantity == null || Number(createForm.item.purchaseQuantity) <= 0) return ElMessage.warning('请输入有效购买数量')
  if (createForm.item.unitPrice == null || Number(createForm.item.unitPrice) <= 0) return ElMessage.warning('请输入大于0的零售价')
  await createMemberPurchase({ ...createForm, purchaseDate: toIsoDateTime(createForm.purchaseDate), items: [createForm.item], idempotencyKey: `pc-${Date.now()}` })
  ElMessage.success('购买单已创建'); createOpen.value = false; await load()
}

async function receivePayment() {
  if (!paymentForm.purchaseId || Number(paymentForm.paymentAmount) <= 0) return ElMessage.warning('请输入有效收款金额')
  if (Number(paymentForm.paymentAmount) > Number(paymentForm.remainingAmount)) return ElMessage.warning('收款金额不能超过剩余应收金额')
  await receiveMemberPurchasePayment(paymentForm.purchaseId, { paymentAmount: paymentForm.paymentAmount, paymentMethod: paymentForm.paymentMethod, remark: paymentForm.remark, idempotencyKey: `pc-pay-${Date.now()}` })
  ElMessage.success('收款成功'); paymentOpen.value = false; await load()
}

async function saveEdit() {
  if (!detail.value?.purchaseId) return ElMessage.warning('购买单不存在')
  await updateMemberPurchase(detail.value.purchaseId, { customerName: detail.value.customerName, customerPhone: detail.value.customerPhone, remark: detail.value.remark, items: detail.value.items?.map((item: any) => ({ itemId: item.itemId, purchaseQuantity: item.purchaseQuantity, giftQuantity: item.giftQuantity, unitPrice: item.unitPrice })) })
  ElMessage.success('购买单已保存')
  await loadDetail(detail.value, true)
  await load()
}

async function savePaymentRecord(record: any) {
  await updateMemberPurchasePayment(detail.value.purchaseId, record.paymentId, { paymentAmount: record.paymentAmount, paymentMethod: record.paymentMethod, paymentDate: record.paymentDate, remark: record.remark })
  ElMessage.success('收款记录已保存')
  await loadDetail(detail.value, true)
  await load()
}

async function saveDeliveryRecord(record: any) {
  const total = Number(record.saleDeliveryQuantity || 0) + Number(record.giftDeliveryQuantity || 0)
  if (total <= 0) return ElMessage.warning('领取数量必须大于0')
  await updateMemberPurchaseDelivery(detail.value.purchaseId, record.deliveryId, { itemId: record.itemId, saleDeliveryQuantity: record.saleDeliveryQuantity, giftDeliveryQuantity: record.giftDeliveryQuantity, totalDeliveryQuantity: total, deliveryDate: record.deliveryDate, receiverName: record.receiverName, remark: record.remark })
  ElMessage.success('领取记录已保存')
  await loadDetail(detail.value, true)
  await load()
}

async function deliver() {
  if (!deliveryForm.purchaseId || !deliveryForm.itemId || Number(deliveryForm.saleDeliveryQuantity || 0) + Number(deliveryForm.giftDeliveryQuantity || 0) <= 0) return ElMessage.warning('请输入领取数量')
  await deliverMemberPurchase(deliveryForm.purchaseId, { itemId: deliveryForm.itemId, saleDeliveryQuantity: deliveryForm.saleDeliveryQuantity || 0, giftDeliveryQuantity: deliveryForm.giftDeliveryQuantity || 0, totalDeliveryQuantity: Number(deliveryForm.saleDeliveryQuantity || 0) + Number(deliveryForm.giftDeliveryQuantity || 0), receiverName: deliveryForm.receiverName, idempotencyKey: `pc-delivery-${Date.now()}` })
  ElMessage.success('领取成功'); deliveryOpen.value = false; await load()
}

async function bind() {
  if (!bindForm.purchaseId || !bindForm.memberId) return ElMessage.warning('请输入会员ID')
  await bindMemberPurchase(bindForm.purchaseId, bindForm.memberId)
  ElMessage.success('绑定成功')
  bindOpen.value = false
  await load()
}

async function cancel(row: any) {
  await ElMessageBox.confirm(`确认作废购买单 ${row.purchaseNo || row.purchaseId} 吗？`, '操作确认', { type: 'warning' })
  await cancelMemberPurchase(row.purchaseId)
  ElMessage.success('购买单已作废')
  await load()
}

onMounted(load)
</script>

<style scoped>
.purchase-summary {
  margin: 0 -6px 14px;
}

.purchase-date-range {
  width: 460px !important;
  min-width: 460px;
}

:deep(.purchase-date-range.el-date-editor--daterange) {
  width: 460px !important;
}

:deep(.purchase-date-range .el-range-input) {
  min-width: 145px;
}

.purchase-summary-card {
  position: relative;
  min-height: 96px;
  margin: 0 6px 12px;
  padding: 16px 18px 13px 20px;
  overflow: hidden;
  border: 1px solid var(--theme-border, #e5eaf1);
  border-radius: 12px;
  background: var(--theme-card-bg, #fff);
  box-shadow: 0 4px 14px rgba(32, 54, 82, 0.06);
}

.purchase-summary-card::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  content: '';
  background: var(--summary-accent, #1677ff);
}

.purchase-summary-label,
.purchase-summary-card small {
  display: block;
  color: var(--theme-app-muted, #64748b);
}

.purchase-summary-label {
  font-size: 13px;
  font-weight: 600;
}

.purchase-summary-card strong {
  display: block;
  margin-top: 5px;
  color: var(--theme-app-text, #1e293b);
  font-size: 21px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.3px;
  white-space: nowrap;
}

.purchase-summary-card small {
  margin-top: 5px;
  font-size: 11px;
}

.purchase-summary-card--orders { --summary-accent: #1677ff; background: #f4f8ff; }
.purchase-summary-card--quantity { --summary-accent: #3b82f6; background: #f6f9ff; }
.purchase-summary-card--gift { --summary-accent: #8b5cf6; background: #faf7ff; }
.purchase-summary-card--receivable { --summary-accent: #ef5b67; background: #fff7f7; }
.purchase-summary-card--paid { --summary-accent: #22a06b; background: #f3fbf7; }
.purchase-summary-card--debt { --summary-accent: #d98b21; background: #fffaf1; }

:deep(.purchase-operation-column .cell) {
  padding: 0 8px;
  white-space: normal;
}

:deep(.purchase-operation-column .el-button) {
  margin: 2px 5px 2px 0;
  padding: 0 2px;
  font-size: 13px;
}

@media (max-width: 900px) {
  .purchase-summary-card strong { font-size: 18px; }
  .purchase-date-range { width: 100% !important; min-width: 300px; }
  :deep(.purchase-date-range.el-date-editor--daterange) { width: 100% !important; }
}
.money-danger { color: #ef5b67; font-weight: 700; }
</style>
