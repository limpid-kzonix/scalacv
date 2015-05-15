package it.callisto.scalacv

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

object FaceDetect extends App with OpenCVImg with OpenCVDetect {
  
  println("\nRunning DetectFaceDemo")
  
  loadNativeLibs()
  
  // instantiate all independent futures before the for comprehension
  val f = Future { getClassifier("/lbpcascade_frontalface.xml") }
  val fromFile = readImg("/Lena.png")
  val p = for {
    faceDetector ← f
    mat_image ← fromFile
    gray ← toGray(mat_image)
    equalized ← equalize(gray)
    faces ← findFaces(equalized, faceDetector)
    _ ← frameFaces(mat_image, faces)
    _ ← writeImg(mat_image, "faceDetection.png")
  } yield ()
  
  Await.ready(p, 5 seconds)
    
}