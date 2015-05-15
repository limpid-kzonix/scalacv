package it.callisto.scalacv

import java.io.ByteArrayInputStream
import java.io.File
import scala.collection.JavaConversions.seqAsJavaList
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Range
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.videoio.VideoCapture
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Service
import javafx.concurrent.Task
import javafx.event.Event
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.text.Font
import javafx.stage.Stage
import javafx.util.Callback
import scala.concurrent._
import ExecutionContext.Implicits.global
import Lad._
import javafx.application.Platform

/**
 * For a discussion of the concepts of this application see http://ladstatt.blogspot.com/
 */
trait Utils {


  /**
   * function to measure execution time of first function, optionally executing a display function,
   * returning the time in milliseconds
   */
  def time[A](a: => A, display: Long => Unit = s => ()): A = {
    val now = System.nanoTime
    val result = a
    val micros = (System.nanoTime - now) / 1000
    display(micros)
    result
  }

}

object Lad {

  trait LadUtils extends Utils {

    def loadNativeLibs() = {
//      val nativeLibName = if (runOnMac) "/Users/lad/Documents/net.ladstatt/opencv/src/main/lib/mac/libopencv_java246.dylib" else "c:/openCV/build/java/x64/opencv_java244.dll"
//      System.load(new File(nativeLibName).getAbsolutePath())
      System.load("/home/mario/dev/tools/opencv-3.0.0-rc1/build/lib/libopencv_java300.so")
    }

    def mat2Image(mat: Mat): Future[Image] = {
      Future {
        val memory = new MatOfByte
        Imgcodecs.imencode(".png", mat, memory)
        new Image(new ByteArrayInputStream(memory.toArray()))
      }
    }

  }

  trait ImageSource {

    def videoCapture: VideoCapture

    def takeImage: Mat = {
      val image = new Mat()
      while (videoCapture.read(image) == false) {}
      image
    }

    def sourceMat: Future[Mat] =
      Future {
        assert(videoCapture.isOpened())
        if (videoCapture.grab) {
          takeImage
        } else
          throw new RuntimeException("Couldn't grab image!")
      }

  }

}

class WebcamService extends Service[Future[Mat]] with LadUtils with JfxUtils with ImageSource {

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

class Ladstatt extends javafx.application.Application with LadUtils with Utils with JfxUtils {

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
    val label = new Label()
    val imageBp = new BorderPane

    label.fontProperty().setValue(Font.font("Verdana", 80))

    imageBp.setCenter(imageView)
    bp.setCenter(imageBp)
    bp.setBottom(label)
    val scene = new Scene(bp, MaxWidth + 100, MaxHeight + 300)
    stage.setScene(scene)
    imageService.setOnSucceeded(
      mkEventHandler(
        event => {
          time(
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
            }, time =>
              label.textProperty.set("%d ms".format(time)))
        }))

    imageService.start
    stage.show()

  }

}