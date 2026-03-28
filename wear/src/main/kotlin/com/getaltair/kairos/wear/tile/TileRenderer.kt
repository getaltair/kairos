package com.getaltair.kairos.wear.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_BOLD
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import com.getaltair.kairos.domain.wear.WearHabitData

object TileRenderer {
    // Colors matching Kairos design system (adapted for OLED watch displays)
    private const val COLOR_BACKGROUND = 0xFF000000.toInt() // Pure black for OLED power savings
    private const val COLOR_PRIMARY = 0xFF6750A4.toInt() // Material3 primary purple
    private const val COLOR_ON_SURFACE = 0xFFE6E1E5.toInt() // Light text on dark surfaces
    private const val COLOR_SURFACE = 0xFF1C1B1F.toInt() // Elevated surface background
    private const val COLOR_MUTED = 0xFF938F99.toInt() // Secondary/hint text
    private const val COLOR_SUCCESS = 0xFF4CAF50.toInt() // Completion checkmark green
    private const val COLOR_ERROR = 0xFFCF6679.toInt() // Error/warning text

    fun renderTile(state: TileState): LayoutElement = Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setModifiers(
            Modifiers.Builder()
                .setBackground(
                    Background.Builder().setColor(argb(COLOR_BACKGROUND)).build(),
                )
                .build(),
        )
        .addContent(
            when (state) {
                is TileState.Loading -> renderLoading()
                is TileState.Empty -> renderEmpty()
                is TileState.AllDone -> renderAllDone()
                is TileState.Error -> buildErrorTile(state.message)
                is TileState.HasHabits -> renderHabits(state)
            },
        )
        .build()

    private fun renderLoading(): LayoutElement = Column.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(
            Text.Builder()
                .setText("Loading...")
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_MUTED))
                        .setSize(sp(14f))
                        .build(),
                )
                .build(),
        )
        .build()

    private fun renderEmpty(): LayoutElement = Column.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(
            Text.Builder()
                .setText("No habits today")
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_ON_SURFACE))
                        .setSize(sp(16f))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build(),
                )
                .build(),
        )
        .addContent(Spacer.Builder().setHeight(dp(8f)).build())
        .addContent(
            Text.Builder()
                .setText("Tap to open app")
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_MUTED))
                        .setSize(sp(12f))
                        .build(),
                )
                .build(),
        )
        .build()

    private fun renderAllDone(): LayoutElement = Column.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(
            Text.Builder()
                .setText("\u2713")
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_SUCCESS))
                        .setSize(sp(32f))
                        .build(),
                )
                .build(),
        )
        .addContent(Spacer.Builder().setHeight(dp(8f)).build())
        .addContent(
            Text.Builder()
                .setText("All done!")
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_ON_SURFACE))
                        .setSize(sp(16f))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build(),
                )
                .build(),
        )
        .build()

    private fun buildErrorTile(message: String): LayoutElement = Column.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(
            Text.Builder()
                .setText(message)
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_ERROR))
                        .setSize(sp(14f))
                        .build(),
                )
                .build(),
        )
        .addContent(Spacer.Builder().setHeight(dp(8f)).build())
        .addContent(
            Text.Builder()
                .setText("Tap to retry")
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_MUTED))
                        .setSize(sp(12f))
                        .build(),
                )
                .build(),
        )
        .build()

    private fun renderHabits(state: TileState.HasHabits): LayoutElement {
        val col = Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)

        // Header
        col.addContent(
            Text.Builder()
                .setText("Today \u2022 ${state.completedCount}/${state.totalCount}")
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_MUTED))
                        .setSize(sp(12f))
                        .build(),
                )
                .build(),
        )
        col.addContent(Spacer.Builder().setHeight(dp(6f)).build())

        val toShow = state.pendingHabits.take(5) // Tile has limited vertical space, show at most 5 pending habits
        toShow.forEach { habit ->
            col.addContent(renderHabitRow(habit))
            col.addContent(Spacer.Builder().setHeight(dp(4f)).build())
        }

        // Footer hint
        col.addContent(Spacer.Builder().setHeight(dp(4f)).build())
        col.addContent(
            Text.Builder()
                .setText("Tap to complete")
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_MUTED))
                        .setSize(sp(10f))
                        .build(),
                )
                .build(),
        )

        return col.build()
    }

    private fun renderHabitRow(habit: WearHabitData): LayoutElement = Row.Builder()
        .setWidth(expand())
        .setHeight(wrap())
        .setModifiers(
            Modifiers.Builder()
                .setClickable(
                    Clickable.Builder()
                        .setId("complete_${habit.id}")
                        .setOnClick(ActionBuilders.LoadAction.Builder().build())
                        .build(),
                )
                .setPadding(Padding.Builder().setAll(dp(4f)).build())
                .setBackground(
                    Background.Builder()
                        .setColor(argb(COLOR_SURFACE))
                        .setCorner(Corner.Builder().setRadius(dp(8f)).build())
                        .build(),
                )
                .build(),
        )
        .addContent(
            Text.Builder()
                .setText("\u25CB ")
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_PRIMARY))
                        .setSize(sp(12f))
                        .build(),
                )
                .build(),
        )
        .addContent(
            Text.Builder()
                .setText(habit.name.take(20)) // Truncate to prevent text overflow on small round displays
                .setFontStyle(
                    FontStyle.Builder()
                        .setColor(argb(COLOR_ON_SURFACE))
                        .setSize(sp(12f))
                        .build(),
                )
                .build(),
        )
        .build()
}
