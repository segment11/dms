package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true, includePackage = false)
class BackupPolicy implements JSONFiled {
    Integer backupTemplateId

    Boolean isAutomaticBackup

    Boolean isBackupWindowSpecify

    Integer retentionPeriod

    String startTime

    Integer durationHours

    String dailyOrHourly

    Boolean isMaintenanceWindowSpecify

    String maintenanceStartDay

    String maintenanceStartTime

    Integer maintenanceDurationHours
}