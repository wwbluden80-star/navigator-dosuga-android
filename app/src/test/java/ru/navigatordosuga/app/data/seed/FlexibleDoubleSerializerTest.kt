package ru.navigatordosuga.app.data.seed

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class FlexibleDoubleSerializerTest {
    private val json=Json { ignoreUnknownKeys=true }

    @Test fun acceptsNumericAndEvidenceGradeScores(){
        val dataset=json.decodeFromString<SeedGeoDataset>(
            """{"items":[
                {"id":"numeric","name":"Numeric","score":72.5,"secondaryScore":"61.25","payload":{}},
                {"id":"grade","name":"Grade","score":0,"secondaryScore":"B-/C+","payload":{}}
            ]}"""
        )

        assertEquals(72.5,dataset.items[0].score,0.0)
        assertEquals(61.25,dataset.items[0].secondaryScore,0.0)
        assertEquals(0.0,dataset.items[1].secondaryScore,0.0)
    }
}
