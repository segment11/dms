package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true, includePackage = false)
class BackupPolicy implements JSONFiled {
    Integer backupTemplateId

    Boolean isAutomaticBackup

    // in days or hours
    Integer retentionPeriod

    Boolean isBackupWindowSpecify

    // format: HH:mm
    String startTime

    Integer durationHours

    String dailyOrHourly

    String endTime() {
        def arr = startTime.split(':')
        def hour = arr[0].toInteger()
        (hour + durationHours).toString() + ':' + arr[1]
    }
}