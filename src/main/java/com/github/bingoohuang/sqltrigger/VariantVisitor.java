package com.github.bingoohuang.sqltrigger;

import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

public class VariantVisitor extends SQLASTVisitorAdapter {
    @Getter private AtomicInteger variantIndex = new AtomicInteger();

    @Override
    public boolean visit(SQLVariantRefExpr x) {
        variantIndex.incrementAndGet();
        return true;
    }

    public int getVarIndex() {
        return variantIndex.get();
    }
}
