package com.junsong.common.core.interceptor;

import com.junsong.common.core.context.TenantContext;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * MyBatis 多租户 SQL 拦截器
 * 拦截 SELECT/UPDATE/DELETE，用 JSqlParser 自动注入 tenant_id 条件
 * INSERT 由 TenantInterceptor + DEFAULT 1 处理
 *
 * @author junsong
 */
@Intercepts({
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, org.apache.ibatis.cache.CacheKey.class, BoundSql.class}),
    @Signature(type = Executor.class, method = "update",
               args = {MappedStatement.class, Object.class})
})
public class TenantSqlInterceptor implements Interceptor
{
    private static final Logger log = LoggerFactory.getLogger(TenantSqlInterceptor.class);

    private static final String TENANT_COLUMN = "tenant_id";

    private static final Pattern TENANT_TABLE_PATTERN =
            Pattern.compile("^(sys_(?!menu$|menu_backup|dict_type$|dict_data$|region$|tenant$|operation_schedule_log$|action_center_touch_log$|action_center_touch_throttle$|data_retention_policy$|data_archive_run$|operation_audit_snapshot$|operation_alert_rule$|operation_alert_event$|mp_module_sort$)|fin_(?!composite_period_item$|composite_pool_dept$|composite_pool_investor$|ance_prediction_factor$|ance_review_knowledge$|ance_review_task$|ance_review_task_log$)|mem_(?!refund_apply$|member_no_sequence$)|lc_|wf_).*");

    private static final Pattern EXCLUDE_TABLES =
            Pattern.compile("^(act_|flw_|qrtz_|gen_).*");

    @Override
    public Object intercept(Invocation invocation) throws Throwable
    {
        if (TenantContext.isIgnore())
        {
            return invocation.proceed();
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null)
        {
            return invocation.proceed();
        }

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        BoundSql boundSql = args.length > 5 && args[5] instanceof BoundSql
                ? (BoundSql) args[5]
                : ms.getBoundSql(parameter);
        String originalSql = boundSql.getSql();

        try
        {
            String processedSql = processSql(originalSql, tenantId);
            if (processedSql != null && !originalSql.equals(processedSql))
            {
                MetaObject metaObject = SystemMetaObject.forObject(boundSql);
                metaObject.setValue("sql", processedSql);
                log.debug("租户SQL注入: tenant_id={}", tenantId);
            }
        }
        catch (Exception e)
        {
            log.warn("租户SQL注入失败: {}", e.getMessage());
        }

        return invocation.proceed();
    }

    private String processSql(String sql, Long tenantId) throws JSQLParserException
    {
        String trimmed = sql.trim().toLowerCase();
        if (trimmed.startsWith("select"))
        {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select)
            {
                return processSelect((Select) statement, tenantId);
            }
        }
        else if (trimmed.startsWith("update"))
        {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Update)
            {
                return processUpdate((Update) statement, tenantId);
            }
        }
        else if (trimmed.startsWith("delete"))
        {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Delete)
            {
                return processDelete((Delete) statement, tenantId);
            }
        }

        return sql;
    }

    private String processSelect(Select select, Long tenantId)
    {
        if (select instanceof PlainSelect)
        {
            processPlainSelect((PlainSelect) select, tenantId);
        }
        else if (select instanceof SetOperationList)
        {
            List<Select> selects = ((SetOperationList) select).getSelects();
            if (selects != null)
            {
                for (Select subSelect : selects)
                {
                    if (subSelect instanceof PlainSelect)
                    {
                        processPlainSelect((PlainSelect) subSelect, tenantId);
                    }
                }
            }
        }
        return select.toString();
    }

    private void processPlainSelect(PlainSelect plainSelect, Long tenantId)
    {
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table)
        {
            Table table = (Table) fromItem;
            if (isTenantTable(table.getName()))
            {
                Expression where = plainSelect.getWhere();
                EqualsTo tenantCondition = createTenantEquals(table, tenantId);
                plainSelect.setWhere(where == null ? tenantCondition : new AndExpression(where, tenantCondition));
            }
        }
    }

    private String processUpdate(Update update, Long tenantId)
    {
        Table table = update.getTable();
        if (table != null && isTenantTable(table.getName()))
        {
            Expression where = update.getWhere();
            EqualsTo tenantCondition = createTenantEquals(table, tenantId);
            update.setWhere(where == null ? tenantCondition : new AndExpression(where, tenantCondition));
        }
        return update.toString();
    }

    private String processDelete(Delete delete, Long tenantId)
    {
        Table table = delete.getTable();
        if (table != null && isTenantTable(table.getName()))
        {
            Expression where = delete.getWhere();
            EqualsTo tenantCondition = createTenantEquals(table, tenantId);
            delete.setWhere(where == null ? tenantCondition : new AndExpression(where, tenantCondition));
        }
        return delete.toString();
    }

    private EqualsTo createTenantEquals(Table table, Long tenantId)
    {
        String prefix = table.getAlias() != null ? table.getAlias().getName() : table.getName();
        Column tenantColumn = new Column(new Table(prefix), TENANT_COLUMN);
        return new EqualsTo(tenantColumn, new LongValue(tenantId));
    }

    private boolean isTenantTable(String tableName)
    {
        if (tableName == null)
        {
            return false;
        }
        String name = tableName.replace("`", "").replace("\"", "").toLowerCase();
        if (EXCLUDE_TABLES.matcher(name).matches())
        {
            return false;
        }
        return TENANT_TABLE_PATTERN.matcher(name).matches();
    }

    @Override
    public Object plugin(Object target)
    {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties)
    {
    }
}
