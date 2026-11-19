package com.bioform.lady_salazar_lab15

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // Las 4 imágenes de prueba copiadas en app/src/main/assets
    private val testImages = listOf("test1.jpg", "test2.jpg", "test3.jpg", "test4.jpg")
    private var current = 0
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val img: ImageView = findViewById(R.id.imageToLabel)
        val txtOutput: TextView = findViewById(R.id.txtOutput)
        val btn: Button = findViewById(R.id.btnTest)

        // Etiquetas en el MISMO orden que imprimió Colab (labels.txt)
        val labels = assets.open("labels.txt").bufferedReader().readLines()
            .map { it.trim() }.filter { it.isNotEmpty() }

        // 1) Cargar el modelo personalizado desde assets
        val localModel = LocalModel.Builder()
            .setAssetFilePath("brain_tumor_classifier.tflite")
            .build()

        // 2) Opciones del etiquetador personalizado
        val options = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.0f)
            .setMaxResultCount(3)
            .build()
        val labeler = ImageLabeling.getClient(options)

        fun showImage() {
            bitmap = assetsToBitmap(testImages[current])
            bitmap?.let { img.setImageBitmap(it) }
            txtOutput.text = "${testImages[current]} (toca la imagen para cambiarla)"
        }
        showImage()

        // Tocar la imagen pasa a la siguiente de las 4 imágenes de prueba
        img.setOnClickListener {
            current = (current + 1) % testImages.size
            showImage()
        }

        btn.setOnClickListener {
            val bmp = bitmap ?: return@setOnClickListener
            val image = InputImage.fromBitmap(bmp, 0)
            labeler.process(image)
                .addOnSuccessListener { results ->
                    // El modelo no trae metadatos: ML Kit devuelve el índice y aquí
                    // se traduce al nombre de la clase.
                    val sb = StringBuilder("${testImages[current]}\n")
                    for (label in results) {
                        val name = labels.getOrElse(label.index) { "clase ${label.index}" }
                        sb.append("$name : ${label.confidence}\n")
                    }
                    txtOutput.text = sb.toString()
                }
                .addOnFailureListener { e ->
                    txtOutput.text = "Error: ${e.message}"
                }
        }
    }

    private fun Context.assetsToBitmap(fileName: String): Bitmap? =
        try {
            assets.open(fileName).use { BitmapFactory.decodeStream(it) }
        } catch (e: IOException) {
            null
        }
}