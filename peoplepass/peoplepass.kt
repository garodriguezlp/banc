///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS info.picocli:picocli:4.6.3
//DEPS org.apache.commons:commons-csv:1.9.0

import org.apache.commons.csv.CSVFormat.TDF
import org.apache.commons.csv.CSVPrinter
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols.getInstance
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ofPattern
import java.util.*
import java.util.concurrent.Callable
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = exitProcess(CommandLine(PeoplePass()).execute(*args))


@Command(
    name = "peoplepass", version = ["peoplepass 0.1"],
    mixinStandardHelpOptions = true,
    description = ["A peoplepass TXT mapper made with jbang"]
)
class PeoplePass : Callable<Int> {

    companion object {
        private const val DATE_REGEX = "\\d{2}\\/\\d{2}\\/\\d{4}"
        private const val DESCRIPTION_REGEX = "[\\w\\s]+"
        private const val AMOUNT_REGEX = "\\$.*"
        private const val TWO_LINES_WE_DO_NOT_CARE_REGEX = "(.*[\\n\\r]{1,2}){2}"
        const val RECORD_REGEX = "($DATE_REGEX)$TWO_LINES_WE_DO_NOT_CARE_REGEX($DESCRIPTION_REGEX)($AMOUNT_REGEX)"
        const val DEBIT_RECORD_TYPE = "COMPRA"
    }

    private val originDateFormat = ofPattern("dd/MM/yyyy")
    private val destinationDateFormat = ofPattern("MM/dd/yyyy")
    private val decimalFormatSymbols = getInstance(Locale("es", "CO"))
    private val amountFormat: DecimalFormat = DecimalFormat("$###,###.00", decimalFormatSymbols);

    @Parameters(index = "0", description = ["data file"], arity = "1")
    private var data: File? = null

    override fun call(): Int {
        val recordsRawData = data?.readText() ?: return 1
        val records = recordsRawData
            .split(Regex("\\d{5,}"))
            .mapNotNull(::processRecord)
        printRecords(records)
        return 0
    }

    private fun processRecord(record: String): Record? {
        val matcher = Regex(RECORD_REGEX).find(record)
        matcher?.let {
            val date = LocalDate.parse(it.groupValues[1], originDateFormat)
            val description = it.groupValues[3].trim()
            val recordType = it.groupValues[2].trim()
            val absAmount = amountFormat.parse(it.groupValues[4]).toDouble()
            val amount = if (recordType == DEBIT_RECORD_TYPE) -absAmount else absAmount
            return Record(date, description, amount)
        }
        return null
    }

    private fun printRecords(records: List<Record>) {
        CSVPrinter(System.out, TDF).use {
            records.forEach { record ->
                it.printRecord( destinationDateFormat.format(record.date), record.description, record.amount)
            }
        }
    }
}

data class Record(val date: LocalDate, val description: String, val amount: Number)
