package io.github.mov2day.unifiedtest.framework.spock;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.model.SpecInfo;

public class SpockGlobalExtension implements IGlobalExtension {

    private static UnifiedTestResultCollector collector;

    public static void setCollector(UnifiedTestResultCollector collector) {
        SpockGlobalExtension.collector = collector;
    }

    public static UnifiedTestResultCollector getCollector() {
        return collector;
    }

    @Override
    public void visitSpec(SpecInfo spec) {
        spec.getAllFeatures().forEach(feature -> {
            feature.getFeatureMethod().addInterceptor(new SpockMethodInterceptor());
        });
    }
}
