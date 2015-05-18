package it.callisto.scalacv

import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.control.CheckBox
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import scala.concurrent._
import scala.collection.JavaConversions._
import ExecutionContext.Implicits.global

object ThresholdDemo {

  def main(args: Array[String]): Unit = {
    Application.launch(classOf[ThresholdDemo], args: _*)
  }

}

class ThresholdDemo extends javafx.application.Application with OpenCVImg with JfxUtils {

  val fxec = JfxExecutionContext.jfxExecutionContext

  override def init(): Unit = loadNativeLibs

  val imageProperty = new SimpleObjectProperty[Image]()

  val MaxWidth = 800.0
  val MaxHeight = 600.0
  
  override def start(stage: Stage): Unit = {
    val im = Imgcodecs.imread(resourcePath("/glicines.png"))

    def redraw(thr: Int, mode: Int): Unit =
      for {
        clone ← Future { im.clone }
        gray ← toGray(clone)
        thresh ← threshold(gray, thr, mode)
        image ← mat2Image(thresh)
      } yield {
        Future {
          imageProperty.set(image)
        }(fxec)
      }

    stage.setTitle("Threshold")

    val thresTxt = sliderText("Threshold" + " ")
    val thresSlider = mkSlider(0, 255, 128, Orientation.HORIZONTAL, 128)
    val thres = sliderValue()
        
    val colorComboBox = {
      class ImgprocColorCell extends ListCell[ImgprocThresh] {

        override def updateItem(item: ImgprocThresh, empty: Boolean): Unit = {
          super.updateItem(item, empty)
          if (item != null) {
            setText(item.id)
          }
        }

      }
      val cb = new ComboBox[ImgprocThresh]
      val sortedItems = threshConstants.values.toSeq.sortWith((a, b) ⇒ a.id < b.id)
      cb.getItems.addAll(sortedItems)
      cb.setValue(sortedItems.head)
      cb.setCellFactory(mkCellFactoryCallback(lv ⇒ new ImgprocColorCell))
//      cb.visibleProperty.bind(colorToggle.selectedProperty)
      cb
    }
    
    val bp = new BorderPane
    val imageView = new ImageView()
    imageView.imageProperty().bind(imageProperty)
    val imageBp = new BorderPane
    val topBox = mkTop
    topBox.getChildren.addAll(thresTxt, thresSlider, thres, colorComboBox)
    imageBp.setCenter(imageView)
    bp.setTop(topBox)
    bp.setCenter(imageBp)
    val scene = new Scene(bp, MaxWidth + 100, MaxHeight + 100)
    stage.setScene(scene)

    thresSlider.valueProperty().addListener(
      new ChangeListener[Number]() {
        override def changed(ov: ObservableValue[_ <: Number], old_val: Number, new_val: Number) {
          thres.textProperty.set(" " + new_val.intValue().toString)
          if (new_val != old_val)
            redraw(new_val.intValue(), colorComboBox.getValue.value)
        }
      })
      
    colorComboBox.setOnAction(mkEventHandler(event ⇒ {
      redraw(thresSlider.getValue.toInt, colorComboBox.getValue.value)
    }))

    thresSlider.adjustValue(50)
    stage.show()

  }

}