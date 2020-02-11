package com.livelike.engagementsdk.widget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.support.annotation.NonNull
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest


class RoundedCornersTransformation @JvmOverloads constructor(
    private val radius: Float,
    private val margin: Int,
    private val cornerType: CornerType = CornerType.ALL
) :
    BitmapTransformation() {
    enum class CornerType {
        ALL, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT, OTHER_TOP_LEFT, OTHER_TOP_RIGHT, OTHER_BOTTOM_LEFT, OTHER_BOTTOM_RIGHT, DIAGONAL_FROM_TOP_LEFT, DIAGONAL_FROM_TOP_RIGHT
    }

    private val diameter: Float = radius * 2


    private fun drawRoundRect(
        canvas: Canvas,
        paint: Paint,
        width: Float,
        height: Float
    ) {
        val right = width - margin
        val bottom = height - margin
        when (cornerType) {
            CornerType.ALL -> canvas.drawRoundRect(
                RectF(
                    margin.toFloat(),
                    margin.toFloat(),
                    right,
                    bottom
                ), radius, radius, paint
            )
            CornerType.TOP_LEFT -> drawTopLeftRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.TOP_RIGHT -> drawTopRightRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.BOTTOM_LEFT -> drawBottomLeftRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.BOTTOM_RIGHT -> drawBottomRightRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.TOP -> drawTopRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.BOTTOM -> drawBottomRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.LEFT -> drawLeftRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.RIGHT -> drawRightRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.OTHER_TOP_LEFT -> drawOtherTopLeftRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.OTHER_TOP_RIGHT -> drawOtherTopRightRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.OTHER_BOTTOM_LEFT -> drawOtherBottomLeftRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.OTHER_BOTTOM_RIGHT -> drawOtherBottomRightRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.DIAGONAL_FROM_TOP_LEFT -> drawDiagonalFromTopLeftRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            CornerType.DIAGONAL_FROM_TOP_RIGHT -> drawDiagonalFromTopRightRoundRect(
                canvas,
                paint,
                right,
                bottom
            )
            else -> canvas.drawRoundRect(
                RectF(margin.toFloat(), margin.toFloat(), right, bottom),
                radius,
                radius,
                paint
            )
        }
    }

    private fun drawTopLeftRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(
                margin.toFloat(),
                margin.toFloat(),
                (margin + diameter),
                (margin + diameter)
            ), radius,
            radius, paint
        )
        canvas.drawRect(
            RectF(
                margin.toFloat(),
                (margin + radius),
                (margin + radius), bottom
            ), paint
        )
        canvas.drawRect(RectF((margin + radius), margin.toFloat(), right, bottom), paint)
    }

    private fun drawTopRightRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(right - diameter, margin.toFloat(), right, (margin + diameter)), radius,
            radius, paint
        )
        canvas.drawRect(RectF(margin.toFloat(), margin.toFloat(), right - radius, bottom), paint)
        canvas.drawRect(RectF(right - radius, (margin + radius), right, bottom), paint)
    }

    private fun drawBottomLeftRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(margin.toFloat(), bottom - diameter, (margin + diameter), bottom),
            radius,
            radius,
            paint
        )
        canvas.drawRect(
            RectF(
                margin.toFloat(),
                margin.toFloat(),
                (margin + diameter), bottom - radius
            ), paint
        )
        canvas.drawRect(RectF((margin + radius), margin.toFloat(), right, bottom), paint)
    }

    private fun drawBottomRightRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(right - diameter, bottom - diameter, right, bottom), radius,
            radius, paint
        )
        canvas.drawRect(RectF(margin.toFloat(), margin.toFloat(), right - radius, bottom), paint)
        canvas.drawRect(RectF(right - radius, margin.toFloat(), right, bottom - radius), paint)
    }

    private fun drawTopRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(margin.toFloat(), margin.toFloat(), right, (margin + diameter)),
            radius,
            radius,
            paint
        )
        canvas.drawRect(RectF(margin.toFloat(), (margin + radius), right, bottom), paint)
    }

    private fun drawBottomRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(margin.toFloat(), bottom - diameter, right, bottom), radius, radius,
            paint
        )
        canvas.drawRect(RectF(margin.toFloat(), margin.toFloat(), right, bottom - radius), paint)
    }

    private fun drawLeftRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(margin.toFloat(), margin.toFloat(), (margin + diameter), bottom),
            radius,
            radius,
            paint
        )
        canvas.drawRect(RectF((margin + radius), margin.toFloat(), right, bottom), paint)
    }

    private fun drawRightRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(right - diameter, margin.toFloat(), right, bottom),
            radius,
            radius,
            paint
        )
        canvas.drawRect(RectF(margin.toFloat(), margin.toFloat(), right - radius, bottom), paint)
    }

    private fun drawOtherTopLeftRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(margin.toFloat(), bottom - diameter, right, bottom), radius, radius,
            paint
        )
        canvas.drawRoundRect(
            RectF(right - diameter, margin.toFloat(), right, bottom),
            radius,
            radius,
            paint
        )
        canvas.drawRect(
            RectF(margin.toFloat(), margin.toFloat(), right - radius, bottom - radius),
            paint
        )
    }

    private fun drawOtherTopRightRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(margin.toFloat(), margin.toFloat(), (margin + diameter), bottom),
            radius,
            radius,
            paint
        )
        canvas.drawRoundRect(
            RectF(margin.toFloat(), bottom - diameter, right, bottom), radius, radius,
            paint
        )
        canvas.drawRect(
            RectF(
                (margin + radius),
                margin.toFloat(),
                right,
                bottom - radius
            ), paint
        )
    }

    private fun drawOtherBottomLeftRoundRect(
        canvas: Canvas,
        paint: Paint,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(margin.toFloat(), margin.toFloat(), right, (margin + diameter)),
            radius,
            radius,
            paint
        )
        canvas.drawRoundRect(
            RectF(right - diameter, margin.toFloat(), right, bottom),
            radius,
            radius,
            paint
        )
        canvas.drawRect(
            RectF(
                margin.toFloat(),
                (margin + radius),
                right - radius,
                bottom
            ), paint
        )
    }

    private fun drawOtherBottomRightRoundRect(
        canvas: Canvas, paint: Paint, right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(margin.toFloat(), margin.toFloat(), right, (margin + diameter)),
            radius,
            radius,
            paint
        )
        canvas.drawRoundRect(
            RectF(margin.toFloat(), margin.toFloat(), (margin + diameter), bottom),
            radius,
            radius,
            paint
        )
        canvas.drawRect(
            RectF(
                (margin + radius),
                (margin + radius), right, bottom
            ), paint
        )
    }

    private fun drawDiagonalFromTopLeftRoundRect(
        canvas: Canvas, paint: Paint, right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(
                margin.toFloat(),
                margin.toFloat(),
                (margin + diameter),
                (margin + diameter)
            ), radius,
            radius, paint
        )
        canvas.drawRoundRect(
            RectF(right - diameter, bottom - diameter, right, bottom), radius,
            radius, paint
        )
        canvas.drawRect(
            RectF(
                margin.toFloat(),
                (margin + radius), right - diameter, bottom
            ), paint
        )
        canvas.drawRect(
            RectF(
                (margin + diameter),
                margin.toFloat(),
                right,
                bottom - radius
            ), paint
        )
    }

    private fun drawDiagonalFromTopRightRoundRect(
        canvas: Canvas, paint: Paint, right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(
            RectF(right - diameter, margin.toFloat(), right, (margin + diameter)), radius,
            radius, paint
        )
        canvas.drawRoundRect(
            RectF(margin.toFloat(), bottom - diameter, (margin + diameter), bottom),
            radius,
            radius,
            paint
        )
        canvas.drawRect(
            RectF(margin.toFloat(), margin.toFloat(), right - radius, bottom - radius),
            paint
        )
        canvas.drawRect(
            RectF(
                (margin + radius),
                (margin + radius), right, bottom
            ), paint
        )
    }

    override fun toString(): String {
        return ("RoundedTransformation(radius=" + radius + ", margin=" + margin + ", diameter="
                + diameter + ", cornerType=" + cornerType.name + ")")
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val width = toTransform.width
        val height = toTransform.height
        val bitmap = pool[width, height, Bitmap.Config.ARGB_8888]
        bitmap.setHasAlpha(true)
//        setCanvasBitmapDensity(toTransform, bitmap)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = BitmapShader(toTransform, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        drawRoundRect(canvas, paint, width.toFloat(), height.toFloat())
        return bitmap
    }

    override fun equals(o: Any?): Boolean {
        return o is RoundedCornersTransformation && o.radius == radius && o.diameter == diameter && o.margin == margin && o.cornerType == cornerType
    }

    override fun hashCode(): Int {
        return (ID.hashCode() + radius * 10000 + diameter * 1000 + margin * 100 + cornerType.ordinal * 10).toInt()
    }

    override fun updateDiskCacheKey(@NonNull messageDigest: MessageDigest) {
        messageDigest.update(
            (ID + radius + diameter + margin + cornerType).toByteArray(
                Key.CHARSET
            )
        )
    }

    companion object {
        private const val VERSION = 1
        private const val ID =
            "com.livelike.$VERSION"
    }

}