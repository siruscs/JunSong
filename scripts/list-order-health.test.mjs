import { readFileSync } from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

function normalizedXml(path) {
  return readFileSync(path, 'utf8').replace(/\s+/g, ' ').toLowerCase();
}

test('member list defaults to member number descending', () => {
  const xml = normalizedXml('junsong-modules/junsong-member/src/main/resources/mapper/member/MemMemberMapper.xml');
  assert.match(
    xml,
    /<select id="selectmemmemberlist"[\s\S]*?order by m\.member_no desc/,
    'selectMemMemberList must order by m.member_no desc'
  );
});

test('expense list defaults to expense date descending then create time descending', () => {
  const xml = normalizedXml('junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinExpenseMapper.xml');
  assert.match(
    xml,
    /<select id="selectfinexpenselist"[\s\S]*?order by e\.expense_date desc,\s*e\.create_time desc/,
    'selectFinExpenseList must order by e.expense_date desc, e.create_time desc'
  );
});
