package metric

import groovy.transform.CompileStatic
import io.prometheus.client.Collector
import org.jetbrains.annotations.TestOnly

@CompileStatic
class SimpleGauge extends Collector {
    private final String familyName

    private final String help

    private final List<String> labels

    SimpleGauge(String familyName, String help, List<String> labels) {
        this.familyName = familyName
        this.help = help
        this.labels = labels
    }

    @CompileStatic
    record ValueWithLabelValues(Double value, List<String> labelValues) {
    }

    @CompileStatic
    interface RawGetter {
        Map<String, ValueWithLabelValues> get()
    }

    private final ArrayList<RawGetter> rawGetterList = []

    void addRawGetter(RawGetter rawGetter) {
        rawGetterList.add(rawGetter)
    }

    @TestOnly
    void clearRawGetterList() {
        rawGetterList.clear()
    }

    @Override
    List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> list = []

        List<MetricFamilySamples.Sample> samples = []
        def dsMfs = new MetricFamilySamples(familyName, Type.GAUGE, help, samples)
        list.add(dsMfs)

        for (rawGetter in rawGetterList) {
            def raw = rawGetter.get()
            if (raw == null) {
                continue
            }

            for (entry in raw.entrySet()) {
                samples << new MetricFamilySamples.Sample(entry.getKey(), labels, entry.value.labelValues, entry.value.value)
            }
        }

        list
    }
}