package dev.lucasnlm.antimine.gdx.controller

import android.util.SizeF
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import dev.lucasnlm.antimine.gdx.models.RenderSettings

class CameraController(
    private val renderSettings: RenderSettings,
    private val camera: Camera,
    private val freeControl: Boolean,
) {
    private val velocity: Vector2 = Vector2.Zero.cpy()

    private fun limitSpeed(minefieldSize: SizeF) {
        val padding = renderSettings.internalPadding
        val zoom =  (camera as OrthographicCamera).zoom
        val screenWidth = if (zoom < 1.0f) Gdx.graphics.width * zoom else Gdx.graphics.width.toFloat()
        val screenHeight = if (zoom < 1.0f) Gdx.graphics.height * zoom else Gdx.graphics.height.toFloat()
        val invZoom = 1.0f / zoom

        camera.run {
            val newX = (position.x - velocity.x)
            val newY = (position.y + velocity.y)
            val start = 0.25f * screenWidth - padding.start * invZoom
            val end = minefieldSize.width - 0.25f * screenWidth + padding.end * invZoom
            val top = 0.75f * minefieldSize.height + padding.top * invZoom
            val bottom = 0.25f * minefieldSize.height - padding.bottom * invZoom - renderSettings.navigationBarHeight

            val virtualHeight =
                screenHeight - renderSettings.appBarWithStatusHeight - renderSettings.navigationBarHeight

            if (screenWidth > minefieldSize.width && !freeControl) {
                velocity.x = 0.0f
            } else if ((newX < start && velocity.x < 0.0) || (newX > end && velocity.x > 0.0)) {
                velocity.x = 0.0f
            } else {
                velocity.x *= RESISTANCE
            }

            if (virtualHeight > minefieldSize.height && !freeControl) {
                velocity.y = 0.0f
            } else if ((newY > top && velocity.y > 0.0) || newY < bottom && velocity.y < 0.0) {
                velocity.y = 0.0f
            } else {
                velocity.y *= RESISTANCE
            }
        }
    }

    fun act(minefieldSize: SizeF) {
        if (velocity.len2() > 0.01f) {
            limitSpeed(minefieldSize)

            camera.run {
                translate(velocity.x, velocity.y, 0f)
                update(true)
                Gdx.graphics.requestRendering()
            }
        }
    }

    fun addVelocity(dx: Float, dy: Float) {
        val zoom = (camera as OrthographicCamera).zoom
        velocity.add(dx * zoom, dy * zoom)
        camera.update(true)
    }

    companion object {
        const val RESISTANCE = 0.65f
    }
}
