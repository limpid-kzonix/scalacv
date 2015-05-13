package it.callisto.scalacv

import scala.concurrent._
import ExecutionContext.Implicits.global

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.objdetect.CascadeClassifier

trait OpenCVUtils {

  def loadNativeLibs(): Unit = {
    System.load("/home/mario/dev/tools/opencv-3.0.0-rc1/build/lib/libopencv_java300.so")
  }
  
  def resourcePath(path: String): String = 
    getClass().getResource(path).getPath()

}

trait OpenCVImg with OpenCVUtils {

  def readImg(path: String): Future[Mat] = Future {
    println("reading image")
    val im = Imgcodecs.imread(resourcePath(path))
    println("done image")
    im
  }

  // convert image to greyscale
  def toGray(image: Mat): Future[Mat] = Future {
    val greyMat = new Mat()
    val dstCn = 1 // number of channels in the destination image
    Imgproc.cvtColor(image, greyMat, Imgproc.COLOR_BGR2GRAY, dstCn)
    greyMat
  }

  // equalize histogram
  def equalize(image: Mat): Future[Mat] = Future {
    val equalizedMat = new Mat()
    Imgproc.equalizeHist(image, equalizedMat)
    equalizedMat
  }
  
  def writeImg(image: Mat, filename: String): Future[Unit] = Future {
    println("Writing %s".format(filename))
    Imgcodecs.imwrite(filename, image)
  }
  
}

trait OpenCVDetect with OpenCVUtils {

   def getClassifier(path: String): Future[CascadeClassifier] = Future {
    println("reading classifier")
    val cc = new CascadeClassifier(resourcePath(path))
    println("done classifier")
    cc
  }
 
  def findFaces(image: Mat, faceDetector: CascadeClassifier): Future[Vector[Rect]] = Future {
    val faceDetections = new MatOfRect()
    faceDetector.detectMultiScale(image, faceDetections)
    val faces = faceDetections.toArray().toVector
    println("Detected %s faces".format(faces.size))
    faces
  }
  
  def frameFaces(image: Mat, faces: Vector[Rect]): Future[Unit] = Future {
    for (i ← 0 until faces.size) {
      val rect = faces(i)
      val lineColor = new Scalar(0, 255, 0)
      val topLeft = new Point(rect.x, rect.y)
      val bottomRight = new Point(rect.x + rect.width, rect.y + rect.height)
      Imgproc.rectangle(image, topLeft, bottomRight, lineColor);
      val textTopLeft = new Point(rect.x, rect.y - 20)
      val fontFace = Core.FONT_HERSHEY_PLAIN
      val fontScale = 2
      Imgproc.putText(image, s"Face $i", textTopLeft, fontFace, fontScale, lineColor)
    }  
  }

}
