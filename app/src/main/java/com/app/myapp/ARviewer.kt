package com.app.myapp

//import com.google.android.filament.utils.Quaternion
//import io.github.sceneview.collision.Quaternion
import android.R.attr.x
import android.R.attr.y
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Space
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
//import androidx.wear.compose.materialcore.toRadians
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.RotationsOrder
import io.github.sceneview.SceneView
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch
import kotlin.math.sqrt


//object ARViewer {
//    var modelNode: ModelNode? = null
//    fun setupSceneView(
//        context: Context,
//        sceneView: SceneView,
//        lifecycleOwner: LifecycleOwner
//    ) {
//        // 設定透明背景
//        sceneView.setZOrderOnTop(true)
//        sceneView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
//        sceneView.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
//        sceneView.uiHelper.setOpaque(false)
//        sceneView.view.blendMode = com.google.android.filament.View.BlendMode.TRANSLUCENT
//        sceneView.scene.skybox = null
//        val options = sceneView.renderer.clearOptions
//        options.clear = true
//        sceneView.renderer.clearOptions = options
//        //sceneView.renderer.clearOptions.setClear(true)
//
//        // 載入模型
//        lifecycleOwner.lifecycleScope.launch {
//            //val modelFile = "models/direction_arrow.glb"
//            val modelFile = "models/mirrow.glb"
//            val modelInstance = sceneView.modelLoader.createModelInstance(modelFile)
//            modelNode = ModelNode(modelInstance, scaleToUnits = 2.0f).apply {
//                scale = Scale(0.18f)
//            }
//            sceneView.addChildNode(modelNode!!)
//        }
//    }
//
//    // 控制模型的函式
//    @SuppressLint("RestrictedApi")
//    fun setModelTransform(rotX: Float, rotY: Float, rotZ: Float) {
//        modelNode?.apply {
//            this.rotation = Rotation(rotX, rotY, rotZ)
//        }
//    }
//
//    fun setModelPosition(posX: Float, posY: Float , posZ: Float) {
//        modelNode?.apply {
//            this.position = Position(posX,posY,posZ)
//        }
//    }
//
////    fun logCurrentRotation() {
////        modelNode?.rotation?.let { rotation ->
////            val x = rotation.x
////            val y = rotation.y
////            val z = rotation.z
////            Log.d("ARViewer", "Current Rotation - X: $x, Y: $y, Z: $z")
////        } ?: run {
////            Log.d("ARViewer", "ModelNode is null or not initialized.")
////        }
////    }
//
//    fun getModelRotationY(): Float {
//        return modelNode?.rotation?.y ?: 0.0f
//    }
//
//    fun setModelTransform(quaternion: Quaternion) {
//        modelNode?.apply {
//            // 假設 Rotation 類可以直接接受四元數
//            this.quaternion = quaternion
//        }
//    }
//
//}

object ARViewer {
    var modelNode: ModelNode? = null
    private var sceneView: SceneView? = null
    private var lifecycleOwner: LifecycleOwner? = null

    private var currentModelName: String = "models/direction_arrow.glb"  // 儲存目前模型名稱

    fun setupSceneView(
        context: Context,
        sceneView: SceneView,
        lifecycleOwner: LifecycleOwner
    ) {
        this.sceneView = sceneView
        this.lifecycleOwner = lifecycleOwner

        sceneView.setZOrderOnTop(true)
        sceneView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        sceneView.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        sceneView.uiHelper.setOpaque(false)
        sceneView.view.blendMode = com.google.android.filament.View.BlendMode.TRANSLUCENT
        sceneView.scene.skybox = null
        sceneView.renderer.clearOptions = sceneView.renderer.clearOptions.apply { clear = true }

        // 預設載入模型
        loadModel("models/direction_arrow.glb")
    }

    fun loadModel(modelFile: String) {
        val view = sceneView
        val owner = lifecycleOwner

        if (view != null && owner != null) {
            currentModelName = modelFile // 更新目前模型名稱

            owner.lifecycleScope.launch {
                val modelInstance = view.modelLoader.createModelInstance(modelFile)
                modelNode?.let { view.removeChildNode(it) } // 移除舊模型
                modelNode = ModelNode(modelInstance, scaleToUnits = 2.0f).apply {
                    scale = Scale(0.18f)
                    position = Position(0f, -0.2f, 0f)
                }
                view.addChildNode(modelNode!!)
            }
        }
    }

    fun setModelTransform(rotX: Float, rotY: Float, rotZ: Float) {
        modelNode?.rotation = Rotation(rotX, rotY, rotZ)
    }

    fun setModelPosition(posX: Float, posY: Float, posZ: Float) {
        modelNode?.position = Position(posX, posY, posZ)
    }

    fun getModelRotationY(): Float {
        return modelNode?.rotation?.y ?: 0.0f
    }

    fun setModelTransform(quaternion: Quaternion) {
        modelNode?.quaternion = quaternion
    }

    fun getCurrentModelName(): String {
        return currentModelName
    }
}







