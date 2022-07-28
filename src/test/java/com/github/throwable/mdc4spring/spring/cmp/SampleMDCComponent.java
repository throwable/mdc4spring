package com.github.throwable.mdc4spring.spring.cmp;

import com.github.throwable.mdc4spring.anno.MDCParam;
import com.github.throwable.mdc4spring.anno.MDCOutParam;
import com.github.throwable.mdc4spring.anno.WithMDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.github.throwable.mdc4spring.MDC.current;

@Service
@SuppressWarnings({"unused", "SameParameterValue"})
public class SampleMDCComponent {
    private final static Logger log = LoggerFactory.getLogger(SampleMDCComponent.class);

    @Autowired
    NestedMDCComponent nestedMDCComponent;
    @Autowired InnerMDCComponent innerMDCComponent;

    private final String sampleFieldValue = "Sample local field value";

    private String getSampleAccessorValue() {
        return "Sample accessor value";
    }

    public String sampleMethodValue(String argument) {
        return argument.toUpperCase();
    }

    @WithMDC
    public void execWithSimpleMDC() {
        current().put("sampleKey", "Some Value");
        log.info("Simple MDC Scope trace");
    }

    @WithMDC(name = "component1")
    public void execWithNamedMDC() {
        current().put("sampleKey", "Some Value");
        log.info("Prefixed MDC keys");
    }

    @WithMDC(name = "component1")
    public void execWithNestedMDCs() {
        current().put("sampleKey", "Some Value");
        log.info("Before nested MDC");
        nestedMDCComponent.execWithNewMDC();
        log.info("After nested MDC");
    }

    @WithMDC(name = "component1")
    public void execWithNestedSameNameMDC() {
        current().put("sampleKey", "Some Value");
        log.info("Before nested MDC");
        nestedMDCComponent.execWithSameNameMDC();
        log.info("After nested MDC");
    }

    @WithMDC
    public void execInvocationInCurrentMDC() {
        nestedMDCComponent.execWithCurrentMDC("value1");
        log.info("After invocation");
    }

    @MDCParam(name = "keyParam1", eval = "'Sample string'")
    public void execParamMethodWithoutMDC() {
        log.info("MDC must be created implicitly");
    }


    @WithMDC
    @MDCParam(name = "keyParam1", eval = "'Sample string'")
    public void execWithMethodOnlyMDCParameter() {
        log.info("Only one parameter");
    }

    @WithMDC
    @MDCParam(name = "keyParam1", eval = "'Sample string'")
    @MDCParam(name = "keyParam2", eval = "'Number ' + 5")
    public void execWithFixedMDCParameters() {
        log.info("Fixed parameters");
    }

    @WithMDC
    @MDCParam(name = "localFieldParam", eval = "sampleFieldValue")
    @MDCParam(name = "localAccessorParam", eval = "sampleAccessorValue")
    @MDCParam(name = "localMethodParam", eval = "'Transformed: ' + sampleMethodValue(sampleFieldValue)")
    @MDCParam(name = "environmentProperty", eval = "#environment['sample.property']")
    @MDCParam(name = "systemProperty", eval = "#systemProperties['user.home']")
    @MDCParam(name = "externalParameterBeanValue", eval = "@externalParameterBean.externalBeanValue")
    @MDCParam(name = "method", eval = "#className + '/' + #methodName")
    public void execWithMDCParametersReferencingContext() {
        log.info("Parameters referencing local bean and environment");
    }

    @WithMDC
    public void execWithMethodArgumentAsMDCParameter(@MDCParam String param1, String param2)
    {
        log.info("Argument as MDC parameter");
    }

    @WithMDC
    @MDCParam(name = "concatAllArgumentsParam", eval = "#param1 + #param2 + #param3 + #clazz + #notIncluded")
    public void execWithMethodArgumentsAsMDCParameters(@MDCParam String param1,
                                                       @MDCParam("param2") int myArg,
                                                       @MDCParam BigDecimal param3,
                                                       @MDCParam(eval = "name") Class<?> clazz,
                                                       String notIncluded)
    {
        log.info("Arguments as MDC parameters");
    }

    @WithMDC
    public void execLocalNonPublicMethods() {
        samplePackagePrivateMethod("package-private");
        sampleProtectedMethod("protected");
        samplePrivateMethod("private");
    }


    @WithMDC
    void samplePackagePrivateMethod(@MDCParam String scope) {
        log.info("Package-private method");
    }

    @WithMDC
    protected void sampleProtectedMethod(@MDCParam String scope) {
        log.info("Protected method");
    }

    @WithMDC
    private void samplePrivateMethod(@MDCParam String scope) {
        log.info("Private method");
    }

    @WithMDC
    public void execRemoteNonPublicMethods() {
        innerMDCComponent.samplePackagePrivateMethod("package-private");
        innerMDCComponent.sampleProtectedMethod("protected");
        innerMDCComponent.samplePrivateMethod("private");
    }


    @Service
    public static class InnerMDCComponent {
        @WithMDC
        void samplePackagePrivateMethod(@MDCParam String scope) {
            log.info("Package-private method");
        }

        @WithMDC
        protected void sampleProtectedMethod(@MDCParam String scope) {
            log.info("Protected method");
        }

        @WithMDC
        private void samplePrivateMethod(@MDCParam String scope) {
            log.info("Private method");
        }
    }

    @WithMDC
    public void returnOutputParameters() {
        nestedMDCComponent.returnOutputParameterWithoutNestedMDC();
        log.info("message1 param must be present here");
        nestedMDCComponent.returnOutputParameterWithNestedMDC();
        log.info("message2 param must also be present here");
    }

    @MDCOutParam(name = "test", eval = "'rest'")
    public void returnOutputParameterWithoutScope() {
    }
}
