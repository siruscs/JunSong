package com.junsong.finance.domain.vo;

import java.util.List;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinAdvance;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinInvestRecord;
import com.junsong.finance.domain.FinInvestorPayment;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.domain.FinPurchase;
import com.junsong.finance.domain.FinSalePayment;
import com.junsong.finance.domain.FinSaleRecord;

public class AccountingPeriodDetailVO
{
    private FinAccountingPeriod period;
    private List<FinExpense> expenses;
    private List<FinAdvance> advances;
    private List<FinPurchase> purchases;
    private List<FinInvestRecord> investRecords;
    private List<FinSaleRecord> sales;
    private List<FinSalePayment> salePayments;
    private List<FinProfitShareRecord> profitShares;
    private List<FinInvestorPayment> investorPayments;

    public FinAccountingPeriod getPeriod() { return period; }
    public void setPeriod(FinAccountingPeriod period) { this.period = period; }
    public List<FinExpense> getExpenses() { return expenses; }
    public void setExpenses(List<FinExpense> expenses) { this.expenses = expenses; }
    public List<FinAdvance> getAdvances() { return advances; }
    public void setAdvances(List<FinAdvance> advances) { this.advances = advances; }
    public List<FinPurchase> getPurchases() { return purchases; }
    public void setPurchases(List<FinPurchase> purchases) { this.purchases = purchases; }
    public List<FinInvestRecord> getInvestRecords() { return investRecords; }
    public void setInvestRecords(List<FinInvestRecord> investRecords) { this.investRecords = investRecords; }
    public List<FinSaleRecord> getSales() { return sales; }
    public void setSales(List<FinSaleRecord> sales) { this.sales = sales; }
    public List<FinSalePayment> getSalePayments() { return salePayments; }
    public void setSalePayments(List<FinSalePayment> salePayments) { this.salePayments = salePayments; }
    public List<FinProfitShareRecord> getProfitShares() { return profitShares; }
    public void setProfitShares(List<FinProfitShareRecord> profitShares) { this.profitShares = profitShares; }
    public List<FinInvestorPayment> getInvestorPayments() { return investorPayments; }
    public void setInvestorPayments(List<FinInvestorPayment> investorPayments) { this.investorPayments = investorPayments; }
}
