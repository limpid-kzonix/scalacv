package it.callisto.scalacv

import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.CheckBox
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.text.Font
import javafx.stage.Stage
import scala.concurrent._
import ExecutionContext.Implicits.global

object BrightnessContrastDemo {

  def main(args: Array[String]): Unit = {
    Application.launch(classOf[BrightnessContrastDemo], args: _*)
  }

}

class BrightnessContrastDemo extends javafx.application.Application with OpenCVImg with JfxUtils {

  val fxec = JfxExecutionContext.jfxExecutionContext

  override def init(): Unit = loadNativeLibs

  val imageProperty = new SimpleObjectProperty[Image]()

  val MaxWidth = 800.0
  val MaxHeight = 600.0

  override def start(stage: Stage): Unit = {
    val im = Imgcodecs.imread(resourcePath("/Lena.png"))

    def redraw(α: Double, β: Double): Unit =
      for {
        clone ← Future { im.clone }
        bc ← brightnessContrast(clone, α, β)
        image ← mat2Image(bc)
      } yield {
        Future {
          imageProperty.set(image)
        }(fxec)
      }

    stage.setTitle("Brightness and contrast")
    val brightness = new Label()
    brightness.fontProperty().setValue(Font.font("Verdana", 14))
    brightness.setMinWidth(75)
    val brightnessSlider = mkSlider(0, 100, 20, Orientation.HORIZONTAL, 50)
    val brightnessTxt = new Label()
    brightnessTxt.fontProperty().setValue(Font.font("Verdana", 14))
    brightnessTxt.textProperty.set("Brightness" + " ")

    val contrast = new Label()
    contrast.fontProperty().setValue(Font.font("Verdana", 14))
    contrast.setMinWidth(75)
    val contrastSlider = mkSlider(1, 3, 2, Orientation.HORIZONTAL, 2)
    val contrastTxt = new Label()
    contrastTxt.fontProperty().setValue(Font.font("Verdana", 14))
    contrastTxt.textProperty.set("Contrast" + " ")
    
    val bp = new BorderPane
    val imageView = new ImageView()
    imageView.imageProperty().bind(imageProperty)
    val imageBp = new BorderPane
    val topBox = mkTop
    topBox.getChildren.addAll(brightnessTxt, brightnessSlider, brightness, contrastTxt, contrastSlider, contrast)
    imageBp.setCenter(imageView)
    bp.setTop(topBox)
    bp.setCenter(imageBp)
    val scene = new Scene(bp, MaxWidth + 100, MaxHeight + 100)
    stage.setScene(scene)

    brightnessSlider.valueProperty().addListener(
      new ChangeListener[Number]() {
        override def changed(ov: ObservableValue[_ <: Number], old_val: Number, new_val: Number) {
          brightness.textProperty.set(" " + new_val.intValue().toString)
          if (new_val != old_val)
            redraw(contrastSlider.getValue, new_val.doubleValue())
        }
      })

    contrastSlider.valueProperty().addListener(
      new ChangeListener[Number]() {
        override def changed(ov: ObservableValue[_ <: Number], old_val: Number, new_val: Number) {
          contrast.textProperty.set(" " + new_val.floatValue().toString.take(5))
          if (new_val != old_val)
            redraw(new_val.doubleValue(), brightnessSlider.getValue)
        }
      })

    brightnessSlider.adjustValue(50)
    contrast.textProperty.set(" " + "2.0")
    stage.show()

  }

}