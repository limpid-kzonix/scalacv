package it.callisto.scalacv

import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import scala.concurrent._
import ExecutionContext.Implicits.global

object CannySlider {

  def main(args: Array[String]): Unit = {
    Application.launch(classOf[CannySlider], args: _*)
  }

}

class CannySlider extends javafx.application.Application with OpenCVImg with OpenCVDetect with JfxUtils {

  val fxec = JfxExecutionContext.jfxExecutionContext

  override def init(): Unit = loadNativeLibs

  val imageProperty = new SimpleObjectProperty[Image]()

  val MaxWidth = 800.0
  val MaxHeight = 600.0

  override def start(stage: Stage): Unit = {
    val camService = new WebcamService

    val im = Imgcodecs.imread(resourcePath("/Lena.png"))

    stage.setTitle("Webcam face detection")
    val bp = new BorderPane
    val imageView = new ImageView()
    imageView.imageProperty().bind(imageProperty)
    val imageBp = new BorderPane
    val effectSlider = mkSlider(1, 100, 10, Orientation.HORIZONTAL)
    val topBox = mkTop
    topBox.getChildren.addAll(effectSlider)
    imageBp.setCenter(imageView)
    bp.setTop(topBox)
    bp.setCenter(imageBp)
    val scene = new Scene(bp, MaxWidth + 100, MaxHeight + 100)
    stage.setScene(scene)

    effectSlider.valueProperty().addListener(
      new ChangeListener[Number]() {
        override def changed(ov: ObservableValue[_ <: Number], old_val: Number, new_val: Number) {
          if (new_val != old_val) {
            for {
              gray ← toGray(im)
              blurred ← blur(gray)
              canned ← canny(blurred, new_val.intValue())
              image ← mat2Image(canned)
            } yield {
              Future {
                imageProperty.set(image)
              }(fxec)
            }
          }
        }
      })

    stage.show()
    effectSlider.adjustValue(50)

  }

}