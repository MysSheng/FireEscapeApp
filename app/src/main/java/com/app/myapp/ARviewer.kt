package com.app.myapp

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.BlendMode
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch

object ARViewer {
    var modelNode: ModelNode? = null
    fun setupSceneView(
        context: Context,
        sceneView: SceneView,
        lifecycleOwner: LifecycleOwner
    ) {
        // 設定透明背景
        sceneView.setZOrderOnTop(true)
        sceneView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        sceneView.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        sceneView.uiHelper.setOpaque(false)
        sceneView.view.blendMode = com.google.android.filament.View.BlendMode.TRANSLUCENT
        sceneView.scene.skybox = null
        val options = sceneView.renderer.clearOptions
        options.clear = true
        sceneView.renderer.clearOptions = options
        //sceneView.renderer.clearOptions.setClear(true)

        // 載入模型
        lifecycleOwner.lifecycleScope.launch {
            val modelFile = "models/direction_arrow.glb"
            val modelInstance = sceneView.modelLoader.createModelInstance(modelFile)
            modelNode = ModelNode(modelInstance, scaleToUnits = 2.0f).apply {
                scale = Scale(0.18f)
            }
            sceneView.addChildNode(modelNode!!)
        }
    }

    // 控制模型的函式
    fun setModelTransform(rotX: Float, rotY: Float , rotZ: Float) {
        modelNode?.apply {
            this.rotation = Rotation(rotX, rotY, rotZ)
        }
    }

    fun setModelPosition(posX: Float, posY: Float , posZ: Float) {
        modelNode?.apply {
            this.position = Position(posX,posY,posZ)
        }
    }

//    fun logCurrentRotation() {
//        modelNode?.rotation?.let { rotation ->
//            val x = rotation.x
//            val y = rotation.y
//            val z = rotation.z
//            Log.d("ARViewer", "Current Rotation - X: $x, Y: $y, Z: $z")
//        } ?: run {
//            Log.d("ARViewer", "ModelNode is null or not initialized.")
//        }
//    }

    fun getModelRotationY(): Float {
        return modelNode?.rotation?.y ?: 0.0f
    }

}