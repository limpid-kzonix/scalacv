package it.callisto.scalacv

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

object FaceDetect extends App with OpenCVUtils with OpenCVImg {
  
  println("\nRunning DetectFaceDemo")
  
  loadNativeLibs()
  
  // instantiate all independent futures before the for comprehension
  val f = getClassifier("/lbpcascade_frontalface.xml")
  val i = readImg("/Lena.png")
  val p = for {
    faceDetector ← f
    image ← i
    gray ← toGray(image)
    equalized ← equalize(gray)
    faces ← findFaces(equalized, faceDetector)
    _ ← frameFaces(image, faces)
    _ ← writeImg(image, "faceDetection.png")
  } yield ()
  
  Await.ready(p, 5 seconds)
    
}