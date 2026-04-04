/*
 * Custom ImageVector definitions for Material Icons Extended (Filled).
 * Replaces the ~60 MB material-icons-extended dependency with only the icons
 * actually used in this project. Path data sourced from the official
 * androidx.compose.material:material-icons-extended library (Apache 2.0).
 */

@file:Suppress("ktlint:standard:backing-property-naming")

package com.getaltair.kairos.ui.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Adjust
// ---------------------------------------------------------------------------

private var _adjust: ImageVector? = null

val Icons.Filled.Adjust: ImageVector
    get() {
        if (_adjust != null) return _adjust!!
        _adjust = ImageVector.Builder(
            name = "Filled.Adjust",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 2.0f)
                curveTo(6.49f, 2.0f, 2.0f, 6.49f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.49f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.49f, 10.0f, -10.0f)
                reflectiveCurveTo(17.51f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(12.0f, 20.0f)
                curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
                reflectiveCurveToRelative(3.59f, -8.0f, 8.0f, -8.0f)
                reflectiveCurveToRelative(8.0f, 3.59f, 8.0f, 8.0f)
                reflectiveCurveToRelative(-3.59f, 8.0f, -8.0f, 8.0f)
                close()
                moveTo(15.0f, 12.0f)
                curveToRelative(0.0f, 1.66f, -1.34f, 3.0f, -3.0f, 3.0f)
                reflectiveCurveToRelative(-3.0f, -1.34f, -3.0f, -3.0f)
                reflectiveCurveToRelative(1.34f, -3.0f, 3.0f, -3.0f)
                reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
                close()
            }
        }.build()
        return _adjust!!
    }

// ---------------------------------------------------------------------------
// Archive
// ---------------------------------------------------------------------------

private var _archive: ImageVector? = null

val Icons.Filled.Archive: ImageVector
    get() {
        if (_archive != null) return _archive!!
        _archive = ImageVector.Builder(
            name = "Filled.Archive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.54f, 5.23f)
                lineToRelative(-1.39f, -1.68f)
                curveTo(18.88f, 3.21f, 18.47f, 3.0f, 18.0f, 3.0f)
                horizontalLineTo(6.0f)
                curveToRelative(-0.47f, 0.0f, -0.88f, 0.21f, -1.16f, 0.55f)
                lineTo(3.46f, 5.23f)
                curveTo(3.17f, 5.57f, 3.0f, 6.02f, 3.0f, 6.5f)
                verticalLineTo(19.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(6.5f)
                curveToRelative(0.0f, -0.48f, -0.17f, -0.93f, -0.46f, -1.27f)
                close()
                moveTo(12.0f, 17.5f)
                lineTo(6.5f, 12.0f)
                horizontalLineTo(10.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(3.5f)
                lineTo(12.0f, 17.5f)
                close()
                moveTo(5.12f, 5.0f)
                lineToRelative(0.81f, -1.0f)
                horizontalLineToRelative(12.0f)
                lineToRelative(0.94f, 1.0f)
                horizontalLineTo(5.12f)
                close()
            }
        }.build()
        return _archive!!
    }

// ---------------------------------------------------------------------------
// AutoAwesome
// ---------------------------------------------------------------------------

private var _autoAwesome: ImageVector? = null

val Icons.Filled.AutoAwesome: ImageVector
    get() {
        if (_autoAwesome != null) return _autoAwesome!!
        _autoAwesome = ImageVector.Builder(
            name = "Filled.AutoAwesome",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.0f, 9.0f)
                lineToRelative(1.25f, -2.75f)
                lineTo(23.0f, 5.0f)
                lineToRelative(-2.75f, -1.25f)
                lineTo(19.0f, 1.0f)
                lineToRelative(-1.25f, 2.75f)
                lineTo(15.0f, 5.0f)
                lineToRelative(2.75f, 1.25f)
                lineTo(19.0f, 9.0f)
                close()
                moveTo(11.5f, 9.5f)
                lineTo(9.0f, 4.0f)
                lineTo(6.5f, 9.5f)
                lineTo(1.0f, 12.0f)
                lineToRelative(5.5f, 2.5f)
                lineTo(9.0f, 20.0f)
                lineToRelative(2.5f, -5.5f)
                lineTo(17.0f, 12.0f)
                lineToRelative(-5.5f, -2.5f)
                close()
                moveTo(19.0f, 15.0f)
                lineToRelative(-1.25f, 2.75f)
                lineTo(15.0f, 19.0f)
                lineToRelative(2.75f, 1.25f)
                lineTo(19.0f, 23.0f)
                lineToRelative(1.25f, -2.75f)
                lineTo(23.0f, 19.0f)
                lineToRelative(-2.75f, -1.25f)
                lineTo(19.0f, 15.0f)
                close()
            }
        }.build()
        return _autoAwesome!!
    }

// ---------------------------------------------------------------------------
// Bedtime
// ---------------------------------------------------------------------------

private var _bedtime: ImageVector? = null

val Icons.Filled.Bedtime: ImageVector
    get() {
        if (_bedtime != null) return _bedtime!!
        _bedtime = ImageVector.Builder(
            name = "Filled.Bedtime",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.34f, 2.02f)
                curveTo(6.59f, 1.82f, 2.0f, 6.42f, 2.0f, 12.0f)
                curveToRelative(0.0f, 5.52f, 4.48f, 10.0f, 10.0f, 10.0f)
                curveToRelative(3.71f, 0.0f, 6.93f, -2.02f, 8.66f, -5.02f)
                curveTo(13.15f, 16.73f, 8.57f, 8.55f, 12.34f, 2.02f)
                close()
            }
        }.build()
        return _bedtime!!
    }

// ---------------------------------------------------------------------------
// Book
// ---------------------------------------------------------------------------

private var _book: ImageVector? = null

val Icons.Filled.Book: ImageVector
    get() {
        if (_book != null) return _book!!
        _book = ImageVector.Builder(
            name = "Filled.Book",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.0f, 2.0f)
                horizontalLineTo(6.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(16.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(12.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(4.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(6.0f, 4.0f)
                horizontalLineToRelative(5.0f)
                verticalLineToRelative(8.0f)
                lineToRelative(-2.5f, -1.5f)
                lineTo(6.0f, 12.0f)
                verticalLineTo(4.0f)
                close()
            }
        }.build()
        return _book!!
    }

// ---------------------------------------------------------------------------
// CheckBox
// ---------------------------------------------------------------------------

private var _checkBox: ImageVector? = null

val Icons.Filled.CheckBox: ImageVector
    get() {
        if (_checkBox != null) return _checkBox!!
        _checkBox = ImageVector.Builder(
            name = "Filled.CheckBox",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.0f, 3.0f)
                lineTo(5.0f, 3.0f)
                curveToRelative(-1.11f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.11f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(21.0f, 5.0f)
                curveToRelative(0.0f, -1.1f, -0.89f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(10.0f, 17.0f)
                lineToRelative(-5.0f, -5.0f)
                lineToRelative(1.41f, -1.41f)
                lineTo(10.0f, 14.17f)
                lineToRelative(7.59f, -7.59f)
                lineTo(19.0f, 8.0f)
                lineToRelative(-9.0f, 9.0f)
                close()
            }
        }.build()
        return _checkBox!!
    }

// ---------------------------------------------------------------------------
// CheckBoxOutlineBlank
// ---------------------------------------------------------------------------

private var _checkBoxOutlineBlank: ImageVector? = null

val Icons.Filled.CheckBoxOutlineBlank: ImageVector
    get() {
        if (_checkBoxOutlineBlank != null) return _checkBoxOutlineBlank!!
        _checkBoxOutlineBlank = ImageVector.Builder(
            name = "Filled.CheckBoxOutlineBlank",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.0f, 5.0f)
                verticalLineToRelative(14.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(14.0f)
                moveToRelative(0.0f, -2.0f)
                horizontalLineTo(5.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(5.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
            }
        }.build()
        return _checkBoxOutlineBlank!!
    }

// ---------------------------------------------------------------------------
// CloudOff
// ---------------------------------------------------------------------------

private var _cloudOff: ImageVector? = null

val Icons.Filled.CloudOff: ImageVector
    get() {
        if (_cloudOff != null) return _cloudOff!!
        _cloudOff = ImageVector.Builder(
            name = "Filled.CloudOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.35f, 10.04f)
                curveTo(18.67f, 6.59f, 15.64f, 4.0f, 12.0f, 4.0f)
                curveToRelative(-1.48f, 0.0f, -2.85f, 0.43f, -4.01f, 1.17f)
                lineToRelative(1.46f, 1.46f)
                curveTo(10.21f, 6.23f, 11.08f, 6.0f, 12.0f, 6.0f)
                curveToRelative(3.04f, 0.0f, 5.5f, 2.46f, 5.5f, 5.5f)
                verticalLineToRelative(0.5f)
                horizontalLineTo(19.0f)
                curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
                curveToRelative(0.0f, 1.13f, -0.64f, 2.11f, -1.56f, 2.62f)
                lineToRelative(1.45f, 1.45f)
                curveTo(23.16f, 18.16f, 24.0f, 16.68f, 24.0f, 15.0f)
                curveToRelative(0.0f, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f)
                close()
                moveTo(3.0f, 5.27f)
                lineToRelative(2.75f, 2.74f)
                curveTo(2.56f, 8.15f, 0.0f, 10.77f, 0.0f, 14.0f)
                curveToRelative(0.0f, 3.31f, 2.69f, 6.0f, 6.0f, 6.0f)
                horizontalLineToRelative(11.73f)
                lineToRelative(2.0f, 2.0f)
                lineTo(21.0f, 20.73f)
                lineTo(4.27f, 4.0f)
                lineTo(3.0f, 5.27f)
                close()
                moveTo(7.73f, 10.0f)
                lineToRelative(8.0f, 8.0f)
                horizontalLineTo(6.0f)
                curveToRelative(-2.21f, 0.0f, -4.0f, -1.79f, -4.0f, -4.0f)
                reflectiveCurveToRelative(1.79f, -4.0f, 4.0f, -4.0f)
                horizontalLineToRelative(1.73f)
                close()
            }
        }.build()
        return _cloudOff!!
    }

// ---------------------------------------------------------------------------
// Compress
// ---------------------------------------------------------------------------

private var _compress: ImageVector? = null

val Icons.Filled.Compress: ImageVector
    get() {
        if (_compress != null) return _compress!!
        _compress = ImageVector.Builder(
            name = "Filled.Compress",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.0f, 19.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(3.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineToRelative(3.0f)
                lineToRelative(-4.0f, -4.0f)
                lineToRelative(-4.0f, 4.0f)
                close()
                moveTo(16.0f, 4.0f)
                horizontalLineToRelative(-3.0f)
                lineTo(13.0f, 1.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(3.0f)
                lineTo(8.0f, 4.0f)
                lineToRelative(4.0f, 4.0f)
                lineToRelative(4.0f, -4.0f)
                close()
                moveTo(4.0f, 9.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(16.0f)
                lineTo(20.0f, 9.0f)
                lineTo(4.0f, 9.0f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(4.0f, 12.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(4.0f)
                close()
            }
        }.build()
        return _compress!!
    }

// ---------------------------------------------------------------------------
// Devices
// ---------------------------------------------------------------------------

private var _devices: ImageVector? = null

val Icons.Filled.Devices: ImageVector
    get() {
        if (_devices != null) return _devices!!
        _devices = ImageVector.Builder(
            name = "Filled.Devices",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4.0f, 6.0f)
                horizontalLineToRelative(18.0f)
                lineTo(22.0f, 4.0f)
                lineTo(4.0f, 4.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(11.0f)
                lineTo(0.0f, 17.0f)
                verticalLineToRelative(3.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(-3.0f)
                lineTo(4.0f, 17.0f)
                lineTo(4.0f, 6.0f)
                close()
                moveTo(23.0f, 8.0f)
                horizontalLineToRelative(-6.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(10.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(6.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                lineTo(24.0f, 9.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                close()
                moveTo(22.0f, 17.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-7.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(7.0f)
                close()
            }
        }.build()
        return _devices!!
    }

// ---------------------------------------------------------------------------
// DirectionsRun
// ---------------------------------------------------------------------------

private var _directionsRun: ImageVector? = null

val Icons.Filled.DirectionsRun: ImageVector
    get() {
        if (_directionsRun != null) return _directionsRun!!
        _directionsRun = ImageVector.Builder(
            name = "Filled.DirectionsRun",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13.49f, 5.48f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                close()
                moveTo(9.89f, 19.38f)
                lineToRelative(1.0f, -4.4f)
                lineToRelative(2.1f, 2.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-7.5f)
                lineToRelative(-2.1f, -2.0f)
                lineToRelative(0.6f, -3.0f)
                curveToRelative(1.3f, 1.5f, 3.3f, 2.5f, 5.5f, 2.5f)
                verticalLineToRelative(-2.0f)
                curveToRelative(-1.9f, 0.0f, -3.5f, -1.0f, -4.3f, -2.4f)
                lineToRelative(-1.0f, -1.6f)
                curveToRelative(-0.4f, -0.6f, -1.0f, -1.0f, -1.7f, -1.0f)
                curveToRelative(-0.3f, 0.0f, -0.5f, 0.1f, -0.8f, 0.1f)
                lineToRelative(-5.2f, 2.2f)
                verticalLineToRelative(4.7f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-3.4f)
                lineToRelative(1.8f, -0.7f)
                lineToRelative(-1.6f, 8.1f)
                lineToRelative(-4.9f, -1.0f)
                lineToRelative(-0.4f, 2.0f)
                lineToRelative(7.0f, 1.4f)
                close()
            }
        }.build()
        return _directionsRun!!
    }

// ---------------------------------------------------------------------------
// DragHandle
// ---------------------------------------------------------------------------

private var _dragHandle: ImageVector? = null

val Icons.Filled.DragHandle: ImageVector
    get() {
        if (_dragHandle != null) return _dragHandle!!
        _dragHandle = ImageVector.Builder(
            name = "Filled.DragHandle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.0f, 9.0f)
                horizontalLineTo(4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(16.0f)
                verticalLineTo(9.0f)
                close()
                moveTo(4.0f, 15.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(15.0f)
                close()
            }
        }.build()
        return _dragHandle!!
    }

// ---------------------------------------------------------------------------
// Error
// ---------------------------------------------------------------------------

private var _error: ImageVector? = null

val Icons.Filled.Error: ImageVector
    get() {
        if (_error != null) return _error!!
        _error = ImageVector.Builder(
            name = "Filled.Error",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(13.0f, 17.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(13.0f, 13.0f)
                horizontalLineToRelative(-2.0f)
                lineTo(11.0f, 7.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(6.0f)
                close()
            }
        }.build()
        return _error!!
    }

// ---------------------------------------------------------------------------
// FitnessCenter
// ---------------------------------------------------------------------------

private var _fitnessCenter: ImageVector? = null

val Icons.Filled.FitnessCenter: ImageVector
    get() {
        if (_fitnessCenter != null) return _fitnessCenter!!
        _fitnessCenter = ImageVector.Builder(
            name = "Filled.FitnessCenter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.57f, 14.86f)
                lineTo(22.0f, 13.43f)
                lineTo(20.57f, 12.0f)
                lineTo(17.0f, 15.57f)
                lineTo(8.43f, 7.0f)
                lineTo(12.0f, 3.43f)
                lineTo(10.57f, 2.0f)
                lineTo(9.14f, 3.43f)
                lineTo(7.71f, 2.0f)
                lineTo(5.57f, 4.14f)
                lineTo(4.14f, 2.71f)
                lineTo(2.71f, 4.14f)
                lineToRelative(1.43f, 1.43f)
                lineTo(2.0f, 7.71f)
                lineToRelative(1.43f, 1.43f)
                lineTo(2.0f, 10.57f)
                lineTo(3.43f, 12.0f)
                lineTo(7.0f, 8.43f)
                lineTo(15.57f, 17.0f)
                lineTo(12.0f, 20.57f)
                lineTo(13.43f, 22.0f)
                lineToRelative(1.43f, -1.43f)
                lineTo(16.29f, 22.0f)
                lineToRelative(2.14f, -2.14f)
                lineToRelative(1.43f, 1.43f)
                lineToRelative(1.43f, -1.43f)
                lineToRelative(-1.43f, -1.43f)
                lineTo(22.0f, 16.29f)
                close()
            }
        }.build()
        return _fitnessCenter!!
    }

// ---------------------------------------------------------------------------
// LocalDrink
// ---------------------------------------------------------------------------

private var _localDrink: ImageVector? = null

val Icons.Filled.LocalDrink: ImageVector
    get() {
        if (_localDrink != null) return _localDrink!!
        _localDrink = ImageVector.Builder(
            name = "Filled.LocalDrink",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3.0f, 2.0f)
                lineToRelative(2.01f, 18.23f)
                curveTo(5.13f, 21.23f, 5.97f, 22.0f, 7.0f, 22.0f)
                horizontalLineToRelative(10.0f)
                curveToRelative(1.03f, 0.0f, 1.87f, -0.77f, 1.99f, -1.77f)
                lineTo(21.0f, 2.0f)
                lineTo(3.0f, 2.0f)
                close()
                moveTo(12.0f, 19.0f)
                curveToRelative(-1.66f, 0.0f, -3.0f, -1.34f, -3.0f, -3.0f)
                curveToRelative(0.0f, -2.0f, 3.0f, -5.4f, 3.0f, -5.4f)
                reflectiveCurveToRelative(3.0f, 3.4f, 3.0f, 5.4f)
                curveToRelative(0.0f, 1.66f, -1.34f, 3.0f, -3.0f, 3.0f)
                close()
                moveTo(18.33f, 8.0f)
                lineTo(5.67f, 8.0f)
                lineToRelative(-0.44f, -4.0f)
                horizontalLineToRelative(13.53f)
                lineToRelative(-0.43f, 4.0f)
                close()
            }
        }.build()
        return _localDrink!!
    }

// ---------------------------------------------------------------------------
// MusicNote
// ---------------------------------------------------------------------------

private var _musicNote: ImageVector? = null

val Icons.Filled.MusicNote: ImageVector
    get() {
        if (_musicNote != null) return _musicNote!!
        _musicNote = ImageVector.Builder(
            name = "Filled.MusicNote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 3.0f)
                verticalLineToRelative(10.55f)
                curveToRelative(-0.59f, -0.34f, -1.27f, -0.55f, -2.0f, -0.55f)
                curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
                reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
                verticalLineTo(7.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(3.0f)
                horizontalLineToRelative(-6.0f)
                close()
            }
        }.build()
        return _musicNote!!
    }

// ---------------------------------------------------------------------------
// Pause
// ---------------------------------------------------------------------------

private var _pause: ImageVector? = null

val Icons.Filled.Pause: ImageVector
    get() {
        if (_pause != null) return _pause!!
        _pause = ImageVector.Builder(
            name = "Filled.Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.0f, 19.0f)
                horizontalLineToRelative(4.0f)
                lineTo(10.0f, 5.0f)
                lineTo(6.0f, 5.0f)
                verticalLineToRelative(14.0f)
                close()
                moveTo(14.0f, 5.0f)
                verticalLineToRelative(14.0f)
                horizontalLineToRelative(4.0f)
                lineTo(18.0f, 5.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
        }.build()
        return _pause!!
    }

// ---------------------------------------------------------------------------
// PersonOff
// ---------------------------------------------------------------------------

private var _personOff: ImageVector? = null

val Icons.Filled.PersonOff: ImageVector
    get() {
        if (_personOff != null) return _personOff!!
        _personOff = ImageVector.Builder(
            name = "Filled.PersonOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.65f, 5.82f)
                curveTo(9.36f, 4.72f, 10.6f, 4.0f, 12.0f, 4.0f)
                curveToRelative(2.21f, 0.0f, 4.0f, 1.79f, 4.0f, 4.0f)
                curveToRelative(0.0f, 1.4f, -0.72f, 2.64f, -1.82f, 3.35f)
                lineTo(8.65f, 5.82f)
                close()
                moveTo(20.0f, 17.17f)
                curveToRelative(-0.02f, -1.1f, -0.63f, -2.11f, -1.61f, -2.62f)
                curveToRelative(-0.54f, -0.28f, -1.13f, -0.54f, -1.77f, -0.76f)
                lineTo(20.0f, 17.17f)
                close()
                moveTo(21.19f, 21.19f)
                lineTo(2.81f, 2.81f)
                lineTo(1.39f, 4.22f)
                lineToRelative(8.89f, 8.89f)
                curveToRelative(-1.81f, 0.23f, -3.39f, 0.79f, -4.67f, 1.45f)
                curveTo(4.61f, 15.07f, 4.0f, 16.1f, 4.0f, 17.22f)
                verticalLineTo(20.0f)
                horizontalLineToRelative(13.17f)
                lineToRelative(2.61f, 2.61f)
                lineTo(21.19f, 21.19f)
                close()
            }
        }.build()
        return _personOff!!
    }

// ---------------------------------------------------------------------------
// Restaurant
// ---------------------------------------------------------------------------

private var _restaurant: ImageVector? = null

val Icons.Filled.Restaurant: ImageVector
    get() {
        if (_restaurant != null) return _restaurant!!
        _restaurant = ImageVector.Builder(
            name = "Filled.Restaurant",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.0f, 9.0f)
                lineTo(9.0f, 9.0f)
                lineTo(9.0f, 2.0f)
                lineTo(7.0f, 2.0f)
                verticalLineToRelative(7.0f)
                lineTo(5.0f, 9.0f)
                lineTo(5.0f, 2.0f)
                lineTo(3.0f, 2.0f)
                verticalLineToRelative(7.0f)
                curveToRelative(0.0f, 2.12f, 1.66f, 3.84f, 3.75f, 3.97f)
                lineTo(6.75f, 22.0f)
                horizontalLineToRelative(2.5f)
                verticalLineToRelative(-9.03f)
                curveTo(11.34f, 12.84f, 13.0f, 11.12f, 13.0f, 9.0f)
                lineTo(13.0f, 2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(7.0f)
                close()
                moveTo(16.0f, 6.0f)
                verticalLineToRelative(8.0f)
                horizontalLineToRelative(2.5f)
                verticalLineToRelative(8.0f)
                lineTo(21.0f, 22.0f)
                lineTo(21.0f, 2.0f)
                curveToRelative(-2.76f, 0.0f, -5.0f, 2.24f, -5.0f, 4.0f)
                close()
            }
        }.build()
        return _restaurant!!
    }

// ---------------------------------------------------------------------------
// Restore
// ---------------------------------------------------------------------------

private var _restore: ImageVector? = null

val Icons.Filled.Restore: ImageVector
    get() {
        if (_restore != null) return _restore!!
        _restore = ImageVector.Builder(
            name = "Filled.Restore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13.0f, 3.0f)
                curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
                lineTo(1.0f, 12.0f)
                lineToRelative(3.89f, 3.89f)
                lineToRelative(0.07f, 0.14f)
                lineTo(9.0f, 12.0f)
                lineTo(6.0f, 12.0f)
                curveToRelative(0.0f, -3.87f, 3.13f, -7.0f, 7.0f, -7.0f)
                reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f)
                reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f)
                curveToRelative(-1.93f, 0.0f, -3.68f, -0.79f, -4.94f, -2.06f)
                lineToRelative(-1.42f, 1.42f)
                curveTo(8.27f, 19.99f, 10.51f, 21.0f, 13.0f, 21.0f)
                curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
                reflectiveCurveToRelative(-4.03f, -9.0f, -9.0f, -9.0f)
                close()
                moveTo(12.0f, 8.0f)
                verticalLineToRelative(5.0f)
                lineToRelative(4.28f, 2.54f)
                lineToRelative(0.72f, -1.21f)
                lineToRelative(-3.5f, -2.08f)
                lineTo(13.5f, 8.0f)
                lineTo(12.0f, 8.0f)
                close()
            }
        }.build()
        return _restore!!
    }

// ---------------------------------------------------------------------------
// RestartAlt
// ---------------------------------------------------------------------------

private var _restartAlt: ImageVector? = null

val Icons.Filled.RestartAlt: ImageVector
    get() {
        if (_restartAlt != null) return _restartAlt!!
        _restartAlt = ImageVector.Builder(
            name = "Filled.RestartAlt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 5.0f)
                verticalLineTo(2.0f)
                lineTo(8.0f, 6.0f)
                lineToRelative(4.0f, 4.0f)
                verticalLineTo(7.0f)
                curveToRelative(3.31f, 0.0f, 6.0f, 2.69f, 6.0f, 6.0f)
                curveToRelative(0.0f, 2.97f, -2.17f, 5.43f, -5.0f, 5.91f)
                verticalLineToRelative(2.02f)
                curveToRelative(3.95f, -0.49f, 7.0f, -3.85f, 7.0f, -7.93f)
                curveTo(20.0f, 8.58f, 16.42f, 5.0f, 12.0f, 5.0f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.0f, 13.0f)
                curveToRelative(0.0f, -1.65f, 0.67f, -3.15f, 1.76f, -4.24f)
                lineTo(6.34f, 7.34f)
                curveTo(4.9f, 8.79f, 4.0f, 10.79f, 4.0f, 13.0f)
                curveToRelative(0.0f, 4.08f, 3.05f, 7.44f, 7.0f, 7.93f)
                verticalLineToRelative(-2.02f)
                curveTo(8.17f, 18.43f, 6.0f, 15.97f, 6.0f, 13.0f)
                close()
            }
        }.build()
        return _restartAlt!!
    }

// ---------------------------------------------------------------------------
// SelfImprovement
// ---------------------------------------------------------------------------

private var _selfImprovement: ImageVector? = null

val Icons.Filled.SelfImprovement: ImageVector
    get() {
        if (_selfImprovement != null) return _selfImprovement!!
        _selfImprovement = ImageVector.Builder(
            name = "Filled.SelfImprovement",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 6.0f)
                moveToRelative(-2.0f, 0.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, true, true, 4.0f, 0.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, true, true, -4.0f, 0.0f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(21.0f, 16.0f)
                verticalLineToRelative(-2.0f)
                curveToRelative(-2.24f, 0.0f, -4.16f, -0.96f, -5.6f, -2.68f)
                lineToRelative(-1.34f, -1.6f)
                curveTo(13.68f, 9.26f, 13.12f, 9.0f, 12.53f, 9.0f)
                horizontalLineToRelative(-1.05f)
                curveToRelative(-0.59f, 0.0f, -1.15f, 0.26f, -1.53f, 0.72f)
                lineToRelative(-1.34f, 1.6f)
                curveTo(7.16f, 13.04f, 5.24f, 14.0f, 3.0f, 14.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(2.77f, 0.0f, 5.19f, -1.17f, 7.0f, -3.25f)
                verticalLineTo(15.0f)
                lineToRelative(-3.88f, 1.55f)
                curveTo(5.45f, 16.82f, 5.0f, 17.48f, 5.0f, 18.21f)
                curveTo(5.0f, 19.2f, 5.8f, 20.0f, 6.79f, 20.0f)
                horizontalLineTo(9.0f)
                verticalLineToRelative(-0.5f)
                curveToRelative(0.0f, -1.38f, 1.12f, -2.5f, 2.5f, -2.5f)
                horizontalLineToRelative(3.0f)
                curveToRelative(0.28f, 0.0f, 0.5f, 0.22f, 0.5f, 0.5f)
                reflectiveCurveTo(14.78f, 18.0f, 14.5f, 18.0f)
                horizontalLineToRelative(-3.0f)
                curveToRelative(-0.83f, 0.0f, -1.5f, 0.67f, -1.5f, 1.5f)
                verticalLineTo(20.0f)
                horizontalLineToRelative(7.21f)
                curveTo(18.2f, 20.0f, 19.0f, 19.2f, 19.0f, 18.21f)
                curveToRelative(0.0f, -0.73f, -0.45f, -1.39f, -1.12f, -1.66f)
                lineTo(14.0f, 15.0f)
                verticalLineToRelative(-2.25f)
                curveTo(15.81f, 14.83f, 18.23f, 16.0f, 21.0f, 16.0f)
                close()
            }
        }.build()
        return _selfImprovement!!
    }

// ---------------------------------------------------------------------------
// SkipNext
// ---------------------------------------------------------------------------

private var _skipNext: ImageVector? = null

val Icons.Filled.SkipNext: ImageVector
    get() {
        if (_skipNext != null) return _skipNext!!
        _skipNext = ImageVector.Builder(
            name = "Filled.SkipNext",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.0f, 18.0f)
                lineToRelative(8.5f, -6.0f)
                lineTo(6.0f, 6.0f)
                verticalLineToRelative(12.0f)
                close()
                moveTo(16.0f, 6.0f)
                verticalLineToRelative(12.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(-2.0f)
                close()
            }
        }.build()
        return _skipNext!!
    }

// ---------------------------------------------------------------------------
// Timer
// ---------------------------------------------------------------------------

private var _timer: ImageVector? = null

val Icons.Filled.Timer: ImageVector
    get() {
        if (_timer != null) return _timer!!
        _timer = ImageVector.Builder(
            name = "Filled.Timer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9.0f, 1.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-6.0f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.03f, 7.39f)
                lineToRelative(1.42f, -1.42f)
                curveToRelative(-0.43f, -0.51f, -0.9f, -0.99f, -1.41f, -1.41f)
                lineToRelative(-1.42f, 1.42f)
                curveTo(16.07f, 4.74f, 14.12f, 4.0f, 12.0f, 4.0f)
                curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
                curveToRelative(0.0f, 4.97f, 4.02f, 9.0f, 9.0f, 9.0f)
                reflectiveCurveToRelative(9.0f, -4.03f, 9.0f, -9.0f)
                curveTo(21.0f, 10.88f, 20.26f, 8.93f, 19.03f, 7.39f)
                close()
                moveTo(13.0f, 14.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineTo(8.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(14.0f)
                close()
            }
        }.build()
        return _timer!!
    }

// ---------------------------------------------------------------------------
// Visibility
// ---------------------------------------------------------------------------

private var _visibility: ImageVector? = null

val Icons.Filled.Visibility: ImageVector
    get() {
        if (_visibility != null) return _visibility!!
        _visibility = ImageVector.Builder(
            name = "Filled.Visibility",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 4.5f)
                curveTo(7.0f, 4.5f, 2.73f, 7.61f, 1.0f, 12.0f)
                curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
                reflectiveCurveToRelative(9.27f, -3.11f, 11.0f, -7.5f)
                curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
                close()
                moveTo(12.0f, 17.0f)
                curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
                reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
                reflectiveCurveToRelative(-2.24f, 5.0f, -5.0f, 5.0f)
                close()
                moveTo(12.0f, 9.0f)
                curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
                reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
                reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
                close()
            }
        }.build()
        return _visibility!!
    }

// ---------------------------------------------------------------------------
// VisibilityOff
// ---------------------------------------------------------------------------

private var _visibilityOff: ImageVector? = null

val Icons.Filled.VisibilityOff: ImageVector
    get() {
        if (_visibilityOff != null) return _visibilityOff!!
        _visibilityOff = ImageVector.Builder(
            name = "Filled.VisibilityOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 7.0f)
                curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
                curveToRelative(0.0f, 0.65f, -0.13f, 1.26f, -0.36f, 1.83f)
                lineToRelative(2.92f, 2.92f)
                curveToRelative(1.51f, -1.26f, 2.7f, -2.89f, 3.43f, -4.75f)
                curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
                curveToRelative(-1.4f, 0.0f, -2.74f, 0.25f, -3.98f, 0.7f)
                lineToRelative(2.16f, 2.16f)
                curveTo(10.74f, 7.13f, 11.35f, 7.0f, 12.0f, 7.0f)
                close()
                moveTo(2.0f, 4.27f)
                lineToRelative(2.28f, 2.28f)
                lineToRelative(0.46f, 0.46f)
                curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1.0f, 12.0f)
                curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
                curveToRelative(1.55f, 0.0f, 3.03f, -0.3f, 4.38f, -0.84f)
                lineToRelative(0.42f, 0.42f)
                lineTo(19.73f, 22.0f)
                lineTo(21.0f, 20.73f)
                lineTo(3.27f, 3.0f)
                lineTo(2.0f, 4.27f)
                close()
                moveTo(7.53f, 9.8f)
                lineToRelative(1.55f, 1.55f)
                curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
                curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
                curveToRelative(0.22f, 0.0f, 0.44f, -0.03f, 0.65f, -0.08f)
                lineToRelative(1.55f, 1.55f)
                curveToRelative(-0.67f, 0.33f, -1.41f, 0.53f, -2.2f, 0.53f)
                curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                curveToRelative(0.0f, -0.79f, 0.2f, -1.53f, 0.53f, -2.2f)
                close()
                moveTo(11.84f, 9.02f)
                lineToRelative(3.15f, 3.15f)
                lineToRelative(0.02f, -0.16f)
                curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f)
                lineToRelative(-0.17f, 0.01f)
                close()
            }
        }.build()
        return _visibilityOff!!
    }
