package spam.blocker.ui.setting.regex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import spam.blocker.R
import spam.blocker.db.NumberRuleTable
import spam.blocker.db.RegexRule
import spam.blocker.ui.theme.Salmon
import spam.blocker.ui.theme.SkyBlue
import spam.blocker.ui.widgets.DropdownWrapper
import spam.blocker.ui.widgets.HtmlText
import spam.blocker.ui.widgets.LabelItem
import spam.blocker.ui.widgets.LongPressButton
import spam.blocker.ui.widgets.PopupDialog
import spam.blocker.ui.widgets.ResIcon
import spam.blocker.ui.widgets.Str
import spam.blocker.ui.widgets.rememberFileReadChooser
import spam.blocker.util.Csv
import spam.blocker.util.Lambda

@Composable
fun ImportRuleButton(
    vm: RuleViewModel,
    onClick: Lambda,
) {
    val ctx = LocalContext.current

    val fileReader = rememberFileReadChooser()
    fileReader.Compose()

    val warningTrigger = rememberSaveable { mutableStateOf(false) }
    if (warningTrigger.value) {
        PopupDialog(
            trigger = warningTrigger,
            content = {
                HtmlText(html = ctx.getString(R.string.failed_to_import_from_csv))
            },
            icon = { ResIcon(R.drawable.ic_fail_red, color = Salmon) },
        )
    }


    val importRuleItems = remember {
        ctx.resources.getStringArray(R.array.import_csv_type).mapIndexed { menuItemIndex, label ->

            LabelItem(
                label = label,

                onClick = {
                    fileReader.popup { fn: String?, raw: ByteArray? ->
                        if (raw == null)
                            return@popup

                        val csv = Csv.parse(raw)
                        // show error if there is no column `pattern`, because it will generate empty rows.
                        if (!csv.headers.contains("pattern")) {
                            warningTrigger.value = true
                            return@popup
                        }

                        val rules = csv.rows.map {
                            RegexRule.fromMap(it)
                        }

                        when (menuItemIndex) {
                            0 -> { // import as single rule
                                val joined = rules.map {
                                    spam.blocker.util.Util.clearNumber(it.pattern)
                                }.filter {
                                    it.isNotEmpty()
                                }.joinToString ( separator = "|" )

                                val rule = RegexRule().apply {
                                    pattern = "($joined)"
                                    description = fn ?: ""
                                }
                                // 1. add to db
                                val table = NumberRuleTable()
                                table.addNewRule(ctx, rule)

                                // 2. refresh gui
                                vm.reloadDb(ctx)
                            }
                            1 -> { // import as multi rules
                                // 1. add to db
                                val table = NumberRuleTable()
                                rules.forEach {
                                    table.addNewRule(ctx, it)
                                }

                                // 2. refresh gui
                                vm.reloadDb(ctx)
                            }
                        }
                    }
                }
            )
        }
    }
    DropdownWrapper(items = importRuleItems) { expanded ->
        LongPressButton(
            label = Str(R.string.new_),
            color = SkyBlue,
            onClick = onClick,
            onLongClick = {
                expanded.value = true
            },
        )
    }
}
