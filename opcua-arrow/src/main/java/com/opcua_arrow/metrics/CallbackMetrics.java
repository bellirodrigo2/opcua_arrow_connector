package com.opcua_arrow.metrics;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.opcua_arrow.ICallBack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

public class CallbackMetrics implements ICallBack {

        private static final Logger log = LoggerFactory.getLogger(CallbackMetrics.class);

        private final PrometheusMeterRegistry registry;
        private final Tracer tracer;
        private final PushGatewayService pushGatewayService;

        private final Counter.Builder failureCounter;
        private final Counter.Builder successCounter;

        private final DistributionSummary batchSummary;

        private final Timer.Builder timerBuilder = Timer.builder("callback.duration")
                        .publishPercentileHistogram()
                        .distributionStatisticExpiry(Duration.ofMinutes(5));

        public CallbackMetrics(PrometheusMeterRegistry registry, Tracer tracer, PushGatewayService pushService) {
                this.registry = registry;
                this.tracer = tracer;
                this.pushGatewayService = pushService;

                batchSummary = DistributionSummary.builder("callback.batch.size")
                                .publishPercentileHistogram()
                                .register(registry);

                failureCounter = Counter.builder("callback.failure");
                successCounter = Counter.builder("callback.success");
        }

        @Override
        public ICallBackObject startCallback(String label, Collection<?> batch) {
                int batchSize = batch != null ? batch.size() : 0;

                batchSummary.record(batchSize);

                Timer timer = timerBuilder
                                .tag("label", label)
                                .tag("batchSize", String.valueOf(batchSize))
                                .register(registry);

                long start = System.nanoTime();

                Span span = tracer.spanBuilder(label)
                                .setAttribute("batchSize", batchSize)
                                .setAttribute("label", label)
                                .startSpan();

                log.info("event=start label={} batchSize={}", label, batchSize);

                return new Callback(label, batchSize, timer, start, span);
        }

        private class Callback implements ICallBackObject {

                private final String label;
                private final int batchSize;
                private final Timer timer;
                private final long start;
                private final Span span;
                private boolean failed;

                Callback(String label, int batchSize, Timer timer, long start, Span span) {
                        this.label = label;
                        this.batchSize = batchSize;
                        this.timer = timer;
                        this.start = start;
                        this.span = span;
                }

                @Override
                public void markFailure(Throwable t) {
                        failed = true;

                        failureCounter.tag("label", label)
                                        .tag("batchSize", String.valueOf(batchSize))
                                        .register(registry)
                                        .increment();

                        span.recordException(t);
                        span.setStatus(StatusCode.ERROR);

                        log.error("event=failure label={} batch={} error={}",
                                        label, batchSize, t.toString());
                }

                @Override
                public void addKeyValue(String key, Object value) {
                        span.setAttribute(key, String.valueOf(value));
                }

                @Override
                public void close() {
                        long time = System.nanoTime() - start;
                        timer.record(time, TimeUnit.NANOSECONDS);

                        if (!failed) {
                                successCounter
                                                .tag("label", label)
                                                .tag("batchSize", String.valueOf(batchSize))
                                                .register(registry)
                                                .increment();
                                span.setStatus(StatusCode.OK);
                        }

                        span.end();

                        pushGatewayService.push("opcua-callbacks");

                        log.info("event=end label={} batch={} durationMs={} failed={}",
                                        label, batchSize, time / 1_000_000.0, failed);
                }
        }
}
