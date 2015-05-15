package it.callisto.scalacv

import scala.concurrent._
import ExecutionContext.Implicits.global

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.objdetect.CascadeClassifier
import org.opencv.videoio.VideoCapture

import java.io.ByteArrayInputStream

import javafx.scene.image.Image

trait OpenCVUtils {

  def loadNativeLibs(): Unit = {
    System.load("/home/mario/dev/tools/opencv-3.0.0-rc1/build/lib/libopencv_java300.so")
  }

  def resourcePath(path: String): String =
    getClass().getResource(path).getPath()

}

trait OpenCVImg extends OpenCVUtils {

  def readImg(path: String): Future[Mat] = Future {
    println("reading image")
    val im = Imgcodecs.imread(resourcePath(path))
    println("done image")
    im
  }

  private def toDst(src: Mat, f: (Mat, Mat) => Unit): Mat = {
    val dst = new Mat()
    f(src, dst)
    dst
  }

  // convert image to greyscale
  def toGray(mat: Mat): Future[Mat] = Future {
    val dstCn = 1 // number of channels in the destination image
    toDst(mat, (s, d) ⇒ Imgproc.cvtColor(s, d, Imgproc.COLOR_BGR2GRAY, dstCn))
  }

  // equalize histogram
  def equalize(mat: Mat): Future[Mat] = Future {
    toDst(mat, (s, d) ⇒ Imgproc.equalizeHist(s, d))
  }

  def writeImg(mat: Mat, filename: String): Future[Unit] = Future {
    println("Writing %s".format(filename))
    Imgcodecs.imwrite(filename, mat)
  }

  def mat2Image(mat: Mat): Future[Image] = Future {
    val memory = new MatOfByte
    Imgcodecs.imencode(".png", mat, memory)
    new Image(new ByteArrayInputStream(memory.toArray()))
  }

  def gaussianBlur(mat: Mat): Future[Mat] = Future {
    val border_default = 4
    toDst(mat, (s, d) ⇒ Imgproc.GaussianBlur(s, d, new Size(3, 3), 0, 0, border_default))
  }

  def blur(mat: Mat): Future[Mat] = Future {
    toDst(mat, (s, d) ⇒ Imgproc.blur(s, d, new Size(3, 3)))
  }

  def sobel(mat: Mat, x_order: Int, y_order: Int): Future[Mat] = Future {
    val border_default = 4
    toDst(mat, (s, d) ⇒ Imgproc.Sobel(s, d, CvType.CV_16S, x_order, y_order, 3, 1, 0, border_default))
  }

  def laplace(mat: Mat): Future[Mat] = Future {
    val border_default = 4
    toDst(mat, (s, d) ⇒ Imgproc.Laplacian(s, d, CvType.CV_16S, 3, 1, 0, border_default))
  }

  def convertScaleAbs(mat: Mat): Future[Mat] = Future {
    toDst(mat, (s, d) ⇒ Core.convertScaleAbs(s, d))
  }

  def addWeighted(mat1: Mat, mat2: Mat): Future[Mat] = Future {
    val weighted = new Mat()
    Core.addWeighted(mat1, 0.5, mat2, 0.5, 0, weighted)
    weighted
  }

}

trait OpenCVCombos extends OpenCVImg {

  def reduceNoise(mat: Mat): Future[Mat] =
    for {
      blurred ← gaussianBlur(mat)
      gray ← toGray(blurred)
    } yield (gray)

  def approxGradient(mat: Mat): Future[Mat] = {
    val gradient_x = for {
      sobel ← sobel(mat, 1, 0)
      scaled ← convertScaleAbs(sobel)
    } yield (scaled)

    val gradient_y = for {
      sobel ← sobel(mat, 0, 1)
      scaled ← convertScaleAbs(sobel)
    } yield (scaled)

    for {
      x ← gradient_x
      y ← gradient_y
      weighted ← addWeighted(x, y)
    } yield (weighted)
  }

}

trait OpenCVDetect extends OpenCVUtils {

  type CC = CascadeClassifier

  def getClassifier(path: String): CC = {
    println("reading classifier")
    val cc = new CascadeClassifier(resourcePath(path))
    println("done classifier")
    cc
  }

  def findFaces(image: Mat, faceDetector: CC): Future[Vector[Rect]] = Future {
    val faceDetections = new MatOfRect()
    faceDetector.detectMultiScale(image, faceDetections)
    val faces = faceDetections.toArray().toVector
    println("Detected %s faces".format(faces.size))
    faces
  }

  def findEyes(image: Mat, faces: Vector[Rect], lEye: CC, rEye: CC): Future[Vector[(Vector[Rect], Vector[Rect])]] = Future {
    for (rect ← faces) yield {

      val hWidth = rect.width / 2
      val hHeight = rect.height / 2

      // the left eye should be in the top-left quarter of the face area
      val leftFaceMat = new Mat(image, new Rect(rect.x, rect.y, hWidth, hHeight))
      val leftEyeDetections = new MatOfRect()
      lEye.detectMultiScale(leftFaceMat, leftEyeDetections)

      // the right eye should be in the top-right quarter of the face area
      val rightFaceMat = new Mat(image, new Rect(rect.x + hWidth, rect.y, hWidth, hHeight))
      val rightEyeDetections = new MatOfRect()
      rEye.detectMultiScale(rightFaceMat, rightEyeDetections)

      (leftEyeDetections.toArray().toVector, rightEyeDetections.toArray().toVector)
    }
  }

  def frameFaces(image: Mat, faces: Vector[Rect]): Future[Unit] = Future {
    val lineColor = new Scalar(0, 255, 0)
    val fontFace = Core.FONT_HERSHEY_PLAIN
    val fontScale = 2
    for (i ← 0 until faces.size) {
      val rect = faces(i)
      val topLeft = new Point(rect.x, rect.y)
      val bottomRight = new Point(rect.x + rect.width, rect.y + rect.height)
      Imgproc.rectangle(image, topLeft, bottomRight, lineColor);
      val textTopLeft = new Point(rect.x, rect.y - 20)
      Imgproc.putText(image, s"Face $i", textTopLeft, fontFace, fontScale, lineColor)
    }
  }

  def frameEyes(image: Mat, faces: Vector[Rect], eyes: Vector[(Vector[Rect], Vector[Rect])]): Future[Unit] = Future {
    def coords(face: Rect, eye: Rect, isLeft: Boolean = true): (Point, Point) = {
      val hWidth = if (isLeft) 0 else face.width / 2
      val topLeft = new Point(face.x + hWidth + eye.x, face.y + eye.y)
      val bottomRight = new Point(face.x + hWidth + eye.x + eye.width, face.y + eye.y + eye.height)
      (topLeft, bottomRight)
    }

    val lineColor = new Scalar(0, 0, 255)
    for (i ← 0 until faces.size) {
      val rect = faces(i)
      val lpoints = eyes(i)._1.headOption match {
        case Some(l) ⇒ Vector(coords(rect, l))
        case None    ⇒ Vector()
      }
      val rpoints = eyes(i)._2.headOption match {
        case Some(r) ⇒ Vector(coords(rect, r, false))
        case None    ⇒ Vector()
      }
      for (rects ← lpoints ++ rpoints) {
        Imgproc.rectangle(image, rects._1, rects._2, lineColor)
      }
    }
  }

}

trait OpenCVVideo {

  def videoCapture: VideoCapture

  def takeImage: Mat = {
    val image = new Mat()
    while (videoCapture.read(image) == false) {}
    image
  }

  def sourceMat: Future[Mat] = Future {
    assert(videoCapture.isOpened())
    if (videoCapture.grab)
      takeImage
    else
      throw new RuntimeException("Couldn't grab image!")
  }

}
