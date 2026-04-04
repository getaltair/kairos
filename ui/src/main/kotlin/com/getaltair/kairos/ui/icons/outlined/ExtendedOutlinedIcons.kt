/*
 * Custom ImageVector definitions for Material Icons Extended (Outlined).
 * Replaces the ~60 MB material-icons-extended dependency with only the icons
 * actually used in this project. Path data sourced from the official
 * androidx.compose.material:material-icons-extended library (Apache 2.0).
 */

@file:Suppress("ktlint:standard:backing-property-naming")

package com.getaltair.kairos.ui.icons.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// AddTask (Outlined)
// ---------------------------------------------------------------------------

private var _addTask: ImageVector? = null

val Icons.Outlined.AddTask: ImageVector
    get() {
        if (_addTask != null) return _addTask!!
        _addTask = ImageVector.Builder(
            name = "Outlined.AddTask",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22.0f, 5.18f)
                lineTo(10.59f, 16.6f)
                lineToRelative(-4.24f, -4.24f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(2.83f, 2.83f)
                lineToRelative(10.0f, -10.0f)
                lineTo(22.0f, 5.18f)
                close()
                moveTo(12.0f, 20.0f)
                curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
                reflectiveCurveToRelative(3.59f, -8.0f, 8.0f, -8.0f)
                curveToRelative(1.57f, 0.0f, 3.04f, 0.46f, 4.28f, 1.25f)
                lineToRelative(1.45f, -1.45f)
                curveTo(16.1f, 2.67f, 14.13f, 2.0f, 12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                curveToRelative(1.73f, 0.0f, 3.36f, -0.44f, 4.78f, -1.22f)
                lineToRelative(-1.5f, -1.5f)
                curveTo(14.28f, 19.74f, 13.17f, 20.0f, 12.0f, 20.0f)
                close()
                moveTo(19.0f, 15.0f)
                horizontalLineToRelative(-3.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(3.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-3.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineTo(15.0f)
                close()
            }
        }.build()
        return _addTask!!
    }

// ---------------------------------------------------------------------------
// Circle (Outlined)
// ---------------------------------------------------------------------------

private var _circle: ImageVector? = null

val Icons.Outlined.Circle: ImageVector
    get() {
        if (_circle != null) return _circle!!
        _circle = ImageVector.Builder(
            name = "Outlined.Circle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 2.0f)
                curveTo(6.47f, 2.0f, 2.0f, 6.47f, 2.0f, 12.0f)
                curveToRelative(0.0f, 5.53f, 4.47f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.47f, 10.0f, -10.0f)
                curveTo(22.0f, 6.47f, 17.53f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(12.0f, 20.0f)
                curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
                curveToRelative(0.0f, -4.42f, 3.58f, -8.0f, 8.0f, -8.0f)
                reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
                curveTo(20.0f, 16.42f, 16.42f, 20.0f, 12.0f, 20.0f)
                close()
            }
        }.build()
        return _circle!!
    }
