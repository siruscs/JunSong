package com.junsong.finance.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.*;
import com.junsong.finance.domain.vo.ExpenseVerifyVO;
import com.junsong.finance.domain.vo.ExpenseUnverifyVO;
import com.junsong.finance.mapper.*;
import com.junsong.finance.service.IFinAccountingPeriodService;

class FinExpenseVerificationServiceImplTest
{
    @Test void verifyWithoutAdvanceCreatesBatchAndVerifiesExpenses() {
        Harness h=new Harness(List.of(expense(1,10,"0",100,null)), List.of());
        assertEquals(77L,h.verify("r1",List.of(1L),null)); assertEquals(1,h.batchWrites); assertEquals(1,h.expenseDetails.size()); assertTrue(h.advanceDetails.isEmpty()); assertEquals(1,h.expenseUpdates);
    }
    @Test void equalAmountsVerifySourceAdvancesWithoutGeneratedRecord() {
        Harness h=new Harness(List.of(expense(1,10,"0",100,null)),List.of(advance(2,10,"0",100,null)));
        h.verify("r2",List.of(1L),List.of(2L)); assertEquals(1,h.advanceDetails.size()); assertEquals(FinAdvanceVerifyDetail.RELATION_SOURCE,h.advanceDetails.get(0).getRelationType()); assertEquals(0,h.generatedAdvances);
    }
    @Test void expenseGreaterThanAdvanceCreatesSupplementDetail() {
        Harness h=new Harness(List.of(expense(1,10,"0",150,null)),List.of(advance(2,10,"0",100,null)));
        h.verify("r3",List.of(1L),List.of(2L)); assertEquals(1,h.generatedAdvances); assertEquals(FinAdvanceVerifyDetail.RELATION_SUPPLEMENT,h.advanceDetails.get(1).getRelationType()); assertEquals(new BigDecimal("50.00"),h.advanceDetails.get(1).getAdvanceAmount());
    }
    @Test void expenseLessThanAdvanceCreatesSurplusDetail() {
        Harness h=new Harness(List.of(expense(1,10,"0",80,null)),List.of(advance(2,10,"0",100,null)));
        h.verify("r4",List.of(1L),List.of(2L)); assertEquals(FinAdvanceVerifyDetail.RELATION_SURPLUS,h.advanceDetails.get(1).getRelationType()); assertEquals(new BigDecimal("20.00"),h.advanceDetails.get(1).getAdvanceAmount()); assertEquals("0",h.generatedAdvance.getStatus()); assertNull(h.generatedAdvance.getVerifyBy()); assertNull(h.generatedAdvance.getVerifyTime()); assertEquals("0",h.advanceDetails.get(1).getOriginalStatus());
    }
    @Test void expenseGreaterThanAdvanceCreatesVerifiedSupplement() { Harness h=new Harness(List.of(expense(1,10,"0",150,null)),List.of(advance(2,10,"0",100,null))); h.verify("supp-state",List.of(1L),List.of(2L)); assertEquals("1",h.generatedAdvance.getStatus()); assertEquals("finance",h.generatedAdvance.getVerifyBy()); assertNotNull(h.generatedAdvance.getVerifyTime()); }
    @Test void verificationCandidateUsesTenantAndCurrentDepartmentScope() { Harness h=new Harness(List.of(expense(1,10,"0",100,11L)),List.of()); assertEquals(1L,h.service.getVerificationCandidate(1L).getExpenseId()); assertEquals(1L,h.scopedTenant); assertEquals(10L,h.scopedDept); }
    @Test void verificationCandidateRejectsMissingOrNonUnverifiedRows() { Harness missing=new Harness(List.of(),List.of()); assertThrows(ServiceException.class,()->missing.service.getVerificationCandidate(1L)); Harness verified=new Harness(List.of(expense(1,10,"1",100,11L)),List.of()); assertThrows(ServiceException.class,()->verified.service.getVerificationCandidate(1L)); }
    @Test void duplicateRequestReturnsExistingResultWithoutWritingAgain() {
        Harness h=new Harness(List.of(expense(1,10,"0",100,null)),List.of()); h.existingBatch=88L; h.seedReplay(List.of(1L),List.of());
        assertEquals(88L,h.verify("same",List.of(1L),null)); assertEquals(0,h.totalWrites());
    }
    @Test void duplicateRequestWithDifferentPayloadIsRejected() { Harness h=new Harness(List.of(expense(1,10,"0",100,11L)),List.of()); h.existingBatch=88L; h.seedReplay(List.of(2L),List.of()); assertThrows(ServiceException.class,()->h.verify("same",List.of(1L),null)); }
    @Test void duplicateIdsAreRejectedBeforeReplayLookup() { Harness h=new Harness(List.of(expense(1,10,"0",100,11L)),List.of()); h.existingBatch=88L; h.seedReplay(List.of(1L),List.of()); assertThrows(ServiceException.class,()->h.verify("same",List.of(1L,1L),null)); assertEquals(0,h.selectRequestCalls); }
    @Test void nullExpensePeriodFailsBeforeWrites() { Harness h=new Harness(List.of(expense(1,10,"0",100,11L)),List.of()); h.expenses.get(0).setPeriodId(null); assertThrows(ServiceException.class,()->h.verify("null-e",List.of(1L),null)); assertEquals(0,h.totalWrites()); }
    @Test void nullAdvancePeriodFailsBeforeWrites() { Harness h=new Harness(List.of(expense(1,10,"0",100,11L)),List.of(advance(2,10,"0",100,11L))); h.advances.get(0).setPeriodId(null); assertThrows(ServiceException.class,()->h.verify("null-a",List.of(1L),List.of(2L))); assertEquals(0,h.totalWrites()); }
    @Test void verifiedExpenseRejectsEntireBatch() {
        Harness h=new Harness(List.of(expense(1,10,"1",100,null)),List.of());
        assertThrows(ServiceException.class,()->h.verify("r6",List.of(1L),null)); assertEquals(0,h.totalWrites());
    }
    @Test void recordsFromDifferentDepartmentsAreRejected() {
        Harness h=new Harness(List.of(expense(1,10,"0",50,null),expense(2,11,"0",50,null)),List.of());
        assertThrows(ServiceException.class,()->h.verify("r7",List.of(1L,2L),null)); assertEquals(0,h.totalWrites());
    }
    @Test void lockedPeriodRejectsBeforeAnyWrite() {
        Harness h=new Harness(List.of(expense(1,10,"0",100,9L)),List.of()); h.lockPeriod=true;
        assertThrows(ServiceException.class,()->h.verify("r8",List.of(1L),null)); assertEquals(0,h.totalWrites());
    }
    @Test void conditionalUpdateConflictRollsBackBatch() {
        Harness h=new Harness(List.of(expense(1,10,"0",100,null)),List.of()); h.expenseUpdateResult=0;
        ServiceException ex=assertThrows(ServiceException.class,()->h.verify("r9",List.of(1L),null)); assertEquals("费用状态已变化，请刷新后重试",ex.getMessage()); assertEquals(1,h.batchWrites); assertEquals(0,h.successfulExpenseUpdates);
    }
    @Test void concurrentDuplicateInsertReturnsWinnerBatch() { Harness h=new Harness(List.of(expense(1,10,"0",100,null)),List.of()); h.duplicateOnInsert=true; h.seedReplay(List.of(1L),List.of()); assertEquals(99L,h.verify("race",List.of(1L),null)); assertEquals(0,h.detailWrites); }
    @Test void generatedAdvanceWithoutPrimaryKeyFails() { Harness h=new Harness(List.of(expense(1,10,"0",150,null)),List.of(advance(2,10,"0",100,null))); h.omitGeneratedKey=true; assertThrows(ServiceException.class,()->h.verify("key",List.of(1L),List.of(2L))); assertEquals(0,h.detailWrites-1); }
    @Test void scopedQueriesAndConditionalUpdatesReceiveTenantAndCurrentDept() { Harness h=new Harness(List.of(expense(1,10,"0",100,null)),List.of()); h.verify("scope",List.of(1L),null); assertEquals(1L,h.scopedTenant); assertEquals(10L,h.scopedDept); assertEquals(1L,h.updateTenant); assertEquals(10L,h.updateDept); }
    @Test void unverifyRestoresExpenseAndSourceAdvanceSnapshots() { Harness h=Harness.reversal(); assertEquals(1,h.unverify("u1")); assertEquals(1,h.expenseRestores); assertEquals(1,h.advanceRestores); assertEquals(1,h.batchReverses); assertEquals("0",h.restoredExpenseStatus); assertEquals(44L,h.restoredExpenseAdvanceId); assertEquals("0",h.restoredAdvanceStatus); }
    @Test void unverifyInvalidatesGeneratedSupplement() { Harness h=Harness.reversal(); h.addGenerated(9,FinAdvanceVerifyDetail.RELATION_SUPPLEMENT,50); h.unverify("u2"); assertEquals(List.of(9L),h.invalidatedAdvances); }
    @Test void unverifyInvalidatesGeneratedSurplus() { Harness h=Harness.reversal(); h.addGenerated(10,FinAdvanceVerifyDetail.RELATION_SURPLUS,20); h.unverify("u3"); assertEquals(List.of(10L),h.invalidatedAdvances); }
    @Test void unverifyAllowsGeneratedUnverifiedSurplusWithBlankVerifyBy() { Harness h=Harness.reversal(); h.addGenerated(10,FinAdvanceVerifyDetail.RELATION_SURPLUS,60); FinAdvance surplus=h.advances.get(1); surplus.setStatus("0"); surplus.setVerifyBy(""); surplus.setVerifyTime(null); assertEquals(1,h.unverify("surplus-blank")); assertEquals(1,h.expenseRestores); assertEquals(1,h.advanceRestores); assertEquals(List.of(10L),h.invalidatedAdvances); assertEquals(1,h.batchReverses); }
    @Test void lockedExpensePeriodForbidsUnverifyWithoutWrites() { Harness h=Harness.reversal(); h.expenseDetails.get(0).setPeriodId(8L); h.lockPeriod=true; assertThrows(ServiceException.class,()->h.unverify("u4")); assertEquals(0,h.reversalWrites()); }
    @Test void carriedForwardAdvancePeriodForbidsUnverifyWithoutWrites() { Harness h=Harness.reversal(); h.advanceDetails.get(0).setPeriodId(9L); h.lockPeriod=true; assertThrows(ServiceException.class,()->h.unverify("u5")); assertEquals(0,h.reversalWrites()); }
    @Test void downstreamUseOfGeneratedAdvanceForbidsUnverify() { Harness h=Harness.reversal(); h.addGenerated(9,FinAdvanceVerifyDetail.RELATION_SUPPLEMENT,50); h.downstreamReferences=1; ServiceException ex=assertThrows(ServiceException.class,()->h.unverify("u6")); assertEquals("核销生成的借支记录已被后续业务使用，不能反核销",ex.getMessage()); assertEquals(0,h.reversalWrites()); }
    @Test void legacyBatchForbidsAutomaticUnverify() { Harness h=Harness.reversal(); h.batch.setSourceType(FinExpenseVerifyBatch.SOURCE_LEGACY); assertThrows(ServiceException.class,()->h.unverify("u7")); assertEquals(0,h.reversalWrites()); }
    @Test void alreadyReversedBatchIsIdempotentForSameRequest() { Harness h=Harness.reversal(); h.batch.setStatus(FinExpenseVerifyBatch.STATUS_REVERSED); h.batch.setReverseRequestId("same"); assertEquals(0,h.unverify("same")); assertEquals(0,h.reversalWrites()); }
    @Test void alreadyReversedBatchRejectsDifferentRequest() { Harness h=Harness.reversal(); h.batch.setStatus(FinExpenseVerifyBatch.STATUS_REVERSED); h.batch.setReverseRequestId("old"); assertThrows(ServiceException.class,()->h.unverify("new")); }
    @Test void optimisticConflictRollsBackAllRestores() { Harness h=Harness.reversal(); h.batchReverseResult=0; ServiceException ex=assertThrows(ServiceException.class,()->h.unverify("u8")); assertEquals("反核销状态已变化，请刷新后重试",ex.getMessage()); assertEquals(1,h.expenseRestores); assertEquals(1,h.advanceRestores); }
    @Test void nullSnapshotPeriodForbidsUnverify() { Harness h=Harness.reversal(); h.expenseDetails.get(0).setPeriodId(null); assertThrows(ServiceException.class,()->h.unverify("null-snapshot")); assertEquals(0,h.reversalWrites()); }
    @Test void nullCurrentPeriodForbidsUnverify() { Harness h=Harness.reversal(); h.expenses.get(0).setPeriodId(null); assertThrows(ServiceException.class,()->h.unverify("null-current")); assertEquals(0,h.reversalWrites()); }
    @Test void currentPeriodMismatchForbidsUnverify() { Harness h=Harness.reversal(); h.expenses.get(0).setPeriodId(12L); assertThrows(ServiceException.class,()->h.unverify("period-drift")); assertEquals(0,h.reversalWrites()); }
    @Test void currentLockedPeriodForbidsUnverifyBeforeWrites() { Harness h=Harness.reversal(); h.expenses.get(0).setPeriodId(11L); h.expenseDetails.get(0).setPeriodId(11L); h.lockPeriod=true; assertThrows(ServiceException.class,()->h.unverify("current-lock")); assertEquals(0,h.reversalWrites()); }
    @Test void expenseAssociationDriftForbidsUnverify() { Harness h=Harness.reversal(); h.expenses.get(0).setAdvanceId(999L); assertThrows(ServiceException.class,()->h.unverify("association-drift")); assertEquals(0,h.reversalWrites()); }

    static final class Harness {
        final List<FinExpense> expenses; final List<FinAdvance> advances; List<FinExpenseVerifyDetail> expenseDetails=List.of(); List<FinAdvanceVerifyDetail> advanceDetails=List.of();
        int batchWrites,detailWrites,expenseUpdates,successfulExpenseUpdates,advanceUpdates,generatedAdvances, selectRequestCalls;
        int expenseRestores,advanceRestores,batchReverses,downstreamReferences; int expenseUpdateResult=1,batchReverseResult=1; Long existingBatch,scopedTenant,scopedDept,updateTenant,updateDept; boolean lockPeriod,duplicateOnInsert,omitGeneratedKey;
        FinExpenseVerifyBatch batch; FinAdvance generatedAdvance; String restoredExpenseStatus,restoredAdvanceStatus; Long restoredExpenseAdvanceId; final List<Long> invalidatedAdvances=new ArrayList<>();
        final FinExpenseVerificationServiceImpl service;
        Harness(List<FinExpense> e,List<FinAdvance> a) { expenses=new ArrayList<>(e); advances=new ArrayList<>(a);
            FinExpenseMapper em=fake(FinExpenseMapper.class,(n,x)-> { if(n.equals("selectFinExpenseByExpenseIdsScoped")){scopedTenant=(Long)x[1];scopedDept=(Long)x[2];return expenses;} if(n.equals("markExpenseVerified")){expenseUpdates++;updateTenant=(Long)x[4];updateDept=(Long)x[5];if(expenseUpdateResult>0)successfulExpenseUpdates++; return expenseUpdateResult;} return zero(n); });
            FinAdvanceMapper am=fake(FinAdvanceMapper.class,(n,x)-> { if(n.equals("selectFinAdvanceByAdvanceIdsScoped"))return advances; if(n.equals("markAdvanceVerified")){advanceUpdates++;return 1;} if(n.equals("insertFinAdvance")){generatedAdvances++;generatedAdvance=(FinAdvance)x[0];if(!omitGeneratedKey)generatedAdvance.setAdvanceId(900L);return 1;} return zero(n); });
            FinExpenseVerifyBatchMapper bm=fake(FinExpenseVerifyBatchMapper.class,(n,x)-> { if(n.equals("selectByRequestId")){selectRequestCalls++; Long id=existingBatch!=null?existingBatch:(duplicateOnInsert&&selectRequestCalls>1?99L:null);if(id==null)return null;FinExpenseVerifyBatch b=new FinExpenseVerifyBatch();b.setBatchId(id);return b;} if(n.equals("selectBatchForUpdate"))return batch; if(n.equals("selectCurrentExpensesForUpdate"))return expenses; if(n.equals("selectCurrentAdvancesForUpdate"))return advances; if(n.equals("insertBatch")){if(duplicateOnInsert)throw new DuplicateKeyException("race");batchWrites++;((FinExpenseVerifyBatch)x[0]).setBatchId(77L);return 1;} if(n.equals("insertExpenseDetails")){detailWrites++;expenseDetails=new ArrayList<>((List<FinExpenseVerifyDetail>)x[0]);return expenseDetails.size();} if(n.equals("insertAdvanceDetails")){detailWrites++;advanceDetails=new ArrayList<>((List<FinAdvanceVerifyDetail>)x[0]);return advanceDetails.size();} if(n.equals("selectExpenseDetails"))return expenseDetails; if(n.equals("selectAdvanceDetails"))return advanceDetails; if(n.equals("restoreExpenseSnapshot")){expenseRestores++;restoredExpenseStatus=(String)x[1];restoredExpenseAdvanceId=(Long)x[2];return 1;} if(n.equals("restoreAdvanceSnapshot")){advanceRestores++;restoredAdvanceStatus=(String)x[1];return 1;} if(n.equals("invalidateGeneratedAdvance")){invalidatedAdvances.add((Long)x[0]);return 1;} if(n.equals("countGeneratedAdvanceDownstreamReferences"))return downstreamReferences; if(n.equals("markBatchReversed")){batchReverses++;return batchReverseResult;} return zero(n); });
            IFinAccountingPeriodService ps=fake(IFinAccountingPeriodService.class,(n,x)-> { if(n.equals("assertPeriodEditable")&&lockPeriod)throw new ServiceException("核算周期已锁定"); return zero(n); });
            FinAccountingPeriodMapper pm=fake(FinAccountingPeriodMapper.class,(n,x)-> { if(n.equals("selectPeriodForUpdate")){FinAccountingPeriod p=new FinAccountingPeriod();p.setPeriodId((Long)x[0]);p.setDeptId((Long)x[2]);p.setStatus("0");return p;} return zero(n); });
            service=new FinExpenseVerificationServiceImpl(em,am,bm,ps,pm,null,()->10L);
        }
        Long verify(String request,List<Long> e,List<Long> a){ExpenseVerifyVO v=new ExpenseVerifyVO();v.setRequestId(request);v.setExpenseIds(e);v.setAdvanceIds(a);return service.verify(v,"finance");}
        int unverify(String request){ExpenseUnverifyVO v=new ExpenseUnverifyVO();v.setRequestId(request);v.setReason("录入错误");return service.unverify(77L,v,"finance");}
        int totalWrites(){return batchWrites+detailWrites+expenseUpdates+advanceUpdates+generatedAdvances;}
        int reversalWrites(){return expenseRestores+advanceRestores+invalidatedAdvances.size()+batchReverses;}
        void addGenerated(long id,String relation,long amount){FinAdvanceVerifyDetail d=advanceDetail(id,relation,"1",11L);advanceDetails.add(d);FinAdvance a=advance(id,10,"1",amount,11L);a.setVerifyBy(batch.getVerifyBy());a.setVerifyTime(batch.getVerifyTime());advances.add(a);}
        void seedReplay(List<Long> expenseIds,List<Long> advanceIds){expenseDetails=new ArrayList<>();for(Long id:expenseIds){FinExpenseVerifyDetail d=new FinExpenseVerifyDetail();d.setExpenseId(id);expenseDetails.add(d);}advanceDetails=new ArrayList<>();for(Long id:advanceIds)advanceDetails.add(advanceDetail(id,FinAdvanceVerifyDetail.RELATION_SOURCE,"0",11L));}
        static Harness reversal(){Date t=new Date(12345L);Harness h=new Harness(List.of(expense(1,10,"1",100,11L)),List.of(advance(2,10,"1",100,11L)));h.batch=new FinExpenseVerifyBatch();h.batch.setBatchId(77L);h.batch.setTenantId(1L);h.batch.setDeptId(10L);h.batch.setStatus(FinExpenseVerifyBatch.STATUS_VERIFIED);h.batch.setSourceType(FinExpenseVerifyBatch.SOURCE_NORMAL);h.batch.setVersion(0);h.batch.setVerifyBy("finance");h.batch.setVerifyTime(t);h.expenses.get(0).setAdvanceId(2L);h.expenses.get(0).setVerifyBy("finance");h.expenses.get(0).setVerifyTime(t);h.advances.get(0).setVerifyBy("finance");h.advances.get(0).setVerifyTime(t);FinExpenseVerifyDetail e=new FinExpenseVerifyDetail();e.setExpenseId(1L);e.setOriginalStatus("0");e.setOriginalAdvanceId(44L);e.setPeriodId(11L);h.expenseDetails=new ArrayList<>(List.of(e));h.advanceDetails=new ArrayList<>(List.of(advanceDetail(2,FinAdvanceVerifyDetail.RELATION_SOURCE,"0",11L)));return h;}
    }
    private static FinExpense expense(long id,long dept,String status,long amount,Long period){FinExpense e=new FinExpense();e.setExpenseId(id);e.setDeptId(dept);e.setStatus(status);e.setExpenseAmount(BigDecimal.valueOf(amount));e.setPeriodId(period == null ? 11L : period);e.setDelFlag("0");return e;}
    private static FinAdvance advance(long id,long dept,String status,long amount,Long period){FinAdvance a=new FinAdvance();a.setAdvanceId(id);a.setDeptId(dept);a.setStatus(status);a.setAdvanceAmount(BigDecimal.valueOf(amount));a.setPeriodId(period == null ? 11L : period);a.setDelFlag("0");return a;}
    private static FinAdvanceVerifyDetail advanceDetail(long id,String relation,String generated,Long period){FinAdvanceVerifyDetail d=new FinAdvanceVerifyDetail();d.setAdvanceId(id);d.setRelationType(relation);d.setGeneratedFlag(generated);d.setOriginalStatus("0");d.setPeriodId(period);return d;}
    interface Answer{Object get(String n,Object[] a);}
    @SuppressWarnings("unchecked") static <T>T fake(Class<T> c,Answer a){return(T)Proxy.newProxyInstance(c.getClassLoader(),new Class<?>[]{c},(p,m,x)->a.get(m.getName(),x));}
    static Object zero(String n){return n.startsWith("select")?null:n.startsWith("count")||n.startsWith("insert")||n.startsWith("update")||n.startsWith("delete")||n.startsWith("mark")||n.startsWith("restore")?0:null;}
}
