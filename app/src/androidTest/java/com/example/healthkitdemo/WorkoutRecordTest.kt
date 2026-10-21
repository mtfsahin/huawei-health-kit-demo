package com.example.healthkitdemo

import com.example.healthkitdemo.healthkit.WorkoutRecord
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutRecordTest {

    @Test
    fun readsARecordAndItsSummary() {
        val json = JSONObject(
            """
            {
              "id": "r1",
              "name": "Run",
              "startTime": 1000,
              "endTime": 1801000,
              "activityType": 102,
              "activeTime": 1800000,
              "timeZone": "+0300",
              "activitySummary": {
                "dataSummary": [
                  { "dataTypeName": "com.huawei.distance.delta", "value": [ { "floatValue": 5200.0 } ] }
                ]
              }
            }
            """
        )

        val record = WorkoutRecord.fromJson(json)

        assertEquals("r1", record.id)
        assertEquals(30, record.durationMinutes)
        assertEquals("5200", record.summary["com.huawei.distance.delta"])
        assertEquals("Distance delta", record.summaryLabel("com.huawei.distance.delta"))
    }
}
