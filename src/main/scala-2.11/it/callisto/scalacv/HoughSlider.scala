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

object HoughSlider {

  def main(args: Array[String]): Unit = {
    Application.launch(classOf[HoughSlider], args: _*)
  }

}

class HoughSlider extends javafx.application.Application with OpenCVImg with JfxUtils {

  val fxec = JfxExecutionContext.jfxExecutionContext

  override def init(): Unit = loadNativeLibs

  val imageProperty = new SimpleObjectProperty[Image]()

  val MaxWidth = 800.0
  val MaxHeight = 600.0

  override def start(stage: Stage): Unit = {
    val im = Imgcodecs.imread(resourcePath("/Lena.png"))

    def redraw(lower: Int, minLineLength: Int): Unit =
      for {
        clone ← Future { im.clone }
        gray ← toGray(clone)
        blurred ← blur(gray)
        canned ← canny(blurred, 50, 4.0)
        lines ← houghLinesP(canned, lower, minLineLength)
        lined ← addVec4iLines(clone, lines)
        image ← mat2Image(lined)
      } yield {
        Future {
          imageProperty.set(image)
        }(fxec)
      }

    stage.setTitle("Probabilistic Hough Line Transform")
    val threshold = new Label()
    threshold.fontProperty().setValue(Font.font("Verdana", 14))
    threshold.setMinWidth(75)
    val thresholdSlider = mkSlider(20, 180, 20, Orientation.HORIZONTAL, 50)
    val thresholdTxt = new Label()
    thresholdTxt.fontProperty().setValue(Font.font("Verdana", 14))
    thresholdTxt.textProperty.set("Intersections threshold" + " ")

    val lineLength = new Label()
    lineLength.fontProperty().setValue(Font.font("Verdana", 14))
    lineLength.setMinWidth(75)
    val lineLengthSlider = mkSlider(20, 180, 20, Orientation.HORIZONTAL, 50)
    val lineLengthTxt = new Label()
    lineLengthTxt.fontProperty().setValue(Font.font("Verdana", 14))
    lineLengthTxt.textProperty.set("Min line length" + " ")
    
    val bp = new BorderPane
    val imageView = new ImageView()
    imageView.imageProperty().bind(imageProperty)
    val imageBp = new BorderPane
    val topBox = mkTop
    topBox.getChildren.addAll(thresholdTxt, thresholdSlider, threshold, lineLengthTxt, lineLengthSlider, lineLength)
    imageBp.setCenter(imageView)
    bp.setTop(topBox)
    bp.setCenter(imageBp)
    val scene = new Scene(bp, MaxWidth + 100, MaxHeight + 100)
    stage.setScene(scene)

    thresholdSlider.valueProperty().addListener(
      new ChangeListener[Number]() {
        override def changed(ov: ObservableValue[_ <: Number], old_val: Number, new_val: Number) {
          threshold.textProperty.set(" " + new_val.intValue().toString)
          if (new_val != old_val)
            redraw(new_val.intValue(), lineLengthSlider.getValue.toInt)
        }
      })

    lineLengthSlider.valueProperty().addListener(
      new ChangeListener[Number]() {
        override def changed(ov: ObservableValue[_ <: Number], old_val: Number, new_val: Number) {
          lineLength.textProperty.set(" " + new_val.intValue().toString)
          if (new_val != old_val)
            redraw(thresholdSlider.getValue.toInt, new_val.intValue())
        }
      })

    thresholdSlider.adjustValue(50)
    lineLength.textProperty.set(" " + "50")
    stage.show()

  }

}