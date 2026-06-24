const XLSX = require('xlsx');
const fs = require('fs');
const path = require('path');

const PROJECT_DIR = '/Users/sirius/Documents/TRAE/JunSong-Cloud';
const SQL_OUTPUT = '/tmp/import.sql';

function excelDateToSQL(serial) {
  if (!serial || serial === '' || isNaN(serial)) return null;
  const ms = (Number(serial) - 25569) * 86400 * 1000;
  const d = new Date(ms);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function escapeSQL(str) {
  if (str === null || str === undefined) return 'NULL';
  return "'" + String(str).replace(/'/g, "''") + "'";
}

function mapPaymentMethod(method) {
  if (!method) return '现金';
  if (method === '直接付款' || method === '自行垫付') return '现金';
  return method;
}

function mapStatus(status) {
  if (!status) return '0';
  if (status === '已核销') return '1';
  return '0';
}

function today() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
}

function padNum(n, len) {
  return String(n).padStart(len, '0');
}

const sqls = [];
const stats = {
  mem_member: 0,
  fin_expense: 0,
  fin_advance: 0,
  fin_purchase: 0,
  fin_cost_accounting: 0,
};

// ============================================================
// 1. 会员信息
// ============================================================
console.log('处理 会员信息...');
const wb1 = XLSX.readFile(path.join(PROJECT_DIR, '盛和里会员列表.xlsx'));
const ws1 = wb1.Sheets['会员信息'];
const memberData = XLSX.utils.sheet_to_json(ws1, { header: 1, raw: true, defval: '' });

for (let i = 2; i < memberData.length; i++) {
  const row = memberData[i];
  const memberNo = row[0];
  const memberName = row[1];
  if (!memberNo || !memberName || !String(memberNo).startsWith('S')) continue;
  if (String(memberName).trim() === '') continue;

  const phone = row[3] ? String(row[3]).trim() : '';
  const joinDateSerial = row[4];
  const joinDate = joinDateSerial ? excelDateToSQL(joinDateSerial) : null;
  const address = row[5] ? String(row[5]).trim() : '';

  sqls.push(
    `INSERT IGNORE INTO mem_member (dept_id, member_no, member_name, phone, join_date, address, status, del_flag, create_by, create_time) VALUES ` +
    `(200, ${escapeSQL(memberNo)}, ${escapeSQL(memberName)}, ${phone ? escapeSQL(phone) : 'NULL'}, ${joinDate ? escapeSQL(joinDate) : 'NULL'}, ${address ? escapeSQL(address) : 'NULL'}, '0', '0', 'admin', NOW());`
  );
  stats.mem_member++;
}
console.log(`  会员: ${stats.mem_member} 条`);

// ============================================================
// 2. 明细记录表 (支出 + 预支)
// ============================================================
console.log('处理 明细记录表...');
const wb2 = XLSX.readFile(path.join(PROJECT_DIR, '盛和里开支记录.xlsx'));
const wsDetail = wb2.Sheets['明细记录表'];
const detailData = XLSX.utils.sheet_to_json(wsDetail, { header: 1, raw: true, defval: '' });

let expenseSeq = 1;
let advanceSeq = 1;
let lastKnownDate = null;

for (let i = 2; i < detailData.length; i++) {
  const row = detailData[i];
  const timeVal = row[0];
  const type = String(row[1] || '').trim();
  const projectName = String(row[2] || '').trim();
  const amountVal = row[3];
  const paymentMethod = String(row[4] || '').trim();
  const verifyStatus = String(row[5] || '').trim();

  if (type !== '支出' && type !== '预支') continue;
  if (projectName === '小计' || projectName.includes('合计')) continue;
  if (projectName === '' && (amountVal === '' || amountVal === 0 || amountVal === null)) continue;

  let amount = 0;
  if (amountVal !== '' && amountVal !== null && amountVal !== undefined) {
    amount = parseFloat(amountVal);
    if (isNaN(amount)) amount = 0;
  }

  let expenseDate;
  if (timeVal && timeVal !== '' && !isNaN(timeVal)) {
    expenseDate = excelDateToSQL(timeVal);
    lastKnownDate = expenseDate;
  } else {
    expenseDate = lastKnownDate || '2026-04-06';
  }

  const payment = mapPaymentMethod(paymentMethod);
  const status = mapStatus(verifyStatus);

  if (type === '支出') {
    if (amount === 0 && projectName === '') continue;

    const dateStr = expenseDate.replace(/-/g, '');
    const no = `FE${dateStr}${padNum(expenseSeq++, 3)}`;
    const content = projectName || '其他';
    const expenseType = '开支';

    sqls.push(
      `INSERT IGNORE INTO fin_expense (dept_id, expense_no, expense_date, expense_type, expense_content, payment_method, expense_amount, status, del_flag, create_by, create_time) VALUES ` +
      `(200, ${escapeSQL(no)}, ${escapeSQL(expenseDate)}, ${escapeSQL(expenseType)}, ${escapeSQL(content)}, ${escapeSQL(payment)}, ${amount}, ${escapeSQL(status)}, '0', 'admin', NOW());`
    );
    stats.fin_expense++;
  } else if (type === '预支') {
    if (amount === 0) continue;

    const dateStr = expenseDate.replace(/-/g, '');
    const no = `FA${dateStr}${padNum(advanceSeq++, 3)}`;
    const purpose = projectName || '日常支出';

    sqls.push(
      `INSERT IGNORE INTO fin_advance (dept_id, advance_no, advance_date, advance_amount, purpose, status, del_flag, create_by, create_time) VALUES ` +
      `(200, ${escapeSQL(no)}, ${escapeSQL(expenseDate)}, ${amount}, ${escapeSQL(purpose)}, ${escapeSQL(status)}, '0', 'admin', NOW());`
    );
    stats.fin_advance++;
  }
}
console.log(`  支出: ${stats.fin_expense} 条, 预支: ${stats.fin_advance} 条`);

// ============================================================
// 3. 未核销费用表 (数据为空，但仍处理逻辑)
// ============================================================
console.log('处理 未核销费用表...');
const wsUnverified = wb2.Sheets['未核销费用表'];
const unverifiedData = XLSX.utils.sheet_to_json(wsUnverified, { header: 1, raw: true, defval: '' });

for (let i = 2; i < unverifiedData.length; i++) {
  const row = unverifiedData[i];
  const timeVal = row[0];
  const type = String(row[1] || '').trim();
  const projectName = String(row[2] || '').trim();
  const amountVal = row[3];
  const paymentMethod = String(row[4] || '').trim();
  const verifyStatus = String(row[5] || '').trim();

  if (type !== '支出' && type !== '预支') continue;
  if (projectName === '' && (amountVal === '' || amountVal === 0 || amountVal === null)) continue;

  let amount = 0;
  if (amountVal !== '' && amountVal !== null && amountVal !== undefined) {
    amount = parseFloat(amountVal);
    if (isNaN(amount)) amount = 0;
  }

  if (amount === 0 && projectName === '') continue;

  let expenseDate;
  if (timeVal && timeVal !== '' && !isNaN(timeVal)) {
    expenseDate = excelDateToSQL(timeVal);
  } else {
    expenseDate = today();
  }

  const payment = mapPaymentMethod(paymentMethod);
  const status = mapStatus(verifyStatus);

  if (type === '支出') {
    const dateStr = expenseDate.replace(/-/g, '');
    const no = `FE${dateStr}${padNum(expenseSeq++, 3)}`;
    const content = projectName || '其他';

    sqls.push(
      `INSERT IGNORE INTO fin_expense (dept_id, expense_no, expense_date, expense_type, expense_content, payment_method, expense_amount, status, del_flag, create_by, create_time) VALUES ` +
      `(200, ${escapeSQL(no)}, ${escapeSQL(expenseDate)}, '开支', ${escapeSQL(content)}, ${escapeSQL(payment)}, ${amount}, ${escapeSQL(status)}, '0', 'admin', NOW());`
    );
    stats.fin_expense++;
  } else if (type === '预支' && amount > 0) {
    const dateStr = expenseDate.replace(/-/g, '');
    const no = `FA${dateStr}${padNum(advanceSeq++, 3)}`;
    const purpose = projectName || '日常支出';

    sqls.push(
      `INSERT IGNORE INTO fin_advance (dept_id, advance_no, advance_date, advance_amount, purpose, status, del_flag, create_by, create_time) VALUES ` +
      `(200, ${escapeSQL(no)}, ${escapeSQL(expenseDate)}, ${amount}, ${escapeSQL(purpose)}, ${escapeSQL(status)}, '0', 'admin', NOW());`
    );
    stats.fin_advance++;
  }
}
console.log(`  未核销表处理完成，累计支出: ${stats.fin_expense}, 累计预支: ${stats.fin_advance}`);

// ============================================================
// 4. 进货记录表
// ============================================================
console.log('处理 进货记录表...');
const wsPurchase = wb2.Sheets['进货记录表'];
const purchaseData = XLSX.utils.sheet_to_json(wsPurchase, { header: 1, raw: true, defval: '' });

let purchaseSeq = 1;
for (let i = 1; i < purchaseData.length; i++) {
  const row = purchaseData[i];
  const category = String(row[0] || '').trim();
  if (!category || category === '汇总') continue;

  const purchaseDateSerial = row[1];
  const quantity = parseFloat(row[2]) || 0;
  const unit = String(row[3] || '').trim();
  const unitPrice = parseFloat(row[4]) || 0;
  const address = String(row[5] || '').trim();

  const purchaseDate = purchaseDateSerial ? excelDateToSQL(purchaseDateSerial) : today();
  const totalAmount = quantity * unitPrice;
  const dateStr = purchaseDate.replace(/-/g, '');
  const no = `FP${dateStr}${padNum(purchaseSeq++, 3)}`;

  sqls.push(
    `INSERT IGNORE INTO fin_purchase (purchase_no, supplier_id, purchase_date, total_amount, paid_amount, total_quantity, status, dept_id, del_flag, create_by, create_time, remark) VALUES ` +
    `(${escapeSQL(no)}, 1, ${escapeSQL(purchaseDate)}, ${totalAmount}, 0, ${quantity}, '0', 200, '0', 'admin', NOW(), ${escapeSQL(category)});`
  );
  stats.fin_purchase++;
}
console.log(`  进货: ${stats.fin_purchase} 条`);

// ============================================================
// 5. 成本核算表
// ============================================================
console.log('处理 成本核算表...');
const wsCost = wb2.Sheets['成本核算表'];
const costData = XLSX.utils.sheet_to_json(wsCost, { header: 1, raw: true, defval: '' });

if (costData.length >= 3) {
  const row = costData[2];
  const instrumentPayment = parseFloat(row[2]) || 0;
  const rentDeposit = parseFloat(row[3]) || 0;
  const otherExpense = parseFloat(row[4]) || 0;
  const purchaseAmount = parseFloat(row[5]) || 0;
  const salePayment = parseFloat(row[6]) || 0;

  const totalInvest = instrumentPayment + rentDeposit + otherExpense + purchaseAmount;

  sqls.push(
    `INSERT IGNORE INTO fin_cost_accounting (dept_id, accounting_no, start_date, end_date, total_expense, total_invest, return_situation, del_flag, create_by, create_time) VALUES ` +
    `(200, 'FCA001', '2026-03-01', CURDATE(), ${totalInvest}, ${totalInvest}, ${salePayment}, '0', 'admin', NOW());`
  );
  stats.fin_cost_accounting++;
}
console.log(`  成本核算: ${stats.fin_cost_accounting} 条`);

// ============================================================
// 6. 写入 SQL 文件
// ============================================================
const header = `-- 盛和里数据导入脚本\n-- 生成时间: ${new Date().toISOString()}\n-- dept_id=200\n\nSET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS = 0;\n\n`;
const footer = `\nSET FOREIGN_KEY_CHECKS = 1;\n`;

fs.writeFileSync(SQL_OUTPUT, header + sqls.join('\n') + footer, 'utf8');
console.log(`\nSQL 文件已写入: ${SQL_OUTPUT}`);
console.log(`总 SQL 语句数: ${sqls.length}`);
console.log('\n=== 导入统计 ===');
console.log(`mem_member:          ${stats.mem_member} 条`);
console.log(`fin_expense:         ${stats.fin_expense} 条`);
console.log(`fin_advance:         ${stats.fin_advance} 条`);
console.log(`fin_purchase:        ${stats.fin_purchase} 条`);
console.log(`fin_cost_accounting: ${stats.fin_cost_accounting} 条`);
