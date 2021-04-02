package dev.lucasnlm.antimine.gdx.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import dev.lucasnlm.antimine.core.getPos
import dev.lucasnlm.antimine.core.models.Area
import dev.lucasnlm.antimine.gdx.GdxLocal
import dev.lucasnlm.antimine.gdx.alpha
import dev.lucasnlm.antimine.gdx.dim
import dev.lucasnlm.antimine.gdx.drawAsset
import dev.lucasnlm.antimine.gdx.drawRegion
import dev.lucasnlm.antimine.gdx.drawTexture
import dev.lucasnlm.antimine.gdx.events.GdxEvent
import dev.lucasnlm.antimine.gdx.toGdxColor
import dev.lucasnlm.antimine.gdx.toOppositeMax
import dev.lucasnlm.antimine.ui.model.AppTheme
import dev.lucasnlm.antimine.ui.model.minesAround

class AreaActor(
    size: Float,
    initialAreaForm: AreaForm,
    private var area: Area,
    private var focusScale: Float = 1.0f,
    private var isPressed: Boolean = false,
    private val pieces: MutableList<String> = mutableListOf(),
    private val theme: AppTheme,
    private val squareDivider: Float = 0f,
    private val onInputEvent: (GdxEvent) -> Unit,
) : Actor() {

    private var areaForm: AreaForm? = null

    init {
        width = size
        height = size
        x = area.posX * width
        y = area.posY * height

        addListener(object : InputListener() {
            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                super.touchUp(event, x, y, pointer, button)
                onInputEvent(GdxEvent.TouchUpEvent(area.id))
                isPressed = false
                toBack()
                Gdx.graphics.requestRendering()
            }

            override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                toFront()
                GdxLocal.highlightAlpha = 0.45f
                isPressed = true
                onInputEvent(GdxEvent.TouchDownEvent(area.id))
                Gdx.graphics.requestRendering()
                return true
            }
        })

        bindArea(true, area, initialAreaForm)
    }

    fun boundAreaId() = area.id

    fun bindArea(reset: Boolean, area: Area, areaForm: AreaForm) {
        if (reset) {
            this.isPressed = false
        }

        if ((area.isCovered || area.hasMine) && this.areaForm != areaForm) {
            this.areaForm = areaForm

            pieces.clear()
            val newPieces = mapOf(
                "core" to true,
                "top" to areaForm.top,
                "left" to areaForm.left,
                "bottom" to areaForm.bottom,
                "right" to areaForm.right,
                "corner-top-left" to (!areaForm.top && !areaForm.left),
                "corner-top-right" to (!areaForm.top && !areaForm.right),
                "corner-bottom-right" to (!areaForm.bottom && !areaForm.right),
                "corner-bottom-left" to (!areaForm.bottom && !areaForm.left),
                "bc-top-right" to (areaForm.top && areaForm.right && !areaForm.topRight),
                "bc-top-left" to (areaForm.top && areaForm.left && !areaForm.topLeft),
                "bc-bottom-right" to (areaForm.bottom && areaForm.right && !areaForm.bottomRight),
                "bc-bottom-left" to (areaForm.bottom && areaForm.left && !areaForm.bottomLeft),
                "t-l-tl" to (areaForm.top && areaForm.left && areaForm.topLeft),
                "t-r-tr" to (areaForm.top && areaForm.right && areaForm.topRight),
                "b-l-bl" to (areaForm.bottom && areaForm.left && areaForm.bottomLeft),
                "b-r-br" to (areaForm.bottom && areaForm.right && areaForm.bottomRight),
            ).filter {
                it.value
            }.keys
            pieces.addAll(newPieces)
        }

        this.area = area
    }

    override fun act(delta: Float) {
        super.act(delta)
        val area = this.area

        touchable = if (
            (area.isCovered || area.minesAround > 0) &&
            GdxLocal.qualityZoomLevel < 2 &&
            GdxLocal.actionsEnabled
        ) Touchable.enabled else Touchable.disabled

        val isCurrentFocus = GdxLocal.currentFocus?.id == area.id
        val isEnterPressed = Gdx.input.isKeyPressed(Input.Keys.ENTER)

        if (isCurrentFocus && touchable == Touchable.enabled) {
            if (zIndex != Int.MAX_VALUE && isEnterPressed && !isPressed) {
                toFront()
                onInputEvent(GdxEvent.TouchDownEvent(area.id))
                isPressed = true
            } else if (!isEnterPressed && isPressed) {
                toBack()
                onInputEvent(GdxEvent.TouchUpEvent(area.id))
                isPressed = false
            }
        }

        focusScale = if (isPressed) {
            (focusScale + Gdx.graphics.deltaTime).coerceAtMost(MAX_SCALE)
        } else {
            (focusScale - Gdx.graphics.deltaTime).coerceAtLeast(MIN_SCALE)
        }
    }

    private fun drawBackground(batch: Batch, isOdd: Boolean) {
        val internalPadding = squareDivider / GdxLocal.zoom

        if (!isOdd && !area.isCovered) {
            GdxLocal.gameTextures?.areaTextures?.get(areaNoForm)?.let {
                batch.drawTexture(
                    texture = it,
                    x = x + internalPadding,
                    y = y + internalPadding,
                    width = width - internalPadding * 2,
                    height = height - internalPadding * 2,
                    blend = false,
                    color = theme.palette.background.toOppositeMax(GdxLocal.zoomLevelAlpha).alpha(0.025f)
                )
            }
        }
    }

    private fun drawCovered(batch: Batch, isOdd: Boolean) {
        val internalPadding = squareDivider / GdxLocal.zoom
        val coverColor = if (isOdd) { theme.palette.coveredOdd } else { theme.palette.covered }

        GdxLocal.areaAtlas?.let { atlas ->
            pieces.forEach { piece ->
                batch.drawRegion(
                    texture = atlas.findRegion(piece),
                    x = x + internalPadding - 1,
                    y = y + internalPadding - 1,
                    width = width - internalPadding * 2 + 2,
                    height = height - internalPadding * 2 + 2,
                    color = coverColor.toGdxColor(1.0f),
                    blend = false,
                )
            }
        }
    }

    private fun drawMineBackground(batch: Batch, isOdd: Boolean) {
        val internalPadding = squareDivider / GdxLocal.zoom
        val coverColor = theme.palette.covered.toOppositeMax().mul(0.8f, 0.3f, 0.3f, 1.0f)

        GdxLocal.areaAtlas?.let { atlas ->
            pieces.forEach { piece ->
                batch.drawRegion(
                    texture = atlas.findRegion(piece),
                    x = x + internalPadding - 1,
                    y = y + internalPadding - 1,
                    width = width - internalPadding * 2 + 2,
                    height = height - internalPadding * 2 + 2,
                    color = coverColor,
                    blend = false,
                )
            }
        }
    }

    private fun drawCoveredIcons(batch: Batch) {
        val isAboveOthers = isPressed
        GdxLocal.gameTextures?.let {
            when {
                area.mark.isFlag() -> {
                    val color = if (area.mistake) {
                        theme.palette.covered.toOppositeMax(0.8f).mul(1.0f, 0.5f, 0.5f, 1.0f)
                    } else {
                        theme.palette.covered.toOppositeMax(0.8f)
                    }

                    drawAsset(
                        batch = batch,
                        texture = it.flag,
                        color = color,
                        scale = if (isAboveOthers) focusScale else BASE_ICON_SCALE
                    )
                }
                area.mark.isQuestion() -> {
                    val color = theme.palette.covered.toOppositeMax(1.0f)
                    drawAsset(
                        batch = batch,
                        texture = it.question,
                        color = color,
                        scale = if (isAboveOthers) focusScale else BASE_ICON_SCALE
                    )
                }
                area.revealed -> {
                    val color = theme.palette.uncovered
                    drawAsset(
                        batch = batch,
                        texture = it.mine,
                        color = color.toGdxColor(0.65f),
                        scale = BASE_ICON_SCALE
                    )
                }
            }
        }
    }

    private fun drawUncoveredIcons(batch: Batch) {
        GdxLocal.gameTextures?.let {
            if (area.minesAround > 0) {
                drawAsset(
                    batch = batch,
                    texture = it.aroundMines[area.minesAround - 1],
                    color =
                    theme.palette.minesAround(area.minesAround - 1)
                        .toGdxColor(GdxLocal.zoomLevelAlpha),
                )
            }

            if (area.hasMine) {
                val color = theme.palette.uncovered
                drawAsset(
                    batch = batch,
                    texture = it.mine,
                    color = color.toOppositeMax(1.0f),
                    scale = BASE_ICON_SCALE
                )
            }
        }
    }

    private fun drawPressed(batch: Batch, isOdd: Boolean) {
        val internalPadding = squareDivider / GdxLocal.zoom
        val coverColor = if (isOdd) { theme.palette.coveredOdd } else { theme.palette.covered }

        if ((isPressed || focusScale > 1.0f) && area.isCovered) {
            if (area.isCovered) {
                GdxLocal.gameTextures?.detailedArea?.let {
                    batch.drawTexture(
                        texture = it,
                        x = x + internalPadding,
                        y = y + internalPadding,
                        width = width - internalPadding * 2,
                        height = height - internalPadding * 2,
                        color = coverColor.toGdxColor(1.0f),
                        blend = true,
                    )

                    batch.drawTexture(
                        texture = it,
                        x = x - width * (focusScale - 1.0f) * 0.5f,
                        y = y - height * (focusScale - 1.0f) * 0.5f,
                        width = width * focusScale,
                        height = height * focusScale,
                        color = coverColor.toGdxColor(1.0f).dim(0.8f - (focusScale - 1.0f)),
                        blend = true,
                    )
                }
            }
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        val isOdd: Boolean = if (area.posY % 2 == 0) { area.posX % 2 != 0 } else { area.posX % 2 == 0 }

        batch?.run {
            drawBackground(this, isOdd)

            if (area.isCovered) {
                drawCovered(this, isOdd)
                drawPressed(this, isOdd)
                drawCoveredIcons(this)
            } else {
                if (area.hasMine) {
                    drawMineBackground(this, isOdd)
                }

                drawUncoveredIcons(this)
            }
        }
    }

    companion object {
        const val MIN_SCALE = 1.0f
        const val MAX_SCALE = 1.15f
        const val BASE_ICON_SCALE = 0.8f

        private fun Area.canLigatureTo(area: Area): Boolean {
            return isCovered && mark.ligatureId == area.mark.ligatureId
        }

        fun getForm(area: Area, field: List<Area>): AreaForm {
            if (area.hasMine && !area.isCovered) {
                return areaNoForm
            } else {
                val top = field.getPos(area.posX, area.posY + 1)?.canLigatureTo(area) ?: false
                val bottom = field.getPos(area.posX, area.posY - 1)?.canLigatureTo(area) ?: false
                val left = field.getPos(area.posX - 1, area.posY)?.canLigatureTo(area) ?: false
                val right = field.getPos(area.posX + 1, area.posY)?.canLigatureTo(area) ?: false
                val topLeft = field.getPos(area.posX - 1, area.posY + 1)?.canLigatureTo(area) ?: false
                val topRight = field.getPos(area.posX + 1, area.posY + 1)?.canLigatureTo(area) ?: false
                val bottomLeft = field.getPos(area.posX - 1, area.posY - 1)?.canLigatureTo(area) ?: false
                val bottomRight = field.getPos(area.posX + 1, area.posY - 1)?.canLigatureTo(area) ?: false

                return AreaForm(
                    top = top,
                    bottom = bottom,
                    left = left,
                    right = right,
                    topLeft = topLeft,
                    topRight = topRight,
                    bottomLeft = bottomLeft,
                    bottomRight = bottomRight,
                )
            }
        }
    }
}
