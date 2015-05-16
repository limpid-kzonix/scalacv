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

object CannySlider {

  def main(args: Array[String]): Unit = {
    Application.launch(classOf[CannySlider], args: _*)
  }

}

class CannySlider extends javafx.application.Application with OpenCVImg with JfxUtils {

  val fxec = JfxExecutionContext.jfxExecutionContext

  override def init(): Unit = loadNativeLibs

  val imageProperty = new SimpleObjectProperty[Image]()

  val MaxWidth = 800.0
  val MaxHeight = 600.0

  override def start(stage: Stage): Unit = {
    val im = Imgcodecs.imread(resourcePath("/Lena.png"))

    def redraw(lower: Int, ratio: Double, l2gradient: Boolean): Unit =
      for {
        gray ← toGray(im)
        blurred ← blur(gray)
        canned ← canny(blurred, lower, ratio, l2gradient)
        image ← mat2Image(canned)
      } yield {
        Future {
          imageProperty.set(image)
        }(fxec)
      }

    stage.setTitle("Canny edges")
    val threshold = new Label()
    threshold.fontProperty().setValue(Font.font("Verdana", 14))
    threshold.setMinWidth(75)
    val thresholdSlider = mkSlider(1, 100, 10, Orientation.HORIZONTAL)
    val thresholdTxt = new Label()
    thresholdTxt.fontProperty().setValue(Font.font("Verdana", 14))
    thresholdTxt.textProperty.set("Lower threshold" + " ")

    val ratio = new Label()
    ratio.fontProperty().setValue(Font.font("Verdana", 14))
    ratio.setMinWidth(75)
    val ratioSlider = mkSlider(2, 3, 3, Orientation.HORIZONTAL)
    val ratioTxt = new Label()
    ratioTxt.fontProperty().setValue(Font.font("Verdana", 14))
    ratioTxt.textProperty.set("Higher:lower ratio" + " ")
    
    val l2gradient = new CheckBox()
    l2gradient.setSelected(false)
    val l2gradientTxt = new Label()
    l2gradientTxt.fontProperty().setValue(Font.font("Verdana", 14))
    l2gradientTxt.textProperty.set(" " + "L2 gradient")
    
    val bp = new BorderPane
    val imageView = new ImageView()
    imageView.imageProperty().bind(imageProperty)
    val imageBp = new BorderPane
    val topBox = mkTop
    topBox.getChildren.addAll(thresholdTxt, thresholdSlider, threshold, ratioTxt, ratioSlider, ratio, l2gradient, l2gradientTxt)
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
            redraw(new_val.intValue(), ratioSlider.getValue, l2gradient.isSelected())
        }
      })

    ratioSlider.valueProperty().addListener(
      new ChangeListener[Number]() {
        override def changed(ov: ObservableValue[_ <: Number], old_val: Number, new_val: Number) {
          ratio.textProperty.set(" " + new_val.floatValue().toString.take(5) + ":1")
          if (new_val != old_val)
            redraw(thresholdSlider.getValue.toInt, new_val.doubleValue(), l2gradient.isSelected())
        }
      })
    
    l2gradient.setOnAction(mkEventHandler(event ⇒ {
      redraw(thresholdSlider.getValue.toInt, ratioSlider.getValue, l2gradient.isSelected())
    }))

    thresholdSlider.adjustValue(50)
    ratio.textProperty.set(" " + "3.0:1")
    stage.show()

  }

}