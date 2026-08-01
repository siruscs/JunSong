export const paymentMethods = [
  { label: '现金', value: '现金' },
  { label: '银行转账', value: '银行转账' },
  { label: '微信支付', value: '微信支付' },
  { label: '支付宝', value: '支付宝' },
  { label: '月结', value: '月结' },
  { label: '其他', value: '其他' },
  { label: '直接付款', value: '直接付款' },
  { label: '预支资金', value: '预支资金' },
  { label: '自行垫付', value: '自行垫付' },
  { label: '收入', value: '收入' }
]

const crudPermissions = (perm) => ({
  view: [`finance:${perm}:list`, `finance:${perm}:query`],
  add: `finance:${perm}:add`,
  edit: `finance:${perm}:edit`,
  remove: `finance:${perm}:remove`
})

const memberCrudPermissions = (perm) => ({
  view: [`member:${perm}:list`, `member:${perm}:query`],
  add: `member:${perm}:add`,
  edit: `member:${perm}:edit`,
  remove: `member:${perm}:remove`
})

export const modules = {
  // ===== 会员服务 =====
  member: {
    group: '会员服务',
    title: '会员信息',
    path: '/member/member',
    permissions: memberCrudPermissions('member'),
    idKey: 'memberId',
    searchKey: 'memberName',
    searchKeys: ['memberName', 'memberNo'],
    summary: ['memberNo', 'phone', 'cardType', 'availablePoints', 'growthValue'],
    fields: [
      { key: 'memberNo', label: '会员编号', serverGenerated: true },
      { key: 'memberName', label: '会员姓名', required: true },
      { key: 'phone', label: '手机号码', type: 'phone' },
      { key: 'age', label: '年龄', type: 'number' },
      { key: 'address', label: '住址' },
      { key: 'idCard', label: '身份证号', type: 'idcard', sensitive: true },
      { key: 'cardType', label: '会员卡类型', type: 'select', displayKey: 'cardTypeName', remoteUrl: '/member/level/list', remoteLabel: 'typeName', remoteValue: 'typeCode', remoteFilterStatus: '0', required: true },
      { key: 'growthValue', label: '成长值', type: 'number', formHidden: true },
      { key: 'joinDate', label: '入会日期', type: 'date' },
      { key: 'expireDate', label: '有效期至', type: 'date' },
      { key: 'totalPoints', label: '总积分', type: 'number', formHidden: true },
      { key: 'availablePoints', label: '可用积分', type: 'number', formHidden: true },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: '0' }, { label: '无效', value: '1' }, { label: '已退卡', value: '2' }] },
      { key: 'remark', label: '备注', type: 'textarea' }
    ],
    actions: [
      { name: '设为无效', action: 'edit', url: '/member/member', method: 'PUT', bodyFactory: (item) => ({ memberId: item.memberId, status: '1' }) },
      { name: '退卡', action: 'edit', url: '/member/member', method: 'PUT', bodyFactory: (item) => ({ memberId: item.memberId, status: '2' }) }
    ]
  },
  pointsGoods: {
    group: '会员服务',
    title: '积分物品',
    path: '/member/pointsGoods',
    permissions: memberCrudPermissions('pointsGoods'),
    idKey: 'goodsId',
    searchKey: 'goodsName',
    summary: ['goodsCode', 'pointsPrice', 'stock', 'status'],
    fields: [
      { key: 'goodsName', label: '物品名称', required: true },
      { key: 'goodsCode', label: '物品编码', hidden: true },
      { key: 'goodsValue', label: '物品价值', type: 'number', required: true },
      { key: 'pointsPrice', label: '积分价格', type: 'number', required: true },
      { key: 'stock', label: '库存', type: 'number', required: true },
      { key: 'exchanged', label: '已兑换数量', type: 'number', hidden: true },
      { key: 'goodsImage', label: '物品图片', type: 'image' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: '0' }, { label: '停用', value: '1' }] },
      { key: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  pointsRule: {
    group: '会员服务',
    title: '积分规则',
    path: '/member/pointsRule',
    permissions: memberCrudPermissions('pointsRule'),
    idKey: 'ruleId',
    searchKey: 'ruleName',
    summary: ['ruleCode', 'ruleType', 'pointsPerYuan', 'status'],
    fields: [
      { key: 'ruleName', label: '规则名称', required: true },
      { key: 'ruleCode', label: '规则代码', required: true },
      { key: 'ruleType', label: '计算方式', type: 'select', options: ['进一法', '四舍五入', '舍零取整'] },
      { key: 'pointsPerYuan', label: '每元积分', type: 'number' },
      { key: 'validityDays', label: '有效期天数', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: '0' }, { label: '禁用', value: '1' }] }
    ]
  },
  pointsRecord: {
    group: '会员服务',
    title: '积分记录',
    path: '/member/pointsRecord',
    permissions: memberCrudPermissions('pointsRecord'),
    idKey: 'recordId',
    searchKey: 'memberName',
    addOnly: true,
    summary: ['memberNo', 'recordType', 'points', 'balance'],
    fields: [
      { key: 'memberId', label: '会员ID', type: 'number' },
      { key: 'memberNo', label: '会员编号' },
      { key: 'memberName', label: '会员姓名' },
      { key: 'recordType', label: '类型', type: 'select', options: ['消费得积分', '兑换扣积分', '过期清零', '手动调整'] },
      { key: 'consumeAmount', label: '消费金额', type: 'number' },
      { key: 'points', label: '积分变动', type: 'number' },
      { key: 'balance', label: '变动后余额', type: 'number' },
      { key: 'ruleCode', label: '规则代码' },
      { key: 'expireDate', label: '过期日期', type: 'date' },
      { key: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  pointsExchange: {
    group: '会员服务',
    title: '积分兑换',
    path: '/member/pointsExchange',
    permissions: memberCrudPermissions('pointsExchange'),
    idKey: 'exchangeId',
    searchKey: 'memberName',
    summary: ['exchangeNo', 'goodsName', 'quantity', 'status'],
    fields: [
      { key: 'exchangeNo', label: '兑换单号', hidden: true },
      { key: 'memberId', label: '会员ID', type: 'number', hidden: true },
      { key: 'memberNo', label: '会员编号', required: true },
      { key: 'memberName', label: '会员姓名', hidden: true },
      { key: 'goodsId', label: '兑换物品', type: 'select', remoteUrl: '/member/pointsGoods/list', remoteLabel: 'goodsName', remoteValue: 'goodsId', required: true },
      { key: 'goodsName', label: '物品名称', hidden: true },
      { key: 'exchangeDate', label: '兑换日期', type: 'date', required: true },
      { key: 'quantity', label: '兑换数量', type: 'number', required: true },
      { key: 'pointsDeducted', label: '扣减积分', type: 'number', hidden: true },
      { key: 'paymentMethod', label: '付款方式', type: 'select', dictType: 'finance_payment_method', options: paymentMethods },
      { key: 'extraAmount', label: '补差价金额', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '待领取', value: '0' }, { label: '已领取', value: '1' }, { label: '已取消', value: '2' }], hidden: true },
      { key: 'remark', label: '备注', type: 'textarea' }
    ],
    actions: [{ name: '领取', action: 'edit', url: '/member/pointsExchange/claim', method: 'PUT', body: 'ids' }]
  },
  seckill: {
    group: '会员服务',
    title: '秒杀活动',
    path: '/member/seckill',
    permissions: memberCrudPermissions('seckill'),
    idKey: 'seckillId',
    searchKey: 'seckillName',
    summary: ['seckillNo', 'seckillType', 'seckillPrice', 'remainShares'],
    fields: [
      { key: 'seckillNo', label: '秒杀编号', hidden: true },
      { key: 'seckillName', label: '秒杀名称', required: true },
      { key: 'seckillType', label: '类型', type: 'select', required: true, options: [{ label: '秒杀', value: '1' }, { label: '团购', value: '2' }] },
      { key: 'seckillDate', label: '开始日期', type: 'date', required: true },
      { key: 'endDate', label: '结束日期', type: 'date', required: true },
      { key: 'timeSlot', label: '时间段', hidden: true },
      { key: 'seckillAmount', label: '原价（每份）', type: 'number', required: true },
      { key: 'seckillPrice', label: '秒杀价（每份）', type: 'number', required: true },
      { key: 'totalShares', label: '总份额', type: 'number', required: true },
      { key: 'remainShares', label: '剩余份额', type: 'number', hidden: true },
      { key: 'policy', label: '秒杀政策', type: 'textarea' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '进行中', value: '0' }, { label: '已结束', value: '1' }, { label: '已取消', value: '2' }], hidden: true },
      { key: 'remark', label: '备注', type: 'textarea' }
    ],
    actions: [{ name: '关闭活动', action: 'edit', url: '/member/seckill/{id}/close', idKey: 'seckillId', method: 'PUT' }]
  },
  seckillRecord: {
    group: '会员服务',
    title: '秒杀记录',
    path: '/member/seckillRecord',
    permissions: { ...memberCrudPermissions('seckillRecord'), receive: 'member:seckillRecord:receive' },
    idKey: 'recordId',
    searchKey: 'memberName',
    searchKeys: ['memberName', 'memberNo'],
    addOnly: true,
    summary: ['memberNo', 'shares', 'claimedShares', 'remainingShares'],
    fields: [
      { key: 'seckillId', label: '秒杀ID', type: 'number' },
      { key: 'memberId', label: '会员ID', type: 'number' },
      { key: 'memberNo', label: '会员编号' },
      { key: 'memberName', label: '会员姓名' },
      { key: 'seckillDate', label: '秒杀日期', type: 'date' },
      { key: 'paymentMethod', label: '付款方式', type: 'select', dictType: 'finance_payment_method', options: paymentMethods },
      { key: 'shares', label: '秒杀份额', type: 'number' },
      { key: 'totalAmount', label: '实付金额', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '待领取', value: '0' }, { label: '已领取', value: '1' }, { label: '已取消', value: '2' }, { label: '部分领取', value: '3' }] },
      { key: 'remark', label: '备注', type: 'textarea' }
    ],
    actions: [{ name: '领取', action: 'receive', url: '/member/seckillRecord/claim/{id}', idKey: 'recordId', method: 'PUT' }]
  },

  // ===== 财务管理 =====
  expense: {
    group: '财务管理',
    title: '费用记录',
    path: '/finance/expense',
    permissions: {
      ...crudPermissions('expense'),
      verify: 'finance:expense:verify',
      unverify: 'finance:expense:unverify'
    },
    idKey: 'expenseId',
    searchKey: 'expenseContent',
    summary: ['expenseNo', 'expenseType', 'expenseAmount', 'status'],
    fields: [
      { key: 'expenseNo', label: '费用单号', hidden: true },
      { key: 'expenseDate', label: '费用日期', type: 'date' },
      { key: 'expenseType', label: '费用类别', type: 'select', dictType: 'finance_expense_type' },
      { key: 'expenseContent', label: '花销内容', required: true },
      { key: 'paymentMethod', label: '付款方式', type: 'select', dictType: 'finance_payment_method', options: paymentMethods },
      { key: 'expenseAmount', label: '费用金额', type: 'number', required: true },
      { key: 'advanceId', label: '关联借支ID', type: 'number', hidden: true },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '未核销', value: '0' }, { label: '已核销', value: '1' }], hidden: true }
    ]
  },
  advance: {
    group: '财务管理',
    title: '借支记录',
    path: '/finance/advance',
    permissions: crudPermissions('advance'),
    idKey: 'advanceId',
    searchKey: 'borrower',
    summary: ['advanceNo', 'advanceDate', 'advanceAmount', 'status'],
    fields: [
      { key: 'advanceNo', label: '借支单号', hidden: true },
      { key: 'advanceDate', label: '借支日期', type: 'date', required: true },
      { key: 'advanceAmount', label: '借支金额', type: 'number', required: true },
      { key: 'borrower', label: '借款人', type: 'select', remoteDeptStaff: true, remoteLabel: 'nickName', remoteValue: 'nickName', required: true },
      { key: 'purpose', label: '借支用途', type: 'textarea', required: true },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '未核销', value: '0' }, { label: '已核销', value: '1' }], hidden: true },
      { key: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  product: {
    group: '财务管理',
    title: '商品管理',
    path: '/finance/product',
    permissions: crudPermissions('product'),
    idKey: 'productId',
    searchKey: 'productName',
    summary: ['productCode', 'unit', 'salePrice', 'stockNum'],
    fields: [
      { key: 'productCode', label: '商品编码', hidden: true },
      { key: 'productName', label: '商品名称', required: true },
      { key: 'categoryId', label: '分类ID', type: 'number', hidden: true },
      { key: 'unit', label: '计量单位', type: 'select', dictType: 'finance_product_unit', required: true },
      { key: 'purchasePrice', label: '进货价格', type: 'number' },
      { key: 'salePrice', label: '销售价格', type: 'number' },
      { key: 'stockNum', label: '库存数量', type: 'number' },
      { key: 'minStock', label: '最低库存预警', type: 'number' },
      { key: 'status', label: '商品状态', type: 'select', options: [{ label: '正常', value: '0' }, { label: '停用', value: '1' }] },
      { key: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  supplier: {
    group: '财务管理',
    title: '供应商',
    path: '/finance/supplier',
    permissions: crudPermissions('supplier'),
    idKey: 'supplierId',
    searchKey: 'supplierName',
    summary: ['supplierCode', 'contactPerson', 'contactPhone', 'status'],
    fields: [
      { key: 'supplierCode', label: '供应商编码', hidden: true },
      { key: 'supplierName', label: '供应商名称', required: true },
      { key: 'contactPerson', label: '联系人' },
      { key: 'contactPhone', label: '联系电话', type: 'phone' },
      { key: 'address', label: '地址', type: 'textarea' },
      { key: 'status', label: '供应商状态', type: 'select', options: [{ label: '正常', value: '0' }, { label: '停用', value: '1' }] },
      { key: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  purchase: {
    group: '财务管理',
    title: '进货单',
    path: '/finance/purchase',
    permissions: crudPermissions('purchase'),
    idKey: 'purchaseId',
    searchKey: 'purchaseNo',
    summary: ['supplierName', 'purchaseDate', 'totalAmount', 'status'],
    fields: [
      { key: 'purchaseNo', label: '进货单号', hidden: true },
      { key: 'supplierId', label: '供应商', type: 'select', remoteUrl: '/finance/supplier/list', remoteLabel: 'supplierName', remoteValue: 'supplierId', remoteFilterDept: true, required: true },
      { key: 'supplierName', label: '供应商名称', hidden: true },
      { key: 'purchaseDate', label: '进货日期', type: 'date', required: true },
      { key: 'totalAmount', label: '总金额', type: 'number', hidden: true },
      { key: 'paidAmount', label: '已付金额', type: 'number' },
      { key: 'paymentMethod', label: '付款方式', type: 'select', dictType: 'finance_payment_method', options: paymentMethods },
      { key: 'totalQuantity', label: '总数量', type: 'number', hidden: true },
      { key: 'receiverName', label: '收货人姓名' },
      { key: 'receiverPhone', label: '收货人电话', type: 'phone' },
      { key: 'receiverAddress', label: '收货人地址' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '草稿', value: '0' }, { label: '已确认', value: '1' }, { label: '已完成', value: '2' }] },
      { key: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  sale: {
    group: '财务管理',
    title: '销售记录',
    path: '/finance/sale',
    permissions: { ...crudPermissions('sale'), payment: 'finance:sale:edit' },
    idKey: 'saleId',
    searchKey: 'productName',
    summary: ['saleNo', 'saleQuantity', 'saleAmount', 'paidAmount', 'status'],
    fields: [
      { key: 'saleNo', label: '销售单号', hidden: true },
      { key: 'productId', label: '商品', type: 'select', remoteUrl: '/finance/product/list', remoteLabel: 'productName', remoteValue: 'productId', remoteFilterDept: true, required: true, formatter: (record) => record.productName || record.productId || '-' },
      { key: 'productName', label: '商品名称', hidden: true },
      { key: 'saleQuantity', label: '销售数量', type: 'number', required: true, allowNegative: true },
      { key: 'giftQuantity', label: '赠品数量', type: 'number' },
      { key: 'totalQuantity', label: '总数量', type: 'number', hidden: true },
      { key: 'saleAmount', label: '销售金额', type: 'number', required: true, allowNegative: true },
      { key: 'unitPrice', label: '单价', type: 'number' },
      { key: 'paidAmount', label: '已缴金额', type: 'number', hidden: true },
      { key: 'saleDate', label: '销售日期', type: 'date', required: true },
      { key: 'status', label: '状态', type: 'select', hidden: true, options: [{ label: '待缴款', value: '0' }, { label: '部分缴款', value: '1' }, { label: '已缴清', value: '2' }] },
      { key: 'remark', label: '备注', type: 'textarea' }
    ],
    payment: true
  },
  investorPayment: {
    group: '财务管理',
    title: '投资人返款',
    path: '/finance/investorPayment',
    permissions: crudPermissions('investorPayment'),
    idKey: 'paymentId',
    searchKey: 'investorName',
    summary: ['paymentNo', 'paymentDate', 'sourceType', 'amount'],
    fields: [
      { key: 'paymentNo', label: '返款单号' },
      { key: 'periodId', label: '周期ID', type: 'number', hidden: true },
      { key: 'shareId', label: '分润单ID', type: 'number', hidden: true },
      { key: 'investorId', label: '投资人ID', type: 'number' },
      { key: 'paymentDate', label: '日期', type: 'date' },
      { key: 'paymentType', label: '类型', type: 'select', options: [{ label: '返款', value: 'return' }] },
      { key: 'investorName', label: '投资人姓名', required: true },
      { key: 'amount', label: '金额', type: 'number', required: true },
      { key: 'sourceType', label: '来源', type: 'select', options: [{ label: '手工返款', value: '0' }, { label: '结转自动返款', value: '1' }] },
      { key: 'paymentStatus', label: '状态', type: 'select', options: [{ label: '待返款', value: '0' }, { label: '已返款', value: '1' }] },
      { key: 'investRatio', label: '投资占比', type: 'number' }
    ]
  },
  investor: {
    group: '财务管理',
    title: '投资人管理',
    path: '/finance/investor',
    permissions: crudPermissions('investor'),
    idKey: 'investorId',
    searchKey: 'investorName',
    summary: ['investorName', 'phone', 'status'],
    fields: [
      { key: 'deptId', label: '机构ID', type: 'number', hidden: true },
      { key: 'investorName', label: '投资人姓名', required: true },
      { key: 'phone', label: '联系电话', type: 'phone' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: '0' }, { label: '停用', value: '1' }] },
      { key: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  investRecord: {
    group: '财务管理',
    title: '投资款记录',
    path: '/finance/investRecord',
    permissions: crudPermissions('investRecord'),
    idKey: 'investId',
    searchKey: 'investorName',
    summary: ['investorName', 'investAmount', 'investTime'],
    fields: [
      { key: 'deptId', label: '机构ID', type: 'number', hidden: true },
      { key: 'periodId', label: '周期ID', type: 'number', hidden: true },
      { key: 'investorId', label: '投资人', type: 'select', remoteUrl: '/finance/investor/list', remoteFilterDept: true, remoteLabel: 'investorName', remoteValue: 'investorId', required: true },
      { key: 'investorName', label: '投资人姓名', hidden: true },
      { key: 'investAmount', label: '投资金额', type: 'number', required: true },
      { key: 'investTime', label: '投资时间', type: 'date', required: true },
      { key: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  deptProfitConfig: {
    group: '财务管理',
    title: '店面分润配置',
    path: '/finance/deptProfitConfig',
    permissions: crudPermissions('deptProfitConfig'),
    idKey: 'configId',
    searchKey: 'deptId',
    summary: ['deptId', 'managerProfitRate', 'autoCreateInvestorPayment', 'status'],
    fields: [
      { key: 'deptId', label: '机构', type: 'select', remoteUrl: '/system/dept/list', remoteLabel: 'deptName', remoteValue: 'deptId', required: true },
      { key: 'managerProfitRate', label: '店长分润比例', type: 'number', required: true },
      { key: 'autoCreateInvestorPayment', label: '自动投资人返款', type: 'select', options: [{ label: '否', value: '0' }, { label: '是', value: '1' }] },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: '0' }, { label: '停用', value: '1' }] },
      { key: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  accountingPeriod: {
    group: '财务管理',
    title: '核算周期',
    path: '/finance/accountingPeriod',
    detailPath: '/finance/accountingPeriod/detail',
    permissions: { ...crudPermissions('accountingPeriod'), adjustStartTime: 'finance:accountingPeriod:opsAdjustStartTime', checkBreakEven: 'finance:accountingPeriod:edit', carryForward: 'finance:profitShare:add' },
    idKey: 'periodId',
    searchKey: 'periodNo',
    summary: ['periodNo', 'status', 'totalSalePayment', 'netProfit'],
    readonlyFields: ['deptId', 'periodNo', 'endTime', 'status', 'breakEvenTime', 'carryForwardTime', 'totalVerifiedExpense', 'totalPurchase', 'totalSalePayment', 'totalUnverifiedAdvance', 'netProfit', 'managerProfitRate', 'managerProfitAmount', 'investorProfitAmount'],
    fields: [
      { key: 'deptId', label: '机构ID', type: 'number' },
      { key: 'periodNo', label: '周期编号' },
      { key: 'startTime', label: '周期开始时间' },
      { key: 'endTime', label: '周期结束时间' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '进行中', value: '0' }, { label: '已回本待结转', value: '1' }, { label: '已结转', value: '2' }] },
      { key: 'breakEvenTime', label: '回本时间' },
      { key: 'carryForwardTime', label: '结转时间' },
      { key: 'totalVerifiedExpense', label: '已核销费用', type: 'number' },
      { key: 'totalPurchase', label: '进货款', type: 'number' },
      { key: 'totalSalePayment', label: '销售缴款', type: 'number' },
      { key: 'totalUnverifiedAdvance', label: '借支未核销', type: 'number' },
      { key: 'netProfit', label: '净利', type: 'number' },
      { key: 'managerProfitRate', label: '店长分润比例', type: 'number' },
      { key: 'managerProfitAmount', label: '店长分润', type: 'number' },
      { key: 'investorProfitAmount', label: '投资人返款', type: 'number' }
    ],
    actions: [
      { name: '起始时间调整', action: 'adjustStartTime' },
      { name: '回本检测', action: 'checkBreakEven', url: '/finance/accountingPeriod/current/{deptId}/trialBreakEven', idKey: 'deptId', method: 'POST' },
      { name: '结转分润', action: 'carryForward', url: '/finance/accountingPeriod/current/{deptId}/carryForward', idKey: 'deptId', method: 'POST' }
    ]
  },
  profitShare: {
    group: '财务管理',
    title: '分润结转',
    path: '/finance/profitShare',
    permissions: crudPermissions('profitShare'),
    idKey: 'shareId',
    searchKey: 'shareNo',
    addOnly: true,
    summary: ['shareNo', 'netProfit', 'managerProfitAmount', 'investorProfitAmount'],
    fields: [
      { key: 'deptId', label: '机构ID', type: 'number' },
      { key: 'periodId', label: '周期ID', type: 'number' },
      { key: 'shareNo', label: '分润单号' },
      { key: 'netProfit', label: '净利', type: 'number' },
      { key: 'managerProfitRate', label: '店长分润比例', type: 'number' },
      { key: 'managerProfitAmount', label: '店长分润', type: 'number' },
      { key: 'investorProfitAmount', label: '投资人返款', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: '0' }, { label: '已取消', value: '1' }] },
      { key: 'shareTime', label: '分润时间' }
    ]
  },
  costAccounting: {
    group: '财务管理',
    title: '成本核算',
    path: '/finance/costAccounting',
    permissions: crudPermissions('costAccounting'),
    idKey: 'accountingId',
    searchKey: 'accountingNo',
    detailPath: '/finance/costAccounting/detail',
    addOnly: true,
    summary: ['accountingNo', 'startDate', 'endDate', 'returnSituation'],
    fields: [
      { key: 'startDate', label: '开始日期', type: 'date', required: true },
      { key: 'endDate', label: '结束日期', type: 'date', required: true }
    ],
    readonlyFields: ['periodNo', 'totalVerifiedExpense', 'totalPurchase', 'totalSalePayment', 'totalUnverifiedAdvance', 'netProfit', 'managerProfitRate', 'managerProfitAmount', 'investorProfitAmount', 'returnSituation'],
    pageActions: [{ name: '预览', action: 'view', url: '/finance/costAccounting/preview', method: 'GET' }]
  },
  verificationRecord: {
    group: '财务管理',
    title: '核销记录',
    customPage: '/pages/verification-record/index',
    permissions: { view: ['finance:expense:verificationRecord:list'] }
  },

  // ===== 系统管理（管理员专用）=====
  deptManage: {
    group: '系统管理',
    title: '部门管理',
    path: '/system/dept',
    permissions: { view: ['system:dept:list', 'system:dept:query'], add: 'system:dept:add', edit: 'system:dept:edit', remove: 'system:dept:remove' },
    idKey: 'deptId',
    searchKey: 'deptName',
    summary: ['deptName', 'leader', 'phone', 'fullAddress'],
    fields: [
      { key: 'parentId', label: '上级部门ID', type: 'number', required: true },
      { key: 'deptName', label: '部门名称', required: true },
      { key: 'orderNum', label: '显示排序', type: 'number', required: true },
      { key: 'leader', label: '负责人' },
      { key: 'phone', label: '联系电话', type: 'phone' },
      { key: 'email', label: '邮箱' },
      { key: 'regionPath', label: '所在地址', type: 'region', virtual: true },
      { key: 'fullAddress', label: '所在地址', virtual: true, hidden: true, formatter: (item) => [item.provinceName, item.cityName, item.districtName, item.streetName, item.detailAddress].filter(Boolean).join(' / ') },
      { key: 'provinceCode', label: '省份编码', hidden: true },
      { key: 'provinceName', label: '省份名称', hidden: true },
      { key: 'cityCode', label: '城市编码', hidden: true },
      { key: 'cityName', label: '城市名称', hidden: true },
      { key: 'districtCode', label: '区县编码', hidden: true },
      { key: 'districtName', label: '区县名称', hidden: true },
      { key: 'streetCode', label: '街道编码', hidden: true },
      { key: 'streetName', label: '街道名称', hidden: true },
      { key: 'detailAddress', label: '详细地址', type: 'textarea' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: '0' }, { label: '停用', value: '1' }] }
    ]
  },
  userManage: {
    group: '系统管理',
    title: '用户管理',
    path: '/system/user',
    permissions: { view: ['system:user:list', 'system:user:query'], add: 'system:user:add', edit: 'system:user:edit', remove: 'system:user:remove' },
    idKey: 'userId',
    searchKey: 'userName',
    searchKeys: ['userName', 'phonenumber'],
    summary: ['userName', 'nickName', 'phonenumber', 'status'],
    fields: [
      { key: 'userName', label: '用户名', required: true },
      { key: 'nickName', label: '昵称', required: true },
      { key: 'phonenumber', label: '手机号码', type: 'phone' },
      { key: 'email', label: '邮箱' },
      { key: 'sex', label: '性别', type: 'select', options: [{ label: '男', value: '0' }, { label: '女', value: '1' }, { label: '未知', value: '2' }] },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: '0' }, { label: '停用', value: '1' }] },
      { key: 'deptId', label: '部门ID', type: 'number' },
      { key: 'password', label: '密码', hidden: true },
      { key: 'roleIds', label: '角色', hidden: true }
    ],
    actions: [
      { name: '重置密码', action: 'edit', url: '/system/user/resetPwd', method: 'PUT', bodyFactory: (item) => ({ userId: item.userId, password: '123456' }) }
    ]
  },

  // ===== 移动办公 =====
  wfTodo: {
    group: '移动办公',
    title: '任务中心',
    customPage: '/pages/workflow/todo',
    permissions: { view: ['workflow:mobile:todo'] }
  },
  wfDone: {
    group: '移动办公',
    title: '已办任务',
    customPage: '/pages/workflow/todo?tab=done',
    permissions: { view: ['workflow:mobile:done'] }
  },
  wfNotify: {
    group: '移动办公',
    title: '消息通知',
    customPage: '/pages/notification/index',
    permissions: { view: ['workflow:mobile:notify'] }
  }
}

export const moduleList = Object.keys(modules).map((key) => ({ key, ...modules[key] }))

const memberServiceOrder = ['member', 'seckillRecord', 'pointsRecord', 'seckill', 'pointsExchange', 'pointsGoods']
const orderedMemberServices = memberServiceOrder.map((key) => ({ key, ...modules[key] })).filter((item) => item.group === '会员服务')

export const groups = [
  { name: '会员服务', items: orderedMemberServices },
  { name: '财务管理', items: moduleList.filter((item) => item.group === '财务管理') },
  { name: '系统管理', items: moduleList.filter((item) => item.group === '系统管理') },
  { name: '移动办公', items: moduleList.filter((item) => item.group === '移动办公') }
]

export function getModule(key) {
  return modules[key]
}

export function getFieldLabel(moduleKey, key) {
  return modules[moduleKey]?.fields.find((field) => field.key === key)?.label || key
}

export function displayValue(field, value, item) {
  if (value === undefined || value === null || value === '') return '-'
  if (field?.displayKey && item && item[field.displayKey] !== undefined && item[field.displayKey] !== null && item[field.displayKey] !== '') {
    return item[field.displayKey]
  }
  const options = field?.options || []
  if (Array.isArray(options) && options.length) {
    const hit = options.find((item) => {
      if (typeof item === 'string') return item === value
      return String(item.value) === String(value)
    })
    if (hit) return typeof hit === 'string' ? hit : hit.label
  }
  return value
}

const MONEY_KEYS = [
  'expenseAmount', 'advanceAmount', 'purchasePrice', 'salePrice', 'totalAmount', 'paidAmount',
  'saleAmount', 'unitPrice', 'amount', 'investAmount', 'totalVerifiedExpense', 'totalPurchase',
  'totalSalePayment', 'totalSaleAmount', 'totalUnverifiedAdvance', 'netProfit',
  'managerProfitAmount', 'investorProfitAmount', 'totalExpense', 'totalSale', 'totalPayment',
  'totalInvest', 'currentAdvance', 'extraAmount', 'consumeAmount', 'seckillAmount', 'seckillPrice',
  'goodsValue'
]

const POINT_KEYS = ['totalPoints', 'availablePoints', 'pointsPrice', 'points', 'balance', 'pointsDeducted']
const PERCENT_KEYS = ['managerProfitRate', 'investRatio']
const COUNT_KEYS = ['stock', 'stockNum', 'minStock', 'quantity', 'saleQuantity', 'giftQuantity', 'totalQuantity', 'totalShares', 'remainShares', 'shares', 'exchanged']

function isNumericLike(value) {
  return value !== '' && value !== null && value !== undefined && !Number.isNaN(Number(value))
}

function trimNumber(value) {
  const num = Number(value)
  return Number.isInteger(num) ? String(num) : num.toFixed(3).replace(/\.?0+$/, '')
}

export function formatDisplayValue(field, value, item) {
  const base = displayValue(field, value, item)
  if (base === '-') return base
  const key = field?.key || ''
  if (MONEY_KEYS.includes(key) && isNumericLike(value)) return '¥' + Number(value).toFixed(2)
  if (POINT_KEYS.includes(key) && isNumericLike(value)) return trimNumber(value) + ' 积分'
  if (PERCENT_KEYS.includes(key) && isNumericLike(value)) return trimNumber(value) + '%'
  if (COUNT_KEYS.includes(key) && isNumericLike(value)) return trimNumber(value)
  return base
}

export function getValueTone(field, value) {
  if (!field || value === undefined || value === null || value === '') return ''
  const key = field.key || ''
  if (MONEY_KEYS.includes(key)) {
    const num = Number(value)
    if (Number.isNaN(num)) return 'tone-money'
    if (num < 0) return 'tone-danger'
    return 'tone-money'
  }
  if (POINT_KEYS.includes(key)) return 'tone-points'
  if (PERCENT_KEYS.includes(key)) return 'tone-percent'
  if (field.type === 'select' || key === 'status' || key === 'paymentStatus' || key === 'sourceType') {
    const label = String(displayValue(field, value))
    if (/正常|启用|进行中|已领取|已缴清|已返款|已完成|上架/.test(label)) return 'status-ok'
    if (/待|未|部分|草稿/.test(label)) return 'status-warn'
    if (/无效|停用|取消|结束|退卡|下架/.test(label)) return 'status-danger'
    return 'status-info'
  }
  return ''
}
