/*
Copyright 2023 Christian Felde

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

import sigbla.app.*
import sigbla.charts.*
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun parseTempLog(file: File): List<Pair<ZonedDateTime, Double>> {
    val values = file
        .readLines()
        .filter { it.isNotBlank() }
        .withIndex().partition { it.index % 2 == 0 }

    return (0 until values.first.size.coerceAtMost(values.second.size)).map {
        values.first[it].value to values.second[it].value
    }.mapNotNull {
        val temp = it.second.toDoubleOrNull() ?: return@mapNotNull null
        val time = ZonedDateTime.parse(it.first, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(ZoneId.of("Etc/UTC"))
        time to temp
    }.toList()
}

fun insertTempLog(timestampColumn: Column, tempColumn: Column, data: List<Pair<ZonedDateTime, Double>>) {
    data.forEach {
        val time = it.first
        val value = it.second

        // Use row index as key based on time
        val index = time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")).toLong()

        timestampColumn[index] = time.toLocalDateTime().toString()
        tempColumn[index] = value
    }
}

fun main(args: Array<String>) {
    TableView[Port] = 8080

    println("Loading ${args[0]}")
    val internalTemps = parseTempLog(File(args[0]))

    println("Loading ${args[1]}")
    val externalTemps = parseTempLog(File(args[1]))

    val dataTable = Table["temps"]

    insertTempLog(dataTable["Timestamp"], dataTable["Internal"], internalTemps)
    insertTempLog(dataTable["Timestamp"], dataTable["External"], externalTemps)

    // Remove rows with missing values
    dataTable["Timestamp"].forEach {
        if (dataTable["Internal"][it.index].asDouble == null || dataTable["External"][it.index].asDouble == null) {
            remove(dataTable[it.index])
        }
    }

    // Remove empty rows
    compact(dataTable)

    println("Have ${dataTable["Timestamp"].count()} rows")

    val dataTableUrl = show(dataTable)
    println(dataTableUrl)

    val chartTable = Table["chart"]
    val chartTableView = TableView[chartTable]

    val labels = dataTable["Timestamp"].map { it.toString() }.toList()
    val dataset1 = "RPi temperature" to dataTable["Timestamp"].map { dataTable["Internal", it.index].asDouble ?: 0.0 }
    val dataset2 = "Room temperature" to dataTable["Timestamp"].map { dataTable["External", it.index].asDouble ?: 0.0 }

    chartTableView["Temps", 0] = line(
        labels,
        dataset1,
        dataset2
    )

    chartTableView["Temps"][CellWidth] = 900
    chartTableView[0][CellHeight] = 600

    val chartTableViewUrl = show(chartTableView)
    println(chartTableViewUrl)
}
