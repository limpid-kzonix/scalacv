package it.callisto.scalacv

import org.opencv.core.Mat
import org.opencv.objdetect.CascadeClassifier
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
import java.util.concurrent.Executor

class WebcamService extends Service[Future[Mat]] with OpenCVVideo with JfxUtils {

  val videoCapture: VideoCapture = new VideoCapture(0)

  def createTask = mkTask(sourceMat)

}

object CamFaceDetect {

  def main(args: Array[String]): Unit = {
    Application.launch(classOf[CamFaceDetect], args: _*)
  }

}

trait JfxUtils {

  def mkEventHandler[E <: Event](f: E ⇒ Unit) = new EventHandler[E] {
    def handle(e: E) = f(e)
  }

  def mkTask[X](callFn: ⇒ X): Task[X] = new Task[X] {
    override def call(): X = callFn
  }

}

object JfxExecutionContext {
  implicit val jfxExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(new Executor {
    def execute(command: Runnable): Unit = Platform.runLater(command)
  })
}

class CamFaceDetect extends javafx.application.Application with OpenCVImg with OpenCVDetect with JfxUtils {

  val fxec = JfxExecutionContext.jfxExecutionContext

  override def init(): Unit = loadNativeLibs

  lazy val faceDetector = getClassifier("/lbpcascade_frontalface.xml")
  lazy val lEyeDetector = getClassifier("/haarcascade_lefteye_2splits.xml")
  lazy val rEyeDetector = getClassifier("/haarcascade_righteye_2splits.xml")

  val imageProperty = new SimpleObjectProperty[Image]()

  val MaxWidth = 640.0
  val MaxHeight = 480.0

  override def start(stage: Stage): Unit = {
    val camService = new WebcamService

    stage.setTitle("Webcam face detection")
    val bp = new BorderPane
    val imageView = new ImageView()
    imageView.imageProperty().bind(imageProperty)
    val imageBp = new BorderPane
    imageBp.setCenter(imageView)
    bp.setCenter(imageBp)
    val scene = new Scene(bp, MaxWidth + 100, MaxHeight + 100)
    stage.setScene(scene)

    camService.setOnSucceeded(
      mkEventHandler(
        event ⇒ {
          for {
            mat_image ← event.getSource.getValue.asInstanceOf[Future[Mat]]
            gray ← toGray(mat_image)
            equalized ← equalize(gray)
            faces ← findFaces(equalized, faceDetector)
            eyes ← findEyes(equalized, faces, lEyeDetector, rEyeDetector)
            _ ← frameFaces(mat_image, faces)
            _ ← frameEyes(mat_image, faces, eyes)
            image ← mat2Image(mat_image)
          } yield {
            Future {
              imageProperty.set(image)
              camService.restart
            }(fxec)
          }
        }))

    camService.start
    stage.show()

  }

}