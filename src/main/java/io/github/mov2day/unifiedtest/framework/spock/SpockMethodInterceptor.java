package io.github.mov2day.unifiedtest.framework.spock;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import org.spockframework.runtime.extension.IMethodInvocation;
import org.spockframework.runtime.extension.IMethodInterceptor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SpockMethodInterceptor implements IMethodInterceptor {

    @Override
    public void intercept(IMethodInvocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        Throwable throwable = null;

        try {
            invocation.proceed();
        } catch (Throwable t) {
            throwable = t;
        } finally {
            long endTime = System.currentTimeMillis();
            String failureMessage = null;
            String stackTrace = null;
            if (throwable != null) {
                failureMessage = throwable.getMessage();
                StringWriter sw = new StringWriter();
                throwable.printStackTrace(new PrintWriter(sw));
                stackTrace = sw.toString();
            }

            UnifiedTestResult result = new UnifiedTestResult(
                invocation.getFeature().getParent().getName(),
                invocation.getFeature().getDisplayName(),
                throwable == null ? "PASSED" : "FAILED",
                failureMessage,
                stackTrace,
                endTime - startTime
            );
            UnifiedTestResultCollector collector = SpockGlobalExtension.getCollector();
            if (collector != null) {
                collector.addResult(result);
            }
        }
        if (throwable != null) {
            throw throwable;
        }
    }
}
