package it.callisto.scalacv

import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task
import javafx.geometry.Orientation
import javafx.event.Event
import javafx.event.EventHandler
import javafx.scene.control.Slider
import javafx.scene.layout.HBox
import scala.concurrent._
import javafx.application.Platform
import java.util.concurrent.Executor

object JfxExecutionContext {
  
  implicit val jfxExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(new Executor {
    def execute(command: Runnable): Unit = Platform.runLater(command)
  })
  
}

trait JfxUtils {

  def mkEventHandler[E <: Event](f: E ⇒ Unit) = new EventHandler[E] {
    def handle(e: E) = f(e)
  }

  def mkTask[X](callFn: ⇒ X): Task[X] = new Task[X] {
    override def call(): X = callFn
  }

  def mkTop: HBox = {
    val hbox = new HBox()
    hbox.setStyle("-fx-padding: 15;" +
      "-fx-background-color: #333333," +
      "linear-gradient(#f3f3f3 0%, #ced3da 100%);" +
      "-fx-background-insets: 0, 0 0 1 0;")
    hbox
  }

  def mkSlider(min: Double, max: Double, initialValue: Double, orientation: Orientation, mtu: Int = 100): Slider = {
    require(min <= initialValue)
    require(initialValue <= max)
    val slider = new Slider()
    slider.setMin(min)
    slider.setMax(max)
    slider.setValue(initialValue)
    slider.setShowTickLabels(true)
    slider.setShowTickMarks(true)
    slider.setMajorTickUnit(mtu)
    slider.setMinorTickCount(mtu / 5)
    slider.setBlockIncrement(1)
    slider.setOrientation(orientation)
    slider
  }

}