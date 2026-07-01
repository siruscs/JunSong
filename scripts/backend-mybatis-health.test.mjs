import test from 'node:test'
import assert from 'node:assert/strict'
import { writeFileSync, mkdtempSync, rmSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'

import { checkMapperFile } from './backend-mybatis-health.mjs'

function writeTempMapper(name, content) {
  const dir = mkdtempSync(join(tmpdir(), 'mybatis-test-'))
  const file = join(dir, name)
  writeFileSync(file, content, 'utf8')
  return { file, dir }
}

test('detects empty select statement', () => {
  const { file, dir } = writeTempMapper('TestMapper.xml', `<?xml version="1.0" encoding="UTF-8"?>
<mapper namespace="com.test.TestMapper">
  <select id="countAll" resultType="Long">
  </select>
</mapper>`)
  try {
    const issues = checkMapperFile(file)
    assert.equal(issues.length, 1)
    assert.equal(issues[0].tag, 'select')
    assert.equal(issues[0].id, 'countAll')
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
})

test('passes non-empty select statement', () => {
  const { file, dir } = writeTempMapper('TestMapper.xml', `<?xml version="1.0" encoding="UTF-8"?>
<mapper namespace="com.test.TestMapper">
  <select id="countAll" resultType="Long">
    SELECT COUNT(*) FROM users
  </select>
</mapper>`)
  try {
    const issues = checkMapperFile(file)
    assert.equal(issues.length, 0)
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
})

test('passes dynamic SQL with child tags', () => {
  const { file, dir } = writeTempMapper('TestMapper.xml', `<?xml version="1.0" encoding="UTF-8"?>
<mapper namespace="com.test.TestMapper">
  <select id="findByCondition" resultType="User">
    <if test="name != null">
      SELECT * FROM users WHERE name = #{name}
    </if>
  </select>
</mapper>`)
  try {
    const issues = checkMapperFile(file)
    assert.equal(issues.length, 0)
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
})

test('detects comment-only statement as empty', () => {
  const { file, dir } = writeTempMapper('TestMapper.xml', `<?xml version="1.0" encoding="UTF-8"?>
<mapper namespace="com.test.TestMapper">
  <select id="countAll" resultType="Long">
    <!-- TODO: implement this query -->
  </select>
</mapper>`)
  try {
    const issues = checkMapperFile(file)
    assert.equal(issues.length, 1)
    assert.equal(issues[0].tag, 'select')
    assert.equal(issues[0].id, 'countAll')
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
})

test('ignores sql fragment tags', () => {
  const { file, dir } = writeTempMapper('TestMapper.xml', `<?xml version="1.0" encoding="UTF-8"?>
<mapper namespace="com.test.TestMapper">
  <sql id="Base_Column_List">
    id, name, status
  </sql>
  <select id="countAll" resultType="Long">
    SELECT COUNT(*) FROM users
  </select>
</mapper>`)
  try {
    const issues = checkMapperFile(file)
    assert.equal(issues.length, 0)
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
})

test('detects multiple empty statements across types', () => {
  const { file, dir } = writeTempMapper('TestMapper.xml', `<?xml version="1.0" encoding="UTF-8"?>
<mapper namespace="com.test.TestMapper">
  <select id="countAll" resultType="Long"></select>
  <insert id="insertUser"></insert>
  <update id="updateUser"></update>
  <delete id="deleteUser"></delete>
</mapper>`)
  try {
    const issues = checkMapperFile(file)
    assert.equal(issues.length, 4)
    assert.deepEqual(issues.map(i => i.tag).sort(), ['delete', 'insert', 'select', 'update'])
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
})
