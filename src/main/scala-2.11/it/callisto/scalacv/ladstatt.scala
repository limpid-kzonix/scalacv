package it.callisto.scalacv

import org.opencv.core.Mat
import org.opencv.videoio.VideoCapture
import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Service
import javafx.concurrent.Task
import javafx.event.Event
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import scala.concurrent._
import ExecutionContext.Implicits.global
import javafx.application.Platform

class WebcamService extends Service[Future[Mat]] with OpenCVVideo with JfxUtils {

  val videoCapture: VideoCapture = new VideoCapture(0)

  def createTask = mkTask(sourceMat)

}

object Ladstatt {

  def main(args: Array[String]): Unit = {
    Application.launch(classOf[Ladstatt], args: _*)
  }

}

trait JfxUtils {

  def mkEventHandler[E <: Event](f: E => Unit) = new EventHandler[E] {
    def handle(e: E) = f(e)
  }

  def mkTask[X](callFn: => X): Task[X] = new Task[X] {
    override def call(): X = callFn
  }

}

class Ladstatt extends javafx.application.Application with OpenCVImg with JfxUtils {

  def execOnUIThread(f: => Unit) {
    Platform.runLater(new Runnable {
      override def run() = f
    })
  }

  override def init(): Unit = loadNativeLibs // important to have this statement on the "right" thread

  val imageProperty = new SimpleObjectProperty[Image]()

  def setImage(image: Image) = execOnUIThread(imageProperty.set(image))

  def getImage(): Image = imageProperty.get

  val MaxWidth = 1024.0
  val MaxHeight = 720.0

  override def start(stage: Stage): Unit = {
    val imageService = new WebcamService
    stage.setTitle("Webcam snapshot with face detection")
    val bp = new BorderPane
    val imageView = new ImageView()
    imageView.imageProperty().bind(imageProperty)
    val imageBp = new BorderPane

    imageBp.setCenter(imageView)
    bp.setCenter(imageBp)
    val scene = new Scene(bp, MaxWidth + 100, MaxHeight + 300)
    stage.setScene(scene)
    imageService.setOnSucceeded(
      mkEventHandler(
        event => {
          for {
            fromCamera <- event.getSource.getValue.asInstanceOf[Future[Mat]]
            image <- mat2Image(fromCamera)
          } {
            setImage(image)
            Platform.runLater(
              new Runnable() {
                def run = {
                  imageService.restart
                }
              })
          }
        }))

    imageService.start
    stage.show()

  }

}